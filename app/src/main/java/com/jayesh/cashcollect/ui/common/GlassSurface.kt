package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jayesh.cashcollect.ui.theme.NothingGlass
import com.jayesh.cashcollect.ui.theme.NothingGlassBorder
import com.jayesh.cashcollect.ui.theme.NothingGlassHighlight
import com.jayesh.cashcollect.ui.theme.NothingGlassTint

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    borderWidth: Dp = 1.dp,
    sheen: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(NothingGlass)
            .background(
                Brush.linearGradient(
                    colors = if (sheen) {
                        listOf(NothingGlassHighlight, NothingGlassTint, Color.Transparent)
                    } else {
                        listOf(NothingGlass, Color.Transparent)
                    }
                )
            )
            .border(borderWidth, NothingGlassBorder, shape),
        content = content
    )
}