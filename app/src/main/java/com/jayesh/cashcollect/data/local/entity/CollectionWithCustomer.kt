package com.jayesh.cashcollect.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus

data class CollectionWithCustomer(
    @Embedded val collection: CollectionEntity,
    @ColumnInfo(name = "customer_name") val customerName: String,
    @ColumnInfo(name = "customer_alias") val customerAlias: String?
) {
    fun toDomain(): CollectionItem {
        return CollectionItem(
            id = collection.id,
            customerId = collection.customerId,
            customerName = customerName,
            customerAlias = customerAlias,
            amountPaise = collection.amountPaise,
            commissionRateSnapshot = collection.commissionRateSnapshot,
            commissionPaise = collection.commissionPaise,
            status = CollectionStatus.valueOf(collection.status),
            createdAt = collection.createdAt,
            receivedAt = collection.receivedAt,
            whatsappOpenedAt = collection.whatsappOpenedAt,
            confirmedSentAt = collection.confirmedSentAt,
            voidedAt = collection.voidedAt,
            voidReason = collection.voidReason,
            replacedById = collection.replacedById,
            replacesId = collection.replacesId,
            note = collection.note,
            lastDispatchError = collection.lastDispatchError,
            lastDispatchAttemptAt = collection.lastDispatchAttemptAt
        )
    }
}
