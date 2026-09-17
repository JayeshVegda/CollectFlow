package com.jayesh.cashcollect.domain.party

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.domain.state.CollectionStatus
import java.util.Calendar

/**
 * Everything known about one party, in one place.
 *
 * The operator works with a small, fixed set of parties — roughly ten or twelve names repeating
 * every day — so the useful view of the account is per party, not per entry. This is the whole of
 * that view: what is still owed by them, what they have actually paid, what it has earned in
 * commission, and the full list of their entries.
 *
 * "Collected" means the cash is in hand: RECEIPT_CONFIRMED or CONFIRMED. That is the same
 * definition the Collect screen's own figure uses, so the two can never disagree.
 */
data class PartyLedger(
    val customer: Customer? = null,
    val entries: List<CollectionItem> = emptyList(),
    val toCollectPaise: Long = 0L,
    val toCollectCount: Int = 0,
    val toCollectCommissionPaise: Long = 0L,
    val inHandPaise: Long = 0L,
    val inHandCount: Int = 0,
    val collectedPaise: Long = 0L,
    val collectedCount: Int = 0,
    val commissionPaise: Long = 0L,
    val monthCollectedPaise: Long = 0L,
    val monthCommissionPaise: Long = 0L,
    val entryCount: Int = 0,
    val lastActivityAt: Long? = null
) {
    /** Falls back to the entry's own copy of the name, so the page titles even before the row loads. */
    val displayName: String
        get() = customer?.displayName
            ?: entries.firstOrNull()?.customerDisplayName
            ?: ""
}

private fun CollectionItem.isCollected(): Boolean =
    status == CollectionStatus.RECEIPT_CONFIRMED || status == CollectionStatus.CONFIRMED

/** The day an entry belongs to: the day the cash came in, or the day it was created. */
private fun CollectionItem.activityAt(): Long = receivedAt ?: createdAt

/**
 * Builds a party's ledger. Pure and explicit about "now", so the arithmetic is unit-testable
 * without a database, a clock or an Android runtime.
 *
 * @param entries the party's entries in any order; the returned list is newest first.
 */
fun buildPartyLedger(
    customer: Customer?,
    entries: List<CollectionItem>,
    now: Long = System.currentTimeMillis()
): PartyLedger {
    val pending = entries.filter { it.status == CollectionStatus.PENDING }
    val inHand = entries.filter { it.status == CollectionStatus.RECEIPT_CONFIRMED }
    val collected = entries.filter { it.isCollected() }

    val monthStart = startOfMonth(now)
    val monthCollected = collected.filter { it.activityAt() >= monthStart }

    return PartyLedger(
        customer = customer,
        entries = entries.sortedByDescending { it.activityAt() },
        toCollectPaise = pending.sumOf { it.amountPaise },
        toCollectCount = pending.size,
        toCollectCommissionPaise = pending.sumOf { it.commissionPaise },
        inHandPaise = inHand.sumOf { it.amountPaise },
        inHandCount = inHand.size,
        collectedPaise = collected.sumOf { it.amountPaise },
        collectedCount = collected.size,
        commissionPaise = collected.sumOf { it.commissionPaise },
        monthCollectedPaise = monthCollected.sumOf { it.amountPaise },
        monthCommissionPaise = monthCollected.sumOf { it.commissionPaise },
        entryCount = entries.size,
        lastActivityAt = entries.maxOfOrNull { it.activityAt() }
    )
}

/** Local start of the month containing [now]. */
private fun startOfMonth(now: Long): Long = Calendar.getInstance().apply {
    timeInMillis = now
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis