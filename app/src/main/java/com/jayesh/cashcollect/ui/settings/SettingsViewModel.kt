package com.jayesh.cashcollect.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.backup.EncryptedBackupManager
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.widget.CashCollectWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val backupManager: EncryptedBackupManager
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
            // Coerced, not asserted: SettingsRepository rejects a negative rate with `require`,
            // and a failure inside viewModelScope.launch is an uncaught exception on the main
            // dispatcher — a crash, not a message. KeyboardType.Number does not stop every
            // keyboard from offering a minus sign, so the value is clamped here instead.
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
            // Same reasoning as [saveCommissionRate]: the repository asserts the bound, so the
            // UI's free-text field is clamped before it gets there rather than being allowed to
            // throw on the main dispatcher.
            settingsRepo.updateNotificationDelay(
                delayMs.coerceIn(0, AppSettings.MAX_NOTIFICATION_DELAY_MS)
            )
        }
    }

    /**
     * Restores the ledger from a backup file the operator chose.
     *
     * Reported through a Toast rather than a return value: the screen that started the work may
     * already be gone by the time the decrypt finishes, and the operator needs to know either way.
     * The widgets are refreshed because a restore can change every figure they show.
     */
    fun restoreBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            val outcome = backupManager.restoreFromUri(uri)
            CashCollectWidgetProvider.notifyDataChanged(context)
            val message = outcome.fold(
                onSuccess = { count -> "Restored $count entries from backup." },
                onFailure = { error -> "Restore failed: ${error.message ?: "unknown error"}" }
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    class Factory(
        private val settingsRepo: SettingsRepository,
        private val backupManager: EncryptedBackupManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepo, backupManager) as T
        }
    }
}