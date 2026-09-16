package com.jayesh.cashcollect.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jayesh.cashcollect.domain.analytics.AnalyticsEngine
import com.jayesh.cashcollect.domain.analytics.DailyTrendBucket
import com.jayesh.cashcollect.domain.analytics.MonthlyBucket
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.ui.common.GlassSurface
import com.jayesh.cashcollect.ui.theme.NothingBlack
import com.jayesh.cashcollect.ui.theme.NothingBorder
import com.jayesh.cashcollect.ui.theme.NothingCard
import com.jayesh.cashcollect.ui.theme.NothingGlass
import com.jayesh.cashcollect.ui.theme.NothingGlassBorder
import com.jayesh.cashcollect.ui.theme.NothingGray
import com.jayesh.cashcollect.ui.theme.NothingGreen
import com.jayesh.cashcollect.ui.theme.NothingMuted
import com.jayesh.cashcollect.ui.theme.NothingRed
import com.jayesh.cashcollect.ui.theme.NothingWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    collections: List<CollectionItem>
) {
    val insights = remember(collections) { AnalyticsEngine.computeInsights(collections) }

    Scaffold(
        containerColor = NothingBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "INSIGHTS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 17.sp,
                        color = NothingWhite
                    )
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "TODAY'S CASH COLLECTED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = NothingGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Paise(insights.todayTotalPaise).toFormattedRupees(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = NothingWhite
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Metric("COMMISSION", Paise(insights.todayCommissionPaise).toFormattedRupees(), NothingGreen)
                            Metric("TXNS", "${insights.todayCount}", NothingWhite)
                            Metric("7D TOTAL", Paise(insights.weeklyTotalPaise).toFormattedRupees(), NothingWhite)
                        }
                    }
                }
            }

            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LAST 7 DAYS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = NothingGray
                            )
                            Text(
                                text = Paise(insights.weeklyTotalPaise).toFormattedRupees(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = NothingWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        VolumeBarChart(buckets = insights.last7DaysTrend)
                    }
                }
            }

            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "LAST 30 DAYS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = NothingGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Metric("COLLECTED", Paise(insights.monthlyTotalPaise).toFormattedRupees(), NothingWhite)
                            Metric("COMMISSION", Paise(insights.monthlyCommissionPaise).toFormattedRupees(), NothingGreen)
                            Metric("AVG / DAY", Paise(insights.monthAvgPerDayPaise).toFormattedRupees(), NothingWhite)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "MONTHLY BREAKDOWN",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = NothingGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(insights.last6MonthsTrend) { bucket ->
                MonthlyRow(bucket)
            }

            item {
                Text(
                    text = "TOP COUNTERPARTIES",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = NothingGray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (insights.topCustomers.isEmpty()) {
                item {
                    Text(
                        text = "No collection records yet.",
                        fontFamily = FontFamily.Monospace,
                        color = NothingMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(insights.topCustomers) { customer ->
                    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (customer.customerAlias.isNullOrBlank()) customer.customerName else "${customer.customerName} (${customer.customerAlias})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NothingWhite
                                )
                                Text(
                                    text = "${customer.transactionCount} transactions",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = NothingGray
                                )
                            }
                            Text(
                                text = Paise(customer.totalAmountPaise).toFormattedRupees(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NothingWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = NothingGray)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = valueColor)
    }
}

@Composable
private fun MonthlyRow(bucket: MonthlyBucket) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(bucket.monthLabel, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NothingWhite)
                Text("${bucket.count} txns", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NothingGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Paise(bucket.totalPaise).toFormattedRupees(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NothingWhite
                )
                Text(
                    text = "Comm ${Paise(bucket.commissionPaise).toFormattedRupees()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NothingGreen
                )
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
            .height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        for (bucket in buckets) {
            val fraction = (bucket.totalPaise.toFloat() / maxAmount.toFloat()).coerceIn(0.08f, 1.0f)
            val isToday = bucket.dayLabel == "Today"

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (isToday) NothingRed else if (bucket.totalPaise > 0) Color.White else NothingCard)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bucket.dayLabel.take(3),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) NothingRed else NothingGray
                )
            }
        }
    }
}