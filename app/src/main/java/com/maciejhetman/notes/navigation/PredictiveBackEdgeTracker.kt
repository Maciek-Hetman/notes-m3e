package com.maciejhetman.notes.navigation

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController

/**
 * Physical horizontal direction (+1 = toward right edge, -1 = toward left edge) that the
 * current predictive-back gesture is pulling the top screen toward. Read by the NavHost
 * pop-transition lambdas so slides follow the finger's edge instead of guessing a side.
 */
class PredictiveBackEdgeTracker {
    var swipeSign by mutableFloatStateOf(1f)
}

/**
 * Passively records the swipe edge of in-progress predictive-back gestures without consuming
 * them: start/progress/cancel events are broadcast to every enabled callback, and the final
 * back press stays with NavHost because its own callback is registered after this one (later
 * registrations win). Disabled at the root so the system back-to-home animation is untouched.
 */
@Composable
fun rememberPredictiveBackEdgeTracker(
    navController: NavController,
): PredictiveBackEdgeTracker {
    val tracker = remember { PredictiveBackEdgeTracker() }
    val enabled = navController.previousBackStackEntry != null
    val dispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(dispatcherOwner, lifecycleOwner, enabled) {
        val callback = object : OnBackPressedCallback(enabled) {
            private fun record(event: BackEventCompat) {
                tracker.swipeSign =
                    if (event.swipeEdge == BackEventCompat.EDGE_LEFT) -1f else 1f
            }

            override fun handleOnBackStarted(backEvent: BackEventCompat) = record(backEvent)
            override fun handleOnBackProgressed(backEvent: BackEventCompat) = record(backEvent)

            /** Never consumes: commit belongs to NavHost's own (later-registered) callback. */
            override fun handleOnBackPressed() = Unit
        }
        dispatcherOwner!!.onBackPressedDispatcher.addCallback(lifecycleOwner, callback)
        onDispose { callback.remove() }
    }
    return tracker
}
