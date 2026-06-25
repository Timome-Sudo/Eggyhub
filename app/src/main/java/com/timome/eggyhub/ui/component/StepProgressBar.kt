package com.timome.eggyhub.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 步骤进度条组件
 *
 * - 动画效果细腻，步骤圆点和连接线使用平滑颜色渐变过渡
 * - 步骤序号显示在圆形中，已完成的步骤显示对勾图标
 * - 当前步骤有明显高亮效果，未完成步骤为灰色
 *
 * @param currentStep 当前步骤（从1开始）
 * @param totalSteps 总步骤数
 * @param stepLabels 每个步骤的标签（可选），为 null 时只显示圆形进度条
 */
@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    stepLabels: List<String>? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 背景连接线（灰色）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 绘制所有连接线（背景色）
            for (i in 1 until totalSteps) {
                // 背景连接线
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        // 已完成的连接线（从左到右动画）
        if (currentStep > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1 until totalSteps) {
                    val shouldHighlight = i < currentStep
                    val lineProgress by animateFloatAsState(
                        targetValue = if (shouldHighlight) 1f else 0f,
                        animationSpec = tween(durationMillis = 500, delayMillis = (i - 1) * 100),
                        label = "lineProgress_$i"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(lineProgress)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        // 步骤圆点（覆盖在连接线之上）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..totalSteps) {
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep

                // 步骤圆点
                StepCircle(
                    step = step,
                    isCompleted = isCompleted,
                    isCurrent = isCurrent
                )

                // 连接线由外层 Box 绘制，这里不需要处理
            }
        }

        // 步骤标签（如果有）
        if (stepLabels != null && stepLabels.size >= totalSteps) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (step in 1..totalSteps) {
                    val isCurrent = step == currentStep
                    val textColor by animateColorAsState(
                        targetValue = if (isCurrent)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 300),
                        label = "labelColor_$step"
                    )
                    Text(
                        text = stepLabels[step - 1],
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCircle(
    step: Int,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    // 圆点背景色动画
    val bgColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primary
            isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 400),
        label = "bgColor_$step"
    )

    // 圆点边框颜色动画
    val borderColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.primary
            isCurrent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 400),
        label = "borderColor_$step"
    )

    // 圆点大小动画（当前步骤稍大）
    val circleSize by animateFloatAsState(
        targetValue = if (isCurrent) 32f else 28f,
        animationSpec = tween(durationMillis = 300),
        label = "circleSize_$step"
    )

    // 文字颜色
    val textColor by animateColorAsState(
        targetValue = when {
            isCompleted -> MaterialTheme.colorScheme.onPrimary
            isCurrent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 400),
        label = "textColor_$step"
    )

    Box(
        modifier = Modifier
            .size(circleSize.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            // 已完成步骤显示对勾
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        } else {
            Text(
                text = step.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
    }
}
