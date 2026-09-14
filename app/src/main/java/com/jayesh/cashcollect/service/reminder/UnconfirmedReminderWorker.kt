package com.jayesh.cashcollect.service.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.service.notification.AppNotificationManager
import java.util.concurrent.TimeUnit

class UnconfirmedReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val fifteenMinutesAgo = System.currentTimeMillis() - (15 * 60 * 1000L)

        val unconfirmedOlder = database.collectionDao().getUnconfirmedOlderThan(fifteenMinutesAgo)
        if (unconfirmedOlder.isNotEmpty()) {
            val notificationManager = AppNotificationManager(applicationContext)
            notificationManager.showPendingNagNotification(unconfirmedOlder.size)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "unconfirmed_collections_reminder_work"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<UnconfirmedReminderWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
