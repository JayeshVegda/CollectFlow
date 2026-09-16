package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingGreenBg
import com.jayesh.cashcollect.ui.theme.NothingGreenBorder
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingRedBg
import com.jayesh.cashcollect.ui.theme.NothingRedBorder
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.SurfaceDivider
import com.jayesh.cashcollect.ui.theme.SurfaceRaised
import com.jayesh.cashcollect.ui.theme.TextSecondary

/**
 * Status badge for a collection.
 *
 * Labels are written in the operator's own language — the loop is
 * *promise → collect cash → tell my brother*. So:
 *
 *   PENDING            -> TO COLLECT   (brother told me: collect ₹X from Y)
 *   RECEIPT_CONFIRMED  -> TO REPORT    (cash in hand, brother not told yet)
 *   CONFIRMED          -> REPORTED     (told — done)
 *   VOIDED             -> VOIDED       (cancelled)
 *
 * The previous wording ("CASH RECVD (UNSENT)" / "CONFIRMED SENT") implied an automatic
 * sender, which no longer exists now that reporting is a manual WhatsApp hand-off.
 */
@Composable
fun StatusBadge(status: CollectionStatus, modifier: Modifier = Modifier) {
    val spec = when (status) {
        CollectionStatus.PENDING -> BadgeSpec(
            label = "TO COLLECT",
            background = SurfaceRaised,
            border = SurfaceDivider,
            text = TextSecondary
        )
        CollectionStatus.RECEIPT_CONFIRMED -> BadgeSpec(
            label = "TO REPORT",
            background = NothingAmberBg,
            border = NothingAmberBorder,
            text = NothingAmber
        )
        CollectionStatus.CONFIRMED -> BadgeSpec(
            label = "REPORTED",
            background = NothingGreenBg,
            border = NothingGreenBorder,
            text = NothingGreen
        )
        CollectionStatus.VOIDED -> BadgeSpec(
            label = "VOIDED",
            background = NothingRedBg,
            border = NothingRedBorder,
            text = NothingRed
        )
    }

    Text(
        text = spec.label,
        color = spec.text,
        style = AppType.labelMono,
        modifier = modifier
            .background(spec.background, RoundedCornerShape(Radius.chip))
            .border(1.dp, spec.border, RoundedCornerShape(Radius.chip))
            .padding(horizontal = Space.sm, vertical = Space.xs)
    )
}

private data class BadgeSpec(
    val label: String,
    val background: Color,
    val border: Color,
    val text: Color
)
