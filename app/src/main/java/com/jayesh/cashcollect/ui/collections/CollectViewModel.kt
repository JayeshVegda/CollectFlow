package com.jayesh.cashcollect.ui.collections

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.data.repository.CustomerRepository
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.service.notification.AppNotificationManager
import com.jayesh.cashcollect.service.whatsapp.WhatsAppLauncher
import com.jayesh.cashcollect.widget.CashCollectWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectViewModel(
    private val collectionRepo: CollectionRepository,
    private val customerRepo: CustomerRepository,
    private val settingsRepo: SettingsRepository,
    private val notificationManager: AppNotificationManager,
    private val telegramManager: com.jayesh.cashcollect.service.telegram.TelegramManager
) : ViewModel() {

    val outstandingList: StateFlow<List<CollectionItem>> = collectionRepo.getOutstandingConfirmations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingList: StateFlow<List<CollectionItem>> = collectionRepo.getPendingCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commissionRate: StateFlow<Int> = settingsRepo.getSettings()
        .map { it.commissionRatePerThousand }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    private val _isQuickCaptureOpen = MutableStateFlow(false)
    val isQuickCaptureOpen: StateFlow<Boolean> = _isQuickCaptureOpen.asStateFlow()

    private val _selectedItemForConfirm = MutableStateFlow<CollectionItem?>(null)
    val selectedItemForConfirm: StateFlow<CollectionItem?> = _selectedItemForConfirm.asStateFlow()

    private val _selectedItemForEdit = MutableStateFlow<CollectionItem?>(null)
    val selectedItemForEdit: StateFlow<CollectionItem?> = _selectedItemForEdit.asStateFlow()

    fun openQuickCapture() {
        _isQuickCaptureOpen.value = true
    }

    fun dismissQuickCapture() {
        _isQuickCaptureOpen.value = false
    }

    fun openConfirm(item: CollectionItem) {
        _selectedItemForConfirm.value = item
    }

    fun dismissConfirm() {
        _selectedItemForConfirm.value = null
    }

    fun openEdit(item: CollectionItem) {
        _selectedItemForEdit.value = item
    }

    fun dismissEdit() {
        _selectedItemForEdit.value = null
    }

    fun saveQuickCapture(context: Context, name: String, amountPaise: Long, note: String?) {
        viewModelScope.launch {
            val rate = commissionRate.value
            val customerId = customerRepo.addCustomer(name, null)
            collectionRepo.createPendingCollection(
                customerId = customerId,
                amountPaise = amountPaise,
                commissionRateSnapshot = rate,
                note = note
            )
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(context, "Saved: $name", Toast.LENGTH_SHORT).show()
        }
    }

    fun confirmReceive(context: Context, item: CollectionItem) {
        viewModelScope.launch {
            dismissConfirm()
            val committed = collectionRepo.markReceivedAndCommit(item.id)
            CashCollectWidgetProvider.notifyDataChanged(context)
            notificationManager.showImmediateReceiptNotification(committed)

            val settings = settingsRepo.getSettingsSync()
            if (settings.telegramEnabled && telegramManager.isReady()) {
                val res = telegramManager.sendCollectionReceipt(
                    collection = committed,
                    recipient = settings.telegramRecipient,
                    template = settings.messageTemplate
                )
                if (res.isSuccess) {
                    Toast.makeText(context, "Telegram auto-send queued ⚡", Toast.LENGTH_SHORT).show()
                    return@launch
                } else {
                    if (!settings.telegramFallbackWhatsApp) {
                        Toast.makeText(context, "Telegram error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }
            }

            // Fallback to WhatsApp
            openWhatsAppInternal(context, committed, settings)
        }
    }

    fun confirmReceiveAndOpenWhatsApp(context: Context, item: CollectionItem) {
        confirmReceive(context, item)
    }

    private suspend fun openWhatsAppInternal(context: Context, item: CollectionItem, settings: com.jayesh.cashcollect.domain.model.AppSettings) {
        val msg = WhatsAppLauncher.buildReceiptMessage(item, settings.messageTemplate)
        val intent = WhatsAppLauncher.createSendIntent(context, settings.brotherWhatsAppNumber, msg)
        collectionRepo.logWhatsAppOpened(item.id)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp. Receipt is saved.", Toast.LENGTH_LONG).show()
        }
    }

    fun openWhatsAppAgain(context: Context, item: CollectionItem) {
        viewModelScope.launch {
            collectionRepo.logWhatsAppOpened(item.id)
            val settings = settingsRepo.getSettingsSync()
            val msg = WhatsAppLauncher.buildReceiptMessage(item, settings.messageTemplate)
            val intent = WhatsAppLauncher.createSendIntent(context, settings.brotherWhatsAppNumber, msg)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun confirmSent(context: Context, id: Long) {
        viewModelScope.launch {
            collectionRepo.confirmSent(id)
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(context, "Confirmed Sent", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteCollection(context: Context, id: Long) {
        viewModelScope.launch {
            collectionRepo.deleteCollection(id)
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateCollection(context: Context, id: Long, amountPaise: Long, note: String?) {
        viewModelScope.launch {
            collectionRepo.updateCollection(id, amountPaise, note)
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(context, "Entry updated", Toast.LENGTH_SHORT).show()
        }
    }

    fun voidAndReplace(
        context: Context,
        originalId: Long,
        reason: String,
        newAmountPaise: Long,
        note: String?,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val rate = commissionRate.value
            val newId = collectionRepo.voidAndReplace(originalId, reason, newAmountPaise, rate, note)
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(context, "Voided & replaced with #$newId", Toast.LENGTH_SHORT).show()
            onSuccess(newId)
        }
    }

    class Factory(
        private val collectionRepo: CollectionRepository,
        private val customerRepo: CustomerRepository,
        private val settingsRepo: SettingsRepository,
        private val notificationManager: AppNotificationManager,
        private val telegramManager: com.jayesh.cashcollect.service.telegram.TelegramManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CollectViewModel(collectionRepo, customerRepo, settingsRepo, notificationManager, telegramManager) as T
        }
    }
}
