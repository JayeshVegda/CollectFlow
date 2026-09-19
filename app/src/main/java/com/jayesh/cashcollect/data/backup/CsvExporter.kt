package com.jayesh.cashcollect.data.backup

import android.content.Context
import com.jayesh.cashcollect.domain.model.CollectionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(private val context: Context) {

    suspend fun exportHistoryToCsv(items: List<CollectionItem>): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val csvFile = File(exportDir, "cash_collections_$timeStamp.csv")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        csvFile.bufferedWriter().use { writer ->
            writer.write(CSV_HEADER)
            for (row in buildCsvRows(items, dateFormat)) {
                writer.write(row)
                writer.write("\n")
            }
        }

        csvFile
    }
}

/**
 * The CSV contract.
 *
 * Money appears twice, on purpose:
 *  - `(Paise)` is the exact stored integer — the column to reconcile against, because it is the
 *    same number the database holds and so cannot disagree with the ledger.
 *  - `(Rupees)` is that value as a plain decimal (`1200.50`, no thousands separators) for a human
 *    reading the sheet.
 *
 * The previous version exported integer *rupees* only (`paise / 100`), which silently truncated
 * every paise: a ₹1,200.50 collection exported as `1200` and could not be reconciled against the
 * app. Both columns are produced with integer arithmetic — no floating point appears in this file.
 */
internal const val CSV_HEADER =
    "ID,Customer Name,Customer Alias,Amount (Paise),Amount (Rupees)," +
        "Commission (Paise),Commission (Rupees),Rate (per 1000),Status," +
        "Created At,Received At,Confirmed At,Void Reason,Note\n"

/** Pure row formatting (no Android dependencies) so the CSV contract can be unit-tested. */
internal fun buildCsvRows(items: List<CollectionItem>, dateFormat: SimpleDateFormat): List<String> =
    items.map { item ->
        val createdStr = dateFormat.format(Date(item.createdAt))
        val receivedStr = item.receivedAt?.let { dateFormat.format(Date(it)) } ?: ""
        val confirmedStr = item.confirmedSentAt?.let { dateFormat.format(Date(it)) } ?: ""

        buildString {
            append(item.id).append(",")
            append(escapeCsvValue(item.customerName)).append(",")
            append(escapeCsvValue(item.customerAlias.orEmpty())).append(",")
            // Exact paise first, then the same value for a human to read.
            append(item.amountPaise).append(",")
            append(paiseToPlainRupees(item.amountPaise)).append(",")
            append(item.commissionPaise).append(",")
            append(paiseToPlainRupees(item.commissionPaise)).append(",")
            append(item.commissionRateSnapshot).append(",")
            append(item.status.name).append(",")
            append(createdStr).append(",")
            append(receivedStr).append(",")
            append(confirmedStr).append(",")
            append(escapeCsvValue(item.voidReason.orEmpty())).append(",")
            append(escapeCsvValue(item.note.orEmpty()))
        }
    }

/**
 * `1234567` paise -> `"12345.67"`.
 *
 * Integer-only and locale-independent: the fractional part is the remainder of a division by 100,
 * never a rounded float, so the value exported is always exactly the value stored.
 */
internal fun paiseToPlainRupees(paise: Long): String {
    val negative = paise < 0L
    val absolute = if (negative) -paise else paise
    val rupees = absolute / 100L
    val fraction = absolute % 100L
    val fractionText = if (fraction < 10L) "0$fraction" else fraction.toString()
    return (if (negative) "-" else "") + rupees + "." + fractionText
}

internal fun escapeCsvValue(value: String): String {
    return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}
