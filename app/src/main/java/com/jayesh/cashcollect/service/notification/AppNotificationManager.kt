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

    fun showImmediateReceiptNotification(collection: CollectionItem) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_COLLECTION_ID", collection.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            collection.id.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Generic public notification for lock screen privacy
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Cash Collection")
            .setContentText("Receipt recorded. Tap to confirm WhatsApp dispatch.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Detailed notification for unlocked view
        val amountStr = Paise(collection.amountPaise).toFormattedRupees()
        val detailedNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Receipt recorded: $amountStr")
            .setContentText("Confirm WhatsApp sent for ${collection.customerDisplayName}")
            .setContentIntent(pendingIntent)
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
