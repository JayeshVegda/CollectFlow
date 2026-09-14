package com.jayesh.cashcollect.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Long = 1L,
    @ColumnInfo(name = "brother_whatsapp_number")
    val brotherWhatsAppNumber: String = "",
    @ColumnInfo(name = "commission_rate_per_thousand")
    val commissionRatePerThousand: Int = 3,
    @ColumnInfo(name = "last_backup_at")
    val lastBackupAt: Long? = null
)
