package com.sbby.aqzlgj.kotlin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sbby.aqzlgj.kotlin.data.CodeData
import com.sbby.aqzlgj.kotlin.data.CodeItem
import com.sbby.aqzlgj.kotlin.data.PrivilegeManager
import com.sbby.aqzlgj.kotlin.templateApp

data class CommandsUiState(
    val loading: Boolean = true,
    val categories: List<String> = CodeData.getCategories(),
    val items: List<CodeItem> = emptyList(),
    val rootAvailable: Boolean = false,
) {
    fun countOf(category: String): Int = items.count { it.category == category }
}

class CommandsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CommandsUiState(loading = true))
    val uiState: StateFlow<CommandsUiState> = _uiState.asStateFlow()

    init {
        // root 检测与数据加载并行，互不阻塞
        viewModelScope.launch {
            _uiState.update { it.copy(rootAvailable = PrivilegeManager.checkRoot()) }
        }
        viewModelScope.launch {
            val items = CodeData.loadCodes(templateApp)
            _uiState.update {
                it.copy(loading = false, categories = CodeData.getCategories(), items = items)
            }
        }
    }

    fun itemsForCategory(category: String): List<CodeItem> =
        _uiState.value.items.filter { it.category == category }

    fun search(query: String): List<CodeItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return _uiState.value.items.filter {
            it.title.lowercase().contains(q) || it.code.lowercase().contains(q)
        }
    }
}
