package com.sbby.aqzlgj.kotlin.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private var lastCheckTime = 0L
    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val prevLatest = _uiState.value.latestVersionInfo
            val prevRoot = _uiState.value.rootAvailable
            val baseState = withContext(Dispatchers.IO) { buildState() }
            // 保留上次检查结果和 root 状态，避免更新卡/root模式闪烁
            _uiState.update {
                baseState.copy(latestVersionInfo = prevLatest, rootAvailable = prevRoot)
            }

            // root 检测独立并行，不阻塞其他任务
            launch {
                val root = withContext(Dispatchers.IO) { PrivilegeManager.checkRoot() }
                _uiState.update { it.copy(rootAvailable = root) }
            }

            // 更新检查独立并行，不受 root 检测阻塞
            android.util.Log.d("HomeVM", "refresh: checkUpdateEnabled=${baseState.checkUpdateEnabled}")
            if (baseState.checkUpdateEnabled) {
                launch {
                    val hideUpdate = templateApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .getBoolean("hide_update_dialog", false)
                    android.util.Log.d("HomeVM", "update check: hideUpdate=$hideUpdate")
                    if (!hideUpdate) {
                        // 5 分钟内不重复检查（避免频繁切换页面导致卡片闪烁/重复请求）
                        val now = System.currentTimeMillis()
                        if (now - lastCheckTime < 5 * 60 * 1000 && prevLatest.versionCode > 0) {
                            return@launch
                        }
                        lastCheckTime = now
                        // 等应用启动稳定、网络/DNS 就绪后再检查（避开刚启动时 oplus DNS 代理 408）
                        delay(8000)
                        var info = withContext(Dispatchers.IO) { checkNewVersion() }
                        android.util.Log.d("HomeVM", "check #1: versionCode=${info.versionCode}")
                        if (info.versionCode == 0) {
                            delay(3000)
                            info = withContext(Dispatchers.IO) { checkNewVersion() }
                            android.util.Log.d("HomeVM", "check #2: versionCode=${info.versionCode}")
                        }
                        if (info.versionCode == 0) {
                            delay(5000)
                            info = withContext(Dispatchers.IO) { checkNewVersion() }
                            android.util.Log.d("HomeVM", "check #3: versionCode=${info.versionCode}")
                        }
                        _uiState.update { it.copy(latestVersionInfo = info) }
                    }
                }
            }
        }
    }

    private fun buildState(): HomeUiState {
        val appVersion = getAppVersion(templateApp)

        return HomeUiState(
            checkUpdateEnabled = templateApp.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("check_update", true),
            latestVersionInfo = LatestVersionInfo(),
            currentAppVersionCode = appVersion.versionCode,
            systemInfo = SystemInfo(
                appVersion = "${appVersion.versionName} (${appVersion.versionCode})",
            ),
            rootAvailable = false,
        )
    }
}
