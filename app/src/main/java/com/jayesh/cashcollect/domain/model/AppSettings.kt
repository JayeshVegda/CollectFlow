package com.jayesh.cashcollect.domain.model

data class AppSettings(
    val id: Long = 1L,
    val brotherWhatsAppNumber: String = "",
    val commissionRatePerThousand: Int = 3,
    val lastBackupAt: Long? = null,
    val messageTemplate: String = "",
    val telegramEnabled: Boolean = false,
    val telegramApiId: String = "",
    val telegramApiHash: String = "",
    val telegramRecipient: String = "",
    val telegramFallbackWhatsApp: Boolean = true
)
