package com.jayesh.cashcollect.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings

/**
 * Motion tokens.
 *
 * The Nothing design system is specific here: 150–250ms for micro-interactions,
 * 300–400ms for transitions, a subtle ease-out, **no spring or bounce**, and
 * "prefer opacity over position — elements fade, don't slide."
 *
 * Apple's contribution is the deference half: motion should explain a change in state
 * or spatial continuity, never decorate. So these are the only two durations we use.
 */
object Motion {

    /** Micro-interactions: press states, chip selects, swatch changes. */
    const val microMs = 180

    /** Transitions: screen changes, list reordering, sheets. */
    const val standardMs = 320

    /** cubic-bezier(0.25, 0.1, 0.25, 1) — subtle ease-out, per the Nothing token set. */
    val easing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    fun <T> micro(): TweenSpec<T> = tween(durationMillis = microMs, easing = easing)

    fun <T> standard(): TweenSpec<T> = tween(durationMillis = standardMs, easing = easing)
}

/**
 * True when the user has asked the OS to remove animations
 * (Settings > Accessibility > Remove animations, or animator duration scale = 0).
 *
 * Respecting this is part of the Apple-derived rule "set your accessibility floor first".
 */
@Composable
@ReadOnlyComposable
fun reduceMotion(): Boolean {
    val context = LocalContext.current
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    return scale == 0f
}
