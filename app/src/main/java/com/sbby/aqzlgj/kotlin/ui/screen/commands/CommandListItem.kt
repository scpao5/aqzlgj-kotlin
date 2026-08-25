package com.sbby.aqzlgj.kotlin.ui.screen.commands

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbby.aqzlgj.kotlin.R
import com.sbby.aqzlgj.kotlin.data.CodeItem
import com.sbby.aqzlgj.kotlin.data.PrivilegeManager
import com.sbby.aqzlgj.kotlin.ui.LocalUiMode
import com.sbby.aqzlgj.kotlin.ui.UiMode
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 复制指令到剪贴板（不弹 Toast） */
fun copyCommand(context: Context, item: CodeItem) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(item.title, item.code))
}

/** 执行指令（root 广播优先，失败回退普通广播，不弹 Toast） */
fun executeCommand(context: Context, item: CodeItem) {
    PrivilegeManager.execute(context, item.code)
}

/** 指令卡片：按 UI 模式分发 Miuix / Material 两套风格 */
@Composable
fun CommandListItem(
    item: CodeItem,
    showCategory: Boolean = false,
    modifier: Modifier = Modifier,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> CommandListItemMiuix(item, showCategory, modifier)
        UiMode.Material -> CommandListItemMaterial(item, showCategory, modifier)
    }
}

/** Miuix 风格指令卡片 */
@Composable
fun CommandListItemMiuix(
    item: CodeItem,
    showCategory: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    MiuixCard(
        modifier = modifier.fillMaxWidth(),
        colors = MiuixCardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCategory) {
                    MiuixText(
                        text = item.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                MiuixText(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // 文字按钮（第一版样式）
                MiuixTextButton(text = stringResource(R.string.cmd_copy), onClick = { copyCommand(context, item) })
                MiuixTextButton(text = stringResource(R.string.cmd_execute), onClick = { executeCommand(context, item) })
            }
            MiuixText(
                text = item.code,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/** Material 风格指令卡片 */
@Composable
fun CommandListItemMaterial(
    item: CodeItem,
    showCategory: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCategory) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                // 文字按钮（第一版样式）
                TextButton(onClick = { copyCommand(context, item) }) {
                    Text(stringResource(R.string.cmd_copy))
                }
                TextButton(onClick = { executeCommand(context, item) }) {
                    Text(stringResource(R.string.cmd_execute), color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = item.code,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
