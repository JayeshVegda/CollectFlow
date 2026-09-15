package com.jayesh.cashcollect.service.telegram

import android.content.Context
import android.graphics.Bitmap
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.service.whatsapp.WhatsAppLauncher
import io.github.tdlibandroid.ktx.TdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

sealed class TelegramAuthState {
    object Uninitialized : TelegramAuthState()
    object Initializing : TelegramAuthState()
    object WaitingParameters : TelegramAuthState()
    data class ShowingQr(val link: String, val qrBitmap: ImageBitmap?) : TelegramAuthState()
    data class WaitingPassword(val passwordHint: String?) : TelegramAuthState()
    data class Ready(val userId: Long, val firstName: String, val username: String?) : TelegramAuthState()
    data class Error(val message: String) : TelegramAuthState()
    object Closed : TelegramAuthState()
}

class TelegramManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var client: TdClient? = null

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Uninitialized)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    // Cache resolved chat IDs: recipient string (username/phone/id) -> chatId
    private val chatCache = ConcurrentHashMap<String, Long>()

    // Pending dispatches: temporary Message ID -> collectionId
    private val pendingOutboundMessages = ConcurrentHashMap<Long, Long>()

    // Callbacks registered for send results
    var onMessageSendSucceeded: ((collectionId: Long) -> Unit)? = null
    var onMessageSendFailed: ((collectionId: Long, error: String) -> Unit)? = null

    private var currentApiId: Int = 0
    private var currentApiHash: String = ""

    companion object {
        private const val TAG = "TelegramManager"
        private const val KEYSTORE_ALIAS = "collectflow_tdlib_aes_v1"
        private const val PREFS_NAME = "tdlib_secure_prefs"
        private const val PREF_KEY_ENC_KEY = "enc_db_key"
        private const val PREF_KEY_IV = "enc_db_iv"

        init {
            try {
                System.loadLibrary("tdjni")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load libtdjni.so in static init", e)
            }
        }
    }

    /**
     * Start/Initialize TDLib native engine with credentials.
     */
    fun start(apiId: Int, apiHash: String) {
        if (apiId <= 0 || apiHash.isBlank()) {
            _authState.value = TelegramAuthState.Error("API ID and API Hash are required")
            return
        }

        currentApiId = apiId
        currentApiHash = apiHash

        if (client != null) {
            return
        }

        _authState.value = TelegramAuthState.Initializing

        try {
            val tdlibDir = File(context.filesDir, "tdlib").apply { if (!exists()) mkdirs() }

            val newClient = TdClient(
                filesDir = tdlibDir.absolutePath,
                verbosityLevel = 1,
                apiId = apiId,
                apiHash = apiHash,
                dispatcher = Dispatchers.IO
            )

            client = newClient
            newClient.init()

            scope.launch {
                newClient.updates.collect { update ->
                    handleUpdate(update)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing TDLib client", e)
            _authState.value = TelegramAuthState.Error("Init error: ${e.message}")
        }
    }

    private suspend fun handleUpdate(update: TdApi.Update) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                when (val state = update.authorizationState) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        _authState.value = TelegramAuthState.WaitingParameters
                        sendTdlibParameters()
                    }

                    is TdApi.AuthorizationStateWaitPhoneNumber -> {
                        // Request QR Code Auth directly
                        requestQrCodeAuth()
                    }

                    is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> {
                        val qrBitmap = generateQrCodeBitmap(state.link, 512)
                        _authState.value = TelegramAuthState.ShowingQr(state.link, qrBitmap)
                    }

                    is TdApi.AuthorizationStateWaitPassword -> {
                        _authState.value = TelegramAuthState.WaitingPassword(state.passwordHint)
                    }

                    is TdApi.AuthorizationStateReady -> {
                        try {
                            val me = client?.send(TdApi.GetMe())
                            if (me is TdApi.User) {
                                val uname = me.usernames?.activeUsernames?.firstOrNull()
                                _authState.value = TelegramAuthState.Ready(
                                    userId = me.id,
                                    firstName = me.firstName,
                                    username = uname
                                )
                            } else {
                                _authState.value = TelegramAuthState.Ready(0L, "Connected", null)
                            }
                        } catch (e: Exception) {
                            _authState.value = TelegramAuthState.Ready(0L, "Connected", null)
                        }
                    }

                    is TdApi.AuthorizationStateClosed -> {
                        _authState.value = TelegramAuthState.Closed
                        client = null
                    }

                    is TdApi.AuthorizationStateLoggingOut -> {
                        _authState.value = TelegramAuthState.Initializing
                    }
                }
            }

            is TdApi.UpdateMessageSendSucceeded -> {
                val tempId = update.oldMessageId
                val collectionId = pendingOutboundMessages.remove(tempId)
                    ?: pendingOutboundMessages.remove(update.message.id)
                if (collectionId != null) {
                    Log.i(TAG, "Message dispatch confirmed by Telegram server for collection #$collectionId")
                    onMessageSendSucceeded?.invoke(collectionId)
                }
            }

            is TdApi.UpdateMessageSendFailed -> {
                val tempId = update.oldMessageId
                val collectionId = pendingOutboundMessages.remove(tempId)
                    ?: pendingOutboundMessages.remove(update.message.id)
                if (collectionId != null) {
                    val errMsg = update.errorMessage.ifBlank { "Error code: ${update.errorCode}" }
                    Log.e(TAG, "Message send failed for collection #$collectionId: $errMsg")
                    onMessageSendFailed?.invoke(collectionId, errMsg)
                }
            }
        }
    }

    private suspend fun sendTdlibParameters() {
        val c = client ?: return
        val dbDir = File(context.filesDir, "tdlib_db").apply { if (!exists()) mkdirs() }
        val filesDir = File(context.filesDir, "tdlib_files").apply { if (!exists()) mkdirs() }

        val encryptionKey = getOrCreateDatabaseKey()

        val params = TdApi.SetTdlibParameters().apply {
            databaseDirectory = dbDir.absolutePath
            this.filesDirectory = filesDir.absolutePath
            databaseEncryptionKey = encryptionKey
            useFileDatabase = true
            useChatInfoDatabase = true
            useMessageDatabase = true
            useSecretChats = false
            apiId = currentApiId
            apiHash = currentApiHash
            systemLanguageCode = "en"
            deviceModel = "Nothing Phone (2a) Plus"
            systemVersion = "Android 15"
            applicationVersion = "2.0.0"
        }

        try {
            c.send(params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send TdlibParameters", e)
            _authState.value = TelegramAuthState.Error("Parameters failed: ${e.message}")
        }
    }

    /**
     * Request QR Code login from TDLib.
     */
    fun requestQrCodeAuth() {
        scope.launch {
            try {
                client?.send(TdApi.RequestQrCodeAuthentication())
            } catch (e: Exception) {
                Log.e(TAG, "RequestQrCodeAuthentication failed", e)
            }
        }
    }

    /**
     * Submit 2FA password if user account requires it.
     */
    fun checkPassword(password: String) {
        scope.launch {
            try {
                val res = client?.send(TdApi.CheckAuthenticationPassword(password))
                if (res is TdApi.Error) {
                    _authState.value = TelegramAuthState.Error("Invalid 2FA password: ${res.message}")
                }
            } catch (e: Exception) {
                _authState.value = TelegramAuthState.Error("Password check error: ${e.message}")
            }
        }
    }

    /**
     * Log out session.
     */
    fun logOut() {
        scope.launch {
            try {
                client?.send(TdApi.LogOut())
            } catch (e: Exception) {
                Log.e(TAG, "LogOut error", e)
            }
        }
    }

    /**
     * Send collection receipt message to brother's Telegram.
     */
    suspend fun sendCollectionReceipt(
        collection: CollectionItem,
        recipient: String,
        template: String
    ): Result<Long> {
        val c = client ?: return Result.failure(IllegalStateException("Telegram client is not running"))

        if (_authState.value !is TelegramAuthState.Ready) {
            return Result.failure(IllegalStateException("Telegram is not logged in"))
        }

        val cleanRecipient = recipient.trim()
        if (cleanRecipient.isBlank()) {
            return Result.failure(IllegalArgumentException("Recipient Telegram username or chat ID is not configured in Settings"))
        }

        return try {
            val chatId = resolveChatId(cleanRecipient)
                ?: return Result.failure(IllegalArgumentException("Could not find Telegram user/chat for: $cleanRecipient"))

            val msgText = WhatsAppLauncher.buildReceiptMessage(collection, template)

            val inputContent = TdApi.InputMessageText().apply {
                text = TdApi.FormattedText().apply {
                    this.text = msgText
                    entities = emptyArray()
                }
            }

            val sendReq = TdApi.SendMessage().apply {
                this.chatId = chatId
                this.inputMessageContent = inputContent
            }

            val sentMsg = c.send(sendReq)
            if (sentMsg is TdApi.Message) {
                pendingOutboundMessages[sentMsg.id] = collection.id
                Log.i(TAG, "Enqueued TDLib message tempId=${sentMsg.id} for collection #${collection.id}")
                Result.success(sentMsg.id)
            } else if (sentMsg is TdApi.Error) {
                Result.failure(Exception(sentMsg.message))
            } else {
                Result.failure(Exception("Unknown TDLib response: $sentMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Telegram message", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves username, numeric chat ID, or phone to TDLib chatId.
     */
    private suspend fun resolveChatId(recipient: String): Long? {
        val c = client ?: return null

        chatCache[recipient]?.let { return it }

        // 1. Numeric ID
        val numericId = recipient.toLongOrNull()
        if (numericId != null) {
            chatCache[recipient] = numericId
            return numericId
        }

        // 2. Username search (e.g. @brother or brother)
        val username = recipient.removePrefix("@")
        try {
            val chat = c.send(TdApi.SearchPublicChat(username))
            if (chat is TdApi.Chat) {
                chatCache[recipient] = chat.id
                return chat.id
            }
        } catch (e: Exception) {
            Log.w(TAG, "SearchPublicChat failed for $username: ${e.message}")
        }

        // 3. Fallback: Search Contacts
        try {
            val contacts = c.send(TdApi.SearchContacts(username, 5))
            if (contacts is TdApi.Users && contacts.userIds.isNotEmpty()) {
                val userId = contacts.userIds[0]
                val privChat = c.send(TdApi.CreatePrivateChat(userId, false))
                if (privChat is TdApi.Chat) {
                    chatCache[recipient] = privChat.id
                    return privChat.id
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SearchContacts failed for $username: ${e.message}")
        }

        return null
    }

    /**
     * Generate or retrieve a 256-bit AES database encryption key protected by Android KeyStore.
     */
    private fun getOrCreateDatabaseKey(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!ks.containsAlias(KEYSTORE_ALIAS)) {
                val kpg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                kpg.init(
                    KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                kpg.generateKey()
            }

            val secretKey = ks.getKey(KEYSTORE_ALIAS, null) as SecretKey
            val encKeyHex = prefs.getString(PREF_KEY_ENC_KEY, null)
            val ivHex = prefs.getString(PREF_KEY_IV, null)

            if (encKeyHex != null && ivHex != null) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, hexToBytes(ivHex)))
                return cipher.doFinal(hexToBytes(encKeyHex))
            } else {
                val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val encryptedKey = cipher.doFinal(rawKey)
                val iv = cipher.iv

                prefs.edit()
                    .putString(PREF_KEY_ENC_KEY, bytesToHex(encryptedKey))
                    .putString(PREF_KEY_IV, bytesToHex(iv))
                    .apply()

                return rawKey
            }
        } catch (e: Exception) {
            Log.e(TAG, "KeyStore encryption error; using fallback persistent key", e)
            // Fallback: generate persistent 32 bytes in secure private prefs if Keystore fails on device
            var fallbackHex = prefs.getString("fallback_raw_key", null)
            if (fallbackHex == null) {
                val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
                fallbackHex = bytesToHex(rawKey)
                prefs.edit().putString("fallback_raw_key", fallbackHex).apply()
            }
            return hexToBytes(fallbackHex)
        }
    }

    private fun generateQrCodeBitmap(contents: String, size: Int): ImageBitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(contents, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render QR Code bitmap", e)
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
