package com.jayesh.cashcollect.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii. Previously these were hardcoded per file (4, 8, 14, 16, 999 all appeared
 * ad hoc), which is a large part of why the screens felt inconsistent. One scale now.
 */
object Radius {
    /** Chips, badges, small controls. */
    val chip = 8.dp

    /** The default card / ledger row. */
    val card = 14.dp

    /** Large surfaces. */
    val large = 16.dp

    /** Bottom sheets. */
    val sheet = 20.dp

    /** Fully round (buttons, pills, the capture bar). */
    val pill = 999.dp
}

/** Material3 shape slots wired to [Radius] so Material components inherit the scale. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.chip),
    small = RoundedCornerShape(Radius.chip),
    medium = RoundedCornerShape(Radius.card),
    large = RoundedCornerShape(Radius.large),
    extraLarge = RoundedCornerShape(Radius.sheet)
)
