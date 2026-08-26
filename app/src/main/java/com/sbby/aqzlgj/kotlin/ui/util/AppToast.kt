package com.sbby.aqzlgj.kotlin.ui.util

import android.content.Context
import android.os.SystemClock
import android.widget.Toast

/**
 * 全局 Toast 控制器：
 * - 设置里开启「隐藏 Toast 提示」后，所有 Toast 都不再弹出
 * - 防抖：短时间(500ms)内重复触发只显示第一条，避免频繁点击时 Toast 排队刷屏
 */
object AppToast {
    private var toast: Toast? = null
    private var lastShownAt = 0L
    private const val MIN_INTERVAL_MS = 500L

    fun show(context: Context, msg: String) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("hide_toast", false)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastShownAt < MIN_INTERVAL_MS) return
        lastShownAt = now
        toast?.cancel()
        toast = Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).apply { show() }
    }
}
