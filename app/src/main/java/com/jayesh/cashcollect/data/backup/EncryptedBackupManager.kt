package com.jayesh.cashcollect.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.jayesh.cashcollect.data.local.AppDatabase
import com.jayesh.cashcollect.data.local.entity.CollectionEntity
import com.jayesh.cashcollect.data.local.entity.CustomerEntity
import com.jayesh.cashcollect.data.local.entity.SettingsEntity
import com.jayesh.cashcollect.domain.model.AppSettings
import com.jayesh.cashcollect.domain.money.CommissionCalculator
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
                })
            }
            put("collections", collectionsArray)

            if (settings != null) {
                put("settings", JSONObject().apply {
                    put("brother_whatsapp_number", settings.brotherWhatsAppNumber)
                    put("commission_rate_per_thousand", settings.commissionRatePerThousand)
                    put("last_backup_at", settings.lastBackupAt ?: JSONObject.NULL)
                    put("message_template", settings.messageTemplate)
                    // Every setting is exported, not just the ones that were here first. Omitting a
                    // field makes restore silently reset it to its default.
                    put("notification_delay_ms", settings.notificationDelayMs)
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

    /**
     * Restores a backup the operator picked through the system file picker.
     *
     * A `content://` URI is only readable while the grant lasts and `EncryptedFile` needs a real
     * path, so the stream is staged into the app's private cache first. The staged copy is removed
     * in `finally` — it is ciphertext, but it has no reason to linger in a shared cache directory.
     */
    suspend fun restoreFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        val staged = File(context.cacheDir, "restore_pending.enc")
        try {
            val opened = context.contentResolver.openInputStream(uri)
            if (opened == null) {
                return@withContext Result.failure(
                    IllegalArgumentException("Could not read the selected file.")
                )
            }
            opened.use { input ->
                staged.outputStream().use { output -> input.copyTo(output) }
            }
            restoreEncryptedBackup(staged)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { staged.delete() }
        }
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
                        note = if (obj.isNull("note")) null else obj.getString("note")
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
                    // Every field comes back out of the file, including the receipt-nudge delay and
                    // the last-backup stamp. The previous version rebuilt the row from hardcoded
                    // defaults, so restoring silently reset a configured delay back to 4s — a
                    // backup that does not round-trip is not a backup.
                    database.settingsDao().insertOrUpdate(
                        SettingsEntity(
                            id = 1L,
                            brotherWhatsAppNumber = settingsObj.optString("brother_whatsapp_number", ""),
                            commissionRatePerThousand = settingsObj.optInt(
                                "commission_rate_per_thousand",
                                CommissionCalculator.DEFAULT_RATE_PER_THOUSAND
                            ),
                            lastBackupAt = if (settingsObj.isNull("last_backup_at")) {
                                System.currentTimeMillis()
                            } else {
                                settingsObj.optLong("last_backup_at", System.currentTimeMillis())
                            },
                            messageTemplate = settingsObj.optString(
                                "message_template",
                                MessageTemplateEngine.DEFAULT_TEMPLATE
                            ),
                            // Absent in backups written before this field existed, which is why it
                            // is read with the app's own default rather than assuming it is there.
                            notificationDelayMs = settingsObj.optInt(
                                "notification_delay_ms",
                                AppSettings.DEFAULT_NOTIFICATION_DELAY_MS
                            )
                        )
                    )
                }
            }

            Result.success(parsedCollections.size)
        } catch (e: Exception) {
            Result.failure(
                if (isDecryptionFailure(e)) {
                    // Worth distinguishing: "wrong file" and "right file, wrong device" need
                    // different actions from the operator, and the second one is not obvious.
                    IllegalStateException(
                        "This backup cannot be opened on this device. It was encrypted with a key " +
                            "that lives in the Android Keystore of the phone that created it, and " +
                            "that key never leaves that phone.",
                        e
                    )
                } else {
                    e
                }
            )
        }
    }

    /**
     * True when the failure is a key/authentication failure rather than a missing or malformed file.
     *
     * EncryptedFile surfaces these at different depths — Tink throws `GeneralSecurityException`, and
     * the streaming reader wraps some of them in `IOException` — so the cause chain is walked.
     */
    private fun isDecryptionFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 5) {
            if (current is javax.crypto.AEADBadTagException) return true
            if (current is java.security.GeneralSecurityException) return true
            current = current.cause
            depth++
        }
        return false
    }
}
