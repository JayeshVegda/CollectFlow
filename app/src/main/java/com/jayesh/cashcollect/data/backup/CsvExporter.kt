package com.jayesh.cashcollect.data.backup

import android.content.Context
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
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
            // Header
            writer.write("ID,Customer Name,Customer Alias,Amount (Rupees),Commission (Rupees),Rate (per 1000),Status,Created At,Received At,Confirmed At,Void Reason,Note\n")

            for (item in items) {
                val createdStr = dateFormat.format(Date(item.createdAt))
                val receivedStr = item.receivedAt?.let { dateFormat.format(Date(it)) } ?: ""
                val confirmedStr = item.confirmedSentAt?.let { dateFormat.format(Date(it)) } ?: ""
                // Raw integers only: formatted values contain thousands separators (4,00,000),
                // which would shift every column in the CSV.
                val amountRupees = item.amountPaise / 100L
                val commissionRupees = item.commissionPaise / 100L

                val line = buildString {
                    append(item.id).append(",")
                    append(escapeCsv(item.customerName)).append(",")
                    append(escapeCsv(item.customerAlias.orEmpty())).append(",")
                    append(amountRupees).append(",")
                    append(commissionRupees).append(",")
                    append(item.commissionRateSnapshot).append(",")
                    append(item.status.name).append(",")
                    append(createdStr).append(",")
                    append(receivedStr).append(",")
                    append(confirmedStr).append(",")
                    append(escapeCsv(item.voidReason.orEmpty())).append(",")
                    append(escapeCsv(item.note.orEmpty()))
                    append("\n")
                }
                writer.write(line)
            }
        }

        csvFile
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
