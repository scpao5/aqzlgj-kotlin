package com.sbby.aqzlgj.kotlin.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sbby.aqzlgj.kotlin.data.PrivilegeManager
import com.sbby.aqzlgj.kotlin.templateApp
import com.sbby.aqzlgj.kotlin.ui.screen.home.HomeUiState
import com.sbby.aqzlgj.kotlin.ui.screen.home.SystemInfo
import com.sbby.aqzlgj.kotlin.ui.screen.home.getAppVersion
import com.sbby.aqzlgj.kotlin.ui.util.LatestVersionInfo
import com.sbby.aqzlgj.kotlin.ui.util.checkNewVersion

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val rootAvailable = withContext(Dispatchers.IO) { PrivilegeManager.checkRoot() }
            val baseState = withContext(Dispatchers.IO) { buildState(rootAvailable) }
            _uiState.update { baseState }
            if (baseState.checkUpdateEnabled) {
                val hideUpdate = templateApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .getBoolean("hide_update_dialog", false)
                if (!hideUpdate) {
                    val latestVersionInfo = withContext(Dispatchers.IO) { checkNewVersion() }
                    _uiState.update { it.copy(latestVersionInfo = latestVersionInfo) }
                }
            }
        }
    }

    private fun buildState(rootAvailable: Boolean = false): HomeUiState {
        val appVersion = getAppVersion(templateApp)

        return HomeUiState(
            checkUpdateEnabled = templateApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("check_update", true),
            latestVersionInfo = LatestVersionInfo(),
            currentAppVersionCode = appVersion.versionCode,
            systemInfo = SystemInfo(
                appVersion = "${appVersion.versionName} (${appVersion.versionCode})",
            ),
            rootAvailable = rootAvailable,
        )
    }
}
