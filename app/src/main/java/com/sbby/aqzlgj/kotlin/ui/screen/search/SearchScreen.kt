package com.sbby.aqzlgj.kotlin.ui.screen.search

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.sbby.aqzlgj.kotlin.data.CodeData
import com.sbby.aqzlgj.kotlin.data.CodeItem
import com.sbby.aqzlgj.kotlin.ui.LocalUiMode
import com.sbby.aqzlgj.kotlin.ui.UiMode
import com.sbby.aqzlgj.kotlin.ui.navigation3.LocalNavigator
import kotlinx.coroutines.delay

/** 全局搜索页：数据加载 + 实时过滤 + 分段渲染 + 双风格分发 */
@Composable
fun SearchScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var allItems by remember { mutableStateOf<List<CodeItem>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        allItems = CodeData.loadCodes(context)
        loading = false
        delay(200)
        focusRequester.requestFocus()
    }

    val results = remember(allItems, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else allItems.filter {
            it.title.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }

    // 分段渲染：每批 100 条
    val batchSize = 100
    val listState = rememberLazyListState()
    var visibleCount by remember { mutableIntStateOf(batchSize) }
    val visibleResults = remember(results, visibleCount) {
        if (results.isEmpty()) emptyList() else results.subList(0, visibleCount.coerceAtMost(results.size))
    }
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= visibleCount - 10 && visibleCount < results.size
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && visibleCount < results.size) {
            visibleCount = minOf(visibleCount + batchSize, results.size)
        }
    }

    val onBack = { navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Miuix -> SearchScreenMiuix(
            query = query,
            onQueryChange = { query = it },
            loading = loading,
            blank = query.isBlank(),
            results = visibleResults,
            empty = results.isEmpty(),
            listState = listState,
            focusRequester = focusRequester,
            onBack = onBack,
        )

        UiMode.Material -> SearchScreenMaterial(
            query = query,
            onQueryChange = { query = it },
            loading = loading,
            blank = query.isBlank(),
            results = visibleResults,
            empty = results.isEmpty(),
            listState = listState,
            focusRequester = focusRequester,
            onBack = onBack,
        )
    }
}
