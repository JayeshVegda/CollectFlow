package com.jayesh.cashcollect.service.telegram

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.jayesh.cashcollect.domain.model.CollectionItem
import com.jayesh.cashcollect.service.whatsapp.WhatsAppLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.drinkless.tdlib.TdApi
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Authentication state of the personal Telegram session.
 *
 * The QR-code ("link a desktop device") flow was removed in favour of the plain
 * phone-number + login-code + 2FA flow, which is the flow Telegram itself supports most reliably
 * and which can be recovered from when a step fails.
 */
sealed class TelegramAuthState {
    object Uninitialized : TelegramAuthState()
    object Initializing : TelegramAuthState()
    object WaitingParameters : TelegramAuthState()

    /** Phone number not submitted yet (or rejected). */
    data class WaitingPhoneNumber(val lastTriedPhone: String? = null) : TelegramAuthState()

    /** Login code required. [channel] is a human readable description of where it was sent. */
    data class WaitingCode(
        val phoneNumber: String,
        val channel: String,
        val isCodeInTelegramApp: Boolean
    ) : TelegramAuthState()

    data class WaitingPassword(
        val hint: String?,
        val recoveryEmail: String?
    ) : TelegramAuthState()

    data class Ready(
        val userId: Long,
        val firstName: String,
        val username: String?
    ) : TelegramAuthState()

    /** Real, human-readable failure reason (TDLib error text is preserved). */
    data class Error(val message: String, val code: Int? = null) : TelegramAuthState()

    object LoggingOut : TelegramAuthState()
    object Closed : TelegramAuthState()
}

/** Network reachability of the TDLib client, shown as a status pill. */
enum class TelegramConnection { Unknown, Connecting, Online, Offline }

class TelegramManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val tdClient = TdLibClient(tag = TAG) { level, message ->
        Log.println(if (level == "E") Log.ERROR else Log.DEBUG, TAG, message)
    }

    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Uninitialized)
    val authState: StateFlow<TelegramAuthState> = _authState.asStateFlow()

    private val _connection = MutableStateFlow(TelegramConnection.Unknown)
    val connection: StateFlow<TelegramConnection> = _connection.asStateFlow()

    /** Ring buffer of raw TDLib log lines, surfaced by the in-app diagnostics screen. */
    private val _diagnostics = MutableStateFlow<List<String>>(emptyList())
    val diagnostics: StateFlow<List<String>> = _diagnostics.asStateFlow()

    // Cache resolved chat IDs: recipient string (username/phone/id) -> chatId
    private val chatCache = ConcurrentHashMap<String, Long>()

    // Pending dispatches: temporary Message ID -> collectionId
    private val pendingOutboundMessages = ConcurrentHashMap<Long, Long>()

    // Serialises "queue send + record tempId" against "delivery ACK handled", so an ACK that
    // arrives the instant TDLib accepts the message can never be processed before the tempId
    // mapping exists (which would strand the entry as RECEIPT_CONFIRMED forever).
    private val outboundLock = Mutex()

    // Callbacks registered for send results
    var onMessageSendSucceeded: ((collectionId: Long) -> Unit)? = null
    var onMessageSendFailed: ((collectionId: Long, error: String) -> Unit)? = null

    fun isReady(): Boolean = _authState.value is TelegramAuthState.Ready

    private var currentApiId: Int = 0
    private var currentApiHash: String = ""
    private var collectorsStarted = false

    private val databaseDir: File
        get() = File(context.filesDir, "tdlib_db").apply { if (!exists()) mkdirs() }

    private val tdFilesDir: File
        get() = File(context.filesDir, "tdlib_files").apply { if (!exists()) mkdirs() }

    private fun appendDiagnostic(line: String) {
        // Also mirror into logcat. The in-memory buffer is only visible inside the app, which made
        // every TDLib failure invisible during on-device debugging.
        Log.println(if (line.startsWith("[E]")) Log.ERROR else Log.DEBUG, TAG, line)
        val current = _diagnostics.value
        _diagnostics.value = if (current.size >= MAX_DIAGNOSTIC_LINES) {
            current.drop(current.size - MAX_DIAGNOSTIC_LINES + 1) + line
        } else {
            current + line
        }
    }

    fun clearDiagnostics() {
        _diagnostics.value = emptyList()
    }

    companion object {
        private const val TAG = "TelegramManager"
        private const val KEYSTORE_ALIAS = "collectflow_tdlib_aes_v1"
        private const val PREFS_NAME = "tdlib_secure_prefs"
        private const val PREF_KEY_ENC_KEY = "enc_db_key"
        private const val PREF_KEY_IV = "enc_db_iv"
        private const val PREF_KEY_FALLBACK = "fallback_raw_key"
        private const val MAX_DIAGNOSTIC_LINES = 300
        private const val STARTUP_TIMEOUT_MS = 30_000L

        init {
            try {
                System.loadLibrary("tdjni")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load libtdjni.so in static init", e)
            }
        }
    }

    /**
     * Starts the TDLib engine with the given credentials.
     *
     * Order matters: the update collector is registered *before* the native client is created, so no
     * authorization update can be missed. The authoritative current state is then fetched with
     * [TdApi.GetAuthorizationState], so the UI never depends on catching that first update.
     */
    fun start(apiId: Int, apiHash: String, logToFile: Boolean = false) {
        if (apiId <= 0 || apiHash.isBlank()) {
            _authState.value = TelegramAuthState.Error(
                "API ID and API Hash are required. Create them at my.telegram.org"
            )
            return
        }
        currentApiId = apiId
        currentApiHash = apiHash

        if (tdClient.isStarted) {
            scope.launch { refreshAuthorizationState() }
            return
        }

        parametersSent = false
        _authState.value = TelegramAuthState.Initializing
        _connection.value = TelegramConnection.Connecting

        scope.launch {
            if (!collectorsStarted) {
                collectorsStarted = true
                // UNDISPATCHED: run each collector synchronously until it suspends on the flow
                // subscription. A plain launch() only *schedules* the body, so tdClient.start()
                // below could create the native client and let TDLib emit its first
                // UpdateAuthorizationState before anything was subscribed — the update was then
                // dropped and the login flow hung on CONNECTING forever.
                launch(start = CoroutineStart.UNDISPATCHED) {
                    tdClient.updates.collect { handleUpdate(it) }
                }
                launch(start = CoroutineStart.UNDISPATCHED) {
                    tdClient.logLines.collect { appendDiagnostic(it) }
                }
            }
            try {
                removeLegacyUnencryptedDatabase()
                tdClient.start(
                    logFilePath = if (logToFile) {
                        File(databaseDir, "tdlib.log").absolutePath
                    } else {
                        null
                    }
                )
                refreshAuthorizationState()
            } catch (e: Throwable) {
                val code = (e as? TdException)?.code
                Log.e(TAG, "Failed to start TDLib", e)
                appendDiagnostic("[E] start failed: ${e.message}")
                _authState.value = TelegramAuthState.Error(
                    e.message ?: "Could not start the Telegram engine",
                    code
                )
            }
            delay(STARTUP_TIMEOUT_MS)
            if (_authState.value is TelegramAuthState.Initializing) {
                _authState.value = TelegramAuthState.Error(
                    "Telegram did not respond within ${STARTUP_TIMEOUT_MS / 1000}s. " +
                        "Check this phone's internet connection."
                )
            }
        }
    }

    /** Closes and recreates the native client. Use when the session is wedged. */
    fun restart(logToFile: Boolean = false) {
        val apiId = currentApiId
        val apiHash = currentApiHash
        scope.launch {
            runCatching { tdClient.stop() }
            _authState.value = TelegramAuthState.Uninitialized
            _connection.value = TelegramConnection.Unknown
            if (apiId > 0 && apiHash.isNotBlank()) {
                start(apiId, apiHash, logToFile)
            }
        }
    }

    private suspend fun refreshAuthorizationState() {
        val state = try {
            tdClient.send(TdApi.GetAuthorizationState())
        } catch (e: Exception) {
            appendDiagnostic("[W] GetAuthorizationState failed: ${e.message}")
            null
        } ?: return
        applyAuthorizationState(state)
    }

    /**
     * The previous implementation let the third-party wrapper create an *unencrypted* database in
     * `files/tdlib`. That database cannot be reused with a different encryption key, so it is
     * removed once, the first time the encrypted database is created.
     */
    private fun removeLegacyUnencryptedDatabase() {
        if (File(context.filesDir, "tdlib_db").exists()) return
        val legacy = File(context.filesDir, "tdlib")
        if (legacy.exists() && legacy.deleteRecursively()) {
            appendDiagnostic("[W] removed legacy unencrypted TDLib database (${legacy.name})")
        }
    }

    private suspend fun handleUpdate(update: TdApi.Update) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> applyAuthorizationState(update.authorizationState)

            is TdApi.UpdateConnectionState -> {
                _connection.value = when (update.state) {
                    is TdApi.ConnectionStateReady -> TelegramConnection.Online
                    is TdApi.ConnectionStateWaitingForNetwork -> TelegramConnection.Offline
                    is TdApi.ConnectionStateConnecting,
                    is TdApi.ConnectionStateConnectingToProxy,
                    is TdApi.ConnectionStateUpdating -> TelegramConnection.Connecting

                    else -> TelegramConnection.Unknown
                }
            }

            is TdApi.UpdateMessageSendSucceeded -> {
                val tempId = update.oldMessageId
                val realId = update.message.id
                val collectionId = outboundLock.withLock {
                    pendingOutboundMessages.remove(tempId)
                        ?: pendingOutboundMessages.remove(realId)
                }
                if (collectionId != null) {
                    appendDiagnostic("[I] Telegram confirmed delivery for entry #$collectionId")
                    onMessageSendSucceeded?.invoke(collectionId)
                }
            }

            is TdApi.UpdateMessageSendFailed -> {
                val tempId = update.oldMessageId
                val realId = update.message.id
                val collectionId = outboundLock.withLock {
                    pendingOutboundMessages.remove(tempId)
                        ?: pendingOutboundMessages.remove(realId)
                }
                if (collectionId != null) {
                    val errMsg = update.error?.message?.ifBlank { "Error code: ${update.error?.code}" }
                        ?: "Send failed"
                    Log.e(TAG, "Message send failed for collection #$collectionId: $errMsg")
                    appendDiagnostic("[E] send failed for #$collectionId: $errMsg")
                    onMessageSendFailed?.invoke(collectionId, errMsg)
                }
            }
        }
    }

    private suspend fun applyAuthorizationState(state: TdApi.AuthorizationState) {
        appendDiagnostic("[I] auth state -> ${state.javaClass.simpleName}")
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                _authState.value = TelegramAuthState.WaitingParameters
                runCatching { sendTdlibParameters() }
                    .onFailure { appendDiagnostic("[E] could not send TDLib parameters: ${it.message}") }
            }

            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                _authState.value = TelegramAuthState.WaitingPhoneNumber()
            }

            is TdApi.AuthorizationStateWaitCode -> {
                val info = state.codeInfo
                _authState.value = TelegramAuthState.WaitingCode(
                    phoneNumber = info?.phoneNumber.orEmpty(),
                    channel = describeCodeChannel(info?.type),
                    isCodeInTelegramApp = info?.type is TdApi.AuthenticationCodeTypeTelegramMessage
                )
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                _authState.value = TelegramAuthState.WaitingPassword(
                    hint = state.passwordHint?.takeIf { it.isNotBlank() },
                    recoveryEmail = state.recoveryEmailAddressPattern?.takeIf { it.isNotBlank() }
                )
            }

            is TdApi.AuthorizationStateWaitRegistration -> {
                _authState.value = TelegramAuthState.Error(
                    "This phone number has no Telegram account yet. Sign up in the Telegram app " +
                        "first, then try again."
                )
            }

            is TdApi.AuthorizationStateWaitEmailAddress,
            is TdApi.AuthorizationStateWaitEmailCode -> {
                _authState.value = TelegramAuthState.Error(
                    "Telegram is asking for an email code, which this app cannot complete. " +
                        "Open Telegram once on this phone to finish, then try again."
                )
            }

            is TdApi.AuthorizationStateReady -> {
                _connection.value = TelegramConnection.Online
                val me = try {
                    tdClient.send(TdApi.GetMe())
                } catch (e: Exception) {
                    appendDiagnostic("[W] GetMe failed: ${e.message}")
                    null
                }
                _authState.value = if (me is TdApi.User) {
                    TelegramAuthState.Ready(
                        userId = me.id,
                        firstName = me.firstName.orEmpty(),
                        username = me.usernames?.activeUsernames?.firstOrNull()
                    )
                } else {
                    TelegramAuthState.Ready(0L, "Connected", null)
                }
            }

            is TdApi.AuthorizationStateLoggingOut -> {
                _authState.value = TelegramAuthState.LoggingOut
            }

            is TdApi.AuthorizationStateClosed -> {
                _authState.value = TelegramAuthState.Closed
                _connection.value = TelegramConnection.Unknown
            }

            else -> appendDiagnostic("[I] unhandled auth state ${state.javaClass.simpleName}")
        }
    }

    private fun describeCodeChannel(type: TdApi.AuthenticationCodeType?): String = when (type) {
        is TdApi.AuthenticationCodeTypeSms -> "SMS"
        is TdApi.AuthenticationCodeTypeCall -> "phone call"
        is TdApi.AuthenticationCodeTypeFlashCall -> "flash call"
        is TdApi.AuthenticationCodeTypeMissedCall -> "missed call"
        is TdApi.AuthenticationCodeTypeSmsWord -> "SMS"
        is TdApi.AuthenticationCodeTypeSmsPhrase -> "SMS"
        is TdApi.AuthenticationCodeTypeTelegramMessage -> "the Telegram app"
        is TdApi.AuthenticationCodeTypeFragment -> "Fragment"
        is TdApi.AuthenticationCodeTypeFirebaseAndroid -> "SMS"
        else -> "Telegram"
    }

    private val parametersMutex = Mutex()
    private var parametersSent = false

    private val _transientError = MutableStateFlow<String?>(null)

    /** Non-fatal error (e.g. a wrong 2FA password) that must not destroy the current auth state. */
    val transientError: StateFlow<String?> = _transientError.asStateFlow()

    fun clearTransientError() {
        _transientError.value = null
    }

    private suspend fun sendTdlibParameters() = parametersMutex.withLock {
        if (parametersSent) return@withLock
        val params = TdApi.SetTdlibParameters().apply {
            databaseDirectory = databaseDir.absolutePath
            filesDirectory = tdFilesDir.absolutePath
            // 256-bit key protected by the Android KeyStore. Required to read the local database.
            databaseEncryptionKey = getOrCreateDatabaseKey()
            useFileDatabase = true
            useChatInfoDatabase = true
            useMessageDatabase = true
            useSecretChats = false
            apiId = currentApiId
            apiHash = currentApiHash
            systemLanguageCode = "en"
            deviceModel = android.os.Build.MODEL ?: "Android"
            systemVersion = "Android ${android.os.Build.VERSION.RELEASE}"
            applicationVersion = "2.1.0"
        }
        // Mark as sent before awaiting: TDLib emits UpdateAuthorizationState(WaitTdlibParameters)
        // as soon as the client exists, and the update collector may call us concurrently. Setting
        // the flag inside the lock, before the suspending send, makes the call exactly-once.
        parametersSent = true
        try {
            tdClient.send(params)
            appendDiagnostic("[I] TDLib parameters sent (Keystore-encrypted database)")
        } catch (e: Throwable) {
            parametersSent = false
            appendDiagnostic("[E] SetTdlibParameters failed: ${e.message}")
            throw e
        }
    }

    /**
     * Step 1 of login: submit the phone number. Telegram then sends a login code (usually to the
     * Telegram app itself, sometimes by SMS).
     */
    fun requestLogin(phoneNumber: String) {
        val normalized = normalizePhoneNumber(phoneNumber)
        if (normalized.length < 8) {
            _authState.value = TelegramAuthState.Error(
                "Enter the Telegram phone number with country code, for example +919510233829"
            )
            return
        }
        scope.launch {
            _transientError.value = null
            _authState.value = TelegramAuthState.Initializing
            val settings = TdApi.PhoneNumberAuthenticationSettings().apply {
                allowFlashCall = false
                allowMissedCall = false
                hasUnknownPhoneNumber = false
                allowSmsRetrieverApi = false
                firebaseAuthenticationSettings = TdApi.FirebaseAuthenticationSettingsAndroid()
                authenticationTokens = emptyArray()
            }
            try {
                tdClient.send(TdApi.SetAuthenticationPhoneNumber(normalized, settings))
                appendDiagnostic("[I] phone number submitted: $normalized")
            } catch (e: Exception) {
                appendDiagnostic("[E] SetAuthenticationPhoneNumber failed: ${e.message}")
                _authState.value = TelegramAuthState.Error(
                    e.message ?: "Telegram rejected that phone number",
                    (e as? TdException)?.code
                )
            }
        }
    }

    /** Step 2 of login: submit the code Telegram just sent. */
    fun submitCode(code: String) {
        val trimmed = code.filter { it.isLetterOrDigit() }
        if (trimmed.isEmpty()) return
        scope.launch {
            _transientError.value = null
            try {
                tdClient.send(TdApi.CheckAuthenticationCode(trimmed))
                appendDiagnostic("[I] login code submitted")
            } catch (e: Exception) {
                appendDiagnostic("[E] CheckAuthenticationCode failed: ${e.message}")
                _transientError.value = e.message ?: "Telegram rejected that login code"
            }
        }
    }

    fun resendCode() {
        scope.launch {
            appendDiagnostic("[I] requesting a new login code")
            tdClient.sendQuietly(TdApi.ResendAuthenticationCode(TdApi.ResendCodeReasonUserRequest()))
        }
    }

    /**
     * Step 3 of login: submit the 2FA cloud password.
     *
     * On failure only [transientError] is set — the [TelegramAuthState.WaitingPassword] state is
     * preserved so a typo does not lock the user out of the dialog.
     */
    fun submitPassword(password: String) {
        if (password.isEmpty()) return
        scope.launch {
            _transientError.value = null
            try {
                tdClient.send(TdApi.CheckAuthenticationPassword(password))
                appendDiagnostic("[I] 2FA password submitted")
            } catch (e: Exception) {
                appendDiagnostic("[E] CheckAuthenticationPassword failed: ${e.message}")
                _transientError.value = e.message ?: "Wrong 2FA password"
            }
        }
    }

    fun logOut() {
        scope.launch {
            _authState.value = TelegramAuthState.LoggingOut
            _transientError.value = null
            chatCache.clear()
            tdClient.sendQuietly(TdApi.LogOut())
        }
    }

    /** Fully stops the native engine (used by the diagnostics screen). */
    fun shutdown() {
        scope.launch {
            runCatching { tdClient.stop() }
            parametersSent = false
            collectorsStarted = false
            pendingOutboundMessages.clear()
            chatCache.clear()
            _authState.value = TelegramAuthState.Closed
            _connection.value = TelegramConnection.Unknown
        }
    }

    private fun normalizePhoneNumber(raw: String): String {
        val cleaned = raw.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
        if (cleaned.isEmpty()) return ""
        return if (cleaned.startsWith("+")) cleaned else "+$cleaned"
    }

    /**
     * Send collection receipt message to brother's Telegram.
     */
    suspend fun sendCollectionReceipt(
        collection: CollectionItem,
        recipient: String,
        template: String
    ): Result<Long> {
        if (!tdClient.isStarted) {
            return Result.failure(IllegalStateException("Telegram engine is not running"))
        }

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

            val sentMsg = outboundLock.withLock {
                val queued = tdClient.send(sendReq)
                pendingOutboundMessages[queued.id] = collection.id
                queued
            }
            appendDiagnostic("[I] queued Telegram message tempId=${sentMsg.id} for entry #${collection.id}")
            Result.success(sentMsg.id)
        } catch (e: TdException) {
            appendDiagnostic("[E] Telegram send failed: ${e.message}")
            Result.failure(Exception(e.message))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send Telegram message", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves username, numeric chat ID, or phone to TDLib chatId.
     */
    private suspend fun resolveChatId(recipient: String): Long? {
        if (!tdClient.isStarted) return null

        val key = recipient.trim()
        chatCache[key]?.let { return it }

        // A phone number must never be used directly as a chat id: that could deliver the receipt
        // to an unrelated chat. Resolve it to a real user first.
        if (looksLikePhoneNumber(key)) {
            val userId = resolveUserIdByPhone(key)
            val chatId = userId?.let { createPrivateChat(it) }
            if (chatId != null) {
                chatCache[key] = chatId
                return chatId
            }
            appendDiagnostic("[W] no Telegram user found for phone number $key")
            return null
        }

        // Pure numeric input that is not a phone number: treat as an explicit Telegram chat id.
        key.toLongOrNull()?.let { numericId ->
            if (numericId != 0L) {
                chatCache[key] = numericId
                return numericId
            }
        }

        val username = key.removePrefix("@")

        // 1. Public username (e.g. @brother)
        val publicChat = tdClient.sendQuietly(TdApi.SearchPublicChat(username))
        if (publicChat is TdApi.Chat) {
            chatCache[key] = publicChat.id
            return publicChat.id
        }
        if (publicChat is TdApi.Error) {
            appendDiagnostic("[W] SearchPublicChat($username): ${publicChat.message}")
        }

        // 2. Existing contact
        val contactMatch = tdClient.sendQuietly(TdApi.SearchContacts(username, 5))
        if (contactMatch is TdApi.Users && contactMatch.userIds.isNotEmpty()) {
            createPrivateChat(contactMatch.userIds[0])?.let { chatId ->
                chatCache[key] = chatId
                return chatId
            }
        }

        // 3. Full display name
        if (key != username) {
            val nameMatch = tdClient.sendQuietly(TdApi.SearchContacts(key, 5))
            if (nameMatch is TdApi.Users && nameMatch.userIds.isNotEmpty()) {
                createPrivateChat(nameMatch.userIds[0])?.let { chatId ->
                    chatCache[key] = chatId
                    return chatId
                }
            }
        }

        return null
    }

    private fun looksLikePhoneNumber(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.startsWith("+")) return true
        val digits = trimmed.filter { it.isDigit() }
        return digits.length in 10..15 && digits.length == trimmed.length
    }

    /**
     * Telegram only resolves arbitrary phone numbers after they have been imported as contacts,
     * so the number is imported first and then looked up.
     */
    private suspend fun resolveUserIdByPhone(phone: String): Long? {
        val normalized = normalizePhoneNumber(phone)

        val imported = tdClient.sendQuietly(
            TdApi.ImportContacts(
                arrayOf(
                    TdApi.ImportedContact().apply {
                        phoneNumber = normalized
                        firstName = "Receipt Recipient"
                        lastName = ""
                        note = TdApi.FormattedText().apply {
                            text = "Added by CollectFlow"
                            entities = emptyArray()
                        }
                    }
                )
            )
        )
        if (imported is TdApi.ImportedContacts) {
            val userId = imported.userIds.firstOrNull { it != 0L }
            if (userId != null) return userId
        }

        val user = tdClient.sendQuietly(TdApi.SearchUserByPhoneNumber(normalized, false))
        if (user is TdApi.User) return user.id

        return null
    }

    private suspend fun createPrivateChat(userId: Long): Long? {
        val chat = tdClient.sendQuietly(TdApi.CreatePrivateChat(userId, false))
        if (chat is TdApi.Chat) return chat.id
        if (chat is TdApi.Error) {
            appendDiagnostic("[W] CreatePrivateChat($userId): ${chat.message}")
        }
        return null
    }

    /**
     * Generate or retrieve a 256-bit AES database encryption key protected by Android KeyStore.
     */
    private fun getOrCreateDatabaseKey(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // A previous run already had to fall back (KeyStore key rotated or invalidated), and the
        // TDLib database on disk was created with that stored raw key. Reuse it unconditionally —
        // otherwise the next launch would mint a different key, find the database unreadable and
        // wipe the Telegram session on every single start.
        prefs.getString(PREF_KEY_FALLBACK, null)?.let { return hexToBytes(it) }

        val encKeyHex = prefs.getString(PREF_KEY_ENC_KEY, null)
        val ivHex = prefs.getString(PREF_KEY_IV, null)

        return try {
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

            if (encKeyHex != null && ivHex != null) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, hexToBytes(ivHex)))
                cipher.doFinal(hexToBytes(encKeyHex))
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

                rawKey
            }
        } catch (e: Exception) {
            fallbackKeyWithoutKeystore(prefs, e)
        }
    }

    /**
     * Called when the AndroidKeyStore key is missing, rotated, or invalidated (which shows up as
     * [javax.crypto.AEADBadTagException] during unwrap).
     *
     * The previous behaviour generated a *fresh random* key on every failure while leaving the old
     * `tdlib_db/db.sqlite` on disk. TDLib then tried to open that database with a key it was not
     * created with, answered `Unexpected setTdlibParameters`, and the login flow hung on CONNECTING
     * forever. Recovery must therefore be: discard the unreadable database and the stale wrapped
     * key together, then start a fresh one.
     */
    private fun fallbackKeyWithoutKeystore(
        prefs: android.content.SharedPreferences,
        cause: Throwable?
    ): ByteArray {
        if (cause != null) {
            Log.e(TAG, "AndroidKeyStore unavailable; resetting TDLib database", cause)
        } else {
            Log.e(TAG, "AndroidKeyStore key unusable; resetting TDLib database")
        }
        appendDiagnostic("[W] Telegram local database key was unusable; starting a fresh database")

        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(KEYSTORE_ALIAS)) ks.deleteEntry(KEYSTORE_ALIAS)
        }

        prefs.edit()
            .remove(PREF_KEY_ENC_KEY)
            .remove(PREF_KEY_IV)
            .remove(PREF_KEY_FALLBACK)
            .apply()

        runCatching { databaseDir.deleteRecursively() }
        runCatching { tdFilesDir.deleteRecursively() }

        // Keystore is genuinely unavailable on this device/build: keep one stable key in app-private
        // storage so a restart reuses the same database instead of wiping it every launch.
        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(PREF_KEY_FALLBACK, bytesToHex(rawKey)).apply()
        return rawKey
    }

    // QR-code rendering was removed together with the QR login flow. Login is now performed with
    // the phone number + login code + 2FA password, and the zxing dependency is no longer needed.

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
