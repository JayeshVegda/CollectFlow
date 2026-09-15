package com.jayesh.cashcollect

import android.app.Application
import android.util.Log
import com.jayesh.cashcollect.data.backup.CsvExporter
import com.jayesh.cashcollect.data.backup.EncryptedBackupManager
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.data.repository.CustomerRepository
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.service.notification.AppNotificationManager
import com.jayesh.cashcollect.service.reminder.UnconfirmedReminderWorker
import com.jayesh.cashcollect.service.telegram.TelegramManager
import com.jayesh.cashcollect.widget.CashCollectWidgetProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CashCollectApplication : Application() {

    lateinit var database: AppDatabase private set
    lateinit var customerRepository: CustomerRepository private set
    lateinit var collectionRepository: CollectionRepository private set
    lateinit var settingsRepository: SettingsRepository private set
    lateinit var notificationManager: AppNotificationManager private set
    lateinit var backupManager: EncryptedBackupManager private set
    lateinit var csvExporter: CsvExporter private set
    lateinit var telegramManager: TelegramManager private set

    /**
     * Application-lifetime scope.
     *
     * A [CoroutineExceptionHandler] is attached because TDLib delivery callbacks can reference
     * entries that the operator has since confirmed, voided or deleted; throwing there would
     * otherwise crash the whole process.
     */
    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Unhandled background failure", throwable)
        }
    )

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        customerRepository = CustomerRepository(database.customerDao())
        collectionRepository = CollectionRepository(database)
        settingsRepository = SettingsRepository(database.settingsDao())
        notificationManager = AppNotificationManager(this)
        backupManager = EncryptedBackupManager(this, database)
        csvExporter = CsvExporter(this)
        telegramManager = TelegramManager(this)

        // Telegram confirmed real delivery -> move the entry to CONFIRMED.
        // Never fatal: the operator may already have confirmed, voided or deleted the entry.
        telegramManager.onMessageSendSucceeded = { collectionId ->
            appScope.launch {
                if (collectionId > 0L) {
                    runCatching { collectionRepository.confirmSentSafely(collectionId) }
                        .onFailure { Log.w(TAG, "Confirm after ACK failed for #$collectionId: ${it.message}") }
                }
                CashCollectWidgetProvider.notifyDataChanged(this@CashCollectApplication)
            }
        }

        // Telegram delivery failed -> never silent. Record it, notify, and let the operator fall
        // back to WhatsApp.
        telegramManager.onMessageSendFailed = { collectionId, error ->
            appScope.launch {
                if (collectionId > 0L) {
                    runCatching { collectionRepository.markDispatchFailed(collectionId, error) }
                        .onFailure { Log.w(TAG, "Recording dispatch failure failed for #$collectionId: ${it.message}") }
                    runCatching { notificationManager.showDispatchFailedNotification(collectionId, error) }
                        .onFailure { Log.w(TAG, "Dispatch-failure notification failed: ${it.message}") }
                }
                CashCollectWidgetProvider.notifyDataChanged(this@CashCollectApplication)
            }
        }

        // Reconcile + auto-connect Telegram in the background.
        appScope.launch {
            runCatching {
                val s = settingsRepository.getSettingsSync()
                val apiId = s.telegramApiId.trim().toIntOrNull() ?: 0
                if (s.telegramEnabled && apiId > 0 && s.telegramApiHash.isNotBlank()) {
                    telegramManager.start(apiId, s.telegramApiHash.trim(), logToFile = true)
                }
            }.onFailure { Log.e(TAG, "Telegram auto-start failed", it) }
        }

        // Schedule the periodic reminder check for unconfirmed collections.
        runCatching { UnconfirmedReminderWorker.schedule(this) }
            .onFailure { Log.e(TAG, "Failed to schedule periodic reminder worker", it) }
    }

    companion object {
        private const val TAG = "CashCollectApp"

        lateinit var instance: CashCollectApplication private set
    }
}
