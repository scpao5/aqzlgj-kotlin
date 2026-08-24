package com.sbby.aqzlgj.kotlin.ui.screen.category

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sbby.aqzlgj.kotlin.data.CodeData
import com.sbby.aqzlgj.kotlin.data.CodeItem
import com.sbby.aqzlgj.kotlin.ui.LocalUiMode
import com.sbby.aqzlgj.kotlin.ui.UiMode
import com.sbby.aqzlgj.kotlin.ui.navigation3.LocalNavigator
import com.sbby.aqzlgj.kotlin.ui.navigation3.Route

/** 分类指令列表页：数据加载 + 分段渲染 + 双风格分发 */
@Composable
fun CategoryScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val categoryName = (navigator.current() as? Route.Category)?.name ?: ""
    var loading by remember { mutableStateOf(true) }
    var allItems by remember { mutableStateOf<List<CodeItem>>(emptyList()) }

    LaunchedEffect(categoryName) {
        allItems = CodeData.loadCodes(context).filter { it.category == categoryName }
        loading = false
    }

    // 分段渲染：每批 100 条，滑到距末尾 10 条内自动加载下一批
    val batchSize = 100
    val listState = rememberLazyListState()
    var visibleCount by remember { mutableIntStateOf(batchSize) }
    val visibleItems = remember(allItems, visibleCount) {
        if (allItems.isEmpty()) emptyList() else allItems.subList(0, visibleCount.coerceAtMost(allItems.size))
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= visibleCount - 10 && visibleCount < allItems.size
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && visibleCount < allItems.size) {
            visibleCount = minOf(visibleCount + batchSize, allItems.size)
        }
    }

    val onBack = { navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Miuix -> CategoryScreenMiuix(
            categoryName = categoryName,
            loading = loading,
            items = visibleItems,
            empty = allItems.isEmpty(),
            listState = listState,
            onBack = onBack,
        )

        UiMode.Material -> CategoryScreenMaterial(
            categoryName = categoryName,
            loading = loading,
            items = visibleItems,
            empty = allItems.isEmpty(),
            listState = listState,
            onBack = onBack,
        )
    }
}
