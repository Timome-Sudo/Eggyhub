package com.timome.eggyhub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 修改邮箱弹窗
 *
 * 与 UsernameEditDialog 的布局结构、样式设计和交互逻辑完全一致
 * - 标题：修改邮箱
 * - 内容：请输入新的邮箱地址
 * - 输入框：filed 样式（OutlinedTextField），符合 Material You 3 设计
 * - 提示文本：新的邮箱地址
 * - 按钮：确定 + 取消
 *
 * @param show 是否显示弹窗
 * @param initialEmail 初始邮箱（默认空）
 * @param onConfirm 点击确定的回调，参数为新的邮箱地址
 * @param onDismiss 点击取消或弹窗外区域的回调
 */
@Composable
fun EmailEditDialog(
    show: Boolean,
    initialEmail: String = "",
    onConfirm: (String) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    var newEmail by remember { mutableStateOf(initialEmail) }
    var inputError by remember { mutableStateOf<String?>(null) }

    /**
     * 邮箱格式校验：xxx@xxx.xxx（xxx部分不做具体内容校验，只要有字符即可）
     */
    fun isValidEmailFormat(email: String): Boolean {
        val atIndex = email.indexOf("@")
        if (atIndex <= 0) return false
        val afterAt = email.substring(atIndex + 1)
        val dotIndex = afterAt.indexOf(".")
        if (dotIndex <= 0) return false
        val afterDot = afterAt.substring(dotIndex + 1)
        return afterDot.isNotEmpty()
    }

    // 全屏半透明背景 + 卡片内容
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.45f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 顶部：邮箱图标 + 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "邮箱",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "修改邮箱",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 内容描述
                Text(
                    text = "请输入新的邮箱地址",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 输入框：filed 样式，符合 Material You 3 设计
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                        if (inputError != null) inputError = null
                    },
                    label = { Text("新的邮箱地址") },
                    placeholder = { Text("请输入新的邮箱地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = inputError != null,
                    supportingText = {
                        inputError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 取消按钮
                    OutlinedButton(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 确定按钮
                    Button(
                        onClick = {
                            when {
                                newEmail.isBlank() -> inputError = "邮箱不能为空"
                                !isValidEmailFormat(newEmail) -> inputError = "邮箱格式不正确，格式应为 xxx@xxx.xxx"
                                else -> onConfirm(newEmail.trim())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
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
