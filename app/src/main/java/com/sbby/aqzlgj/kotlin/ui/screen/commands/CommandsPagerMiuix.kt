package com.sbby.aqzlgj.kotlin.ui.screen.commands

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sbby.aqzlgj.kotlin.R
import com.sbby.aqzlgj.kotlin.permission.PermissionState
import com.sbby.aqzlgj.kotlin.ui.component.miuix.SearchBarFake
import com.sbby.aqzlgj.kotlin.ui.theme.LocalEnableBlur
import com.sbby.aqzlgj.kotlin.ui.util.BlurredBar
import com.sbby.aqzlgj.kotlin.ui.util.rememberBlurBackdrop
import com.sbby.aqzlgj.kotlin.ui.viewmodel.CommandsUiState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun CommandsPagerMiuix(
    state: CommandsUiState,
    permissionState: PermissionState,
    actions: CommandsActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.commands),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // 悬浮窗开关
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.defaultColors(color = colorScheme.surfaceContainerHigh),
                            onClick = actions.onFloatClick,
                            showIndication = true,
                        ) {
                            BasicComponent(
                                title = "悬浮窗",
                                summary = if (permissionState.overlay) {
                                    "权限已授予，点击开启游戏内悬浮窗"
                                } else {
                                    "需要悬浮窗权限，点击申请"
                                },
                                endActions = {
                                    Icon(
                                        imageVector = MiuixIcons.Link,
                                        tint = colorScheme.primary,
                                        contentDescription = null
                                    )
                                },
                                onClick = actions.onFloatClick,
                            )
                        }
                        // 搜索入口
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actions.onSearchClick() }
                        ) {
                            SearchBarFake(
                                label = "搜索指令...",
                                searchBarTopPadding = 0.dp,
                            )
                        }
                    }
                }
                // 分类列表
                items(state.categories.size) { index ->
                    val category = state.categories[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.defaultColors(color = colorScheme.surfaceContainerHigh),
                        onClick = { actions.onCategoryClick(category) },
                        showIndication = true,
                    ) {
                        BasicComponent(
                            title = category,
                            summary = "${state.countOf(category)} 条指令",
                            endActions = {
                                Icon(
                                    imageVector = MiuixIcons.Link,
                                    tint = colorScheme.onSurfaceVariant,
                                    contentDescription = null
                                )
                            },
                            onClick = { actions.onCategoryClick(category) },
                        )
                    }
                }
                item { Spacer(Modifier.height(bottomInnerPadding)) }
            }
        }
    }
}
