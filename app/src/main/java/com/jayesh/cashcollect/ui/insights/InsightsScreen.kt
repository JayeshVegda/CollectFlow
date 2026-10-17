package com.jayesh.cashcollect.ui.insights

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.analytics.AnalyticsEngine
import com.jayesh.cashcollect.domain.analytics.CustomerVolume
import com.jayesh.cashcollect.domain.analytics.InsightsTimeframe
import com.jayesh.cashcollect.domain.analytics.TimeframeStats
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.common.AppScreenTitle
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.Radius
import com.jayesh.cashcollect.ui.theme.Space
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextSecondary
import com.jayesh.cashcollect.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * INSIGHTS — Executive Financial Intelligence.
 *
 * Redesigned from 0 following Apple HIG and Emil Kowalski design engineering principles:
 *  - Slim, disciplined typography in Space Grotesk (no heavy monospace dashboard furniture).
 *  - Apple iOS Segmented Control for timeframes (Today, 7 Days, This Month, All Time).
 *  - Apple Wallet style Hero Card for realized collections and earned commission.
 *  - Pending To Collect alert banner when outstanding cash exists.
 *  - 2×2 Key Performance Metric tiles (Transactions, Avg Ticket, Commission, Active Parties).
 *  - Apple Screen Time style Top Counterparties breakdown with volume share bars.
 *  - Volume velocity bar chart completely omitted per operator directive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    collections: List<CollectionItem>,
    onPartyClick: (Long) -> Unit = {}
) {
    var selectedTimeframe by remember { mutableStateOf(InsightsTimeframe.TODAY) }

    // Run computation asynchronously on background thread
    val emptyStats = remember(selectedTimeframe) {
        AnalyticsEngine.computeTimeframeStats(emptyList(), selectedTimeframe)
    }
    val stats by produceState(initialValue = emptyStats, key1 = collections, key2 = selectedTimeframe) {
        value = withContext(Dispatchers.Default) {
            AnalyticsEngine.computeTimeframeStats(collections, selectedTimeframe)
        }
    }

    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { AppScreenTitle("Insights") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NothingBlack)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Space.gutter),
            contentPadding = PaddingValues(top = Space.xs, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            // 1. iOS-Style Segmented Control
            item(key = "timeframe_segment") {
                AppleSegmentedControl(
                    selectedTimeframe = selectedTimeframe,
                    onSelect = { selectedTimeframe = it }
                )
            }

            // 2. Hero Realized Financial Card
            item(key = "hero_realized") {
                HeroRealizedCard(stats = stats)
            }

            // 3. Pending Cash Banner (if pending cash exists)
            item(key = "pending_banner") {
                PendingStatusCard(stats = stats)
            }

            // 4. 2x2 Key Metric Tiles
            item(key = "metric_grid") {
                KeyMetricsGrid(stats = stats)
            }

            // 5. Apple Screen Time Style Top Counterparties Breakdown
            item(key = "counterparties_breakdown") {
                TopCounterpartiesSection(
                    topCustomers = stats.topCustomers,
                    totalVolume = stats.realizedTotalPaise,
                    onPartyClick = onPartyClick
                )
            }
        }
    }
}

/**
 * Apple iOS Segmented Control.
 * Tactile pill switcher with haptic feedback and smooth selection indicator.
 */
@Composable
private fun AppleSegmentedControl(
    selectedTimeframe: InsightsTimeframe,
    onSelect: (InsightsTimeframe) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141416))
            .border(0.5.dp, Color(0xFF26262B), RoundedCornerShape(10.dp))
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InsightsTimeframe.values().forEach { timeframe ->
                val isSelected = selectedTimeframe == timeframe
                val label = when (timeframe) {
                    InsightsTimeframe.TODAY -> "Today"
                    InsightsTimeframe.LAST_7_DAYS -> "7 Days"
                    InsightsTimeframe.THIS_MONTH -> "Month"
                    InsightsTimeframe.ALL_TIME -> "All Time"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NothingWhite else Color.Transparent)
                        .clickable {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(timeframe)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = AppType.label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.Black else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Apple Wallet / Card style hero card for realized cash collections.
 */
@Composable
private fun HeroRealizedCard(stats: TimeframeStats) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Color(0xFF141416))
            .border(0.5.dp, Color(0xFF26262C), RoundedCornerShape(Radius.card))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL COLLECTED",
                    style = AppType.labelSm,
                    color = TextSecondary
                )
                Text(
                    text = stats.timeframe.label,
                    style = AppType.caption,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(Space.xs))

            // Display Hero Money (28sp Medium with tabular figures)
            Text(
                text = Paise(stats.realizedTotalPaise).toFormattedRupees(),
                style = AppType.displayMoney,
                color = NothingWhite
            )

            Spacer(modifier = Modifier.height(Space.md))

            // Commission & Activity Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Commission Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(Color(0xFF132917))
                        .border(0.5.dp, Color(0xFF22572C), RoundedCornerShape(Radius.pill))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+${Paise(stats.commissionPaise).toFormattedRupees()} commission",
                            style = AppType.labelSm,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF34C759)
                        )
                    }
                }

                Text(
                    text = "${stats.transactionCount} entries completed",
                    style = AppType.caption,
                    color = TextTertiary
                )
            }
        }
    }
}

/**
 * Pending Collections Card / Banner.
 * Alerts operator when outstanding money needs to be collected.
 */
@Composable
private fun PendingStatusCard(stats: TimeframeStats) {
    if (stats.pendingTotalPaise > 0L) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Color(0xFF1C1810))
                .border(0.5.dp, Color(0xFF3D301C), RoundedCornerShape(Radius.card))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5A93C))
                        )
                        Spacer(modifier = Modifier.width(Space.xs))
                        Text(
                            text = "TO COLLECT",
                            style = AppType.labelSm,
                            color = Color(0xFFE5A93C)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stats.pendingCount} collections awaiting cash",
                        style = AppType.caption,
                        color = Color(0xFFD4A843)
                    )
                }

                Text(
                    text = Paise(stats.pendingTotalPaise).toFormattedRupees(),
                    style = AppType.amountLarge,
                    color = NothingWhite
                )
            }
        }
    } else {
        // All cleared status banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(Color(0xFF101411))
                .border(0.5.dp, Color(0xFF1E2D22), RoundedCornerShape(Radius.card))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NothingGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Space.sm))
                Text(
                    text = "All collections cleared for this timeframe",
                    style = AppType.labelSm,
                    color = NothingGreen
                )
            }
        }
    }
}

/**
 * 2x2 Key Performance Metrics Grid.
 * Apple Health style summary tiles.
 */
@Composable
private fun KeyMetricsGrid(stats: TimeframeStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "TRANSACTIONS",
                value = stats.transactionCount.toString(),
                subtext = "${stats.topCustomers.size} parties"
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "AVG / TICKET",
                value = Paise(stats.avgTransactionPaise).toFormattedRupees(),
                subtext = "per entry"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "COMMISSION",
                value = Paise(stats.commissionPaise).toFormattedRupees(),
                subtext = "earned",
                valueColor = Color(0xFF34C759)
            )
            MetricTile(
                modifier = Modifier.weight(1f),
                label = "ACTIVE PARTIES",
                value = stats.topCustomers.size.toString(),
                subtext = "counterparties"
            )
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtext: String,
    valueColor: Color = NothingWhite
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141416))
            .border(0.5.dp, Color(0xFF24242A), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = label,
                style = AppType.labelSm,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(Space.xs))
            Text(
                text = value,
                style = AppType.amountLarge,
                color = valueColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = AppType.caption,
                color = TextTertiary
            )
        }
    }
}

/**
 * Apple Screen Time style Top Counterparties breakdown with volume share bars.
 */
@Composable
private fun TopCounterpartiesSection(
    topCustomers: List<CustomerVolume>,
    totalVolume: Long,
    onPartyClick: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOP COUNTERPARTIES",
                style = AppType.label,
                color = TextSecondary
            )
            Text(
                text = "${topCustomers.size} total",
                style = AppType.caption,
                color = TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(Space.xs))

        if (topCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.card))
                    .background(Color(0xFF141416))
                    .border(0.5.dp, Color(0xFF24242A), RoundedCornerShape(Radius.card))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No counterparty collections in this period",
                    style = AppType.bodySm,
                    color = NothingMuted
                )
            }
        } else {
            val maxVolume = remember(topCustomers) {
                topCustomers.maxOfOrNull { it.totalAmountPaise } ?: 1L
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.card))
                    .background(Color(0xFF141416))
                    .border(0.5.dp, Color(0xFF26262C), RoundedCornerShape(Radius.card))
            ) {
                Column {
                    topCustomers.forEachIndexed { index, customer ->
                        val sharePercent = if (totalVolume > 0) {
                            ((customer.totalAmountPaise.toDouble() / totalVolume.toDouble()) * 100).toInt()
                        } else 0
                        val barFraction = (customer.totalAmountPaise.toFloat() / maxVolume.toFloat()).coerceIn(0.05f, 1f)

                        CounterpartyRow(
                            customer = customer,
                            sharePercent = sharePercent,
                            barFraction = barFraction,
                            onClick = { onPartyClick(customer.customerId) }
                        )

                        if (index < topCustomers.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .height(0.5.dp)
                                    .background(Color(0xFF202026))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterpartyRow(
    customer: CustomerVolume,
    sharePercent: Int,
    barFraction: Float,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials Avatar Circle
            val initials = remember(customer.customerName) {
                customer.customerName.trim().take(2).uppercase()
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E24))
                    .border(0.5.dp, Color(0xFF33333C), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = AppType.label,
                    fontWeight = FontWeight.SemiBold,
                    color = NothingWhite
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.customerName,
                    style = AppType.body,
                    fontWeight = FontWeight.Medium,
                    color = NothingWhite,
                    maxLines = 1
                )
                Text(
                    text = "${customer.transactionCount} entries · $sharePercent% share",
                    style = AppType.caption,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.width(Space.sm))

            AmountText(
                amountPaise = customer.totalAmountPaise,
                style = AppType.amount,
                color = NothingWhite
            )

            Spacer(modifier = Modifier.width(Space.xs))

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = NothingMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slim Volume Share Bar (Apple Screen Time style)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(Color(0xFF222228))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (barFraction > 0.6f) NothingRed else Color(0xFFE5E5EA))
            )
        }
    }
}