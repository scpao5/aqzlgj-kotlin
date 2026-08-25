package com.sbby.aqzlgj.kotlin.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbby.aqzlgj.kotlin.ui.LocalUiMode
import com.sbby.aqzlgj.kotlin.ui.UiMode
import com.sbby.aqzlgj.kotlin.ui.navigation3.Navigator
import com.sbby.aqzlgj.kotlin.ui.navigation3.Route
import com.sbby.aqzlgj.kotlin.ui.util.AppDownloader
import com.sbby.aqzlgj.kotlin.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // 手动检查更新：发现新版后自动打开下载页
    LaunchedEffect(uiState.pendingUpdateUrl) {
        if (uiState.pendingUpdateUrl.isNotBlank()) {
            AppDownloader.downloadApk(context, uiState.pendingUpdateUrl)
            viewModel.consumeUpdateUrl()
        }
    }

    val actions = SettingsScreenActions(
        onSetCheckUpdate = viewModel::setCheckUpdate,
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetUiModeIndex = { index ->
            viewModel.setUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
        },
        onOpenAbout = { navigator.push(Route.About) },
        onSetHideToast = viewModel::setHideToast,
        onSetHideUpdateDialog = viewModel::setHideUpdateDialog,
        onCheckUpdateNow = viewModel::checkUpdateNow,
        onUpdateUrlOpened = viewModel::consumeUpdateUrl,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(uiState, actions, bottomInnerPadding)
        UiMode.Material -> SettingPagerMaterial(uiState, actions, bottomInnerPadding)
    }
}
