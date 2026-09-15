package com.jayesh.cashcollect.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.service.telegram.TelegramAuthState
import com.jayesh.cashcollect.ui.common.StatusBadge
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingCardRaised
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    collections: List<CollectionItem>,
    appSettings: AppSettings,
    telegramAuthState: TelegramAuthState,
    onQuickCaptureClick: () -> Unit,
    onCollectionClick: (Long) -> Unit,
    onGoToHistory: () -> Unit,
    onGoToSettings: () -> Unit,
    onExportCsv: () -> Unit
) {
    // Calculate Today's Stats
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = cal.timeInMillis

    val validItems = collections.filter { it.status != CollectionStatus.VOIDED }
    val todayItems = validItems.filter {
        val t = it.receivedAt ?: it.createdAt
        t >= startOfToday
    }

    // "Collected" means money actually in hand: RECEIPT_CONFIRMED or CONFIRMED only.
    // PENDING entries are promises, not cash — including them inflated the collected total.
    val todayCollectedItems = todayItems.filter {
        it.status == CollectionStatus.RECEIPT_CONFIRMED || it.status == CollectionStatus.CONFIRMED
    }

    val todayTotalPaise = todayCollectedItems.sumOf { it.amountPaise }
    val todayCommPaise = todayCollectedItems.sumOf { it.commissionPaise }
    val todayCount = todayCollectedItems.size

    val outstandingCount = validItems.count { it.status == CollectionStatus.RECEIPT_CONFIRMED }
    val pendingCount = validItems.count { it.status == CollectionStatus.PENDING }

    val recent5 = collections.take(5)

    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DASHBOARD",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 17.sp,
                            color = NothingWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NothingBlack)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. HERO CARD: TODAY GLANCE
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NothingBorderVisible, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = NothingCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TODAY'S COLLECTIONS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingGray
                            )
                            Text(
                                text = if (todayCount == 1) "1 ENTRY" else "$todayCount ENTRIES",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = Paise(todayTotalPaise).toFormattedRupees(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            letterSpacing = (-1).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMMISSION EARNED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = NothingMuted
                            )
                            Text(
                                text = Paise(todayCommPaise).toFormattedRupees(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingGreen
                            )
                        }
                    }
                }
            }

            // 2. DISPATCH & AUTOMATION HEALTH
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = NothingCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "DISPATCH STATUS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingGray
                        )

                        // Telegram row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGoToSettings() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dotColor = when (telegramAuthState) {
                                    is TelegramAuthState.Ready -> NothingGreen
                                    is TelegramAuthState.Error -> NothingRed
                                    is TelegramAuthState.WaitingPhoneNumber,
                                    is TelegramAuthState.WaitingCode,
                                    is TelegramAuthState.WaitingPassword -> NothingAmber
                                    else -> if (appSettings.telegramEnabled) NothingAmber else NothingMuted
                                }
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Telegram Auto-Send",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = NothingWhite
                                )
                            }

                            val statusLabel = when (telegramAuthState) {
                                is TelegramAuthState.Ready -> {
                                    val userStr = telegramAuthState.username?.let { "@$it" } ?: telegramAuthState.firstName
                                    "ONLINE ($userStr)"
                                }
                                is TelegramAuthState.WaitingPhoneNumber -> "SIGN IN NEEDED"
                                is TelegramAuthState.WaitingCode -> "NEEDS LOGIN CODE"
                                is TelegramAuthState.WaitingPassword -> "NEEDS 2FA"
                                is TelegramAuthState.Initializing -> "CONNECTING..."
                                is TelegramAuthState.Error -> "ERROR"
                                else -> if (appSettings.telegramEnabled) "OFFLINE" else "DISABLED"
                            }
                            Text(
                                text = statusLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (telegramAuthState) {
                                    is TelegramAuthState.Ready -> NothingGreen
                                    is TelegramAuthState.Error -> NothingRed
                                    else -> NothingAmber
                                }
                            )
                        }

                        // WhatsApp fallback row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGoToSettings() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (appSettings.brotherWhatsAppNumber.isNotBlank()) NothingGreen else NothingMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WhatsApp Fallback",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = NothingWhite
                                )
                            }
                            Text(
                                text = if (appSettings.brotherWhatsAppNumber.isNotBlank()) "CONFIGURED" else "SET NUMBER",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (appSettings.brotherWhatsAppNumber.isNotBlank()) NothingGreen else NothingMuted
                            )
                        }
                    }
                }
            }

            // 3. PENDING & OUTSTANDING ALERTS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Outstanding Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                1.dp,
                                if (outstandingCount > 0) NothingAmberBorder else NothingBorder,
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (outstandingCount > 0) NothingAmberBg else NothingCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "OUTSTANDING",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (outstandingCount > 0) NothingAmber else NothingMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$outstandingCount",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (outstandingCount > 0) NothingAmber else NothingWhite
                            )
                            Text(
                                text = if (outstandingCount > 0) "Awaiting dispatch" else "All confirmed",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NothingMuted
                            )
                        }
                    }

                    // Pending Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = NothingCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "PENDING CASH",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$pendingCount",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Text(
                                text = "Yet to collect",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NothingMuted
                            )
                        }
                    }
                }
            }

            // 4. QUICK ACTION BAR
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(NothingWhite)
                            .clickable { onQuickCaptureClick() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ QUICK", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, NothingBorderVisible, RoundedCornerShape(999.dp))
                            .clickable { onExportCsv() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT CSV", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NothingWhite)
                        }
                    }
                }
            }

            // 5. RECENT ACTIVITY PREVIEW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ACTIVITY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingGray
                    )
                    Text(
                        text = "VIEW ALL →",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite,
                        modifier = Modifier.clickable { onGoToHistory() }
                    )
                }
            }

            if (recent5.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded transactions yet.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NothingMuted
                        )
                    }
                }
            } else {
                items(recent5, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable { onCollectionClick(item.id) },
                        colors = CardDefaults.cardColors(containerColor = NothingCard),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.customerDisplayName,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NothingWhite
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(item.createdAt)),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = NothingMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Paise(item.amountPaise).toFormattedRupees(),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NothingWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                StatusBadge(status = item.status)
                            }
                        }
                    }
                }
            }
        }
    }
}
