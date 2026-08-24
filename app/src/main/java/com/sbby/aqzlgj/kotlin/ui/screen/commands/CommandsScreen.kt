package com.sbby.aqzlgj.kotlin.ui.screen.commands

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sbby.aqzlgj.kotlin.permission.PermissionManager
import com.sbby.aqzlgj.kotlin.ui.LocalUiMode
import com.sbby.aqzlgj.kotlin.ui.UiMode
import com.sbby.aqzlgj.kotlin.ui.navigation3.Navigator
import com.sbby.aqzlgj.kotlin.ui.navigation3.Route
import com.sbby.aqzlgj.kotlin.service.FloatWindowService
import com.sbby.aqzlgj.kotlin.ui.util.AppToast
import com.sbby.aqzlgj.kotlin.ui.viewmodel.CommandsViewModel

/** 悬浮窗开关：无权限先申请，有权限则启动/停止悬浮窗服务 */
fun openFloatWindow(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        AppToast.show(context, "请授予悬浮窗权限")
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        val serviceIntent = Intent(context, FloatWindowService::class.java)
        if (FloatWindowService.isRunning) {
            context.stopService(serviceIntent)
            AppToast.show(context, "悬浮窗已关闭")
        } else {
            context.startForegroundService(serviceIntent)
            AppToast.show(context, "悬浮窗已开启")
        }
    }
}

data class CommandsActions(
    val onCategoryClick: (String) -> Unit,
    val onSearchClick: () -> Unit,
    val onFloatClick: () -> Unit,
)

/** 「命令」页：悬浮窗开关 + 搜索入口 + 六大分类列表 */
@Composable
fun CommandsPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<CommandsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionManager = remember(context) { PermissionManager(context) }
    val permissionState by permissionManager.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(permissionManager) {
        permissionManager.refresh()
        onPauseOrDispose { }
    }

    val actions = CommandsActions(
        onCategoryClick = { name -> navigator.push(Route.Category(name)) },
        onSearchClick = { navigator.push(Route.Search) },
        onFloatClick = { openFloatWindow(context) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> CommandsPagerMiuix(
            state = uiState,
            permissionState = permissionState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> CommandsPagerMaterial(
            state = uiState,
            permissionState = permissionState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }
}
