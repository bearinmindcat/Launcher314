package com.bearinmind.launcher314.helpers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptic feedback utility for consistent vibration feedback across the app.
 * Inspired by Fossify Launcher's implementation using HapticFeedbackConstants.VIRTUAL_KEY
 */
object HapticFeedbackUtils {

    /**
     * Performs a long press haptic feedback - used when context menus appear
     * or when initiating drag operations.
     */
    fun performLongPressHaptic(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Performs a text handle move haptic feedback - lighter feedback
     * suitable for drag movements or selections.
     */
    fun performTextHandleMoveHaptic(hapticFeedback: HapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

/**
 * Composable helper to get the haptic feedback controller.
 * Use this in your composables to easily access haptic feedback.
 *
 * Usage:
 * ```
 * val hapticFeedback = rememberHapticFeedback()
 * // Then call:
 * hapticFeedback.performLongPress()
 * ```
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackController {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { HapticFeedbackController(hapticFeedback) }
}

/**
 * Controller class that wraps HapticFeedback with convenient methods.
 */
class HapticFeedbackController(private val hapticFeedback: HapticFeedback) {

    /**
     * Performs long press haptic feedback.
     * Use when showing context menus on long press.
     */
    fun performLongPress() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Performs text handle move haptic feedback.
     * Lighter feedback for drag movements.
     */
    fun performTextHandleMove() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Access the underlying HapticFeedback if needed for custom feedback types.
     */
    val native: HapticFeedback get() = hapticFeedback
}
