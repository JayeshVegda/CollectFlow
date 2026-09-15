package com.jayesh.cashcollect.domain.template

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

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

    @Test
    fun `note tag renders the note and never the void reason`() {
        val item = sampleItem.copy(note = "Half cash, half UPI", voidReason = "DISHONOURED")
        val formatted = MessageTemplateEngine.formatMessage("Note: {note}", item)
        assertEquals("Note: Half cash, half UPI", formatted)
        assertFalse(formatted.contains("DISHONOURED"))
    }

    @Test
    fun `missing note renders empty string not the literal tag`() {
        val formatted = MessageTemplateEngine.formatMessage("Note:[{note}]", sampleItem)
        assertEquals("Note:[]", formatted)
    }

    @Test
    fun `commission and alias tags render`() {
        val formatted = MessageTemplateEngine.formatMessage(
            "{name} ({alias}) paid {amount}, fee {commission}",
            sampleItem.copy(customerAlias = "Bapunagar")
        )
        assertEquals("Ramesh Bhai (Bapunagar) paid ₹5,00,000, fee ₹1,500", formatted)
    }

    @Test
    fun `unrecognised tags are left untouched`() {
        val formatted = MessageTemplateEngine.formatMessage("Hi {unknown} {name}", sampleItem)
        assertEquals("Hi {unknown} Ramesh Bhai", formatted)
    }

    @Test
    fun `tags are case insensitive`() {
        val formatted = MessageTemplateEngine.formatMessage("{NAME} {Ref}", sampleItem)
        assertEquals("Ramesh Bhai 42", formatted)
    }

    @Test
    fun `receivedAt wins over createdAt for date and time`() {
        val item = sampleItem.copy(
            createdAt = 0L,
            receivedAt = 1726315200000L
        )
        Locale.setDefault(Locale.US)
        val formatted = MessageTemplateEngine.formatMessage("{date} {time}", item)
        assertEquals("14 Sep 2024 12:00 PM", formatted)
        assertNull(item.voidReason)
    }

    @Test
    fun `preview uses the given template`() {
        val preview = MessageTemplateEngine.preview("{name} | {amount}")
        assertTrue(preview.startsWith("Sambhu Bhai | ₹4,00,000"))
    }
}
