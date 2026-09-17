package com.jayesh.cashcollect.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jayesh.cashcollect.MainActivity
import com.jayesh.cashcollect.R
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise

class AppNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "receipt_reminders_channel"
        const val CHANNEL_NAME = "Receipt & Send Reminders"
        const val NOTIFICATION_ID_BASE = 1000

        /**
         * Extra read by MainActivity when the "YES, REPORTED" action is tapped. The action is
         * routed through the activity rather than a receiver so it lands on the same ViewModel
         * the screen is bound to: the row visibly moves to REPORTED instead of the database
         * changing behind the UI.
         */
        const val EXTRA_MARK_REPORTED_ID = "EXTRA_MARK_REPORTED_ID"

        private const val ACTION_MARK_REPORTED = "com.jayesh.cashcollect.action.MARK_REPORTED"
        private const val ACTION_REQUEST_BASE = 500_000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for cash collections awaiting WhatsApp confirmation"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posted the moment cash is committed from a swipe.
     *
     * The action button is the point of this notification: once the WhatsApp message has actually
     * gone out, one tap in the shade moves the entry to REPORTED, so the operator never has to
     * find the row again. Tapping the body opens that entry instead — the case where the message
     * never got sent and they want to send it again.
     */
    fun showImmediateReceiptNotification(collection: CollectionItem) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_COLLECTION_ID", collection.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            collection.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReportedIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_MARK_REPORTED
            putExtra(EXTRA_MARK_REPORTED_ID, collection.id)
        }
        val markReportedPendingIntent = PendingIntent.getActivity(
            context,
            ACTION_REQUEST_BASE + collection.id.toInt(),
            markReportedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = Paise(collection.amountPaise).toFormattedRupees()

        // Generic public notification for lock screen privacy
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Cash Collection")
            .setContentText("Receipt recorded. Tap to open.")
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .build()

        // Detailed notification for unlocked view, with the one-tap report action.
        val detailedNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reported $amountStr?")
            .setContentText(collection.customerDisplayName + " · cash in hand")
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "YES, REPORTED",
                markReportedPendingIntent
            )
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + collection.id.toInt(),
                detailedNotification
            )
        } catch (e: SecurityException) {
            // Notifications permission not granted on Android 13+
        }
    }

    /** Clears the receipt notification once its entry is reported, voided or deleted. */
    fun cancelReceiptNotification(collectionId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(
                NOTIFICATION_ID_BASE + collectionId.toInt()
            )
        }
    }

    fun showPendingNagNotification(unconfirmedCount: Int) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Outstanding Receipts")
            .setContentText("$unconfirmedCount cash collection(s) still need a WhatsApp receipt.")
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9999, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
