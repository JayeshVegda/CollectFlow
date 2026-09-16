package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.SurfaceDivider
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextTertiary

/**
 * A dot grid — the one decorative motif the Nothing design system allows.
 *
 * Spec (from the token set): dots 1–2px on a uniform 12–16px grid, low opacity, used for
 * data-viz, loading states and empty states. Never as a container border or button style.
 *
 * Implemented with `drawBehind` so it costs nothing in layout and nothing per recomposition.
 */
fun Modifier.dotGrid(
    spacing: Dp = 14.dp,
    dotRadius: Dp = 1.dp,
    color: Color = SurfaceDivider
): Modifier = this.drawBehind {
    val step = spacing.toPx()
    val radius = dotRadius.toPx()
    if (step <= 0f) return@drawBehind

    var y = step / 2f
    while (y < size.height) {
        var x = step / 2f
        while (x < size.width) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

/**
 * Empty state. Uses [TextTertiary] rather than the disabled grey: empty-state copy is
 * content a person has to read, and the disabled token (#666666 ≈ 3.7:1) fails WCAG AA.
 */
@Composable
fun AppEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    showDotGrid: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showDotGrid) Modifier.dotGrid() else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Text(
                text = title,
                style = AppType.body,
                color = TextDisplay,
                textAlign = TextAlign.Center
            )
            if (!hint.isNullOrBlank()) {
                Text(
                    text = hint,
                    style = AppType.bodySm,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}