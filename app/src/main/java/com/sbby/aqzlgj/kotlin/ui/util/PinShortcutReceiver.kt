package com.sbby.aqzlgj.kotlin.ui.util

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 接收 requestPinShortcut 的固定结果回调：固定成功时提示"已固定到桌面"
 * (取消/权限拒绝时不提示)
 */
class PinShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            AppToast.show(context, context.getString(com.sbby.aqzlgj.kotlin.R.string.pin_result_ok))
        }
    }
}
