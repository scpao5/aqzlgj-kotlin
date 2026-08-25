package com.sbby.aqzlgj.kotlin.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbby.aqzlgj.kotlin.BuildConfig
import com.sbby.aqzlgj.kotlin.R
import com.sbby.aqzlgj.kotlin.permission.PermissionState
import com.sbby.aqzlgj.kotlin.ui.component.miuix.WarningCard
import com.sbby.aqzlgj.kotlin.ui.theme.LocalEnableBlur
import com.sbby.aqzlgj.kotlin.ui.util.AppDownloader
import com.sbby.aqzlgj.kotlin.ui.util.BlurredBar
import com.sbby.aqzlgj.kotlin.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

// 升级公告：内容就绪后改为 true 即可显示
private val showAnnouncement: Boolean get() =
    BuildConfig.VERSION_NAME.contains("debug", ignoreCase = true)

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    permissionState: PermissionState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
    val githubUrl = stringResource(R.string.home_example_link_url)
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
                    title = stringResource(R.string.app_name),
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
                        // 权限状态大卡（给完权限变绿）
                        PermissionCardMiuix(permissionState, state.rootAvailable, actions.onPermissionsClick)
                        // 升级公告（暂时隐藏）
                        if (showAnnouncement) {
                            WarningCard(stringResource(R.string.home_sample_notification))
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.defaultColors(color = colorScheme.surfaceContainerHigh),
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.home_app_version),
                            summary = state.systemInfo.appVersion,
                        )
                    }
                }
                // 发现新版本提示
                val hasUpdate = state.latestVersionInfo.versionCode.toLong() > state.currentAppVersionCode &&
                    state.latestVersionInfo.downloadUrl.isNotBlank()
                if (hasUpdate) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.defaultColors(color = colorScheme.primaryContainer),
                            onClick = { AppDownloader.downloadApk(context, state.latestVersionInfo.downloadUrl) },
                            showIndication = true,
                        ) {
                            BasicComponent(
                                title = stringResource(R.string.update_found, state.latestVersionInfo.versionCode.toString()),
                                summary = stringResource(R.string.update_download),
                                endActions = {
                                    Icon(
                                        imageVector = MiuixIcons.Link,
                                        tint = colorScheme.primary,
                                        contentDescription = null
                                    )
                                },
                                onClick = { AppDownloader.downloadApk(context, state.latestVersionInfo.downloadUrl) },
                            )
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.defaultColors(color = colorScheme.surfaceContainerHigh),
                        onClick = { actions.onOpenUrl(githubUrl) },
                        showIndication = true,
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.home_example_link_title),
                            summary = stringResource(R.string.home_example_link_subtitle),
                            endActions = {
                                Icon(
                                    imageVector = MiuixIcons.Link,
                                    tint = colorScheme.primary,
                                    contentDescription = null
                                )
                            },
                            onClick = { actions.onOpenUrl(githubUrl) },
                        )
                    }
                }
                item { Spacer(Modifier.height(bottomInnerPadding)) }
            }
        }
    }
}

/** 权限状态大卡：权限齐了变绿 */
@Composable
private fun PermissionCardMiuix(
    state: PermissionState,
    rootAvailable: Boolean,
    onClick: () -> Unit,
) {
    val requiredGranted = state.requiredGranted
    val iconColor = if (requiredGranted) Color(0xFF36D167) else Color(0xFFF72727)
    val containerColor = if (requiredGranted) Color(0xFFDFFAE4) else Color(0xFFF8E2E2)
    val textColor = Color(0xFF111111)
    val summary =
        if (requiredGranted) {
            stringResource(R.string.permission_ready)
        } else {
            stringResource(R.string.permission_missing)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = containerColor),
        onClick = onClick,
        showIndication = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 70.dp, y = 44.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(182.dp),
                    imageVector =
                        if (requiredGranted) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Cancel,
                    tint = iconColor,
                    contentDescription = null,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, top = 28.dp, end = 148.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text =
                            if (requiredGranted) {
                                stringResource(R.string.permission_status_ready_title)
                            } else {
                                stringResource(R.string.permission_status_missing_title)
                            },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.72f),
                    )
                }
                Text(
                    text =
                        if (requiredGranted) {
                            if (rootAvailable) stringResource(R.string.root_mode) else stringResource(R.string.non_root_mode)
                        } else {
                            stringResource(R.string.permission_action_required)
                        },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.78f),
                )
            }
        }
    }
}
