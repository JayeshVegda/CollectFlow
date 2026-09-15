package com.jayesh.cashcollect.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jayesh.cashcollect.MainActivity
import com.jayesh.cashcollect.R
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class CashCollectWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CashCollectWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
            CashCollectTrackerWidgetProvider.notifyDataChanged(context)
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(context)
                val allCollections = db.collectionDao().getAllSync()

                // Calculate today's total
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = cal.timeInMillis

                val validItems = allCollections.filter { it.status != CollectionStatus.VOIDED.name }
                // Collected cash only: RECEIPT_CONFIRMED or CONFIRMED. PENDING entries are
                // promises, not money in hand — counting them inflated the widget total.
                val todayTotalPaise = validItems.filter {
                    (it.status == CollectionStatus.RECEIPT_CONFIRMED.name ||
                        it.status == CollectionStatus.CONFIRMED.name) &&
                        (it.receivedAt ?: it.createdAt) >= startOfToday
                }.sumOf { it.amountPaise }

                val unconfirmedCount = validItems.count {
                    it.status == CollectionStatus.RECEIPT_CONFIRMED.name
                }

                val amountFormatted = Paise(todayTotalPaise).toFormattedRupees()

                val unconfirmedText = if (unconfirmedCount > 0) {
                    "⚠️ $unconfirmedCount UNSENT"
                } else {
                    "✓ ALL CONFIRMED"
                }
                val unconfirmedColor = if (unconfirmedCount > 0) {
                    0xFFD4A843.toInt() // Nothing Amber
                } else {
                    0xFF4A9E5C.toInt() // Nothing Green
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_cash_collect).apply {
                        setTextViewText(R.id.widget_today_amount, amountFormatted)
                        setTextViewText(R.id.widget_unconfirmed_status, unconfirmedText)
                        setTextColor(R.id.widget_unconfirmed_status, unconfirmedColor)

                        // Tap whole container opens app
                        val openAppIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val openAppPendingIntent = PendingIntent.getActivity(
                            context,
                            0,
                            openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

                        // Tap + Quick opens Quick Capture modal directly
                        val openQuickIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("EXTRA_OPEN_QUICK_CAPTURE", true)
                        }
                        val openQuickPendingIntent = PendingIntent.getActivity(
                            context,
                            1,
                            openQuickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_btn_add, openQuickPendingIntent)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
