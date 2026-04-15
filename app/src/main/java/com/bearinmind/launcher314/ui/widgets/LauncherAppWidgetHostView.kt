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

    /** Current corner radius in dp. Change this and call invalidateOutline() to update. */
    private var cornerRadiusDp: Float = 0f

    init {
        applyRoundedCorners(context)
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
                // FIX: Set the shared widget-touch flag so LauncherWithDrawer's drawer
                // gesture detector skips ownership. This is the primary defense against
                // fast vertical swipes being stolen by drawer/notifications, since the
                // Compose drawer detector can claim on a single large MotionEvent before
                // our own MOVE handler has a chance to block.
                WidgetTouchState.isWidgetTouchActive = true
                // FIX: For single widgets, block parent on DOWN — rock-solid protection
                // against ancestor gestures stealing the widget's own scroll (e.g. calendar).
                // For stack widgets, DON'T block — the parent HorizontalPager needs to
                // see the DOWN event to track horizontal swipes between widgets.
                if (!isInStack) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
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

                // FIX: For stack widgets, block parent once any vertical motion is present
                // UNLESS motion is strongly horizontal (dx > dy * 1.5). Relaxed from strict
                // "dy > dx" so that slightly-diagonal vertical swipes (common for scroll)
                // still trigger the block — previously required surgical precision.
                // Strong horizontal motion still passes through so the stack pager works.
                if (isInStack && dy > 2f && dx < dy * 1.5f) {
                    parent?.requestDisallowInterceptTouchEvent(true)
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