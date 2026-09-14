package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingGreenBg
import com.jayesh.cashcollect.ui.theme.NothingGreenBorder
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingRedBg
import com.jayesh.cashcollect.ui.theme.NothingRedBorder

@Composable
fun StatusBadge(status: CollectionStatus, modifier: Modifier = Modifier) {
    val (label, bgColor, borderColor, textColor) = when (status) {
        CollectionStatus.PENDING -> Quadruple("PENDING", NothingCardRaised, NothingBorder, NothingGray)
        CollectionStatus.RECEIPT_CONFIRMED -> Quadruple("CASH RECVD (UNSENT)", NothingAmberBg, NothingAmberBorder, NothingAmber)
        CollectionStatus.CONFIRMED -> Quadruple("CONFIRMED SENT", NothingGreenBg, NothingGreenBorder, NothingGreen)
        CollectionStatus.VOIDED -> Quadruple("VOIDED", NothingRedBg, NothingRedBorder, NothingRed)
    }

    Text(
        text = label,
        color = textColor,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
