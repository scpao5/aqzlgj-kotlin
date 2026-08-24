package com.sbby.aqzlgj.kotlin.ui.screen.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbby.aqzlgj.kotlin.ui.navigation3.LocalNavigator
import com.sbby.aqzlgj.kotlin.ui.navigation3.Route
import com.sbby.aqzlgj.kotlin.ui.screen.commands.CommandListItem
import com.sbby.aqzlgj.kotlin.ui.viewmodel.CommandsViewModel

/** 分类指令列表页（双主题通用实现） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen() {
    val navigator = LocalNavigator.current
    val categoryName = (navigator.current() as? Route.Category)?.name ?: ""
    val viewModel = viewModel<CommandsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = viewModel.itemsForCategory(categoryName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.loading -> CircularProgressIndicator()
                items.isEmpty() -> Text(
                    text = "该分区暂无指令",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp, vertical = 10.dp
                    ),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { "${it.category}:${it.title}:${it.code}" }) { item ->
                        CommandListItem(item = item)
                    }
                }
            }
        }
    }
}
