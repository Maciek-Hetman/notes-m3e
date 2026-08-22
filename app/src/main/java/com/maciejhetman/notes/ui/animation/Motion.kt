package com.maciejhetman.notes.ui.animation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection

/**
 * Centralized motion tokens for navigation transitions and in-screen animations.
 *
 * Two motion profiles, chosen by how the animation is driven:
 *
 * - **Gesture-driven (predictive back, pops):** plain [LinearEasing] tweens.
 *   Navigation Compose seeks these transitions 1:1 with `BackEvent.progress`, so an eased
 *   curve would run ahead of the finger and read as rubber-banding. Linear keeps every pixel
 *   glued to the gesture, matching stock Android hierarchical back.
 * - **Time-driven (enters, exits, in-screen changes):** Material 3 motion-scheme springs /
 *   effects captured at the call site.
 *
 * Hierarchical pages (folders, Settings) use [Hierarchical] — the Android 17 Settings-style
 * shared-axis push/pop with parent parallax and a seekable predictive-back scale on the child.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
object Motion {
    /** Scene-to-scene navigation moves: push, pop, and predictive back. */
    const val NAV_DURATION_MS = 300

    /** Alpha settles inside a navigation transition faster than the move itself. */
    const val NAV_FADE_DURATION_MS = 200

    /**
     * Duration of pop transitions. These are driven by predictive-back gesture progress,
     * so callers pair them with [LinearEasing] to keep the visuals tracking the finger
     * one-to-one instead of easing ahead of or behind it.
     */
    const val POP_DURATION_MS = 300

    /** In-screen content changes such as menus, toolbars, and indicators. */
    const val CONTENT_DURATION_MS = 300

    /** Small state flips such as icons, chips, and toggles. */
    const val FAST_CONTENT_DURATION_MS = 200

    /**
     * Shared-element bounds morph between list and detail. Uses the Compose default spring so the
     * element follows predictive-back seeks frame-by-frame and lands with a physical settle.
     */
    val DefaultBoundsTransform = BoundsTransform { _, _ ->
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    /**
     * Placement spec for lazy-list items: an almost-critically-damped spring with a hint of the
     * expressive bounce, so inserts/removals feel alive without overshooting into neighbors.
     */
    fun <T> listItemPlacementSpec() = spring<T>(dampingRatio = 0.9f, stiffness = 400f)

    fun popFloatSpec(): FiniteAnimationSpec<Float> =
        tween(POP_DURATION_MS, easing = LinearEasing)

    fun popSlideSpec(): FiniteAnimationSpec<IntOffset> =
        tween(POP_DURATION_MS, easing = LinearEasing)

    /**
     * Stock Android hierarchical (Settings-style) shared-axis transitions.
     *
     * Forward: child enters from the logical end edge; parent recedes by [PARENT_PARALLAX]
     * of its width toward the start edge.
     * Predictive pop: child slides toward the end edge and scales to [POP_EXIT_SCALE] while the
     * parent re-enters from the start-edge parallax offset — all on linear specs so the gesture
     * can seek them 1:1.
     */
    object Hierarchical {
        /** Parent slides by this fraction of its width (stock Activity / Settings parallax). */
        const val PARENT_PARALLAX = 4

        /** Child scale at full predictive-back progress (stock in-app hierarchical preview). */
        const val POP_EXIT_SCALE = 0.9f

        /**
         * +1 for LTR, -1 for RTL. Horizontal IntOffset slides are not auto-mirrored by Compose,
         * so callers multiply edge offsets by this sign.
         */
        fun layoutSign(direction: LayoutDirection): Int =
            if (direction == LayoutDirection.Rtl) -1 else 1

        fun forwardEnter(
            spatialSpec: FiniteAnimationSpec<IntOffset>,
            effectsSpec: FiniteAnimationSpec<Float>,
            layoutDirection: LayoutDirection,
        ): EnterTransition {
            val sign = layoutSign(layoutDirection)
            return slideInHorizontally(animationSpec = spatialSpec) { fullWidth ->
                sign * fullWidth
            } + fadeIn(effectsSpec)
        }

        fun forwardExit(
            spatialSpec: FiniteAnimationSpec<IntOffset>,
            effectsSpec: FiniteAnimationSpec<Float>,
            layoutDirection: LayoutDirection,
        ): ExitTransition {
            val sign = layoutSign(layoutDirection)
            return slideOutHorizontally(animationSpec = spatialSpec) { fullWidth ->
                -sign * fullWidth / PARENT_PARALLAX
            } + fadeOut(effectsSpec)
        }

        fun popEnter(
            slideSpec: FiniteAnimationSpec<IntOffset>,
            layoutDirection: LayoutDirection,
        ): EnterTransition {
            val sign = layoutSign(layoutDirection)
            return slideInHorizontally(animationSpec = slideSpec) { fullWidth ->
                -sign * fullWidth / PARENT_PARALLAX
            }
        }

        fun popExit(
            slideSpec: FiniteAnimationSpec<IntOffset>,
            scaleSpec: FiniteAnimationSpec<Float>,
            layoutDirection: LayoutDirection,
        ): ExitTransition {
            val sign = layoutSign(layoutDirection)
            return slideOutHorizontally(animationSpec = slideSpec) { fullWidth ->
                sign * fullWidth
            } + scaleOut(
                targetScale = POP_EXIT_SCALE,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = scaleSpec,
            )
        }
    }
}
