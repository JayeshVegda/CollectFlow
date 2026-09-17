package com.jayesh.cashcollect.ui.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jayesh.cashcollect.data.repository.CollectionRepository
import com.jayesh.cashcollect.data.repository.CustomerRepository
import com.jayesh.cashcollect.domain.party.PartyLedger
import com.jayesh.cashcollect.domain.party.buildPartyLedger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * One party's ledger.
 *
 * Deliberately read-only: tapping a row goes to the entry, where every action already lives. That
 * keeps exactly one place in the app able to change an entry, and it means this page has no way to
 * get out of step with the queue.
 */
class PartyLedgerViewModel(
    customerId: Long,
    collectionRepo: CollectionRepository,
    customerRepo: CustomerRepository
) : ViewModel() {

    val ledger: StateFlow<PartyLedger> = combine(
        customerRepo.observeCustomer(customerId),
        collectionRepo.getCollectionsForCustomer(customerId)
    ) { customer, entries ->
        buildPartyLedger(customer, entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PartyLedger())

    class Factory(
        private val customerId: Long,
        private val collectionRepo: CollectionRepository,
        private val customerRepo: CustomerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PartyLedgerViewModel(customerId, collectionRepo, customerRepo) as T
        }
    }
}