package com.sbby.aqzlgj.kotlin.ui.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局崩溃捕获(与 LogEvent 的 bugreport 共存):
 * - 自动捕获未处理异常 → 写入外部缓存 crash/crash_时间.txt(MT 可直接查看)
 * - 尽力弹出 CrashActivity 展示堆栈(受系统后台启动限制,可能弹不出,但文件一定写入)
 */
object CrashHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val dateFormat = SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.getDefault())

    fun register(context: Context) {
        val app = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val log = buildLog(app, throwable)
                writeLog(app, log)

                try {
                    val intent = Intent(app, CrashActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(Intent.EXTRA_TEXT, log)
                    }
                    app.startActivity(intent)
                } catch (_: Throwable) {
                }

                throwable.printStackTrace()
                Process.killProcess(Process.myPid())
                System.exit(0)
            } catch (_: Throwable) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun buildLog(context: Context, throwable: Throwable): String {
        val time = dateFormat.format(Date())
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        val versionCode = runCatching {
            if (Build.VERSION.SDK_INT >= 28) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
        }.getOrNull() ?: 0L

        return buildString {
            appendLine("Time Of Crash :    $time")
            appendLine("Device :    ${Build.MANUFACTURER}, ${Build.MODEL}")
            appendLine("Android Version :    ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})")
            appendLine("App Version :    $versionName ($versionCode)")
            appendLine("Kernel :    ${runCatching { File("/proc/version").readText().trim() }.getOrElse { it.message ?: "unknown" }}")
            if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null) {
                appendLine("Support Abis :    ${Build.SUPPORTED_ABIS.joinToString()}")
            }
            appendLine("Fingerprint :    ${Build.FINGERPRINT}")
            appendLine()
            appendLine(Log.getStackTraceString(throwable))
        }
    }

    private fun writeLog(context: Context, log: String) {
        try {
            val dir = File(context.getExternalCacheDir(), "crash")
            dir.mkdirs()
            val file = File(dir, "crash_${dateFormat.format(Date())}.txt")
            file.writeText(log, Charsets.UTF_8)
        } catch (_: Throwable) {
        }
    }
}
