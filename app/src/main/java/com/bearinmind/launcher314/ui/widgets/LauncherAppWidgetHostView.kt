package com.bearinmind.launcher314.ui.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Outline
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import com.bearinmind.launcher314.data.getWidgetCornerRadiusPercent
import com.bearinmind.launcher314.data.getWidgetRoundedCornersEnabled
import com.bearinmind.launcher314.data.WIDGET_MAX_CORNER_RADIUS_DP
import kotlin.math.abs

/**
 * Shared state: true whenever the user is actively touching a widget.
 * LauncherWithDrawer's full-screen vertical-drag detector reads this and
 * skips ownership when a widget is being touched — so fast vertical swipes
 * on widgets beat the drawer/notifications gesture even when the drawer's
 * touchSlop would otherwise claim them on the first MotionEvent.
 */
object WidgetTouchState {
    @Volatile
    var isWidgetTouchActive: Boolean = false
}

/**
 * Custom AppWidgetHostView that handles touch events for the launcher.
 * Based on Fossify Launcher's MyAppWidgetHostView implementation.
 *
 * Key features:
 * - Handler-based long-press detection with system timeout
 * - Movement threshold to distinguish drag from tap
 * - Coordinate tracking for long-press callback
 * - ignoreTouches flag for resize mode
 */
class LauncherAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    private val density = context.resources.displayMetrics.density
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val actionDownCoords = PointF()
    private val currentCoords = PointF()
    private var actionDownMS = 0L

    // Movement threshold (Fossify uses move_gesture_threshold / 4 ≈ 5dp)
    private val moveGestureThreshold = (density * 5).toInt()
    // Larger threshold for "this was clearly a swipe, not a tap" — used by
    // dispatchTouchEvent below to convert the final UP into a CANCEL so the
    // child RemoteViews stops its click tracker. Provider widgets that don't
    // respect the system touchSlop (e.g. Breezy Weather's hourly widget —
    // issue #39) otherwise fire a click at the end of any horizontal swipe
    // across the widget surface.
    private val swipeCancelThreshold = (density * 15).toInt()
    // Per-gesture state for the swipe-vs-tap guard. Reset on every DOWN.
    private var draggedBeyondTap = false
    // Only convert UP→CANCEL when the swipe was primarily horizontal — that
    // way vertical scrollable widgets (calendars, lists) keep their UP and
    // get to fire the fling animation. Provider widgets almost never have
    // internal horizontal scroll, so swallowing horizontal UPs is safe.
    private var swipeWasHorizontal = false

    /** Current corner radius in dp. Change this and call invalidateOutline() to update. */
    private var cornerRadiusDp: Float = 0f

    init {
        applyRoundedCorners(context)
        // Strip any inherited padding immediately so newly-created widgets are
        // already flush before Android's own paths get a chance to add the
        // default ~8dp.
        super.setPadding(0, 0, 0, 0)
        // Match Launcher3's BaseLauncherAppWidgetHostView: hand off RemoteViews
        // inflation to a background executor so a heavy widget update (e.g.
        // a calendar refilling its agenda, weather repopulating an hourly
        // strip) doesn't block the launcher's main thread and visibly stutter
        // scrolling / drag gestures. The shared AsyncTask thread pool is
        // sized for short bursts and is the same pool Launcher3 uses.
        @Suppress("DEPRECATION")
        setExecutor(android.os.AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * AppWidgetHostView calls setPadding internally — both during construction
     * and again whenever the provider info is bound — to apply Android's
     * default widget margin (R.dimen.system_app_widget_internal_padding,
     * typically 8dp). That default gets stacked on TOP of our user-facing
     * Widget Spacing slider, so even at 0% the widget never actually touches
     * its neighbour. Force it to 0 here; the user's slider is then the sole
     * source of widget padding and 0% behaves like Nova's "0 padding" option.
     */
    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, 0, 0, 0)
    }

    /**
     * Read the user's rounded-corner preference and apply (or remove) clipping.
     */
    fun applyRoundedCorners(ctx: Context) {
        val enabled = getWidgetRoundedCornersEnabled(ctx)
        val percent = if (enabled) getWidgetCornerRadiusPercent(ctx) else 0
        val radiusDp = percent / 100f * WIDGET_MAX_CORNER_RADIUS_DP
        cornerRadiusDp = radiusDp

        if (radiusDp > 0f) {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val radiusPx = cornerRadiusDp * density
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                }
            }
        } else {
            clipToOutline = false
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        invalidateOutline()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (cornerRadiusDp > 0f) {
            invalidateOutline()
        }
    }

    /** True if long-press was detected and fired */
    var hasLongPressed = false
        private set

    /**
     * Per-gesture latch: true once we've decided who owns this drag (widget vs.
     * ancestor pager). Set on the first MOVE big enough to read direction;
     * reset on every DOWN. Prevents direction-flip flicker mid-drag.
     */
    private var directionLocked = false

    /** Set to true to ignore all touch events (e.g., during resize) */
    var ignoreTouches = false

    /**
     * True when this widget is part of a widget stack (wrapped by HorizontalPager).
     * When true we DON'T block parent on DOWN so the Compose HorizontalPager can see
     * the DOWN event and track horizontal swipes. When false (single widget) we can
     * safely block on DOWN for rock-solid vertical-scroll protection.
     */
    var isInStack: Boolean = false

    /** Callback when long-press is detected. Passes raw screen coordinates. */
    var longPressListener: ((x: Float, y: Float) -> Unit)? = null

    /** Callback when touch is ignored (used to notify parent) */
    var onIgnoreInterceptedListener: (() -> Unit)? = null

    private val longPressRunnable = Runnable {
        // Only fire if finger hasn't moved much
        if (abs(actionDownCoords.x - currentCoords.x) < moveGestureThreshold &&
            abs(actionDownCoords.y - currentCoords.y) < moveGestureThreshold) {
            longPressHandler.removeCallbacksAndMessages(null)
            hasLongPressed = true
            longPressListener?.invoke(actionDownCoords.x, actionDownCoords.y)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (ignoreTouches) {
            onIgnoreInterceptedListener?.invoke()
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    /**
     * Swipe-vs-tap guard for issue #39. RemoteViews widgets like Breezy
     * Weather's hourly panel attach a click listener on a root container
     * that doesn't cancel on the system touchSlop, so a horizontal drag
     * finishing on the widget surface fires a click. We watch every gesture
     * and — when the finger has drifted past `swipeCancelThreshold` AND the
     * drift was primarily horizontal — forward the terminal UP as a CANCEL
     * instead. The widget's click listener never trips because it never
     * sees the matching UP. Vertical drags still get the real UP so list /
     * calendar widgets keep their scroll fling intact.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                draggedBeyondTap = false
                swipeWasHorizontal = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(actionDownCoords.x - event.rawX)
                val dy = abs(actionDownCoords.y - event.rawY)
                if (!draggedBeyondTap &&
                    (dx > swipeCancelThreshold || dy > swipeCancelThreshold)
                ) {
                    draggedBeyondTap = true
                    swipeWasHorizontal = dx > dy
                }
            }
            MotionEvent.ACTION_UP -> {
                if (draggedBeyondTap && swipeWasHorizontal) {
                    val cancel = MotionEvent.obtain(event).apply {
                        action = MotionEvent.ACTION_CANCEL
                    }
                    val result = super.dispatchTouchEvent(cancel)
                    cancel.recycle()
                    draggedBeyondTap = false
                    swipeWasHorizontal = false
                    return result
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (ignoreTouches || event == null) {
            return true
        }

        // If long-press was just performed, intercept this event
        if (hasLongPressed) {
            hasLongPressed = false
            return true
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Shared flag: tell the drawer / notifications detector to skip
                // ownership while a finger is on a widget. Without this, fast
                // vertical drags can be claimed by the drawer before our own
                // MOVE handler has had a chance to arbitrate direction.
                WidgetTouchState.isWidgetTouchActive = true
                // NOTE: we deliberately do NOT call requestDisallowInterceptTouchEvent
                // on DOWN. Doing so blocks the ancestor HorizontalPager from
                // ever seeing the DOWN — and Compose's drag detector
                // (`awaitFirstDown` then `awaitTouchSlopOrCancellation`)
                // suspends until DOWN, so once it misses the DOWN it never
                // wakes up for the rest of the gesture even if we release the
                // flag later. Instead we arbitrate on the first MOVE: if the
                // user drags vertically OR drags horizontally on a widget
                // whose content can actually scroll horizontally, we block
                // the parent. Otherwise the parent pager's natural 8dp slop
                // claim flips the home page (issue #39 / #43).
                directionLocked = false
                longPressHandler.postDelayed(
                    longPressRunnable,
                    ViewConfiguration.getLongPressTimeout().toLong()
                )
                actionDownCoords.x = event.rawX
                actionDownCoords.y = event.rawY
                currentCoords.x = event.rawX
                currentCoords.y = event.rawY
                actionDownMS = System.currentTimeMillis()
            }

            MotionEvent.ACTION_MOVE -> {
                currentCoords.x = event.rawX
                currentCoords.y = event.rawY

                val dx = abs(actionDownCoords.x - currentCoords.x)
                val dy = abs(actionDownCoords.y - currentCoords.y)

                // Direction-of-drag arbitration. We need to be FASTER than the
                // ancestor pager's slop (~8dp on most devices) — using a 1dp
                // threshold gives us seven-ish dp of headroom to commit
                // ownership before the pager would otherwise claim. Once
                // committed, we don't re-evaluate direction (the latch stops
                // mid-drag flickering between widget-owned and pager-owned).
                if (!directionLocked && (dx > density || dy > density)) {
                    directionLocked = true
                    val isHorizontalDrag = dx > dy
                    val keepWithWidget = if (!isHorizontalDrag) {
                        // Vertical drags always stay with the widget. List /
                        // calendar widgets need them for scroll; static widgets
                        // benefit too since the home-screen pager only handles
                        // horizontal anyway and the drawer detector already
                        // skips us via WidgetTouchState.
                        true
                    } else {
                        // Horizontal drag: keep ONLY if some descendant can
                        // actually scroll horizontally in the drag direction.
                        // Static widgets (Breezy Weather hourly, weather card,
                        // photo widget) → false → parent pager flips the
                        // home page. Widgets with a horizontal RecyclerView /
                        // ListView / HorizontalScrollView → true → we block.
                        val direction = if (currentCoords.x < actionDownCoords.x) 1 else -1
                        anyDescendantCanScrollHorizontally(this, direction)
                    }
                    if (keepWithWidget) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }

                // If finger moved beyond threshold, cancel long-press
                if (dx > moveGestureThreshold || dy > moveGestureThreshold) {
                    resetTouches()
                    return false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetTouches()
                WidgetTouchState.isWidgetTouchActive = false
            }
        }

        return false
    }

    /**
     * Walks the RemoteViews tree under this host and returns true if any
     * descendant view reports that it can scroll horizontally in the given
     * direction (-1 = right-to-left content, +1 = left-to-right content,
     * matching View.canScrollHorizontally semantics). Used by the MOVE
     * handler to decide whether a horizontal swipe should propagate up to
     * the home-screen pager (no scrollable child found → release parent
     * block) or stay with the widget (a child wants the swipe → keep the
     * block in place).
     */
    private fun anyDescendantCanScrollHorizontally(view: View, direction: Int): Boolean {
        if (view !== this && view.canScrollHorizontally(direction)) return true
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (anyDescendantCanScrollHorizontally(view.getChildAt(i), direction)) return true
            }
        }
        return false
    }

    /**
     * Reset touch state and cancel any pending long-press detection.
     */
    fun resetTouches() {
        longPressHandler.removeCallbacksAndMessages(null)
        hasLongPressed = false
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        resetTouches()
    }

    /**
     * Legacy method for setting long-press listener via OnLongClickListener.
     */
    fun setLongPressListener(listener: OnLongClickListener?) {
        setOnLongClickListener(listener)
    }
}