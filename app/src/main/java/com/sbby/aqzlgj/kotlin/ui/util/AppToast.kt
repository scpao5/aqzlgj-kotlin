package com.sbby.aqzlgj.kotlin.ui.util

import android.content.Context
import android.widget.Toast

/** 全局 Toast 控制器：设置里开启「隐藏 Toast 提示」后，所有 Toast 都不再弹出 */
object AppToast {
    fun show(context: Context, msg: String) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("hide_toast", false)) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
