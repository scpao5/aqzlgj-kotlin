package com.sbby.aqzlgj.kotlin.ui.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/**
 * 更新包下载：套 gh-proxy 代理加速，交给系统 DownloadManager，
 * 用户在通知栏查看下载进度。
 */
object AppDownloader {

    /** 加速代理前缀（GitHub 直连在国内不稳定） */
    private const val PROXY_PREFIX = "https://gh-proxy.com/"

    fun downloadApk(context: Context, originalUrl: String) {
        try {
            val proxyUrl = PROXY_PREFIX + originalUrl
            val fileName = originalUrl.substringAfterLast('/').ifBlank { "aqzlgj-kotlin-update.apk" }

            val request = DownloadManager.Request(Uri.parse(proxyUrl)).apply {
                setTitle("暗区指令工具")
                setDescription("正在下载更新：$fileName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setMimeType("application/vnd.android.package-archive")
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            AppToast.show(context, "开始下载，请在通知栏查看进度")
        } catch (e: Exception) {
            e.printStackTrace()
            AppToast.show(context, "下载启动失败")
        }
    }
}
