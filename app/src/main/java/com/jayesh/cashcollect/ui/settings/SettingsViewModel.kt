package com.jayesh.cashcollect.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus
import com.jayesh.cashcollect.service.telegram.TelegramAuthState
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
            val parsedId = apiId.toIntOrNull() ?: 0
            if (enabled && parsedId > 0 && apiHash.isNotBlank()) {
                telegramManager.start(parsedId, apiHash)
            }
        }
    }

    fun connectTelegram() {
        val s = settings.value
        val apiId = s.telegramApiId.toIntOrNull() ?: 0
        if (apiId > 0 && s.telegramApiHash.isNotBlank()) {
            telegramManager.start(apiId, s.telegramApiHash)
            telegramManager.requestQrCodeAuth()
        }
    }

    fun checkTelegramPassword(password: String) {
        telegramManager.checkPassword(password)
    }

    fun logoutTelegram() {
        telegramManager.logOut()
    }

    fun testSendTelegram(context: Context) {
        viewModelScope.launch {
            val s = settings.value
            val sample = CollectionItem(
                id = 999999L,
                customerId = 1L,
                customerDisplayName = "Test Customer",
                customerAlias = null,
                amountPaise = 500000L,
                commissionRateSnapshot = s.commissionRatePerThousand,
                commissionPaise = 15000L,
                status = CollectionStatus.CONFIRMED,
                createdAt = System.currentTimeMillis()
            )
            val res = telegramManager.sendCollectionReceipt(
                collection = sample,
                recipient = s.telegramRecipient,
                template = s.messageTemplate
            )
            if (res.isSuccess) {
                Toast.makeText(context, "Test receipt enqueued to Telegram!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Telegram test failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
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
