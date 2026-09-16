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

data class MonthlyBucket(
    val monthLabel: String,
    val totalPaise: Long,
    val commissionPaise: Long,
    val count: Int
)

data class InsightsSummary(
    val todayTotalPaise: Long,
    val todayCommissionPaise: Long,
    val todayCount: Int,
    val todayPendingPaise: Long,
    val todayPendingCount: Int,
    val weeklyTotalPaise: Long,
    val weeklyCommissionPaise: Long,
    val weeklyCount: Int,
    val monthlyTotalPaise: Long,
    val monthlyCommissionPaise: Long,
    val monthlyCount: Int,
    val monthAvgPerDayPaise: Long,
    val monthBestDayPaise: Long,
    val topCustomers: List<CustomerVolume>,
    val last7DaysTrend: List<DailyTrendBucket>,
    val last6MonthsTrend: List<MonthlyBucket>
)

object AnalyticsEngine {

    fun computeInsights(collections: List<CollectionItem>): InsightsSummary {
        val realizedItems = collections.filter {
            it.status == CollectionStatus.RECEIPT_CONFIRMED || it.status == CollectionStatus.CONFIRMED
        }
        val pendingItems = collections.filter { it.status == CollectionStatus.PENDING }

        val cal = Calendar.getInstance()

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -6)
        val startOf7Days = cal.timeInMillis

        cal.timeInMillis = startOfToday
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val startOf30Days = cal.timeInMillis

        val todayRealized = realizedItems.filter { (it.receivedAt ?: it.createdAt) >= startOfToday }
        val todayTotal = todayRealized.sumOf { it.amountPaise }
        val todayCommission = todayRealized.sumOf { it.commissionPaise }
        val todayCount = todayRealized.size

        val todayPending = pendingItems.filter { it.createdAt >= startOfToday }
        val todayPendingPaise = todayPending.sumOf { it.amountPaise }
        val todayPendingCount = todayPending.size

        var weeklyTotal = 0L
        var weeklyCommission = 0L
        var weeklyCount = 0

        var monthlyTotal = 0L
        var monthlyCommission = 0L
        var monthlyCount = 0

        val customerMap = mutableMapOf<String, CustomerVolume>()

        for (item in realizedItems) {
            val itemTime = item.receivedAt ?: item.createdAt

            if (itemTime >= startOf7Days) {
                weeklyTotal += item.amountPaise
                weeklyCommission += item.commissionPaise
                weeklyCount++
            }

            if (itemTime >= startOf30Days) {
                monthlyTotal += item.amountPaise
                monthlyCommission += item.commissionPaise
                monthlyCount++
            }

            val key = item.customerDisplayName
            val current = customerMap[key]
            customerMap[key] = if (current == null) {
                CustomerVolume(
                    customerName = item.customerName,
                    customerAlias = item.customerAlias,
                    totalAmountPaise = item.amountPaise,
                    transactionCount = 1
                )
            } else {
                current.copy(
                    totalAmountPaise = current.totalAmountPaise + item.amountPaise,
                    transactionCount = current.transactionCount + 1
                )
            }
        }

        val topCustomers = customerMap.values
            .sortedByDescending { it.totalAmountPaise }
            .take(5)

        val trendBuckets = mutableListOf<DailyTrendBucket>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        for (i in 6 downTo 0) {
            val loopCal = Calendar.getInstance()
            loopCal.timeInMillis = startOfToday
            loopCal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = loopCal.timeInMillis
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L) - 1

            val dayItems = realizedItems.filter {
                val t = it.receivedAt ?: it.createdAt
                t in dayStart..dayEnd
            }

            trendBuckets.add(
                DailyTrendBucket(
                    dayLabel = if (i == 0) "Today" else dayFormat.format(Date(dayStart)),
                    dateLabel = dateFormat.format(Date(dayStart)),
                    totalPaise = dayItems.sumOf { it.amountPaise },
                    count = dayItems.size
                )
            )
        }

        val monthBuckets = mutableListOf<MonthlyBucket>()
        val monthLabelFormat = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 5 downTo 0) {
            val loopCal = Calendar.getInstance()
            loopCal.timeInMillis = startOfToday
            loopCal.add(Calendar.MONTH, -i)

            val monthStartCal = loopCal.clone() as Calendar
            monthStartCal.set(Calendar.DAY_OF_MONTH, 1)
            monthStartCal.set(Calendar.HOUR_OF_DAY, 0)
            monthStartCal.set(Calendar.MINUTE, 0)
            monthStartCal.set(Calendar.SECOND, 0)
            monthStartCal.set(Calendar.MILLISECOND, 0)
            val monthStart = monthStartCal.timeInMillis

            val monthEndCal = loopCal.clone() as Calendar
            monthEndCal.set(Calendar.DAY_OF_MONTH, loopCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            monthEndCal.set(Calendar.HOUR_OF_DAY, 23)
            monthEndCal.set(Calendar.MINUTE, 59)
            monthEndCal.set(Calendar.SECOND, 59)
            monthEndCal.set(Calendar.MILLISECOND, 999)
            val monthEnd = monthEndCal.timeInMillis

            val monthItems = realizedItems.filter {
                val t = it.receivedAt ?: it.createdAt
                t in monthStart..monthEnd
            }

            monthBuckets.add(
                MonthlyBucket(
                    monthLabel = monthLabelFormat.format(Date(monthStart)),
                    totalPaise = monthItems.sumOf { it.amountPaise },
                    commissionPaise = monthItems.sumOf { it.commissionPaise },
                    count = monthItems.size
                )
            )
        }

        val monthBestDay = trendBuckets.maxOfOrNull { it.totalPaise } ?: 0L
        val monthAvgPerDay = if (monthlyCount > 0) monthlyTotal / 30 else 0L

        return InsightsSummary(
            todayTotalPaise = todayTotal,
            todayCommissionPaise = todayCommission,
            todayCount = todayCount,
            todayPendingPaise = todayPendingPaise,
            todayPendingCount = todayPendingCount,
            weeklyTotalPaise = weeklyTotal,
            weeklyCommissionPaise = weeklyCommission,
            weeklyCount = weeklyCount,
            monthlyTotalPaise = monthlyTotal,
            monthlyCommissionPaise = monthlyCommission,
            monthlyCount = monthlyCount,
            monthAvgPerDayPaise = monthAvgPerDay,
            monthBestDayPaise = monthBestDay,
            topCustomers = topCustomers,
            last7DaysTrend = trendBuckets,
            last6MonthsTrend = monthBuckets
        )
    }
}