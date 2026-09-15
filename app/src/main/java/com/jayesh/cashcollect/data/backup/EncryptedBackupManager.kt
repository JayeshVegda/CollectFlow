package com.jayesh.cashcollect.data.backup

import android.content.Context
import androidx.room.withTransaction
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.data.local.entity.CustomerEntity
import com.jayesh.cashcollect.data.local.entity.SettingsEntity
import com.jayesh.cashcollect.domain.template.MessageTemplateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EncryptedBackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun createEncryptedBackup(): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupDir, "collectflow_backup_$timeStamp.enc")

        if (backupFile.exists()) backupFile.delete()

        // 1. Gather all database records
        val customers = database.customerDao().getAllSync()
        val collections = database.collectionDao().getAllSync()
        val settings = database.settingsDao().getSettingsSync()

        val rootJson = JSONObject().apply {
            put("version", 1)
            put("exported_at", System.currentTimeMillis())

            val customersArray = JSONArray()
            for (c in customers) {
                customersArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("alias", c.alias ?: JSONObject.NULL)
                    put("created_at", c.createdAt)
                    put("last_used_at", c.lastUsedAt)
                })
            }
            put("customers", customersArray)

            val collectionsArray = JSONArray()
            for (c in collections) {
                collectionsArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("customer_id", c.customerId)
                    put("amount_paise", c.amountPaise)
                    put("commission_rate_snapshot", c.commissionRateSnapshot)
                    put("commission_paise", c.commissionPaise)
                    put("status", c.status)
                    put("created_at", c.createdAt)
                    put("received_at", c.receivedAt ?: JSONObject.NULL)
                    put("whatsapp_opened_at", c.whatsappOpenedAt ?: JSONObject.NULL)
                    put("confirmed_sent_at", c.confirmedSentAt ?: JSONObject.NULL)
                    put("voided_at", c.voidedAt ?: JSONObject.NULL)
                    put("void_reason", c.voidReason ?: JSONObject.NULL)
                    put("replaced_by_id", c.replacedById ?: JSONObject.NULL)
                    put("replaces_id", c.replacesId ?: JSONObject.NULL)
                    put("note", c.note ?: JSONObject.NULL)
                    put("last_dispatch_error", c.lastDispatchError ?: JSONObject.NULL)
                    put("last_dispatch_attempt_at", c.lastDispatchAttemptAt ?: JSONObject.NULL)
                })
            }
            put("collections", collectionsArray)

            if (settings != null) {
                put("settings", JSONObject().apply {
                    put("brother_whatsapp_number", settings.brotherWhatsAppNumber)
                    put("commission_rate_per_thousand", settings.commissionRatePerThousand)
                    put("last_backup_at", settings.lastBackupAt ?: JSONObject.NULL)
                    put("message_template", settings.messageTemplate)
                    put("telegram_enabled", settings.telegramEnabled)
                    put("telegram_api_id", settings.telegramApiId)
                    put("telegram_api_hash", settings.telegramApiHash)
                    put("telegram_recipient", settings.telegramRecipient)
                    put("telegram_fallback_whatsapp", settings.telegramFallbackWhatsApp)
                })
            }
        }

        val jsonBytes = rootJson.toString().toByteArray(Charsets.UTF_8)

        // 2. Encrypt using Jetpack Security Tink MasterKey
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            backupFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(jsonBytes)
            outputStream.flush()
        }

        database.settingsDao().updateLastBackupTimestamp(System.currentTimeMillis())
        backupFile
    }

    suspend fun restoreEncryptedBackup(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val jsonString = encryptedFile.openFileInput().bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }

            val rootJson = JSONObject(jsonString)
            val version = rootJson.optInt("version", 0)
            if (version != 1) {
                return@withContext Result.failure(IllegalArgumentException("Unsupported backup version: $version"))
            }

            val customersArray = rootJson.getJSONArray("customers")
            val collectionsArray = rootJson.getJSONArray("collections")

            val parsedCustomers = mutableListOf<CustomerEntity>()
            for (i in 0 until customersArray.length()) {
                val obj = customersArray.getJSONObject(i)
                parsedCustomers.add(
                    CustomerEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        alias = if (obj.isNull("alias")) null else obj.getString("alias"),
                        createdAt = obj.getLong("created_at"),
                        lastUsedAt = obj.getLong("last_used_at")
                    )
                )
            }

            val parsedCollections = mutableListOf<CollectionEntity>()
            for (i in 0 until collectionsArray.length()) {
                val obj = collectionsArray.getJSONObject(i)
                parsedCollections.add(
                    CollectionEntity(
                        id = obj.getLong("id"),
                        customerId = obj.getLong("customer_id"),
                        amountPaise = obj.getLong("amount_paise"),
                        commissionRateSnapshot = obj.getInt("commission_rate_snapshot"),
                        commissionPaise = obj.getLong("commission_paise"),
                        status = obj.getString("status"),
                        createdAt = obj.getLong("created_at"),
                        receivedAt = if (obj.isNull("received_at")) null else obj.getLong("received_at"),
                        whatsappOpenedAt = if (obj.isNull("whatsapp_opened_at")) null else obj.getLong("whatsapp_opened_at"),
                        confirmedSentAt = if (obj.isNull("confirmed_sent_at")) null else obj.getLong("confirmed_sent_at"),
                        voidedAt = if (obj.isNull("voided_at")) null else obj.getLong("voided_at"),
                        voidReason = if (obj.isNull("void_reason")) null else obj.getString("void_reason"),
                        replacedById = if (obj.isNull("replaced_by_id")) null else obj.getLong("replaced_by_id"),
                        replacesId = if (obj.isNull("replaces_id")) null else obj.getLong("replaces_id"),
                        note = if (obj.isNull("note")) null else obj.getString("note"),
                        lastDispatchError = if (obj.isNull("last_dispatch_error")) null else obj.getString("last_dispatch_error"),
                        lastDispatchAttemptAt = if (obj.isNull("last_dispatch_attempt_at")) null else obj.getLong("last_dispatch_attempt_at")
                    )
                )
            }

            // Restore in single transaction
            database.withTransaction {
                database.collectionDao().deleteAll()
                database.customerDao().deleteAll()

                database.customerDao().insertAll(parsedCustomers)
                database.collectionDao().insertAll(parsedCollections)

                if (rootJson.has("settings")) {
                    val settingsObj = rootJson.getJSONObject("settings")
                    database.settingsDao().insertOrUpdate(
                        SettingsEntity(
                            id = 1L,
                            brotherWhatsAppNumber = settingsObj.optString("brother_whatsapp_number", ""),
                            commissionRatePerThousand = settingsObj.optInt("commission_rate_per_thousand", 3),
                            lastBackupAt = System.currentTimeMillis(),
                            messageTemplate = settingsObj.optString("message_template", MessageTemplateEngine.DEFAULT_TEMPLATE),
                            telegramEnabled = settingsObj.optBoolean("telegram_enabled", false),
                            telegramApiId = settingsObj.optString("telegram_api_id", ""),
                            telegramApiHash = settingsObj.optString("telegram_api_hash", ""),
                            telegramRecipient = settingsObj.optString("telegram_recipient", ""),
                            telegramFallbackWhatsApp = settingsObj.optBoolean("telegram_fallback_whatsapp", true)
                        )
                    )
                }
            }

            Result.success(parsedCollections.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
