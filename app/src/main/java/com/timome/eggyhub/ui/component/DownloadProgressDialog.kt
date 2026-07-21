package com.timome.eggyhub.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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

@Composable
fun DownloadProgressDialog(
    show: Boolean,
    downloadedBytes: Long,
    totalBytes: Long,
    speedBytesPerSec: Double,
    status: String = "等待下载",
    onDismiss: () -> Unit = {}
) {
    val contentHorizontalPadding = 24.dp
    val contentVerticalPadding = 32.dp
    val spacingBetweenProgressAndText = 16.dp
    val progressHeight = 8.dp

    var isVisible by remember { mutableStateOf(show) }
    var isAnimatingOut by remember { mutableStateOf(false) }

    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(300, easing = LinearEasing),
        label = "download_progress"
    )

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.2f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    fun formatSpeed(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> String.format("%.2f KB/s", bytesPerSec / 1024)
            else -> String.format("%.0f B/s", bytesPerSec)
        }
    }

    LaunchedEffect(show) {
        if (show) {
            isVisible = true
            isAnimatingOut = false
        } else {
            isAnimatingOut = true
            delay(300)
            isVisible = false
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
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        Text(
                            text = status,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(spacingBetweenProgressAndText))

                        if (status.contains("等待")) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(progressHeight),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = animatedProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(progressHeight),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatSpeed(speedBytesPerSec),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
