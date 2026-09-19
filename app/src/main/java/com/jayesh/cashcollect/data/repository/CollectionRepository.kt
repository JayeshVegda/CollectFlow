package com.jayesh.cashcollect.data.repository

import androidx.room.withTransaction
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.state.CollectionStateMachine
import com.jayesh.cashcollect.domain.state.CollectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Every read flow maps to domain objects on a background dispatcher.
 *
 * `stateIn(viewModelScope, ...)` collects on the main dispatcher, so `map { it.toDomain() }` — which
 * allocates one domain object per row — ran on the main thread for every row of history, on every
 * database write. `flowOn` moves the query and the mapping off the frame-drawing thread.
 */
class CollectionRepository(
    private val database: AppDatabase
) {
    private val collectionDao = database.collectionDao()
    private val customerDao = database.customerDao()

    fun getOutstandingConfirmations(): Flow<List<CollectionItem>> {
        return collectionDao.getOutstandingConfirmations()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    fun getPendingCollections(): Flow<List<CollectionItem>> {
        return collectionDao.getPendingCollections()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    fun getAllHistory(): Flow<List<CollectionItem>> {
        return collectionDao.getAllHistory()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    /** Every entry for one party, newest first — the party ledger's list. */
    fun getCollectionsForCustomer(customerId: Long): Flow<List<CollectionItem>> {
        return collectionDao.getByCustomer(customerId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    suspend fun getCollectionById(id: Long): CollectionItem? {
        return collectionDao.getWithCustomerById(id)?.toDomain()
    }

    suspend fun checkRecentDuplicate(customerId: Long, amountPaise: Long, withinMinutes: Int = 30): Boolean {
        val since = System.currentTimeMillis() - (withinMinutes * 60 * 1000L)
        val count = collectionDao.countRecentDuplicates(customerId, amountPaise, since)
        return count > 0
    }

    suspend fun createPendingCollection(
        customerId: Long,
        amountPaise: Long,
        commissionRateSnapshot: Int,
        replacesId: Long? = null,
        note: String? = null
    ): Long {
        require(amountPaise > 0L) { "Amount in paise must be positive: $amountPaise" }
        require(commissionRateSnapshot >= 0) { "Commission rate cannot be negative" }

        val commissionPaise = CommissionCalculator.calculate(amountPaise, commissionRateSnapshot)
        val now = System.currentTimeMillis()

        return database.withTransaction {
            val entity = CollectionEntity(
                customerId = customerId,
                amountPaise = amountPaise,
                commissionRateSnapshot = commissionRateSnapshot,
                commissionPaise = commissionPaise,
                status = CollectionStatus.PENDING.name,
                createdAt = now,
                replacesId = replacesId,
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )
            val newId = collectionDao.insert(entity)
            customerDao.updateLastUsed(customerId, now)
            newId
        }
    }

    /**
     * Non-negotiable: persists receipt state to Room BEFORE any WhatsApp intent fires.
     *
     * Only PENDING becomes RECEIPT_CONFIRMED. Any other state has a defined answer rather than a
     * silent no-op that still reports success to the caller:
     *  - already RECEIPT_CONFIRMED / CONFIRMED — the cash is in hand either way, so returning the
     *    existing row is genuine idempotency. Re-committing is deliberately skipped because it
     *    would overwrite `received_at` and rewrite the day the collection belongs to.
     *  - VOIDED — fails loudly. A voided row can still be on screen (it may have been voided from
     *    another screen or by the quick-swipe), and reporting "receipt recorded" for it would tell
     *    the operator that cash was banked against an entry that no longer exists.
     */
    suspend fun markReceivedAndCommit(id: Long): CollectionItem {
        return database.withTransaction {
            val entity = collectionDao.getById(id)
                ?: throw IllegalArgumentException("Collection record #$id not found")

            when (CollectionStatus.valueOf(entity.status)) {
                CollectionStatus.PENDING -> collectionDao.update(
                    entity.copy(
                        status = CollectionStatus.RECEIPT_CONFIRMED.name,
                        receivedAt = System.currentTimeMillis()
                    )
                )

                CollectionStatus.RECEIPT_CONFIRMED, CollectionStatus.CONFIRMED -> Unit

                CollectionStatus.VOIDED -> throw IllegalStateException(
                    "Entry #$id was voided, so its cash cannot be recorded. Refresh the list."
                )
            }

            collectionDao.getWithCustomerById(id)?.toDomain()
                ?: throw IllegalStateException("Could not reload collection #$id")
        }
    }

    suspend fun logWhatsAppOpened(id: Long) {
        collectionDao.updateWhatsAppOpenedAt(id, System.currentTimeMillis())
    }

    suspend fun confirmSent(id: Long) {
        database.withTransaction {
            val entity = collectionDao.getById(id)
                ?: throw IllegalArgumentException("Collection #$id not found")

            val currentStatus = CollectionStatus.valueOf(entity.status)
            CollectionStateMachine.assertValidTransition(currentStatus, CollectionStatus.CONFIRMED)

            val updated = entity.copy(
                status = CollectionStatus.CONFIRMED.name,
                confirmedSentAt = System.currentTimeMillis()
            )
            collectionDao.update(updated)
        }
    }

    /**
     * Idempotent confirm: never throws when the entry was already confirmed or voided.
     */
    suspend fun confirmSentSafely(id: Long) {
        database.withTransaction {
            val entity = collectionDao.getById(id) ?: return@withTransaction
            val current = CollectionStatus.valueOf(entity.status)
            if (current == CollectionStatus.CONFIRMED || current == CollectionStatus.VOIDED) {
                return@withTransaction
            }
            CollectionStateMachine.assertValidTransition(current, CollectionStatus.CONFIRMED)
            collectionDao.update(
                entity.copy(
                    status = CollectionStatus.CONFIRMED.name,
                    confirmedSentAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun voidAndReplace(
        originalId: Long,
        voidReason: String,
        newAmountPaise: Long,
        commissionRateSnapshot: Int,
        note: String? = null
    ): Long {
        val validatedReason = CollectionStateMachine.validateVoidReason(voidReason)

        return database.withTransaction {
            val original = collectionDao.getById(originalId)
                ?: throw IllegalArgumentException("Original collection #$originalId not found")

            val originalStatus = CollectionStatus.valueOf(original.status)
            CollectionStateMachine.assertValidTransition(originalStatus, CollectionStatus.VOIDED)

            val now = System.currentTimeMillis()
            val newCommissionPaise = CommissionCalculator.calculate(newAmountPaise, commissionRateSnapshot)

            val replacementEntity = CollectionEntity(
                customerId = original.customerId,
                amountPaise = newAmountPaise,
                commissionRateSnapshot = commissionRateSnapshot,
                commissionPaise = newCommissionPaise,
                status = CollectionStatus.PENDING.name,
                createdAt = now,
                replacesId = originalId,
                note = note?.trim()?.takeIf { it.isNotEmpty() } ?: original.note
            )
            val newId = collectionDao.insert(replacementEntity)

            val voidedOriginal = original.copy(
                status = CollectionStatus.VOIDED.name,
                voidedAt = now,
                voidReason = validatedReason,
                replacedById = newId
            )
            collectionDao.update(voidedOriginal)

            newId
        }
    }

    suspend fun voidCollection(id: Long, voidReason: String) {
        val validatedReason = CollectionStateMachine.validateVoidReason(voidReason)

        database.withTransaction {
            val original = collectionDao.getById(id)
                ?: throw IllegalArgumentException("Collection #$id not found")

            val originalStatus = CollectionStatus.valueOf(original.status)
            CollectionStateMachine.assertValidTransition(originalStatus, CollectionStatus.VOIDED)

            val voided = original.copy(
                status = CollectionStatus.VOIDED.name,
                voidedAt = System.currentTimeMillis(),
                voidReason = validatedReason
            )
            collectionDao.update(voided)
        }
    }

    /**
     * Hard-deletes an entry. Refuses to touch anything that represents real money
     * (RECEIPT_CONFIRMED / CONFIRMED) — those must be voided so the audit trail survives.
     */
    suspend fun deleteCollection(id: Long) {
        database.withTransaction {
            val existing = collectionDao.getById(id)
                ?: throw IllegalArgumentException("Collection #$id not found")
            val status = CollectionStatus.valueOf(existing.status)
            check(status == CollectionStatus.PENDING || status == CollectionStatus.VOIDED) {
                "Cannot delete a ${status.name} entry. Void it instead to keep the audit trail."
            }
            collectionDao.deleteById(id)
        }
    }

    /**
     * Edits an entry in place: party, amount, date and note.
     *
     * The operator is the only source of truth for their own ledger, so any entry that is still
     * part of the account — PENDING, RECEIPT_CONFIRMED or CONFIRMED — can be corrected here
     * instead of being voided and re-entered. VOIDED entries stay immutable: they are the audit
     * trail of a correction that already happened.
     *
     * Three details that matter:
     *  - Commission is recomputed from the entry's OWN rate snapshot, so changing the rate in
     *    Settings never retroactively rewrites an old entry.
     *  - A rename renames the party (a name fix is a fix for every entry of that party). If the
     *    typed name already belongs to another party, the entry is re-pointed at that party
     *    rather than creating a duplicate customer row.
     *  - The date moves with the entry. For a receipt that already happened, `received_at` moves
     *    too, because that is the timestamp the "collected today" figure counts.
     */
    suspend fun updateCollection(
        id: Long,
        customerName: String,
        amountPaise: Long,
        dateMillis: Long,
        note: String?
    ) {
        val name = customerName.trim()
        require(name.isNotBlank()) { "Party name cannot be empty" }
        require(amountPaise > 0L) { "Amount in paise must be positive: $amountPaise" }

        database.withTransaction {
            val existing = collectionDao.getById(id)
                ?: throw IllegalArgumentException("Collection #$id not found")
            val status = CollectionStatus.valueOf(existing.status)
            check(status != CollectionStatus.VOIDED) {
                "A VOIDED entry cannot be edited. #$id is the audit trail of a correction."
            }

            val currentCustomer = customerDao.getById(existing.customerId)
            val nameMatch = customerDao.findByName(name)

            val targetCustomerId = if (nameMatch != null && nameMatch.id != existing.customerId) {
                // The typed name is an existing party: merge onto it, never duplicate it.
                nameMatch.id
            } else {
                if (currentCustomer != null && currentCustomer.name != name) {
                    customerDao.update(currentCustomer.copy(name = name))
                }
                existing.customerId
            }

            collectionDao.update(
                existing.copy(
                    customerId = targetCustomerId,
                    amountPaise = amountPaise,
                    commissionPaise = CommissionCalculator.calculate(
                        amountPaise,
                        existing.commissionRateSnapshot
                    ),
                    createdAt = dateMillis,
                    receivedAt = existing.receivedAt?.let { dateMillis },
                    note = note?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
    }
}