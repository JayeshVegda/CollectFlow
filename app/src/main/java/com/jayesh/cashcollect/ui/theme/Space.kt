package com.jayesh.cashcollect.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale — 8dp rhythm with a 4dp sub-step.
 *
 * Two ideas are encoded here:
 *  1. Nothing design system: an 8px base grid, so nothing lands on an arbitrary value.
 *  2. Notion's "adjacency" rule (from their page-design write-up): the gap between two
 *     items depends on whether their *neighbour* is the same kind of thing. List items
 *     chunk together tightly; a new group breathes. That is what makes a dense ledger
 *     read as structured instead of flat.
 *
 * Rule of thumb: reach for [listGroupBreak] before reaching for a new font size.
 */
object Space {

    /** Optical adjustments only. */
    val xxs = 2.dp

    /** Icon-to-label gaps, tight inner padding. */
    val xs = 4.dp

    /** Component internal spacing; same-kind list neighbours chunk at this. */
    val sm = 8.dp

    /** Standard padding, element gaps, screen gutters. */
    val md = 16.dp

    /** Group separation. */
    val lg = 24.dp

    /** Section margins. */
    val xl = 32.dp

    /** Major section breaks. */
    val xxl = 48.dp

    /** Page-level vertical rhythm. */
    val xxxl = 64.dp

    /** Horizontal page gutter used by every screen. */
    val gutter = md

    /** Vertical gap between rows of the same kind (Notion adjacency: chunk together). */
    val listAdjacent = sm

    /** Vertical gap when the neighbour changes kind — a new group starts here. */
    val listGroupBreak = xl

    /** Minimum touch target. Android accessibility floor is 48dp. */
    val touchTarget = 48.dp
}
