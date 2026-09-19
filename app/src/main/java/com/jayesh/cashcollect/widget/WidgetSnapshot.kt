package com.jayesh.cashcollect.widget

import android.content.Context
import com.jayesh.cashcollect.data.local.AppDatabase
import java.util.Calendar

/**
 * The figures every widget shows, loaded in one place.
 *
 * All three providers previously duplicated this work: compute local midnight, load the entire
 * `collections` table, filter in Kotlin, total in Kotlin. That triplicated the definition of
 * "today" — the one definition a daily figure must not have three of — and made every widget
 * refresh O(entire history). The aggregation now happens in SQLite, once, and the three providers
 * share it.
 */
internal data class WidgetSnapshot(
    val todayTotalPaise: Long,
    val todayCommissionPaise: Long,
    val todayCount: Int,
    val awaitingReportCount: Int
)

/**
 * Loads the snapshot, or null when the database cannot be read.
 *
 * Null rather than an exception: a widget update runs inside the launcher's process, so a failure
 * here must leave the previous widget content alone rather than take the home screen down.
 */
internal suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot? = runCatching {
    val dao = AppDatabase.getInstance(context).collectionDao()
    val totals = dao.realizedTotalsSince(startOfToday())
    WidgetSnapshot(
        todayTotalPaise = totals.totalPaise,
        todayCommissionPaise = totals.commissionPaise,
        todayCount = totals.entryCount,
        awaitingReportCount = dao.countAwaitingReport()
    )
}.getOrNull()

/** Local midnight in the device's own time zone — the same window the Collect screen uses. */
internal fun startOfToday(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
