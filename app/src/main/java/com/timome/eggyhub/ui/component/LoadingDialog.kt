package com.timome.eggyhub.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
 * 等待进度条弹窗
 *
 * 使用模式：
 * 1. 登录/注册/找回密码等流程（autoDismiss=true, showCloseButton=false）
 *    - 延迟 durationMillis 后自动关闭，不可点击外部/返回键关闭
 * 2. 开发者模式测试（autoDismiss=false, showCloseButton=true）
 *    - 持续显示直到用户点击"关闭"按钮
 *
 * @param show 是否显示弹窗
 * @param message 弹窗下方的提示文字
 * @param durationMillis 自动关闭延迟（仅 autoDismiss=true 时生效）
 * @param autoDismiss 是否自动关闭
 * @param showCloseButton 是否显示关闭按钮
 * @param onDismiss 弹窗关闭回调
 * @param onComplete 动画完成回调（延迟结束后触发）
 */
@Composable
fun LoadingDialog(
    show: Boolean,
    message: String = "正在登录中...",
    durationMillis: Long = 3000,
    autoDismiss: Boolean = true,
    showCloseButton: Boolean = false,
    closeButtonText: String = "关闭",
    onDismiss: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    val progressSize = 56.dp
    val progressStrokeWidth = 4.5.dp
    val contentHorizontalPadding = 24.dp
    val contentVerticalPadding = 32.dp
    val spacingBetweenProgressAndText = 16.dp
    val buttonWidth = 140.dp
    val buttonHeight = 44.dp
    val buttonSpacing = 20.dp

    var isVisible by remember { mutableStateOf(show) }
    var isAnimatingOut by remember { mutableStateOf(false) }
    var userRequestedClose by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            isVisible = true
            isAnimatingOut = false
            userRequestedClose = false

            if (autoDismiss) {
                delay(durationMillis)
                if (!userRequestedClose) {
                    isAnimatingOut = true
                    delay(300)
                    isVisible = false
                    onComplete()
                    onDismiss()
                }
            }
        } else {
            isVisible = false
        }
    }

    LaunchedEffect(userRequestedClose) {
        if (userRequestedClose && isVisible) {
            isAnimatingOut = true
            delay(300)
            isVisible = false
            onComplete()
            onDismiss()
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = { /* 禁止外部/返回键关闭 */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
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
                        .fillMaxWidth(0.7f)
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
                            .padding(
                                horizontal = contentHorizontalPadding,
                                vertical = contentVerticalPadding
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WaveProgressIndicator(
                            size = progressSize,
                            strokeWidth = progressStrokeWidth,
                            modifier = Modifier.size(progressSize)
                        )

                        Spacer(modifier = Modifier.height(spacingBetweenProgressAndText))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (showCloseButton) {
                            Spacer(modifier = Modifier.height(buttonSpacing))
                            Button(
                                onClick = { userRequestedClose = true },
                                modifier = Modifier
                                    .width(buttonWidth)
                                    .height(buttonHeight)
                            ) {
                                Text(
                                    text = closeButtonText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
