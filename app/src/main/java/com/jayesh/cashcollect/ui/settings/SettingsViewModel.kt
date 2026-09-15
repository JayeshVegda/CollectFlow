package com.jayesh.cashcollect.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.service.telegram.TelegramAuthState
import com.jayesh.cashcollect.service.telegram.TelegramConnection
import com.jayesh.cashcollect.service.telegram.TelegramManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val telegramManager: TelegramManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val telegramAuthState: StateFlow<TelegramAuthState> = telegramManager.authState

    val telegramConnection: StateFlow<TelegramConnection> = telegramManager.connection

    /** Raw TDLib log lines, shown on the in-app diagnostics screen. */
    val telegramDiagnostics: StateFlow<List<String>> = telegramManager.diagnostics

    /** Non-fatal error (e.g. wrong 2FA password) that must not destroy the dialog state. */
    val telegramTransientError: StateFlow<String?> = telegramManager.transientError

    fun saveBrotherNumber(number: String) {
        viewModelScope.launch {
            settingsRepo.updateBrotherWhatsAppNumber(number)
        }
    }

    fun saveCommissionRate(rate: Int) {
        viewModelScope.launch {
            settingsRepo.updateCommissionRate(rate)
        }
    }

    fun saveMessageTemplate(template: String) {
        viewModelScope.launch {
            settingsRepo.updateMessageTemplate(template)
        }
    }

    fun saveTelegramSettings(
        enabled: Boolean,
        apiId: String,
        apiHash: String,
        recipient: String,
        fallbackWhatsApp: Boolean
    ) {
        viewModelScope.launch {
            settingsRepo.updateTelegramSettings(
                enabled = enabled,
                apiId = apiId,
                apiHash = apiHash,
                recipient = recipient,
                fallbackWhatsApp = fallbackWhatsApp
            )
            val parsedId = apiId.trim().toIntOrNull() ?: 0
            if (enabled && parsedId > 0 && apiHash.isNotBlank()) {
                // Always on: TDLib's native log is capped at a few MB and is what makes failures
                // diagnosable from inside the app.
                telegramManager.start(parsedId, apiHash.trim(), logToFile = true)
            }
        }
    }

    /** Step 1: submit the account phone number. Telegram will then send a login code. */
    fun startTelegramLogin(phoneNumber: String) {
        if (!ensureEngineStarted()) return
        telegramManager.requestLogin(phoneNumber)
    }

    /** Step 2: submit the login code. */
    fun submitTelegramCode(code: String) {
        telegramManager.submitCode(code)
    }

    fun resendTelegramCode() {
        telegramManager.resendCode()
    }

    /** Step 3: submit the 2FA cloud password. */
    fun submitTelegramPassword(password: String) {
        telegramManager.submitPassword(password)
    }

    fun clearTelegramTransientError() {
        telegramManager.clearTransientError()
    }

    fun restartTelegramEngine() {
        val s = settings.value
        val apiId = s.telegramApiId.trim().toIntOrNull() ?: 0
        if (apiId <= 0 || s.telegramApiHash.isBlank()) {
            return
        }
        telegramManager.restart(logToFile = true)
    }

    fun shutdownTelegramEngine() {
        telegramManager.shutdown()
    }

    fun logoutTelegram() {
        telegramManager.logOut()
    }

    fun clearTelegramDiagnostics() {
        telegramManager.clearDiagnostics()
    }

    /** Ensures the native engine exists before a login step is attempted. */
    private fun ensureEngineStarted(): Boolean {
        if (telegramManager.isReady() || telegramManager.connection.value == TelegramConnection.Online) {
            return true
        }
        val s = settings.value
        val apiId = s.telegramApiId.trim().toIntOrNull() ?: 0
        if (apiId <= 0 || s.telegramApiHash.isBlank()) {
            return false
        }
        telegramManager.start(apiId, s.telegramApiHash.trim(), logToFile = true)
        return true
    }

    /**
     * Sends a real receipt to the configured recipient for verification.
     *
     * The sample uses id 0 so that the delivery ACK never touches the collection database.
     */
    fun testSendTelegram(context: Context) {
        viewModelScope.launch {
            if (!ensureEngineStarted()) {
                Toast.makeText(
                    context,
                    "Add your API ID and API HASH from my.telegram.org first.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val s = settingsRepo.getSettingsSync()
            if (s.telegramRecipient.isBlank()) {
                Toast.makeText(context, "Set a recipient username or phone number first.", Toast.LENGTH_LONG).show()
                return@launch
            }
            val sample = CollectionItem(
                id = 0L,
                customerId = 0L,
                customerName = "TEST ENTRY",
                customerAlias = null,
                amountPaise = 500000L,
                commissionRateSnapshot = s.commissionRatePerThousand,
                commissionPaise = CommissionCalculator.calculate(
                    500000L,
                    s.commissionRatePerThousand
                ),
                status = CollectionStatus.CONFIRMED,
                createdAt = System.currentTimeMillis(),
                receivedAt = System.currentTimeMillis()
            )
            val res = telegramManager.sendCollectionReceipt(
                collection = sample,
                recipient = s.telegramRecipient,
                template = s.messageTemplate
            )
            if (res.isSuccess) {
                Toast.makeText(context, "Test receipt queued to Telegram ⚡", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    "Telegram test failed: ${res.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    class Factory(
        private val settingsRepo: SettingsRepository,
        private val telegramManager: TelegramManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepo, telegramManager) as T
        }
    }
}
