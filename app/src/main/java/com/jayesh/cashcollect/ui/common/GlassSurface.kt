package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.SurfaceCard
import com.jayesh.cashcollect.ui.theme.SurfaceDivider
import com.jayesh.cashcollect.ui.theme.SurfaceEdge
import com.jayesh.cashcollect.ui.theme.SurfaceRaised
import com.jayesh.cashcollect.ui.theme.TextSecondary

/*
 * Surfaces
 * ---------
 * A deliberate change from what was here before.
 *
 * The previous implementation stacked a 7%-white fill, a diagonal linear-gradient "sheen"
 * and an 18%-white 1px border. That is glassmorphism, not Liquid Glass: Liquid Glass
 * refracts and tints whatever is behind it, per frame. On a #000000 OLED canvas there is
 * nothing behind it to sample, so the effect degraded into a flat grey box with a bright
 * wireframe outline — which is exactly why the cards read as "a bit weird".
 *
 * Real translucency also has a measured accessibility cost: independent audits of Apple's
 * own Liquid Glass found surfaces as low as 1.5:1 against the 4.5:1 AA floor, and Apple
 * spent three OS releases walking the transparency default back ("Tinted" mode + a
 * diffusion layer behind complex content).
 *
 * So the rule here is:
 *   - Depth is expressed with SURFACE STEPS (#000 -> #111 -> #1A1A1A), not blur.
 *   - A hairline edge is available when a boundary genuinely needs to be signalled.
 *   - Translucency is reserved for transient layers that actually have content behind
 *     them (bottom sheets, the capture bar) — see [GlassSurface].
 */

/**
 * The default card / row container. Opaque, calm, no border.
 *
 * @param fill one of the surface steps: [SurfaceCard] or [SurfaceRaised].
 * @param edge draw a 1px [SurfaceEdge] hairline. Use sparingly — structure should come
 *   from spacing and type, not from outlining everything.
 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.card),
    fill: Color = SurfaceCard,
    edge: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .then(if (edge) Modifier.border(1.dp, SurfaceEdge, shape) else Modifier),
        content = content
    )
}

/**
 * A surface for transient/layered content — sheets, the floating capture bar, the
 * segmented control. Sits one step above [AppSurface] so layering reads without blur.
 *
 * Kept under its original name so existing call sites continue to work; the internals are
 * now tinted rather than translucent.
 *
 * @param borderWidth explicit edge width. `0.dp` (default) = no edge.
 * @param hairline adds a 1px [SurfaceDivider] edge, which implies a layering boundary
 *   without the bright white outline the previous implementation drew.
 * @param elevated use the raised surface step (#1A1A1A).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.card),
    borderWidth: Dp = 0.dp,
    hairline: Boolean = true,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val edge: Modifier = when {
        borderWidth > 0.dp -> Modifier.border(borderWidth, SurfaceEdge, shape)
        hairline -> Modifier.border(1.dp, SurfaceDivider, shape)
        else -> Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (elevated) SurfaceRaised else SurfaceCard)
            .then(edge),
        content = content
    )
}

/**
 * A horizontal strip of label/value metrics. Used for compact KPI rows.
 * Values carry the colour; labels stay neutral (Nothing rule: colour the value only).
 */
@Composable
fun GlassMetricRow(
    modifier: Modifier = Modifier,
    metrics: List<Triple<String, String, Color>>,
    shape: Shape = RoundedCornerShape(Radius.card)
) {
    AppSurface(modifier = modifier.fillMaxWidth(), shape = shape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.md, vertical = Space.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for ((label, value, valueColor) in metrics) {
                Column {
                    androidx.compose.material3.Text(
                        text = label,
                        style = AppType.labelMono,
                        color = TextSecondary
                    )
                    androidx.compose.material3.Text(
                        text = value,
                        style = AppType.amount,
                        color = valueColor
                    )
                }
            }
        }
    }
}