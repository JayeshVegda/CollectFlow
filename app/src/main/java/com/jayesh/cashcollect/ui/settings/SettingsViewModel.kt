package com.jayesh.cashcollect.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.backup.CsvExporter
import com.jayesh.cashcollect.data.backup.CsvImporter
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.widget.CashCollectWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val collectionRepo: CollectionRepository,
    private val csvExporter: CsvExporter,
    private val csvImporter: CsvImporter
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun saveBrotherNumber(number: String) {
        viewModelScope.launch {
            settingsRepo.updateBrotherWhatsAppNumber(number)
        }
    }

    fun saveCommissionRate(rate: Int) {
        viewModelScope.launch {
            settingsRepo.updateCommissionRate(rate.coerceAtLeast(0))
        }
    }

    fun saveMessageTemplate(template: String) {
        viewModelScope.launch {
            settingsRepo.updateMessageTemplate(template)
        }
    }

    fun saveNotificationDelay(delayMs: Int) {
        viewModelScope.launch {
            settingsRepo.updateNotificationDelay(
                delayMs.coerceIn(0, AppSettings.MAX_NOTIFICATION_DELAY_MS)
            )
        }
    }

    fun exportCsvBackup(context: Context) {
        viewModelScope.launch {
            try {
                val allItems = collectionRepo.getAllHistory().first()
                val csvFile = csvExporter.exportHistoryToCsv(allItems)
                settingsRepo.updateLastBackupTime(System.currentTimeMillis())
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export CSV Ledger"))
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun restoreFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    Toast.makeText(context, "Unable to read selected file", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val result = csvImporter.importFromStream(stream, replaceExisting = true)
                settingsRepo.updateLastBackupTime(System.currentTimeMillis())
                CashCollectWidgetProvider.notifyDataChanged(context)
                if (result.errors.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        "Restored ${result.collectionsImported} entries (${result.errors.size} warnings)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Successfully restored ${result.collectionsImported} entries for ${result.customersCreated} parties.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    class Factory(
        private val settingsRepo: SettingsRepository,
        private val collectionRepo: CollectionRepository,
        private val csvExporter: CsvExporter,
        private val csvImporter: CsvImporter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepo, collectionRepo, csvExporter, csvImporter) as T
        }
    }
}