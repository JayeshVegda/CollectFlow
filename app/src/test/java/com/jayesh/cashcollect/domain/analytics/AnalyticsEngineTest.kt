package com.jayesh.cashcollect.domain.analytics

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AnalyticsEngineTest {

    private fun makeItem(
        id: Long,
        amountPaise: Long,
        commissionPaise: Long = 1000L,
        status: CollectionStatus,
        createdAt: Long,
        receivedAt: Long? = null
    ) = CollectionItem(
        id = id,
        customerId = 1L,
        customerName = "Test Party",
        customerAlias = null,
        amountPaise = amountPaise,
        commissionRateSnapshot = 3,
        commissionPaise = commissionPaise,
        status = status,
        createdAt = createdAt,
        receivedAt = receivedAt
    )

    @Test
    fun `today realized includes receipt_confirmed and confirmed but excludes pending and voided`() {
        val now = System.currentTimeMillis()

        val items = listOf(
            makeItem(1L, 100_000L, 300L, CollectionStatus.CONFIRMED, now, now),
            makeItem(2L, 200_000L, 600L, CollectionStatus.RECEIPT_CONFIRMED, now, now),
            makeItem(3L, 50_000L, 150L, CollectionStatus.PENDING, now),
            makeItem(4L, 500_000L, 1500L, CollectionStatus.VOIDED, now)
        )

        val insights = AnalyticsEngine.computeInsights(items)

        // Realized total should be 100,000 + 200,000 = 300,000 paise (₹3,000)
        assertEquals(300_000L, insights.todayTotalPaise)
        assertEquals(900L, insights.todayCommissionPaise)
        assertEquals(2, insights.todayCount)

        // Pending total should be 50,000 paise (₹500)
        assertEquals(50_000L, insights.todayPendingPaise)
        assertEquals(1, insights.todayPendingCount)
    }
}
