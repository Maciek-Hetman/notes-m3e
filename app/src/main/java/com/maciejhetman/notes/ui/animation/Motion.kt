package com.maciejhetman.notes.ui.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * In-screen motion tokens shared across screens. Navigation transitions use the
 * platform/Navigation Compose defaults (including predictive back gestures).
 */
object Motion {
    /**
     * Placement spec for lazy-list items: an almost-critically-damped spring with a hint of the
     * expressive bounce, so inserts/removals feel alive without overshooting into neighbors.
     */
    fun <T> listItemPlacementSpec() = spring<T>(dampingRatio = 0.9f, stiffness = 400f)

    /** Predictive back: outgoing surface shrinks to this scale; revealed surface grows from it. */
    const val PREDICTIVE_BACK_SCALE = 0.85f

    /** Predictive back: corner radius the outgoing card rounds to while shrinking. */
    const val PREDICTIVE_BACK_CORNER_DP = 26

    /**
     * Predictive back: divisor for the outgoing card's lateral drift, matching the platform
     * design guidance of maxXShift = windowWidth / 20.
     */
    const val PREDICTIVE_BACK_MAX_X_SHIFT = 20

    /**
     * Spec for gesture-driven (predictive back) pop transitions. Navigation Compose seeks these
     * 1:1 with BackEvent.progress, so a plain linear tween keeps every frame glued to the
     * finger instead of easing ahead of or behind it.
     */
    fun <T> predictiveBackSpec(): FiniteAnimationSpec<T> =
        tween(POP_DURATION_MS, easing = LinearEasing)

    /**
     * Spec for the commit fade-out: deliberately back-loaded so the card stays opaque while
     * the finger is dragging (stock keeps the card visible), then collapses to transparent in
     * the last fraction of the transition — covering the swap so nothing snaps.
     */
    fun <T> predictiveBackCommitSpec(): FiniteAnimationSpec<T> =
        tween(POP_DURATION_MS, easing = CubicBezierEasing(0.9f, 0f, 1f, 0.6f))

    private const val POP_DURATION_MS = 200
}

