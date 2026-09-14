package com.jayesh.cashcollect.domain.template

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTemplateEngineTest {

    private val sampleItem = CollectionItem(
        id = 42L,
        customerId = 1L,
        customerName = "Ramesh Bhai",
        customerAlias = "Bapunagar",
        amountPaise = 50_000_000L, // 5 Lakh
        commissionRateSnapshot = 3,
        commissionPaise = 150_000L,
        status = CollectionStatus.RECEIPT_CONFIRMED,
        createdAt = 1726315200000L, // Fixed time
        receivedAt = 1726315200000L
    )

    @Test
    fun `formats default template with curly brace tags`() {
        val template = "Party: {name} | Amount: {amount} | Ref: #{ref}"
        val formatted = MessageTemplateEngine.formatMessage(template, sampleItem)

        assertTrue(formatted.contains("Party: Ramesh Bhai"))
        assertTrue(formatted.contains("Amount: ₹5,00,000"))
        assertTrue(formatted.contains("Ref: #42"))
    }

    @Test
    fun `formats template with angle bracket tags`() {
        val template = "Received from <name>, <amount>, Ref: <ref>"
        val formatted = MessageTemplateEngine.formatMessage(template, sampleItem)

        assertTrue(formatted.contains("Received from Ramesh Bhai"))
        assertTrue(formatted.contains("₹5,00,000"))
        assertTrue(formatted.contains("Ref: 42"))
    }

    @Test
    fun `fallback to default template when blank`() {
        val formatted = MessageTemplateEngine.formatMessage("", sampleItem)
        assertTrue(formatted.contains("Ramesh Bhai"))
        assertTrue(formatted.contains("₹5,00,000"))
    }
}
