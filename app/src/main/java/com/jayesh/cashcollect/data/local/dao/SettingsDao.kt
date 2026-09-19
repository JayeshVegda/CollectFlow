package com.jayesh.cashcollect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jayesh.cashcollect.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SettingsEntity)

    /** Creates the singleton row (id = 1) with defaults if it does not exist yet. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureRow(settings: SettingsEntity)

    @Query("UPDATE settings SET brother_whatsapp_number = :number WHERE id = 1")
    suspend fun updateBrotherWhatsAppNumber(number: String)

    @Query("UPDATE settings SET commission_rate_per_thousand = :rate WHERE id = 1")
    suspend fun updateCommissionRate(rate: Int)

    @Query("UPDATE settings SET message_template = :template WHERE id = 1")
    suspend fun updateMessageTemplate(template: String)

    @Query("UPDATE settings SET notification_delay_ms = :delayMs WHERE id = 1")
    suspend fun updateNotificationDelay(delayMs: Int)

    @Query("UPDATE settings SET last_backup_at = :timestamp WHERE id = 1")
    suspend fun updateLastBackupTimestamp(timestamp: Long)
}