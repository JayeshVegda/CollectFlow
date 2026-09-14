package com.jayesh.cashcollect.domain.template

import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MessageTemplateEngine {

    const val DEFAULT_TEMPLATE = "💰 Cash Received: {name} | Amount: {amount} | Time: {time}, {date} | Ref: #{ref}"

    val AVAILABLE_TAGS = listOf(
        "{name}" to "Party Name",
        "{alias}" to "Area / Shop Alias",
        "{amount}" to "Collection Amount (e.g. ₹4,00,000)",
        "{commission}" to "Commission Amount (e.g. ₹1,200)",
        "{date}" to "Date (e.g. 14 Sep 2026)",
        "{time}" to "Time (e.g. 04:30 PM)",
        "{ref}" to "Reference ID (e.g. 101)",
        "{note}" to "Transaction Note"
    )

    fun formatMessage(template: String, collection: CollectionItem): String {
        val activeTemplate = template.trim().ifEmpty { DEFAULT_TEMPLATE }

        val timestamp = collection.receivedAt ?: collection.createdAt
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        val amountStr = Paise(collection.amountPaise).toFormattedRupees()
        val commissionStr = Paise(collection.commissionPaise).toFormattedRupees()

        var result = activeTemplate

        // Handle both {tag} and <tag> formats
        val replacements = mapOf(
            "name" to collection.customerName,
            "alias" to (collection.customerAlias ?: ""),
            "amount" to amountStr,
            "ammount" to amountStr,
            "commission" to commissionStr,
            "date" to dateStr,
            "time" to timeStr,
            "ref" to collection.id.toString(),
            "note" to (collection.voidReason ?: "") // Note or details
        )

        for ((key, value) in replacements) {
            result = result.replace("{$key}", value, ignoreCase = true)
                .replace("<$key>", value, ignoreCase = true)
        }

        return result
    }

    /**
     * Generates a realistic live preview of what the message will look like.
     */
    fun preview(template: String): String {
        val sampleItem = CollectionItem(
            id = 101L,
            customerId = 1L,
            customerName = "Sambhu Bhai",
            customerAlias = "Market Yard",
            amountPaise = 40_000_000L, // 4 Lakh
            commissionRateSnapshot = 3,
            commissionPaise = 120_000L, // 1,200
            status = com.jayesh.cashcollect.domain.state.CollectionStatus.RECEIPT_CONFIRMED,
            createdAt = System.currentTimeMillis(),
            receivedAt = System.currentTimeMillis()
        )
        return formatMessage(template, sampleItem)
    }
}
