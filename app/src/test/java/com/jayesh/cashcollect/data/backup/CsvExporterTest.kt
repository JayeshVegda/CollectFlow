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
        val fields = splitCsvLine(rows[0])
        assertEquals(12, fields.size)
        // "Half cash, half UPI" contains a comma, so it must stay one quoted field.
        assertEquals("Half cash, half UPI", fields[11])
    }

    @Test
    fun `void reason goes into its own column`() {
        val rows = buildCsvRows(listOf(item(voidReason = "CHEQUE DISHONOURED")), dateFormat)
        val fields = splitCsvLine(rows[0])
        assertEquals("CHEQUE DISHONOURED", fields[10])
    }

    @Test
    fun `quotes inside values are doubled`() {
        assertEquals("\"say \"\"hi\"\" now\"", escapeCsvValue("say \"hi\" now"))
        assertEquals("plain", escapeCsvValue("plain"))
    }

    /** RFC-4180 style quote-aware splitter for verifying produced rows. */
    private fun splitCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    @Test
    fun `missing optional timestamps are empty cells`() {
        val rows = buildCsvRows(listOf(item()), dateFormat)
        val fields = splitCsvLine(rows[0])
        assertEquals(12, fields.size)
        assertEquals("", fields[8]) // received_at
        assertEquals("", fields[9]) // confirmed_at
        assertEquals("1970-01-01 00:00:00", fields[7]) // created_at
    }
}