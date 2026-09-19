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
            CashCollectMiniWidgetProvider.notifyDataChanged(context)
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                // Collected cash only: RECEIPT_CONFIRMED or CONFIRMED. PENDING entries are promises,
                // not money in hand — counting them inflated the widget total. The window and the
                // statuses are now defined once, in loadWidgetSnapshot.
                val snapshot = loadWidgetSnapshot(context) ?: return@launch

                val amountFormatted = Paise(snapshot.todayTotalPaise).toFormattedRupees()

                val unconfirmedText = if (snapshot.awaitingReportCount > 0) {
                    "⚠️ ${snapshot.awaitingReportCount} UNSENT"
                } else {
                    "✓ ALL CONFIRMED"
                }
                val unconfirmedColor = if (snapshot.awaitingReportCount > 0) {
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
