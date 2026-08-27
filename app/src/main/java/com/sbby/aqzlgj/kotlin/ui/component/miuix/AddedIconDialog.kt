package com.sbby.aqzlgj.kotlin.ui.component.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sbby.aqzlgj.kotlin.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

/** Miuix 风格提示对话框：自定义图标添加成功 */
@Composable
fun AddedIconDialog(
    show: Boolean,
    shortcutName: String,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(R.string.icon_added_title),
        onDismissRequest = onDismiss,
        content = {
            Text(
                text = stringResource(R.string.icon_added_msg, shortcutName),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(android.R.string.ok),
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    )
}
