package com.jayesh.cashcollect.ui.collections

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.data.repository.CustomerRepository
import com.jayesh.cashcollect.data.repository.SettingsRepository
import com.jayesh.cashcollect.domain.model.AppSettings
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

import com.jayesh.cashcollect.domain.state.CollectionStatus
import java.util.Calendar

data class TodayStats(
    val totalPaise: Long = 0L,
    val commissionPaise: Long = 0L,
    val count: Int = 0,
    val pendingPaise: Long = 0L,
    val pendingCount: Int = 0
)

class CollectViewModel(
    private val collectionRepo: CollectionRepository,
    private val customerRepo: CustomerRepository,
    private val settingsRepo: SettingsRepository,
    private val notificationManager: AppNotificationManager
) : ViewModel() {

    companion object {
        private const val VOID_REASON_QUICK = "Quick swipe void (no reason given)"
    }

    val todayStats: StateFlow<TodayStats> = collectionRepo.getAllHistory()
        .map { list ->
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayRealized = list.filter {
                (it.status == CollectionStatus.RECEIPT_CONFIRMED || it.status == CollectionStatus.CONFIRMED) &&
                    (it.receivedAt ?: it.createdAt) >= todayStart
            }
            val todayPending = list.filter {
                it.status == CollectionStatus.PENDING && it.createdAt >= todayStart
            }

            TodayStats(
                totalPaise = todayRealized.sumOf { it.amountPaise },
                commissionPaise = todayRealized.sumOf { it.commissionPaise },
                count = todayRealized.size,
                pendingPaise = todayPending.sumOf { it.amountPaise },
                pendingCount = todayPending.size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayStats())

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
     * Swipe-right action: commit the cash receipt immediately, then open WhatsApp prefilled.
     * The receipt is persisted before WhatsApp is launched, so nothing is ever lost.
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
            openWhatsAppInternal(context, committed, settings)
        }
    }

    fun confirmReceiveAndOpenWhatsApp(context: Context, item: CollectionItem) {
        confirmReceive(context, item)
    }

    /** Bulk action: commit several entries at once and open WhatsApp for each. */
    fun receiveAndSendAll(context: Context, items: List<CollectionItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val settings = settingsRepo.getSettingsSync()
            var done = 0
            for (item in items) {
                val committed = runCatching { collectionRepo.markReceivedAndCommit(item.id) }.getOrNull()
                if (committed == null) continue
                done++
                openWhatsAppInternal(context, committed, settings)
            }
            CashCollectWidgetProvider.notifyDataChanged(context)
            Toast.makeText(context, "$done receipt(s) recorded", Toast.LENGTH_LONG).show()
        }
    }

    /** Bulk confirm for entries the operator already sent manually. */
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

    /** Swipe-left action: instant void. The entry is preserved as VOIDED. */
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

    private suspend fun openWhatsAppInternal(context: Context, item: CollectionItem, settings: AppSettings) {
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
        private val notificationManager: AppNotificationManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CollectViewModel(collectionRepo, customerRepo, settingsRepo, notificationManager) as T
        }
    }
}