package com.bearinmind.launcher314.ui.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

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

    private val longPressHandler = Handler(Looper.getMainLooper())
    private val actionDownCoords = PointF()
    private val currentCoords = PointF()
    private var actionDownMS = 0L

    // Movement threshold (Fossify uses move_gesture_threshold / 4 ≈ 5dp)
    private val moveGestureThreshold = (context.resources.displayMetrics.density * 5).toInt()

    /** True if long-press was detected and fired */
    var hasLongPressed = false
        private set

    /** Set to true to ignore all touch events (e.g., during resize) */
    var ignoreTouches = false

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
                // Start long-press detection
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

                // If finger moved beyond threshold, cancel long-press
                if (abs(actionDownCoords.x - currentCoords.x) > moveGestureThreshold ||
                    abs(actionDownCoords.y - currentCoords.y) > moveGestureThreshold) {
                    resetTouches()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                resetTouches()
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