package com.jayesh.cashcollect

import android.app.Application
import com.jayesh.cashcollect.data.backup.CsvExporter
import com.jayesh.cashcollect.data.backup.EncryptedBackupManager
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.data.repository.CustomerRepository
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.service.notification.AppNotificationManager
import com.jayesh.cashcollect.service.reminder.UnconfirmedReminderWorker

class CashCollectApplication : Application() {

    lateinit var database: AppDatabase private set
    lateinit var customerRepository: CustomerRepository private set
    lateinit var collectionRepository: CollectionRepository private set
    lateinit var settingsRepository: SettingsRepository private set
    lateinit var notificationManager: AppNotificationManager private set
    lateinit var backupManager: EncryptedBackupManager private set
    lateinit var csvExporter: CsvExporter private set
    lateinit var telegramManager: com.jayesh.cashcollect.service.telegram.TelegramManager private set

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
        telegramManager = com.jayesh.cashcollect.service.telegram.TelegramManager(this)

        // Listen for TDLib message delivery confirmation to mark Room record CONFIRMED
        telegramManager.onMessageSendSucceeded = { collectionId ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                collectionRepository.confirmSent(collectionId)
                com.jayesh.cashcollect.widget.CashCollectWidgetProvider.notifyDataChanged(this@CashCollectApplication)
            }
        }

        // Auto-connect Telegram if enabled and configured
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val s = settingsRepository.getSettingsSync()
            val apiId = s.telegramApiId.toIntOrNull() ?: 0
            if (s.telegramEnabled && apiId > 0 && s.telegramApiHash.isNotBlank()) {
                telegramManager.start(apiId, s.telegramApiHash)
            }
        }

        // Schedule periodic reminder check for unconfirmed collections safely
        try {
            UnconfirmedReminderWorker.schedule(this)
        } catch (e: Throwable) {
            android.util.Log.e("CashCollectApp", "Failed to schedule periodic reminder worker", e)
        }
    }

    companion object {
        lateinit var instance: CashCollectApplication private set
    }
}
