package com.jayesh.cashcollect.crash

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.jayesh.cashcollect.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {
    private const val TAG = "CollectFlowCrash"
    private const val CRASH_FILE = "latest_crash.txt"

    fun install(application: Application) {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            runCatching {
                val pid = android.os.Process.myPid()
                val am = application.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                am?.runningAppProcesses?.find { it.pid == pid }?.processName
            }.getOrNull()
        }
        if (processName?.endsWith(":crash") == true) {
            return
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stackTrace = getStackTrace(throwable)
                val crashReport = buildCrashReport(application, thread, throwable, stackTrace)

                Log.e(TAG, "Uncaught exception intercepted:\n$crashReport")

                // Save to app internal storage
                saveCrashToFile(application, crashReport)

                // Launch crash reporter activity
                val intent = Intent(application, CrashReportActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, crashReport)
                    putExtra(CrashReportActivity.EXTRA_ERROR_MESSAGE, throwable.localizedMessage ?: throwable.javaClass.simpleName)
                }
                application.startActivity(intent)

                // Brief pause to ensure ActivityManager dispatches the intent before killing process
                try {
                    Thread.sleep(350)
                } catch (ignored: InterruptedException) {}

                // Terminate crashed process safely
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed in CrashHandler", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    private fun buildCrashReport(context: Context, thread: Thread, throwable: Throwable, stackTrace: String): String {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        return buildString {
            appendLine("=== COLLECTFLOW CRASH REPORT ===")
            appendLine("Time: $timeStr")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App Version: ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})")
            appendLine("Thread: ${thread.name} (ID: ${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("--- STACK TRACE ---")
            appendLine(stackTrace)
        }
    }

    private fun saveCrashToFile(context: Context, report: String) {
        runCatching {
            val file = File(context.filesDir, CRASH_FILE)
            file.writeText(report)
        }
    }

    fun getLatestCrashLog(context: Context): String? {
        return runCatching {
            val file = File(context.filesDir, CRASH_FILE)
            if (file.exists()) file.readText() else null
        }.getOrNull()
    }
}
