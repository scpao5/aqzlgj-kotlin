package com.sbby.aqzlgj.kotlin.ui.screen.home

import androidx.compose.runtime.Immutable
import com.sbby.aqzlgj.kotlin.ui.util.LatestVersionInfo

@Immutable
data class HomeUiState(
    val checkUpdateEnabled: Boolean,
    val latestVersionInfo: LatestVersionInfo,
    val currentAppVersionCode: Long,
    val systemInfo: SystemInfo,
    val rootAvailable: Boolean = false,
)

@Immutable
data class HomeActions(
    val onPermissionsClick: () -> Unit,
    val onOpenUrl: (String) -> Unit,
)
