package com.jayesh.cashcollect.domain.model

import com.jayesh.cashcollect.domain.money.Paise
import com.jayesh.cashcollect.domain.state.CollectionStatus

data class CollectionItem(
    val id: Long = 0L,
    val customerId: Long,
    val customerName: String = "",
    val customerAlias: String? = null,
    val amountPaise: Long,
    val commissionRateSnapshot: Int,
    val commissionPaise: Long,
    val status: CollectionStatus,
    val createdAt: Long,
    val receivedAt: Long? = null,
    val whatsappOpenedAt: Long? = null,
    val confirmedSentAt: Long? = null,
    val voidedAt: Long? = null,
    val voidReason: String? = null,
    val replacedById: Long? = null,
    val replacesId: Long? = null,
    val note: String? = null,
    /** Reason the last Telegram dispatch failed, if any. Cleared on a successful send. */
    val lastDispatchError: String? = null,
    val lastDispatchAttemptAt: Long? = null
) {
    val amount: Paise get() = Paise(amountPaise)
    val commission: Paise get() = Paise(commissionPaise)

    val customerDisplayName: String
        get() = if (customerAlias.isNullOrBlank()) customerName else "$customerName ($customerAlias)"
}
