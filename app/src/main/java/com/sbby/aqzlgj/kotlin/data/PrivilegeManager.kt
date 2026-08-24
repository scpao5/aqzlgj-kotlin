package com.sbby.aqzlgj.kotlin.data

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 指令执行管理：与 Java 版行为保持一致。
 * - 有 root：su -c "am broadcast -a android.intent.action.RUN -e cmd '<command>'"
 * - 无 root / su 失败：直接 sendBroadcast
 * 游戏端需实现 android.intent.action.RUN 广播接收器解析 cmd 参数。
 */
object PrivilegeManager {
    private var hasSu = false
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
            try {
                val safeCmd = command.replace("'", "'\\''")
                val amCmd = "am broadcast -a android.intent.action.RUN -e cmd '$safeCmd'"
                ProcessBuilder("su", "-c", amCmd).start()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback: 普通广播
        val intent = Intent("android.intent.action.RUN").apply {
            putExtra("cmd", command)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
    }
}
