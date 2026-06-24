package com.timome.eggyhub.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 导出logcat警告弹窗
 * 提示用户logcat可能包含敏感信息
 */
@Composable
fun ExportLogcatWarningDialog(
    show: Boolean,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                text = "警告",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "logcat日志可能包含敏感信息！导出可能会导致信息泄露！确定导出吗？",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "确定",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onDismiss() },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "取消",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

/**
 * 导出logcat进度弹窗
 * 显示圆形进度条和"正在导出..."文字
 */
@Composable
fun ExportLogcatProgressDialog(
    show: Boolean,
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "正在导出...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}