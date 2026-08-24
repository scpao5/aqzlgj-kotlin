package com.sbby.aqzlgj.kotlin.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CodeData {
    private var allCodes: List<CodeItem>? = null

    suspend fun loadCodes(context: Context): List<CodeItem> = withContext(Dispatchers.IO) {
        allCodes?.let { return@withContext it }

        val list = mutableListOf<CodeItem>()
        val categories = listOf("刀皮类", "战术装备", "钥匙类", "针剂类", "操作指令", "大杂烩")

        categories.forEach { category ->
            val fileName = "$category.txt"
            try {
                context.assets.open(fileName).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            val parts = trimmed.split("|")
                            if (parts.size >= 2) {
                                val title = parts[0].trim()
                                val code = parts.subList(1, parts.size).joinToString("|").trim()
                                if (title.isNotEmpty() && code.isNotEmpty() && !isMetaLine(title)) {
                                    list.add(
                                        CodeItem(
                                            title = title,
                                            code = code,
                                            category = category
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        allCodes = list
        list
    }

    /** 过滤大杂烩.txt 开头的作者声明等非指令行 */
    private fun isMetaLine(title: String): Boolean =
        title == "声明" || title.startsWith("发现bug") || title == "此应用由是白白吖独立制作"

    fun getCategories(): List<String> = listOf("刀皮类", "战术装备", "钥匙类", "针剂类", "操作指令", "大杂烩")
}
