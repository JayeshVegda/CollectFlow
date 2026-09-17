package com.jayesh.cashcollect.domain.party

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.model.Customer
import com.jayesh.cashcollect.domain.state.CollectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class PartyLedgerTest {

    private val now = at(2026, Calendar.SEPTEMBER, 17, 12)
    private val sambhu = Customer(id = 7L, name = "Sambhu", alias = "bhai")

    private fun at(year: Int, month: Int, day: Int, hour: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis

    private fun entry(
        id: Long,
        amountPaise: Long,
        status: CollectionStatus,
        at: Long,
        commissionPaise: Long = 0L
    ) = CollectionItem(
        id = id,
        customerId = 7L,
        customerName = "Sambhu",
        amountPaise = amountPaise,
        commissionRateSnapshot = 3,
        commissionPaise = commissionPaise,
        status = status,
        createdAt = at,
        receivedAt = if (status == CollectionStatus.PENDING) null else at
    )

    @Test
    fun `splits what is owed from what is collected`() {
        val ledger = buildPartyLedger(
            customer = sambhu,
            entries = listOf(
                entry(1L, 40_000_000L, CollectionStatus.PENDING, at(2026, Calendar.SEPTEMBER, 17, 9), 120_000L),
                entry(2L, 35_000_000L, CollectionStatus.RECEIPT_CONFIRMED, at(2026, Calendar.SEPTEMBER, 17, 10), 105_000L),
                entry(3L, 60_000_000L, CollectionStatus.CONFIRMED, at(2026, Calendar.SEPTEMBER, 12, 11), 180_000L)
            ),
            now = now
        )

        assertEquals(40_000_000L, ledger.toCollectPaise)
        assertEquals(1, ledger.toCollectCount)
        assertEquals(120_000L, ledger.toCollectCommissionPaise)

        assertEquals(35_000_000L, ledger.inHandPaise)
        assertEquals(1, ledger.inHandCount)

        // Collected means the cash is in hand: the receipt (Rs 3,50,000) plus the reported entry (Rs 6,00,000).
        assertEquals(95_000_000L, ledger.collectedPaise)
        assertEquals(2, ledger.collectedCount)
        assertEquals(285_000L, ledger.commissionPaise)

        assertEquals(3, ledger.entryCount)
        assertEquals("Sambhu (bhai)", ledger.displayName)
    }

    @Test
    fun `month figures cover only the current month`() {
        val ledger = buildPartyLedger(
            customer = sambhu,
            entries = listOf(
                entry(1L, 35_000_000L, CollectionStatus.RECEIPT_CONFIRMED, at(2026, Calendar.SEPTEMBER, 17, 10), 105_000L),
                entry(2L, 60_000_000L, CollectionStatus.CONFIRMED, at(2026, Calendar.SEPTEMBER, 12, 11), 180_000L),
                entry(3L, 25_000_000L, CollectionStatus.CONFIRMED, at(2026, Calendar.AUGUST, 30, 11), 75_000L)
            ),
            now = now
        )

        assertEquals(95_000_000L, ledger.monthCollectedPaise)
        assertEquals(285_000L, ledger.monthCommissionPaise)
        assertEquals(120_000_000L, ledger.collectedPaise)
        assertEquals(360_000L, ledger.commissionPaise)
    }

    @Test
    fun `voided entries stay in the list but never count as money`() {
        val ledger = buildPartyLedger(
            customer = sambhu,
            entries = listOf(
                entry(1L, 50_000_000L, CollectionStatus.VOIDED, at(2026, Calendar.SEPTEMBER, 16, 10), 150_000L),
                entry(2L, 10_000_000L, CollectionStatus.PENDING, at(2026, Calendar.SEPTEMBER, 16, 11), 30_000L)
            ),
            now = now
        )

        assertEquals(10_000_000L, ledger.toCollectPaise)
        assertEquals(0L, ledger.collectedPaise)
        assertEquals(0L, ledger.commissionPaise)
        assertEquals(0L, ledger.monthCollectedPaise)
        assertEquals(2, ledger.entryCount)
    }

    @Test
    fun `entries come back newest first`() {
        val ledger = buildPartyLedger(
            customer = sambhu,
            entries = listOf(
                entry(1L, 10_000L, CollectionStatus.PENDING, at(2026, Calendar.SEPTEMBER, 10, 9)),
                entry(2L, 20_000L, CollectionStatus.CONFIRMED, at(2026, Calendar.SEPTEMBER, 15, 9)),
                entry(3L, 30_000L, CollectionStatus.PENDING, at(2026, Calendar.SEPTEMBER, 12, 9))
            ),
            now = now
        )

        assertEquals(listOf(2L, 3L, 1L), ledger.entries.map { it.id })
        assertEquals(at(2026, Calendar.SEPTEMBER, 15, 9), ledger.lastActivityAt)
    }

    @Test
    fun `an empty party reads as zeroes rather than exploding`() {
        val ledger = buildPartyLedger(customer = null, entries = emptyList(), now = now)

        assertEquals(0L, ledger.toCollectPaise)
        assertEquals(0L, ledger.collectedPaise)
        assertEquals(0L, ledger.monthCollectedPaise)
        assertEquals(0, ledger.entryCount)
        assertNull(ledger.lastActivityAt)
        assertEquals("", ledger.displayName)
    }

    @Test
    fun `falls back to the entry's own name before the party row loads`() {
        val ledger = buildPartyLedger(
            customer = null,
            entries = listOf(
                entry(1L, 10_000L, CollectionStatus.PENDING, at(2026, Calendar.SEPTEMBER, 17, 9))
            ),
            now = now
        )

        assertEquals("Sambhu", ledger.displayName)
    }
}