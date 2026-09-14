package com.jayesh.cashcollect.domain.analytics

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CustomerVolume(
    val customerName: String,
    val customerAlias: String?,
    val totalAmountPaise: Long,
    val transactionCount: Int
)

data class DailyTrendBucket(
    val dayLabel: String,
    val dateLabel: String,
    val totalPaise: Long,
    val count: Int
)

data class InsightsSummary(
    val todayTotalPaise: Long,
    val todayCommissionPaise: Long,
    val todayCount: Int,
    val weeklyTotalPaise: Long,
    val weeklyCommissionPaise: Long,
    val weeklyCount: Int,
    val monthlyTotalPaise: Long,
    val monthlyCount: Int,
    val topCustomers: List<CustomerVolume>,
    val last7DaysTrend: List<DailyTrendBucket>
)

object AnalyticsEngine {

    fun computeInsights(collections: List<CollectionItem>): InsightsSummary {
        // Exclude VOIDED items from revenue totals
        val validItems = collections.filter { it.status != CollectionStatus.VOIDED }

        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        // Start of today (00:00:00)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        // Start of 7 days ago
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val startOf7Days = cal.timeInMillis

        // Start of 30 days ago
        cal.timeInMillis = startOfToday
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val startOf30Days = cal.timeInMillis

        var todayTotal = 0L
        var todayCommission = 0L
        var todayCount = 0

        var weeklyTotal = 0L
        var weeklyCommission = 0L
        var weeklyCount = 0

        var monthlyTotal = 0L
        var monthlyCount = 0

        val customerMap = mutableMapOf<String, Pair<CustomerVolume, Long>>()

        for (item in validItems) {
            val itemTime = item.receivedAt ?: item.createdAt

            if (itemTime >= startOfToday) {
                todayTotal += item.amountPaise
                todayCommission += item.commissionPaise
                todayCount++
            }

            if (itemTime >= startOf7Days) {
                weeklyTotal += item.amountPaise
                weeklyCommission += item.commissionPaise
                weeklyCount++
            }

            if (itemTime >= startOf30Days) {
                monthlyTotal += item.amountPaise
                monthlyCount++
            }

            // Customer aggregation
            val key = item.customerDisplayName
            val current = customerMap[key]
            if (current == null) {
                customerMap[key] = Pair(
                    CustomerVolume(
                        customerName = item.customerName,
                        customerAlias = item.customerAlias,
                        totalAmountPaise = item.amountPaise,
                        transactionCount = 1
                    ),
                    item.amountPaise
                )
            } else {
                customerMap[key] = Pair(
                    current.first.copy(
                        totalAmountPaise = current.first.totalAmountPaise + item.amountPaise,
                        transactionCount = current.first.transactionCount + 1
                    ),
                    current.first.totalAmountPaise + item.amountPaise
                )
            }
        }

        val topCustomers = customerMap.values
            .map { it.first }
            .sortedByDescending { it.totalAmountPaise }
            .take(5)

        // Generate Last 7 Days daily buckets
        val trendBuckets = mutableListOf<DailyTrendBucket>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val tempCal = Calendar.getInstance()
        tempCal.timeInMillis = startOfToday

        for (i in 6 downTo 0) {
            val loopCal = Calendar.getInstance()
            loopCal.timeInMillis = startOfToday
            loopCal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = loopCal.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L) - 1

            val dayItems = validItems.filter {
                val t = it.receivedAt ?: it.createdAt
                t in dayStart..dayEnd
            }

            val dayTotal = dayItems.sumOf { it.amountPaise }
            trendBuckets.add(
                DailyTrendBucket(
                    dayLabel = if (i == 0) "Today" else dayFormat.format(Date(dayStart)),
                    dateLabel = dateFormat.format(Date(dayStart)),
                    totalPaise = dayTotal,
                    count = dayItems.size
                )
            )
        }

        return InsightsSummary(
            todayTotalPaise = todayTotal,
            todayCommissionPaise = todayCommission,
            todayCount = todayCount,
            weeklyTotalPaise = weeklyTotal,
            weeklyCommissionPaise = weeklyCommission,
            weeklyCount = weeklyCount,
            monthlyTotalPaise = monthlyTotal,
            monthlyCount = monthlyCount,
            topCustomers = topCustomers,
            last7DaysTrend = trendBuckets
        )
    }
}
