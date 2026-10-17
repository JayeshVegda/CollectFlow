package com.jayesh.cashcollect.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.analytics.AnalyticsEngine
import com.jayesh.cashcollect.domain.analytics.CustomerVolume
import com.jayesh.cashcollect.domain.analytics.DailyTrendBucket
import com.jayesh.cashcollect.domain.analytics.InsightsTimeframe
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.common.AmountText
import com.jayesh.cashcollect.ui.common.AppScreenTitle
import com.jayesh.cashcollect.ui.common.GlassSurface
import com.jayesh.cashcollect.ui.theme.AppType
import com.jayesh.cashcollect.ui.theme.NothingAmber
import com.jayesh.cashcollect.ui.theme.NothingAmberBg
import com.jayesh.cashcollect.ui.theme.NothingAmberBorder
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingBorderVisible
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite
import com.jayesh.cashcollect.ui.theme.TextDisplay
import com.jayesh.cashcollect.ui.theme.TextPrimary
import com.jayesh.cashcollect.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    collections: List<CollectionItem>,
    onPartyClick: (Long) -> Unit = {}
) {
    var selectedTimeframe by remember { mutableStateOf(InsightsTimeframe.TODAY) }

    // Run computation asynchronously on background thread whenever collections or timeframe changes
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Timeframe Selector Chips
            item(key = "timeframe_selector") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightsTimeframe.values().forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTimeframe = tf },
                            label = { Text(tf.label, style = AppType.labelMono) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = NothingCard,
                                labelColor = NothingGray,
                                selectedContainerColor = NothingWhite,
                                selectedLabelColor = Color.Black
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = NothingBorderVisible,
                                selectedBorderColor = NothingWhite
                            )
                        )
                    }
                }
            }

            // Hero Financial KPI Card
            item(key = "hero_kpi") {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stats.timeframe.label} REALIZED",
                                style = AppType.labelMono,
                                color = TextSecondary
                            )
                            if (stats.pendingCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(NothingAmberBg)
                                        .border(1.dp, NothingAmberBorder, RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${Paise(stats.pendingTotalPaise).toFormattedRupees()} PENDING (${stats.pendingCount})",
                                        style = AppType.labelMono,
                                        fontSize = 10.sp,
                                        color = NothingAmber
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        AmountText(
                            amountPaise = stats.realizedTotalPaise,
                            style = AppType.displayMoney,
                            color = TextDisplay,
                            align = androidx.compose.ui.text.style.TextAlign.Start
                        )

                        // Realized vs Pending Volume Split Bar
                        if (stats.realizedTotalPaise > 0L || stats.pendingTotalPaise > 0L) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val totalVolume = stats.realizedTotalPaise + stats.pendingTotalPaise
                            val realizedFraction = (stats.realizedTotalPaise.toFloat() / totalVolume.toFloat()).coerceIn(0.02f, 1f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(NothingBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(realizedFraction)
                                        .background(NothingGreen)
                                )
                                if (stats.pendingTotalPaise > 0L) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxSize()
                                            .background(NothingAmber)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3 Sub-Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("COMMISSION", style = AppType.labelMono, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                AmountText(
                                    amountPaise = stats.commissionPaise,
                                    style = AppType.amountLarge,
                                    color = NothingGreen,
                                    align = androidx.compose.ui.text.style.TextAlign.Start
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TRANSACTIONS", style = AppType.labelMono, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${stats.transactionCount}",
                                    style = AppType.amountLarge,
                                    color = NothingWhite
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("AVG / TXN", style = AppType.labelMono, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                AmountText(
                                    amountPaise = stats.avgTransactionPaise,
                                    style = AppType.amountLarge,
                                    color = NothingWhite
                                )
                            }
                        }
                    }
                }
            }

            // Volume Velocity (Daily Trend Bar Chart)
            item(key = "velocity_chart") {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VOLUME VELOCITY (7D)",
                                style = AppType.labelMonoLarge,
                                color = NothingWhite
                            )
                            if (stats.peakDayLabel != null) {
                                Text(
                                    text = stats.peakDayLabel!!,
                                    style = AppType.labelMono,
                                    color = NothingGray
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        VolumeBarChart(buckets = stats.trendBuckets)
                    }
                }
            }

            // Section Label for Counterparties
            item(key = "counterparties_label") {
                Text(
                    text = "TOP COUNTERPARTIES (${stats.timeframe.label})",
                    style = AppType.labelMonoLarge,
                    color = NothingWhite,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Top Counterparties List
            if (stats.topCustomers.isEmpty()) {
                item(key = "empty_counterparties") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No collection records in this timeframe.",
                            style = AppType.bodySm,
                            color = NothingMuted
                        )
                    }
                }
            } else {
                val maxPartyPaise = stats.topCustomers.maxOfOrNull { it.totalAmountPaise } ?: 1L

                items(stats.topCustomers, key = { it.customerId }) { customer ->
                    val shareFraction = if (maxPartyPaise > 0L) {
                        (customer.totalAmountPaise.toFloat() / maxPartyPaise.toFloat()).coerceIn(0.04f, 1f)
                    } else 0f
                    val totalVolume = stats.realizedTotalPaise
                    val pctOfTotal = if (totalVolume > 0L) {
                        (customer.totalAmountPaise.toDouble() / totalVolume.toDouble() * 100).toInt()
                    } else 0

                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPartyClick(customer.customerId) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (customer.customerAlias.isNullOrBlank()) {
                                            customer.customerName
                                        } else {
                                            "${customer.customerName} (${customer.customerAlias})"
                                        },
                                        style = AppType.subheading,
                                        color = NothingWhite
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${customer.transactionCount} txns · $pctOfTotal% of volume",
                                        style = AppType.labelMono,
                                        color = NothingGray
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AmountText(
                                        amountPaise = customer.totalAmountPaise,
                                        style = AppType.amountLarge,
                                        color = NothingWhite
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "View Party Ledger",
                                        tint = NothingGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Horizontal Share Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(NothingBorder)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(shareFraction)
                                        .background(NothingWhite)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeBarChart(buckets: List<DailyTrendBucket>) {
    val maxAmount = remember(buckets) {
        val m = buckets.maxOfOrNull { it.totalPaise } ?: 0L
        if (m <= 0L) 1L else m
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        for (bucket in buckets) {
            val fraction = (bucket.totalPaise.toFloat() / maxAmount.toFloat()).coerceIn(0.06f, 1.0f)
            val isToday = bucket.dayLabel.equals("Today", ignoreCase = true)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (bucket.totalPaise > 0L) {
                    Text(
                        text = Paise(bucket.totalPaise).toShortRupeeString(),
                        style = AppType.labelMono,
                        fontSize = 9.sp,
                        color = if (isToday) NothingRed else NothingGray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (isToday) NothingRed else if (bucket.totalPaise > 0) NothingWhite else NothingBorderVisible)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bucket.dayLabel.take(3).uppercase(),
                    style = AppType.labelMono,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) NothingRed else NothingGray
                )
            }
        }
    }
}