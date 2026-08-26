package com.sbby.aqzlgj.kotlin.ui.screen.colorpalette

import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.sbby.aqzlgj.kotlin.R
import com.sbby.aqzlgj.kotlin.TemplateApplication
import com.sbby.aqzlgj.kotlin.ui.LocalUiMode
import com.sbby.aqzlgj.kotlin.ui.component.miuix.IconNameDialog
import com.sbby.aqzlgj.kotlin.ui.UiMode
import com.sbby.aqzlgj.kotlin.ui.navigation3.LocalNavigator
import com.sbby.aqzlgj.kotlin.ui.theme.ColorMode
import com.sbby.aqzlgj.kotlin.ui.util.AppToast
import com.sbby.aqzlgj.kotlin.ui.util.IconShortcutManager
import com.sbby.aqzlgj.kotlin.ui.viewmodel.SettingsViewModel

@Composable
fun ColorPaletteScreen() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPaletteStyle = try {
        PaletteStyle.valueOf(uiState.colorStyle)
    } catch (_: Exception) {
        PaletteStyle.TonalSpot
    }
    val currentColorSpec = try {
        ColorSpec.SpecVersion.valueOf(uiState.colorSpec)
    } catch (_: Exception) {
        ColorSpec.SpecVersion.Default
    }
    var pendingIcon by remember { mutableStateOf<Bitmap?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var iconName by remember { mutableStateOf("") }
    var iconShortcuts by remember { mutableStateOf(IconShortcutManager.getIconShortcuts(context)) }
    val refreshIconShortcuts: () -> Unit = {
        iconShortcuts = IconShortcutManager.getIconShortcuts(context)
    }
    val dismissIconDialog: () -> Unit = {
        showNameDialog = false
        pendingIcon?.recycle()
        pendingIcon = null
    }
    val confirmIconName: (String) -> Unit = { name ->
        val finalName = name.trim().ifEmpty {
            context.getString(
                R.string.custom_icon_shortcut_name,
                IconShortcutManager.count(context) + 1
            )
        }
        pendingIcon?.let {
            IconShortcutManager.addIconShortcut(context, finalName, it)
            it.recycle()
        }
        pendingIcon = null
        showNameDialog = false
        refreshIconShortcuts()
        AppToast.show(context, context.getString(R.string.cmd_toast_icon_added))
    }

    val state = ColorPaletteUiState(
        uiState = uiState,
        currentColorMode = ColorMode.fromValue(uiState.themeMode),
        currentPaletteStyle = currentPaletteStyle,
        currentColorSpec = currentColorSpec,
        iconShortcuts = iconShortcuts,
    )

    val pickIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val icon = IconShortcutManager.createIconBitmap(context, uri)
            if (icon != null) {
                pendingIcon = icon
                iconName = context.getString(
                    R.string.custom_icon_shortcut_name,
                    IconShortcutManager.count(context) + 1
                )
                showNameDialog = true
            } else {
                AppToast.show(context, context.getString(R.string.cmd_toast_icon_failed))
            }
        }
    }

    if (showNameDialog) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> IconNameDialog(
                show = true,
                initialName = iconName,
                onDismiss = { dismissIconDialog() },
                onConfirm = { confirmIconName(it) }
            )
            UiMode.Material -> AlertDialog(
                onDismissRequest = { dismissIconDialog() },
                title = { Text(stringResource(R.string.settings_custom_icon_name)) },
                text = {
                    OutlinedTextField(
                        value = iconName,
                        onValueChange = { iconName = it },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = { confirmIconName(iconName) }) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dismissIconDialog() }) {
                        Text(stringResource(R.string.float_cancel))
                    }
                }
            )
        }
    }

    val actions = ColorPaletteScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onSetThemeMode = viewModel::setThemeMode,
        onSetMiuixMonet = viewModel::setMiuixMonet,
        onSetKeyColor = viewModel::setKeyColor,
        onSetColorMode = viewModel::setColorMode,
        onSetColorStyle = viewModel::setColorStyle,
        onSetColorSpec = viewModel::setColorSpec,
        onSetEnableBlur = viewModel::setEnableBlur,
        onSetEnableFloatingBottomBar = viewModel::setEnableFloatingBottomBar,
        onSetEnableFloatingBottomBarBlur = viewModel::setEnableFloatingBottomBarBlur,
        onSetEnablePredictiveBack = {
            viewModel.setEnablePredictiveBack(it)
            TemplateApplication.setEnableOnBackInvokedCallback(context.applicationInfo, it)
            activity?.recreate()
        },
        onSetPageScale = viewModel::setPageScale,
        onPickCustomIcon = { pickIconLauncher.launch("image/*") },
        onRemoveIconShortcut = { id ->
            IconShortcutManager.removeIconShortcut(context, id)
            refreshIconShortcuts()
            AppToast.show(context, context.getString(R.string.icon_removed))
        },
        onClearIconShortcuts = {
            val n = IconShortcutManager.clearAll(context)
            refreshIconShortcuts()
            if (n > 0) {
                AppToast.show(context, context.getString(R.string.icon_cleared_all))
            }
        },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> ColorPaletteScreenMiuix(state, actions)
        UiMode.Material -> ColorPaletteScreenMaterial(state, actions)
    }
}
