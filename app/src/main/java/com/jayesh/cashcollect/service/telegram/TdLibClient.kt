package com.jayesh.cashcollect.service.telegram

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Thrown when TDLib answers a request with [TdApi.Error].
 */
class TdException(
    val code: Int,
    override val message: String
) : Exception(message) {

    val isFloodWait: Boolean get() = code == 429
    val isUnauthorized: Boolean get() = code == 401
    val isNotFound: Boolean get() = code == 404
}

/**
 * First-party coroutine wrapper around the TDLib Java bindings (`org.drinkless.tdlib`).
 *
 * This intentionally replaces the third-party `ktx` AAR, because that wrapper:
 *  1. silently sent its own `SetTdlibParameters` (with **no** database encryption key) during
 *     `init()`, which made the application's own parameters call fail and silently discarded the
 *     Keystore-encrypted database key;
 *  2. subscribed to a `MutableSharedFlow(replay = 0)` **after** creating the client, so TDLib's
 *     very first authorization update could be dropped and the login flow would hang forever;
 *  3. never exposed TDLib's native log, so failures were invisible (on Android TDLib writes to
 *     stderr, which is discarded).
 *
 * Owning this code lets us: send parameters exactly once with the Keystore-derived key, subscribe
 * before the client starts, and pipe TDLib's native log into an in-app diagnostics buffer.
 */
class TdLibClient(
    private val tag: String = "TdLibClient",
    private val logSink: (level: String, message: String) -> Unit = { level, message ->
        Log.d(tag, "[$level] $message")
    }
) {

    companion object {
        const val LOG_VERBOSITY: Int = 3
        private const val MAX_LOG_FILE_BYTES = 4L * 1024 * 1024
        private const val CLOSE_TIMEOUT_MS = 5_000L

        /**
         * Hard ceiling on a single TDLib request.
         *
         * Without this, a request that TDLib never answers (which is what
         * `SetAuthenticationPhoneNumber` did on this device) suspends forever. In TelegramManager
         * that request is wrapped in the authorization mutex, so one unanswered call permanently
         * deadlocks the login state machine and the UI stays on CONNECTING with no error.
         * Timing out converts a silent hang into a real, reportable error.
         */
        private const val SEND_TIMEOUT_MS = 30_000L
    }

    private val requestIds = AtomicLong(0L)
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<TdApi.Object>>()

    private val _updates = MutableSharedFlow<TdApi.Update>(
        replay = 0,
        extraBufferCapacity = 1024,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Stream of TDLib updates. */
    val updates: SharedFlow<TdApi.Update> = _updates.asSharedFlow()

    private val _logLines = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Raw TDLib native log lines. Used by the in-app diagnostics screen. */
    val logLines: SharedFlow<String> = _logLines.asSharedFlow()

    @Volatile
    private var nativeClient: Client? = null

    val isStarted: Boolean get() = nativeClient != null

    /**
     * Configures logging and creates the native TDLib instance.
     *
     * MUST be called from a background thread and MUST be preceded by registering an update
     * collector, because TDLib starts emitting updates as soon as the client is created.
     *
     * @param logFilePath when non-null, TDLib also writes its native log to this file.
     */
    fun start(logFilePath: String? = null) {
        if (nativeClient != null) return

        if (logFilePath != null) {
            runCatching {
                File(logFilePath).parentFile?.mkdirs()
                Client.execute(
                    TdApi.SetLogStream(
                        TdApi.LogStreamFile(logFilePath, MAX_LOG_FILE_BYTES, false)
                    )
                )
            }.onFailure { emitLog("E", "SetLogStream failed: ${it.message}") }
        }

        runCatching { Client.execute(TdApi.SetLogVerbosityLevel(LOG_VERBOSITY)) }
            .onFailure { emitLog("E", "SetLogVerbosityLevel failed: ${it.message}") }

        runCatching {
            Client.setLogMessageHandler(LOG_VERBOSITY, Client.LogMessageHandler { level, message ->
                val text = message.orEmpty()
                if (text.isNotBlank()) {
                    emitLog(if (level <= 1) "E" else "D", "tdlib[$level] $text")
                }
            })
        }.onFailure { emitLog("E", "setLogMessageHandler failed: ${it.message}") }

        nativeClient = Client.create(
            Client.ResultHandler { obj -> dispatchIncoming(obj) },
            Client.ExceptionHandler { e -> emitLog("E", "update exception: ${e?.message}") },
            Client.ExceptionHandler { e -> emitLog("E", "default exception: ${e?.message}") }
        )
        emitLog("I", "TDLib client created (logFile=${logFilePath ?: "disabled"})")
    }

    /**
     * Sends a request and suspends until TDLib answers. Throws [TdException] when TDLib answers
     * with an error, and [IllegalStateException] when the client is not started.
     */
    suspend fun <T : TdApi.Object> send(request: TdApi.Function<T>): T {
        val result = sendInternal(request)
        if (result is TdApi.Error) {
            throw TdException(result.code, result.message)
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * Best-effort variant: never throws, returns `null` and logs the reason when TDLib reports an
     * error. Used for calls such as `Close`, `SetLogVerbosityLevel` or cleanup requests.
     */
    suspend fun sendQuietly(request: TdApi.Function<*>): TdApi.Object? {
        return try {
            sendInternal(request).also { result ->
                if (result is TdApi.Error) {
                    emitLog("W", "${request.javaClass.simpleName} -> Error ${result.code}: ${result.message}")
                }
            }
        } catch (e: TdException) {
            emitLog("W", "${request.javaClass.simpleName} -> ${e.message}")
            null
        } catch (e: Exception) {
            emitLog("W", "${request.javaClass.simpleName} failed: ${e.message}")
            null
        }
    }

    private suspend fun sendInternal(request: TdApi.Function<*>): TdApi.Object {
        val client = nativeClient
            ?: throw IllegalStateException("TDLib client is not started yet")
        val queryId = requestIds.incrementAndGet()
        val deferred = CompletableDeferred<TdApi.Object>()
        pendingRequests[queryId] = deferred
        try {
            client.send(
                request,
                Client.ResultHandler { result ->
                    pendingRequests.remove(queryId)
                    deferred.complete(result ?: TdApi.Ok())
                },
                Client.ExceptionHandler { e ->
                    pendingRequests.remove(queryId)
                    deferred.completeExceptionally(
                        e ?: RuntimeException("TDLib request failed without exception detail")
                    )
                }
            )
            val result = withTimeoutOrNull(SEND_TIMEOUT_MS) { deferred.await() }
                ?: run {
                    pendingRequests.remove(queryId)
                    val name = request.javaClass.simpleName
                    emitLog("E", "$name timed out after ${SEND_TIMEOUT_MS / 1000}s")
                    throw TdException(
                        408,
                        "TDLib did not answer $name within ${SEND_TIMEOUT_MS / 1000}s"
                    )
                }
            return result
        } catch (e: Exception) {
            pendingRequests.remove(queryId)
            if (e is TdException) throw e
            throw IllegalStateException("TDLib request failed: ${e.message}", e)
        }
    }

    /** Releases the native client. Safe to call more than once. */
    suspend fun stop() {
        val client = nativeClient ?: return
        nativeClient = null
        val closed = CompletableDeferred<Unit>()
        runCatching {
            client.send(
                TdApi.Close(),
                Client.ResultHandler { closed.complete(Unit) },
                Client.ExceptionHandler { closed.complete(Unit) }
            )
        }.onFailure {
            emitLog("W", "Close failed: ${it.message}")
            closed.complete(Unit)
        }
        withTimeoutOrNull(CLOSE_TIMEOUT_MS) { closed.await() }
        pendingRequests.values.forEach { it.cancel() }
        pendingRequests.clear()
        emitLog("I", "TDLib client closed")
    }

    private fun dispatchIncoming(obj: TdApi.Object?) {
        when (obj) {
            is TdApi.Update -> if (!_updates.tryEmit(obj)) {
                emitLog("W", "Update dropped from buffer: ${obj.javaClass.simpleName}")
            }
            null -> Unit
            else -> emitLog("D", "unexpected non-update object: ${obj.javaClass.simpleName}")
        }
    }

    private fun emitLog(level: String, message: String) {
        val line = "[$level] $message"
        _logLines.tryEmit(line)
        logSink(level, message)
    }
}
