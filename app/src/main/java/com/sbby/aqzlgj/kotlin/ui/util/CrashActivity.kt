package com.sbby.aqzlgj.kotlin.ui.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView

/** 崩溃日志展示页(传统 View 实现):显示堆栈、可复制、返回键重启应用 */
class CrashActivity : Activity() {

    private var log: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        title = getString(com.sbby.aqzlgj.kotlin.R.string.crash_title)
        log = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        val contentView = ScrollView(this).apply { isFillViewport = true }
        val hScroll = HorizontalScrollView(this)
        val textView = TextView(this).apply {
            setPadding(dp2px(16f), dp2px(16f), dp2px(16f), dp2px(16f))
            text = log
            setTextIsSelectable(true)
            typeface = Typeface.DEFAULT
        }
        hScroll.addView(textView)
        contentView.addView(hScroll, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(contentView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, android.R.id.copy, 0, android.R.string.copy)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.copy) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(packageName, log))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        restart()
    }

    private fun restart() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }

    private fun dp2px(dp: Float): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()
}
