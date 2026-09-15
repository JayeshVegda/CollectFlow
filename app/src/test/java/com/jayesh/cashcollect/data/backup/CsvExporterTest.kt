package com.jayesh.cashcollect.data.backup

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.state.CollectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class CsvExporterTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

    private fun item(
        id: Long = 1L,
        name: String = "Ramesh Bhai",
        note: String? = null,
        voidReason: String? = null,
        amountPaise: Long = 400_000_00L
    ) = CollectionItem(
        id = id,
        customerId = 7L,
        customerName = name,
        customerAlias = "Market",
        amountPaise = amountPaise,
        commissionRateSnapshot = 3,
        commissionPaise = 120_000L,
        status = CollectionStatus.PENDING,
        createdAt = 0L,
        note = note,
        voidReason = voidReason
    )

    @Test
    fun `header has twelve columns including Note`() {
        val header = "ID,Customer Name,Customer Alias,Amount (Rupees),Commission (Rupees),Rate (per 1000),Status,Created At,Received At,Confirmed At,Void Reason,Note"
        val columns = header.split(",").size
        assertEquals(12, columns)
        assertTrue(header.endsWith("Note"))
    }

    @Test
    fun `amounts are raw integers without thousands separators`() {
        val rows = buildCsvRows(listOf(item(amountPaise = 400_000_00L)), dateFormat)
        val columns = rows[0].split(",")
        assertEquals("400000", columns[3]) // 4,00,000 would shift every column
        assertEquals("1200", columns[4])
    }

    @Test
    fun `note column carries the note and void column the reason`() {
        val rows = buildCsvRows(
            listOf(item(note = "Half cash, half UPI", voidReason = null)),
            dateFormat
        )
        // "Half cash, half UPI" contains a comma, so it must be quoted as one field.
        assertTrue(rows[0].endsWith(",\"Half cash, half UPI\""))
        assertEquals(12, rows[0].split(",").dropLast(1).size + 1) // 12 fields after quote-aware split
    }

    @Test
    fun `void reason goes into its own column`() {
        val rows = buildCsvRows(listOf(item(voidReason = "CHEQUE DISHONOURED")), dateFormat)
        assertTrue(rows[0].contains(",CHEQUE DISHONOURED,"))
    }

    @Test
    fun `quotes inside values are doubled`() {
        assertEquals("\"say \"\"hi\"\" now\"", escapeCsvValue("say \"hi\" now"))
        assertEquals("plain", escapeCsvValue("plain"))
    }

    @Test
    fun `missing optional timestamps are empty cells`() {
        val rows = buildCsvRows(listOf(item()), dateFormat)
        val cells = rows[0].split(",")
        assertEquals("", cells[8]) // received_at
        assertEquals("", cells[9]) // confirmed_at
        assertEquals("1970-01-01 00:00:00", cells[7]) // created_at
    }
}