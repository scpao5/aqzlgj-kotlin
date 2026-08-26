package com.sbby.aqzlgj.kotlin.ui.screen.colorpalette

import androidx.compose.runtime.Immutable
import androidx.core.content.pm.ShortcutInfoCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.sbby.aqzlgj.kotlin.ui.screen.settings.SettingsUiState
import com.sbby.aqzlgj.kotlin.ui.theme.ColorMode

@Immutable
data class ColorPaletteUiState(
    val uiState: SettingsUiState,
    val currentColorMode: ColorMode,
    val currentPaletteStyle: PaletteStyle,
    val currentColorSpec: ColorSpec.SpecVersion,
    val iconShortcuts: List<ShortcutInfoCompat> = emptyList(),
)

@Immutable
data class ColorPaletteScreenActions(
    val onBack: () -> Unit,
    val onSetThemeMode: (Int) -> Unit,
    val onSetMiuixMonet: (Boolean) -> Unit,
    val onSetKeyColor: (Int) -> Unit,
    val onSetColorMode: (ColorMode) -> Unit,
    val onSetColorStyle: (String) -> Unit,
    val onSetColorSpec: (String) -> Unit,
    val onSetEnableBlur: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBar: (Boolean) -> Unit,
    val onSetEnableFloatingBottomBarBlur: (Boolean) -> Unit,
    val onSetEnablePredictiveBack: (Boolean) -> Unit,
    val onSetPageScale: (Float) -> Unit,
    val onPickCustomIcon: () -> Unit,
    val onRemoveIconShortcut: (String) -> Unit,
    val onClearIconShortcuts: () -> Unit,
)
