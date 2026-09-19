package com.jayesh.cashcollect.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.template.MessageTemplateEngine

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Long = 1L,
    @ColumnInfo(name = "brother_whatsapp_number")
    val brotherWhatsAppNumber: String = "",
    @ColumnInfo(name = "commission_rate_per_thousand")
    val commissionRatePerThousand: Int = 3,
    @ColumnInfo(name = "last_backup_at")
    val lastBackupAt: Long? = null,
    @ColumnInfo(name = "message_template")
    val messageTemplate: String = MessageTemplateEngine.DEFAULT_TEMPLATE,
    /**
     * `defaultValue` is NOT decoration here — it is load-bearing.
     *
     * MIGRATION_5_6 adds this column with `NOT NULL DEFAULT 4000`, because a NOT NULL column
     * cannot be added to a table that already has rows without one. Room validates the live
     * schema against the entity on every open, so if the entity does not declare the same
     * default, the check fails and [com.jayesh.cashcollect.data.local.AppDatabase] falls into
     * its reset-and-rebuild path — which deletes the operator's ledger. Declaring it here keeps
     * the two definitions provably identical, and a unit test pins them together.
     */
    @ColumnInfo(name = "notification_delay_ms", defaultValue = "4000")
    val notificationDelayMs: Int = AppSettings.DEFAULT_NOTIFICATION_DELAY_MS
)