package com.jayesh.cashcollect.domain.model

data class AppSettings(
    val id: Long = 1L,
    val brotherWhatsAppNumber: String = "",
    val commissionRatePerThousand: Int = 3,
    val lastBackupAt: Long? = null
)
