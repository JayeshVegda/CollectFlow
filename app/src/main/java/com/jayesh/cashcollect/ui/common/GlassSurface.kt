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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.ui.theme.NothingGlass
import com.jayesh.cashcollect.ui.theme.NothingGlassBorder
import com.jayesh.cashcollect.ui.theme.NothingGlassHighlight
import com.jayesh.cashcollect.ui.theme.NothingGlassTint
import com.jayesh.cashcollect.ui.theme.NothingGray

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

@Composable
fun GlassMetricRow(
    modifier: Modifier = Modifier,
    metrics: List<Triple<String, String, Color>>,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = shape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for ((label, value, valueColor) in metrics) {
                Column {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NothingGray,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = value,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = valueColor
                    )
                }
            }
        }
    }
}