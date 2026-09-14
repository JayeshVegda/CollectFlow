package com.jayesh.cashcollect.data.repository

import androidx.room.withTransaction
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.state.CollectionStateMachine
import com.jayesh.cashcollect.domain.state.CollectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionRepository(
    private val database: AppDatabase
) {
    private val collectionDao = database.collectionDao()
    private val customerDao = database.customerDao()

    fun getOutstandingConfirmations(): Flow<List<CollectionItem>> {
        return collectionDao.getOutstandingConfirmations().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getPendingCollections(): Flow<List<CollectionItem>> {
        return collectionDao.getPendingCollections().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAllHistory(): Flow<List<CollectionItem>> {
        return collectionDao.getAllHistory().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun filterHistory(
        status: CollectionStatus?,
        fromTimestamp: Long?,
        toTimestamp: Long?,
        searchQuery: String?
    ): Flow<List<CollectionItem>> {
        return collectionDao.filterHistory(
            status = status?.name,
            fromTimestamp = fromTimestamp,
            toTimestamp = toTimestamp,
            searchQuery = searchQuery?.trim()?.takeIf { it.isNotEmpty() }
        ).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getCollectionById(id: Long): CollectionItem? {
        return collectionDao.getWithCustomerById(id)?.toDomain()
    }

    /**
     * Checks if a duplicate entry exists within [withinMinutes].
     */
    suspend fun checkRecentDuplicate(customerId: Long, amountPaise: Long, withinMinutes: Int = 30): Boolean {
        val since = System.currentTimeMillis() - (withinMinutes * 60 * 1000L)
        val count = collectionDao.countRecentDuplicates(customerId, amountPaise, since)
        return count > 0
    }

    /**
     * Creates and stores a new PENDING collection with snapshotted commission rate and commission value.
     */
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
     * Non-negotiable: Persists receipt state to Room BEFORE WhatsApp intent fires.
     * Transitions status from PENDING -> RECEIPT_CONFIRMED.
     * If already RECEIPT_CONFIRMED, leaves status intact and only returns item.
     */
    suspend fun markReceivedAndCommit(id: Long): CollectionItem {
        return database.withTransaction {
            val entity = collectionDao.getById(id)
                ?: throw IllegalArgumentException("Collection record #$id not found")

            val currentStatus = CollectionStatus.valueOf(entity.status)
            if (currentStatus == CollectionStatus.PENDING) {
                val updated = entity.copy(
                    status = CollectionStatus.RECEIPT_CONFIRMED.name,
                    receivedAt = System.currentTimeMillis()
                )
                collectionDao.update(updated)
            }

            collectionDao.getWithCustomerById(id)?.toDomain()
                ?: throw IllegalStateException("Could not reload collection #$id")
        }
    }

    /**
     * Reopening WhatsApp for an already-received item records the reopen timestamp
     * without changing status or duplicating any receipts.
     */
    suspend fun logWhatsAppOpened(id: Long) {
        collectionDao.updateWhatsAppOpenedAt(id, System.currentTimeMillis())
    }

    /**
     * Explicit "Yes, sent" user confirmation moves status to CONFIRMED.
     */
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
     * Corrections: Voids the original record with a mandatory reason, and creates a linked replacement.
     */
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

            // 1. Create replacement row pointing back to original
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

            // 2. Void original row pointing forward to replacement
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

    /**
     * Direct voiding without immediate replacement.
     */
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
}
