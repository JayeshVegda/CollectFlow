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
import com.jayesh.cashcollect.domain.money.Paise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CashCollectTrackerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CashCollectTrackerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                // Collected cash only, over the one shared definition of "today".
                val snapshot = loadWidgetSnapshot(context) ?: return@launch

                val amountFormatted = Paise(snapshot.todayTotalPaise).toFormattedRupees()
                val commFormatted = "COMM: " + Paise(snapshot.todayCommissionPaise).toFormattedRupees()
                val countFormatted = if (snapshot.todayCount == 1) {
                    "1 ENTRY"
                } else {
                    "${snapshot.todayCount} ENTRIES"
                }

                val unconfirmedText = if (snapshot.awaitingReportCount > 0) {
                    "⚠️ ${snapshot.awaitingReportCount} UNSENT RECEIPTS"
                } else {
                    "✓ ALL CONFIRMED"
                }
                val unconfirmedColor = if (snapshot.awaitingReportCount > 0) {
                    0xFFD4A843.toInt() // Nothing Amber
                } else {
                    0xFF4A9E5C.toInt() // Nothing Green
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_cash_collect_wide).apply {
                        setTextViewText(R.id.widget_wide_today_amount, amountFormatted)
                        setTextViewText(R.id.widget_wide_commission, commFormatted)
                        setTextViewText(R.id.widget_wide_count, countFormatted)
                        setTextViewText(R.id.widget_wide_status, unconfirmedText)
                        setTextColor(R.id.widget_wide_status, unconfirmedColor)

                        // Tap whole container opens app
                        val openAppIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val openAppPendingIntent = PendingIntent.getActivity(
                            context,
                            20,
                            openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_wide_container, openAppPendingIntent)

                        // Open button
                        val openFeedIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val openFeedPendingIntent = PendingIntent.getActivity(
                            context,
                            21,
                            openFeedIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_wide_btn_open, openFeedPendingIntent)

                        // Tap + Quick opens Quick Capture modal directly
                        val openQuickIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("EXTRA_OPEN_QUICK_CAPTURE", true)
                        }
                        val openQuickPendingIntent = PendingIntent.getActivity(
                            context,
                            22,
                            openQuickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_wide_btn_quick, openQuickPendingIntent)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
