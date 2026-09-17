package com.jayesh.cashcollect

import android.app.Application
import android.util.Log
import com.jayesh.cashcollect.data.backup.CsvExporter
import com.jayesh.cashcollect.data.backup.EncryptedBackupManager
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.local.SampleHistory
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.data.repository.CustomerRepository
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.service.notification.AppNotificationManager
import com.jayesh.cashcollect.service.reminder.UnconfirmedReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CashCollectApplication : Application() {

    lateinit var database: AppDatabase private set
    lateinit var customerRepository: CustomerRepository private set
    lateinit var collectionRepository: CollectionRepository private set
    lateinit var settingsRepository: SettingsRepository private set
    lateinit var notificationManager: AppNotificationManager private set
    lateinit var backupManager: EncryptedBackupManager private set
    lateinit var csvExporter: CsvExporter private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (isCrashProcess()) {
            return
        }

        com.jayesh.cashcollect.crash.CrashHandler.install(this)

        database = AppDatabase.getInstance(this)
        customerRepository = CustomerRepository(database.customerDao())
        collectionRepository = CollectionRepository(database)
        settingsRepository = SettingsRepository(database.settingsDao())
        notificationManager = AppNotificationManager(this)
        backupManager = EncryptedBackupManager(this, database)
        csvExporter = CsvExporter(this)

        // Pre-filled operating history (see SampleHistory). Fire-and-forget and failure-tolerant:
        // seeding must never delay or crash startup, and it no-ops once the operator has data.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                SampleHistory.seedIfEmpty(
                    database = database,
                    commissionRatePerThousand = settingsRepository.getSettingsSync()
                        .commissionRatePerThousand
                )
            }.onFailure { Log.e(TAG, "Failed to seed sample history", it) }
        }

        runCatching { UnconfirmedReminderWorker.schedule(this) }
            .onFailure { Log.e(TAG, "Failed to schedule periodic reminder worker", it) }
    }

    private fun isCrashProcess(): Boolean {
        val processName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            runCatching {
                val pid = android.os.Process.myPid()
                val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                am?.runningAppProcesses?.find { it.pid == pid }?.processName
            }.getOrNull()
        }
        return processName?.endsWith(":crash") == true
    }

    companion object {
        private const val TAG = "CashCollectApp"

        lateinit var instance: CashCollectApplication private set
    }
}