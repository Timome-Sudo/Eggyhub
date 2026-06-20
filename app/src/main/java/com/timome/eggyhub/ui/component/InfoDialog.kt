package com.timome.eggyhub.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * 信息弹窗组件（成功、提示等）
 *
 * - 带动画进入/退出效果
 * - 可选：单个确认按钮、或者自定义按钮组合
 * - 点击外部和返回键可关闭（可配置禁止）
 *
 * @param show 是否显示
 * @param title 标题
 * @param message 内容提示
 * @param confirmButtonText 确认按钮文字
 * @param onConfirm 点击确认按钮回调
 * @param dismissOnClickOutside 是否允许点击外部关闭
 * @param onDismiss 关闭回调
 */
@Composable
fun InfoDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmButtonText: String = "确认",
    onConfirm: () -> Unit = {},
    dismissOnClickOutside: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(show) }
    var isAnimatingOut by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            isVisible = true
            isAnimatingOut = false
        } else {
            if (isVisible) {
                isAnimatingOut = true
                delay(300)
                isVisible = false
            }
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = {
                if (dismissOnClickOutside) {
                    isAnimatingOut = true
                    onDismiss()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = dismissOnClickOutside,
                dismissOnClickOutside = dismissOnClickOutside,
                usePlatformDefaultWidth = false
            )
        ) {
            AnimatedVisibility(
                visible = !isAnimatingOut,
                enter = fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(300, easing = LinearEasing)
                ) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = androidx.compose.animation.core.tween(300, easing = LinearEasing)
                ),
                exit = fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(300, easing = LinearEasing)
                ) + scaleOut(
                    targetScale = 0.85f,
                    animationSpec = androidx.compose.animation.core.tween(300, easing = LinearEasing)
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isAnimatingOut = true
                                onConfirm()
                                onDismiss()
                            },
                            modifier = Modifier
                                .width(180.dp)
                                .height(44.dp)
                        ) {
                            Text(
                                text = confirmButtonText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
