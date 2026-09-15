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

    companion object {
        /** Automatic reason stored when an entry is voided with a single swipe (no dialog). */
        private const val VOID_REASON_QUICK = "Quick swipe void (no reason given)"
    }

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

    /**
     * Swipe-right action: commit the cash receipt immediately — no confirmation sheet.
     *
     * - Telegram enabled + online  -> background send; the ACK later marks the entry CONFIRMED.
     * - Telegram not ready/disabled -> WhatsApp opens immediately with the prefilled receipt.
     * - Telegram failed             -> the failure is stored on the entry and the row offers
     *                                  WhatsApp / retry, so a receipt is never silently lost.
     */
    fun confirmReceive(context: Context, item: CollectionItem) {
        viewModelScope.launch {
            val committed = runCatching { collectionRepo.markReceivedAndCommit(item.id) }
                .getOrElse { error ->
                    Toast.makeText(context, "Could not confirm: ${error.message}", Toast.LENGTH_LONG).show()
                    return@launch
                }
            dismissConfirm()
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
                    Toast.makeText(context, "Telegram sending in background", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val reason = res.exceptionOrNull()?.message ?: "Telegram send failed"
                collectionRepo.markDispatchFailed(committed.id, reason)
                CashCollectWidgetProvider.notifyDataChanged(context)
                if (!settings.telegramFallbackWhatsApp) {
                    Toast.makeText(context, "Telegram failed: $reason", Toast.LENGTH_LONG).show()
                    return@launch
                }
                Toast.makeText(context, "Telegram failed — opening WhatsApp", Toast.LENGTH_SHORT).show()
            }

            openWhatsAppInternal(context, committed, settings)
        }
    }

    fun confirmReceiveAndOpenWhatsApp(context: Context, item: CollectionItem) {
        confirmReceive(context, item)
    }

    /** Bulk action: commit several entries at once and dispatch them all. */
    fun receiveAndSendAll(context: Context, items: List<CollectionItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val settings = settingsRepo.getSettingsSync()
            var telegramQueued = 0
            var whatsAppNeeded = 0
            var failed = 0

            for (item in items) {
                val committed = runCatching { collectionRepo.markReceivedAndCommit(item.id) }.getOrNull()
                if (committed == null) {
                    failed++
                    continue
                }

                if (settings.telegramEnabled && telegramManager.isReady()) {
                    val res = telegramManager.sendCollectionReceipt(
                        collection = committed,
                        recipient = settings.telegramRecipient,
                        template = settings.messageTemplate
                    )
                    if (res.isSuccess) {
                        telegramQueued++
                        continue
                    }
                    val reason = res.exceptionOrNull()?.message ?: "Telegram send failed"
                    collectionRepo.markDispatchFailed(committed.id, reason)
                }
                whatsAppNeeded++
            }

            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(
                context,
                "Telegram: $telegramQueued queued, $whatsAppNeeded need WhatsApp" +
                    if (failed > 0) ", $failed failed" else "",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Bulk confirm for entries that the operator sent manually via WhatsApp. */
    fun markAllSent(context: Context, items: List<CollectionItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            var done = 0
            for (item in items) {
                runCatching { collectionRepo.confirmSentSafely(item.id) }.onSuccess { done++ }
            }
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(
                context,
                "$done entr${if (done == 1) "y" else "ies"} confirmed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Retries the Telegram dispatch for an entry whose previous send failed. */
    fun retryTelegram(context: Context, item: CollectionItem) {
        viewModelScope.launch {
            val settings = settingsRepo.getSettingsSync()
            if (!settings.telegramEnabled || !telegramManager.isReady()) {
                Toast.makeText(context, "Telegram is not online right now", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val res = telegramManager.sendCollectionReceipt(
                collection = item,
                recipient = settings.telegramRecipient,
                template = settings.messageTemplate
            )
            if (res.isSuccess) {
                collectionRepo.markDispatchFailed(item.id, "")
                Toast.makeText(context, "Retrying Telegram send", Toast.LENGTH_SHORT).show()
            } else {
                val reason = res.exceptionOrNull()?.message ?: "Telegram send failed"
                collectionRepo.markDispatchFailed(item.id, reason)
                Toast.makeText(context, "Still failing: $reason", Toast.LENGTH_LONG).show()
            }
            CashCollectWidgetProvider.notifyDataChanged(context)
        }
    }

    /**
     * Swipe-left action: instant void — no dialog, no undo. The entry is preserved as VOIDED
     * (never hard-deleted) so the audit trail and the totals stay correct.
     */
    fun voidInstantly(context: Context, item: CollectionItem) {
        viewModelScope.launch {
            runCatching { collectionRepo.voidCollection(item.id, VOID_REASON_QUICK) }
                .onFailure {
                    Toast.makeText(context, "Could not void: ${it.message}", Toast.LENGTH_LONG).show()
                }
                .onSuccess {
                    CashCollectWidgetProvider.notifyDataChanged(context)
                    Toast.makeText(context, "Voided ${item.customerDisplayName}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private suspend fun openWhatsAppInternal(context: Context, item: CollectionItem, settings: com.jayesh.cashcollect.domain.model.AppSettings) {
        if (settings.brotherWhatsAppNumber.isBlank()) {
            Toast.makeText(
                context,
                "Set the WhatsApp recipient number in Settings first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
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
            runCatching { collectionRepo.confirmSentSafely(id) }
                .onFailure {
                    Toast.makeText(context, "Could not confirm: ${it.message}", Toast.LENGTH_LONG).show()
                }
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(context, "Confirmed sent", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteCollection(context: Context, id: Long) {
        viewModelScope.launch {
            runCatching { collectionRepo.deleteCollection(id) }
                .onFailure {
                    Toast.makeText(context, it.message ?: "Could not delete", Toast.LENGTH_LONG).show()
                }
                .onSuccess {
                    CashCollectWidgetProvider.notifyDataChanged(context)
                    Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun updateCollection(context: Context, id: Long, amountPaise: Long, note: String?) {
        viewModelScope.launch {
            runCatching { collectionRepo.updateCollection(id, amountPaise, note) }
                .onFailure {
                    Toast.makeText(context, it.message ?: "Could not update", Toast.LENGTH_LONG).show()
                }
                .onSuccess {
                    CashCollectWidgetProvider.notifyDataChanged(context)
                    Toast.makeText(context, "Entry updated", Toast.LENGTH_SHORT).show()
                }
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
            runCatching {
                collectionRepo.voidAndReplace(originalId, reason, newAmountPaise, rate, note)
            }.onSuccess { newId ->
                CashCollectWidgetProvider.notifyDataChanged(context)
                Toast.makeText(context, "Voided & replaced with #$newId", Toast.LENGTH_SHORT).show()
                onSuccess(newId)
            }.onFailure {
                Toast.makeText(context, it.message ?: "Could not replace", Toast.LENGTH_LONG).show()
            }
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
