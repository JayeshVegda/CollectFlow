package com.jayesh.cashcollect.service.whatsapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.domain.money.Paise
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppLauncher {

    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    /**
     * Builds the pre-filled receipt message strictly from the stored collection record.
     */
    fun buildReceiptMessage(collection: CollectionItem): String {
        val amountFormatted = Paise(collection.amountPaise).toFormattedRupees()
        val timeFormatted = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(
            Date(collection.receivedAt ?: collection.createdAt)
        )

        return buildString {
            appendLine("💰 *Cash Received*")
            appendLine("Customer: *${collection.customerDisplayName}*")
            appendLine("Amount: *$amountFormatted*")
            appendLine("Time: $timeFormatted")
            appendLine("Ref: #${collection.id}")
        }
    }

    /**
     * Creates an Intent to send the message to WhatsApp or WhatsApp Business.
     */
    fun createSendIntent(context: Context, brotherPhone: String, message: String): Intent {
        val cleanPhone = brotherPhone.filter { it.isDigit() || it == '+' }
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        // Try direct WhatsApp package intent with phone number
        val packageManager = context.packageManager
        val targetPackage = when {
            isPackageInstalled(packageManager, PACKAGE_WHATSAPP) -> PACKAGE_WHATSAPP
            isPackageInstalled(packageManager, PACKAGE_WHATSAPP_BUSINESS) -> PACKAGE_WHATSAPP_BUSINESS
            else -> null
        }

        val url = if (cleanPhone.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (targetPackage != null) {
            intent.setPackage(targetPackage)
        }

        return intent
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
