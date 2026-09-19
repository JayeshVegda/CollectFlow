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
    fun `header has fourteen columns and names both money representations`() {
        val columns = CSV_HEADER.trim().split(",")
        assertEquals(14, columns.size)
        assertTrue(columns.contains("Amount (Paise)"))
        assertTrue(columns.contains("Amount (Rupees)"))
        assertTrue(columns.contains("Commission (Paise)"))
        assertTrue(columns.contains("Commission (Rupees)"))
        assertTrue(CSV_HEADER.endsWith("Note\n"))
    }

    @Test
    fun `amounts carry exact paise beside a separator-free decimal`() {
        val rows = buildCsvRows(listOf(item(amountPaise = 400_000_00L)), dateFormat)
        val columns = rows[0].split(",")
        assertEquals("40000000", columns[3]) // 4,00,000 rupees, exact paise
        assertEquals("400000.00", columns[4]) // no thousands separator, which would shift columns
        assertEquals("120000", columns[5])
        assertEquals("1200.00", columns[6])
    }

    @Test
    fun `paise that are not a whole rupee survive the export`() {
        // The regression this guards: `paise / 100` exported ₹1,200.50 as 1200, so the sheet could
        // not be reconciled against the ledger.
        assertEquals("1200.50", paiseToPlainRupees(120_050L))
        assertEquals("1200.05", paiseToPlainRupees(120_005L))
        assertEquals("120000.01", paiseToPlainRupees(12_000_001L))
        assertEquals("0.07", paiseToPlainRupees(7L))
        assertEquals("1.00", paiseToPlainRupees(100L))

        val fields = splitCsvLine(
            buildCsvRows(listOf(item(amountPaise = 120_050L)), dateFormat)[0]
        )
        assertEquals("120050", fields[3])
        assertEquals("1200.50", fields[4])
    }

    @Test
    fun `note column carries the note and void column the reason`() {
        val rows = buildCsvRows(
            listOf(item(note = "Half cash, half UPI", voidReason = null)),
            dateFormat
        )
        val fields = splitCsvLine(rows[0])
        assertEquals(14, fields.size)
        // "Half cash, half UPI" contains a comma, so it must stay one quoted field.
        assertEquals("Half cash, half UPI", fields[13])
        assertEquals("", fields[12])
    }

    @Test
    fun `void reason goes into its own column`() {
        val rows = buildCsvRows(listOf(item(voidReason = "CHEQUE DISHONOURED")), dateFormat)
        val fields = splitCsvLine(rows[0])
        assertEquals("CHEQUE DISHONOURED", fields[12])
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
        assertEquals(14, fields.size)
        assertEquals("", fields[10]) // received_at
        assertEquals("", fields[11]) // confirmed_at
        assertEquals("1970-01-01 00:00:00", fields[9]) // created_at
    }
}