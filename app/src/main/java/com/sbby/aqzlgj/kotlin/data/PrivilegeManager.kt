package com.sbby.aqzlgj.kotlin.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 指令执行管理：
 * - 有 root：su -c "am broadcast -a android.intent.action.RUN -e cmd '<command>'"
 * - su 执行失败（退出码≠0）或异常：回退普通广播
 * - 无 root：直接 sendBroadcast
 */
object PrivilegeManager {
    @Volatile
    private var hasSu = false

    @Volatile
    private var checked = false

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        if (checked) return@withContext hasSu
        val result = try {
            val process = ProcessBuilder("su", "-c", "echo test").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val exitCode = process.waitFor()
            exitCode == 0 && line != null && line.contains("test")
        } catch (e: Exception) {
            false
        }
        hasSu = result
        checked = true
        result
    }

    fun execute(context: Context, command: String) {
        if (checked && hasSu) {
            // 后台线程执行 su，避免阻塞主线程；失败回退广播
            Thread {
                try {
                    val safeCmd = command.replace("'", "'\\''")
                    val amCmd = "am broadcast -a android.intent.action.RUN -e cmd '$safeCmd'"
                    val process = ProcessBuilder("su", "-c", amCmd).start()
                    val exitCode = process.waitFor()
                    if (exitCode == 0) return@Thread
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // su 失败/异常 → 回退普通广播
                sendBroadcast(context, command)
            }.start()
        } else {
            sendBroadcast(context, command)
        }
    }

    private fun sendBroadcast(context: Context, command: String) {
        val intent = Intent("android.intent.action.RUN").apply {
            putExtra("cmd", command)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
    }
}
