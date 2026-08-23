package com.maciejhetman.notes.ui.animation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.maciejhetman.notes.navigation.LocalNavAnimatedVisibilityScope
import kotlin.math.round

/**
 * Rounds the caller's corners while its navigation destination plays an exit/pop transition,
 * mirroring the system predictive-back card. Driven by the destination's own transition,
 * so the radius seeks 1:1 with gesture progress. Identity while fully visible.
 */
@Composable
fun Modifier.predictiveBackCorners(): Modifier {
    val scope = LocalNavAnimatedVisibilityScope.current ?: return this
    val maxRadiusPx =
        with(LocalDensity.current) { Motion.PREDICTIVE_BACK_CORNER_DP.dp.toPx() }

    val radius = with(scope.transition) {
        animateFloat(
            transitionSpec = { Motion.predictiveBackSpec() },
            label = "predictive_back_corner",
        ) { state ->
            when (state) {
                EnterExitState.Visible -> 0f
                else -> maxRadiusPx
            }
        }
    }

    return graphicsLayer {
        // Quantized to whole pixels: sub-pixel radius changes would invalidate the outline
        // (and re-record this full-screen layer's display list) every single frame.
        val r = round(radius.value)
        if (r > 0.5f) {
            shape = RoundedCornerShape(r)
            clip = true
        }
    }
}
