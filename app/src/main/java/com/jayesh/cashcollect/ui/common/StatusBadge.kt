package com.jayesh.cashcollect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.ui.theme.AmberWarning
import com.jayesh.cashcollect.ui.theme.AmberWarningBg
import com.jayesh.cashcollect.ui.theme.ErrorRed
import com.jayesh.cashcollect.ui.theme.ErrorRedBg
import com.jayesh.cashcollect.ui.theme.SuccessGreen

@Composable
fun StatusBadge(status: CollectionStatus, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor) = when (status) {
        CollectionStatus.PENDING -> Triple("Pending Cash", Color(0xFFE8EAF6), Color(0xFF283593))
        CollectionStatus.RECEIPT_CONFIRMED -> Triple("Cash Received (Unsent)", AmberWarningBg, AmberWarning)
        CollectionStatus.CONFIRMED -> Triple("Confirmed Sent", Color(0xFFE8F5E9), SuccessGreen)
        CollectionStatus.VOIDED -> Triple("Voided / Corrected", ErrorRedBg, ErrorRed)
    }

    Text(
        text = label,
        color = textColor,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
