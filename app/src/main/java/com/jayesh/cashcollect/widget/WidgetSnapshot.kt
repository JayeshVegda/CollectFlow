package com.jayesh.cashcollect.widget

import android.content.Context
import com.jayesh.cashcollect.data.local.AppDatabase
import java.util.Calendar

/**
 * The figures every widget shows, loaded in one place.
 *
 * Totals are calculated directly inside SQLite via optimized index queries.
 */
internal data class WidgetSnapshot(
    val todayTotalPaise: Long,
    val todayCommissionPaise: Long,
    val todayCount: Int,
    val awaitingReportCount: Int,
    val pendingTotalPaise: Long,
    val pendingCount: Int,
    val pendingPartyCount: Int
)

/**
 * Loads the snapshot, or null when the database cannot be read.
 */
internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot? = runCatching {
    val dao = AppDatabase.getInstance(context).collectionDao()
    val totals = dao.realizedTotalsSince(startOfToday())
    val pending = dao.pendingTotals()
    WidgetSnapshot(
        todayTotalPaise = totals.totalPaise,
        todayCommissionPaise = totals.commissionPaise,
        todayCount = totals.entryCount,
        awaitingReportCount = dao.countAwaitingReport(),
        pendingTotalPaise = pending.totalPaise,
        pendingCount = pending.entryCount,
        pendingPartyCount = pending.partyCount
    )
}.getOrNull()

/** Local midnight in the device's own time zone — the same window the Collect screen uses. */
internal fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
