package com.sbby.aqzlgj.kotlin.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.sbby.aqzlgj.kotlin.data.CodeItem
import com.sbby.aqzlgj.kotlin.ui.screen.commands.CommandListItemMiuix
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix 风格搜索页 */
@Composable
fun SearchScreenMiuix(
    query: String,
    onQueryChange: (String) -> Unit,
    loading: Boolean,
    blank: Boolean,
    results: List<CodeItem>,
    empty: Boolean,
    listState: LazyListState,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                color = MiuixTheme.colorScheme.surface,
                title = "搜索",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator()
                else -> Box(modifier = Modifier.fillMaxSize()) {
                    // 搜索输入框
                    InputField(
                        query = query,
                        onQueryChange = onQueryChange,
                        label = "搜索指令名称或内容...",
                        leadingIcon = {
                            Icon(
                                imageVector = MiuixIcons.Basic.Search,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 12.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceContainerHigh,
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        onSearch = { },
                        enabled = true,
                        expanded = true,
                        onExpandedChange = { },
                    )
                    // 结果区
                    Box(modifier = Modifier.fillMaxSize().padding(top = 72.dp)) {
                        when {
                            blank -> Text(
                                text = "输入关键词搜索指令",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.align(Alignment.Center),
                            )
                            empty -> Text(
                                text = "未找到相关指令",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.align(Alignment.Center),
                            )
                            else -> LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                overscrollEffect = null,
                            ) {
                                items(results, key = { "${it.category}:${it.title}:${it.code}" }) { item ->
                                    CommandListItemMiuix(item = item, showCategory = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
