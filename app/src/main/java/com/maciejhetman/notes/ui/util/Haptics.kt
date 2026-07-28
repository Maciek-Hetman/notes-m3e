package com.maciejhetman.notes.ui.util

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Semantic wrappers around [HapticFeedback] so call sites describe *what just happened*
 * (a tap, a toggle, a confirmation…) instead of picking a raw [HapticFeedbackType] constant
 * inline every time.
 */

/** A lightweight tick for discrete taps: buttons, menu items, chips. */
fun HapticFeedback.tap() = performHapticFeedback(HapticFeedbackType.ContextClick)

/** A long-press, e.g. opening a context menu. */
fun HapticFeedback.longPress() = performHapticFeedback(HapticFeedbackType.LongPress)

/** A switch/checkbox turning on. */
fun HapticFeedback.toggleOn() = performHapticFeedback(HapticFeedbackType.ToggleOn)

/** A switch/checkbox turning off. */
fun HapticFeedback.toggleOff() = performHapticFeedback(HapticFeedbackType.ToggleOff)

/** A convenience toggle that picks on/off based on the new checked state. */
fun HapticFeedback.toggle(checked: Boolean) = if (checked) toggleOn() else toggleOff()

/** A positive, successful completion of an action (e.g. confirming a dialog, undo). */
fun HapticFeedback.confirm() = performHapticFeedback(HapticFeedbackType.Confirm)

/** A negative or destructive outcome (e.g. confirming a delete). */
fun HapticFeedback.reject() = performHapticFeedback(HapticFeedbackType.Reject)

/** A drag gesture crossing an activation threshold, e.g. swipe-to-delete armed to trigger. */
fun HapticFeedback.gestureThresholdActivate() =
    performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
