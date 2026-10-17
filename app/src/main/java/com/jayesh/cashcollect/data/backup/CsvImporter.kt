package com.jayesh.cashcollect.data.backup

import androidx.room.withTransaction
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.data.local.entity.CustomerEntity
import com.jayesh.cashcollect.domain.money.CommissionCalculator
import com.jayesh.cashcollect.domain.state.CollectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Result returned after importing a CSV ledger.
 */
data class CsvImportResult(
    val collectionsImported: Int,
    val customersCreated: Int,
    val errors: List<String>
)

class CsvImporter(private val database: AppDatabase) {

    private val collectionDao = database.collectionDao()
    private val customerDao = database.customerDao()

    suspend fun importFromStream(inputStream: InputStream, replaceExisting: Boolean = true): CsvImportResult =
        withContext(Dispatchers.IO) {
            val content = inputStream.bufferedReader().use { it.readText() }
            importFromCsvText(content, replaceExisting)
        }

    suspend fun importFromCsvText(csvContent: String, replaceExisting: Boolean = true): CsvImportResult =
        withContext(Dispatchers.IO) {
            val rows = parseCsvRows(csvContent)
            if (rows.isEmpty()) {
                return@withContext CsvImportResult(0, 0, listOf("The CSV file is empty."))
            }

            val header = rows.first()
            val headerIndexMap = header.mapIndexed { idx, col -> col.trim().lowercase(Locale.ROOT) to idx }.toMap()

            // Resolve required column indices
            val nameIdx = headerIndexMap["customer name"] ?: 1
            val aliasIdx = headerIndexMap["customer alias"] ?: 2
            val amountPaiseIdx = headerIndexMap["amount (paise)"] ?: 3
            val amountRupeesIdx = headerIndexMap["amount (rupees)"] ?: 4
            val commPaiseIdx = headerIndexMap["commission (paise)"] ?: 5
            val rateIdx = headerIndexMap["rate (per 1000)"] ?: 7
            val statusIdx = headerIndexMap["status"] ?: 8
            val createdAtIdx = headerIndexMap["created at"] ?: 9
            val receivedAtIdx = headerIndexMap["received at"] ?: 10
            val confirmedAtIdx = headerIndexMap["confirmed at"] ?: 11
            val voidReasonIdx = headerIndexMap["void reason"] ?: 12
            val noteIdx = headerIndexMap["note"] ?: 13

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val errors = mutableListOf<String>()

            data class ParsedRow(
                val name: String,
                val alias: String?,
                val amountPaise: Long,
                val commPaise: Long,
                val rate: Int,
                val status: CollectionStatus,
                val createdAt: Long,
                val receivedAt: Long?,
                val confirmedAt: Long?,
                val voidReason: String?,
                val note: String?
            )

            val parsedList = mutableListOf<ParsedRow>()

            for ((rowNum, row) in rows.drop(1).withIndex()) {
                if (row.all { it.isBlank() }) continue // Skip blank lines

                val name = row.getOrNull(nameIdx)?.trim().orEmpty()
                if (name.isBlank()) {
                    errors.add("Row ${rowNum + 2}: Missing party name, skipped.")
                    continue
                }

                val alias = row.getOrNull(aliasIdx)?.trim()?.takeIf { it.isNotBlank() }

                // Parse amount in paise or fallback to plain rupees
                val paiseFromCol = row.getOrNull(amountPaiseIdx)?.trim()?.toLongOrNull()
                val amountPaise = if (paiseFromCol != null && paiseFromCol > 0L) {
                    paiseFromCol
                } else {
                    val rupeesText = row.getOrNull(amountRupeesIdx)?.trim()
                    val rupeesVal = rupeesText?.replace(",", "")?.toDoubleOrNull()
                    if (rupeesVal != null && rupeesVal > 0.0) {
                        (rupeesVal * 100.0 + 0.5).toLong()
                    } else {
                        errors.add("Row ${rowNum + 2}: Invalid amount for party '$name', skipped.")
                        continue
                    }
                }

                val rate = row.getOrNull(rateIdx)?.trim()?.toIntOrNull() ?: 10
                val commPaise = row.getOrNull(commPaiseIdx)?.trim()?.toLongOrNull()
                    ?: CommissionCalculator.calculate(amountPaise, rate)

                val statusRaw = row.getOrNull(statusIdx)?.trim()?.uppercase(Locale.ROOT)
                val status = try {
                    CollectionStatus.valueOf(statusRaw ?: "CONFIRMED")
                } catch (e: Exception) {
                    CollectionStatus.CONFIRMED
                }

                fun parseDate(raw: String?): Long? {
                    val s = raw?.trim().orEmpty()
                    if (s.isBlank()) return null
                    return runCatching { dateFormat.parse(s)?.time }.getOrNull()
                        ?: s.toLongOrNull()
                }

                val createdAt = parseDate(row.getOrNull(createdAtIdx)) ?: System.currentTimeMillis()
                val receivedAt = parseDate(row.getOrNull(receivedAtIdx))
                val confirmedAt = parseDate(row.getOrNull(confirmedAtIdx))
                val voidReason = row.getOrNull(voidReasonIdx)?.trim()?.takeIf { it.isNotBlank() }
                val note = row.getOrNull(noteIdx)?.trim()?.takeIf { it.isNotBlank() }

                parsedList.add(
                    ParsedRow(
                        name = name,
                        alias = alias,
                        amountPaise = amountPaise,
                        commPaise = commPaise,
                        rate = rate,
                        status = status,
                        createdAt = createdAt,
                        receivedAt = receivedAt,
                        confirmedAt = confirmedAt,
                        voidReason = voidReason,
                        note = note
                    )
                )
            }

            var importedCount = 0
            var customerCount = 0

            database.withTransaction {
                if (replaceExisting) {
                    collectionDao.deleteAll()
                    customerDao.deleteAll()
                }

                val customerCache = mutableMapOf<String, Long>()

                for (item in parsedList) {
                    val cacheKey = item.name.lowercase(Locale.ROOT)
                    var custId = customerCache[cacheKey]

                    if (custId == null) {
                        val existing = customerDao.findByName(item.name)
                        if (existing != null) {
                            custId = existing.id
                        } else {
                            val newCustomer = CustomerEntity(
                                name = item.name,
                                alias = item.alias,
                                createdAt = item.createdAt,
                                lastUsedAt = item.createdAt
                            )
                            custId = customerDao.insert(newCustomer)
                            customerCount++
                        }
                        customerCache[cacheKey] = custId
                    }

                    val entity = CollectionEntity(
                        customerId = custId,
                        amountPaise = item.amountPaise,
                        commissionPaise = item.commPaise,
                        commissionRateSnapshot = item.rate,
                        status = item.status.name,
                        createdAt = item.createdAt,
                        receivedAt = item.receivedAt,
                        confirmedSentAt = item.confirmedAt,
                        voidReason = item.voidReason,
                        note = item.note
                    )
                    collectionDao.insert(entity)
                    importedCount++
                }
            }

            CsvImportResult(
                collectionsImported = importedCount,
                customersCreated = customerCount,
                errors = errors
            )
        }
}

/**
 * State-machine CSV parser that handles RFC-4180 quotes, embedded commas, and escaped quotes (`""`).
 */
internal fun parseCsvRows(csv: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val currentRow = mutableListOf<String>()
    val currentField = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < csv.length) {
        val c = csv[i]
        when {
            inQuotes -> {
                if (c == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            }
            c == '"' -> {
                inQuotes = true
            }
            c == ',' -> {
                currentRow.add(currentField.toString())
                currentField.clear()
            }
            c == '\r' -> {
                if (i + 1 < csv.length && csv[i + 1] == '\n') {
                    i++
                }
                currentRow.add(currentField.toString())
                currentField.clear()
                rows.add(currentRow.toList())
                currentRow.clear()
            }
            c == '\n' -> {
                currentRow.add(currentField.toString())
                currentField.clear()
                rows.add(currentRow.toList())
                currentRow.clear()
            }
            else -> {
                currentField.append(c)
            }
        }
        i++
    }

    if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
        currentRow.add(currentField.toString())
        rows.add(currentRow.toList())
    }

    return rows
}
