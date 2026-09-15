package com.jayesh.cashcollect.data.repository

import com.jayesh.cashcollect.data.local.dao.SettingsDao
import com.jayesh.cashcollect.data.local.entity.SettingsEntity
import com.jayesh.cashcollect.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val settingsDao: SettingsDao) {

    fun getSettings(): Flow<AppSettings> {
        return settingsDao.getSettings().map { entity ->
            entity?.toDomain() ?: AppSettings()
        }
    }

    suspend fun getSettingsSync(): AppSettings {
        return settingsDao.getSettingsSync()?.toDomain() ?: AppSettings()
    }

    suspend fun updateBrotherWhatsAppNumber(number: String) {
        val current = settingsDao.getSettingsSync() ?: SettingsEntity()
        settingsDao.insertOrUpdate(current.copy(brotherWhatsAppNumber = number.trim()))
    }

    suspend fun updateCommissionRate(ratePerThousand: Int) {
        require(ratePerThousand >= 0) { "Rate per thousand cannot be negative" }
        val current = settingsDao.getSettingsSync() ?: SettingsEntity()
        settingsDao.insertOrUpdate(current.copy(commissionRatePerThousand = ratePerThousand))
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long = System.currentTimeMillis()) {
        settingsDao.updateLastBackupTimestamp(timestamp)
    }

    suspend fun updateMessageTemplate(template: String) {
        val current = settingsDao.getSettingsSync() ?: SettingsEntity()
        settingsDao.insertOrUpdate(current.copy(messageTemplate = template.trim()))
    }

    suspend fun updateTelegramSettings(
        enabled: Boolean,
        apiId: String,
        apiHash: String,
        recipient: String,
        fallbackWhatsApp: Boolean
    ) {
        val current = settingsDao.getSettingsSync() ?: SettingsEntity()
        settingsDao.insertOrUpdate(
            current.copy(
                telegramEnabled = enabled,
                telegramApiId = apiId.trim(),
                telegramApiHash = apiHash.trim(),
                telegramRecipient = recipient.trim(),
                telegramFallbackWhatsApp = fallbackWhatsApp
            )
        )
    }

    private fun SettingsEntity.toDomain() = AppSettings(
        id = id,
        brotherWhatsAppNumber = brotherWhatsAppNumber,
        commissionRatePerThousand = commissionRatePerThousand,
        lastBackupAt = lastBackupAt,
        messageTemplate = messageTemplate,
        telegramEnabled = telegramEnabled,
        telegramApiId = telegramApiId,
        telegramApiHash = telegramApiHash,
        telegramRecipient = telegramRecipient,
        telegramFallbackWhatsApp = telegramFallbackWhatsApp
    )
}
