package com.sbby.aqzlgj.kotlin.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.sbby.aqzlgj.kotlin.ui.MainActivity

/**
 * 自定义图标：把用户选择的图片生成为桌面图标快捷方式。
 * - 通过 ShortcutManagerCompat 添加动态快捷方式（带自定义图标，点击打开应用）
 * - 用户可在桌面长按该快捷方式 →「固定到主屏幕」变成真正的桌面图标
 */
object IconShortcutManager {

    const val ID_PREFIX = "custom_icon_"

    /** 从相册图片生成正方形圆角图标位图（居中裁剪 + 圆角） */
    fun createIconBitmap(context: Context, uri: Uri, sizePx: Int = 192): Bitmap? {
        return runCatching {
            val raw = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(uri)
            ) ?: return null
            val side = minOf(raw.width, raw.height)
            val cropX = (raw.width - side) / 2
            val cropY = (raw.height - side) / 2
            val square = Bitmap.createBitmap(raw, cropX, cropY, side, side)
            val scaled = Bitmap.createScaledBitmap(square, sizePx, sizePx, true)
            if (square !== raw) square.recycle()
            raw.recycle()

            val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = sizePx * 0.22f
            val path = Path().apply {
                addRoundRect(
                    RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
                    radius, radius, Path.Direction.CW
                )
            }
            canvas.clipPath(path)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            scaled.recycle()
            output
        }.getOrNull()
    }

    /** 添加带自定义图标的动态快捷方式，返回 shortcut id */
    fun addIconShortcut(context: Context, name: String, icon: Bitmap): String {
        val id = ID_PREFIX + System.currentTimeMillis()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(IconCompat.createWithBitmap(icon))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.addDynamicShortcuts(context, listOf(shortcut))
        return id
    }

    /** 本应用添加的自定义图标快捷方式数量 */
    fun count(context: Context): Int =
        ShortcutManagerCompat.getDynamicShortcuts(context).count { it.id.startsWith(ID_PREFIX) }

    /** 本应用添加的全部自定义图标快捷方式 */
    fun getIconShortcuts(context: Context): List<ShortcutInfoCompat> =
        ShortcutManagerCompat.getDynamicShortcuts(context).filter { it.id.startsWith(ID_PREFIX) }

    /** 删除单个自定义图标快捷方式 */
    fun removeIconShortcut(context: Context, id: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id))
    }

    /** 清除本应用添加的全部自定义图标快捷方式 */
    fun clearAll(context: Context): Int {
        val ids = ShortcutManagerCompat.getDynamicShortcuts(context)
            .map { it.id }
            .filter { it.startsWith(ID_PREFIX) }
        if (ids.isNotEmpty()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, ids)
        }
        return ids.size
    }
}
