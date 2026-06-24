package com.timome.eggyhub.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 登录错误通用弹窗
 *
 * 参考 temp/LoginActivity.java 的 showErrorDialog (line 494-526)：
 * 1. 显示 "错误信息：\n" + 解码后的错误内容
 * 2. 提供"复制错误信息"按钮 - 复制到剪贴板
 * 3. 提供"联系开发者"按钮 - 跳转 QQ 群
 * 4. 提供"确定"按钮 - 关闭弹窗
 * 5. 提供"导出logcat"按钮 - 导出日志文件
 */
@Composable
fun LoginErrorDialog(
    show: Boolean,
    title: String = "登录失败",
    message: String = "",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onExportLogcat: () -> Unit = {}
) {
    if (!show) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "错误信息：\n$message",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 第一行：复制错误 + 联系开发者
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("errorInfo", message)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "错误信息已复制", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "复制错误",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/4HHCFlN9M4"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "联系开发者",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 第二行：导出logcat + 确定按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onExportLogcat() },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "导出logcat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = { onConfirm() },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "确定",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 登录错误通用弹窗
 *
 * 参考 temp/LoginActivity.java 的 showErrorDialog (line 494-526)：
 * 1. 显示 "错误信息：\n" + 解码后的错误内容
 * 2. 提供"复制错误信息"按钮 - 复制到剪贴板
 * 3. 提供"联系开发者"按钮 - 跳转 QQ 群
 * 4. 提供"确定"按钮 - 关闭弹窗
 */
@Composable
fun LoginErrorDialog(
    show: Boolean,
    title: String = "登录失败",
    message: String = "",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "错误信息：\n$message",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 第一行：复制错误 + 联系开发者
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("errorInfo", message)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "错误信息已复制", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "复制错误",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/4HHCFlN9M4"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "联系开发者",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 第二行：确定按钮
                OutlinedButton(
                    onClick = { onConfirm() },
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "确定",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
