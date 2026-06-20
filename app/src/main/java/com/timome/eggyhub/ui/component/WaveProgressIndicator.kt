package com.timome.eggyhub.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 波浪环形进度指示器
 *
 * 动画效果：
 * - 三层不同速度的圆环旋转动画
 * - 轻微波浪波纹效果（实验性功能）
 * - 中心点脉动光晕
 */
@Composable
fun WaveProgressIndicator(
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val primary = colorScheme.primary
    val secondary = colorScheme.secondary
    val tertiary = colorScheme.tertiary
    val surface = colorScheme.surface

    // ============ 动画值 ============
    val rotation1 = remember { Animatable(0f) }
    val rotation2 = remember { Animatable(180f) }
    val rotation3 = remember { Animatable(90f) }
    val wavePhase = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }

    // ============ 启动旋转动画 ============

    // rotation1：快速旋转，每 1.8s 增加 360°
    LaunchedEffect(Unit) {
        while (true) {
            val target = rotation1.value + 360f
            rotation1.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 1800)
            )
        }
    }

    // rotation2：中速反向旋转，每 2.4s 减少 360°
    LaunchedEffect(Unit) {
        while (true) {
            val target = rotation2.value - 360f
            rotation2.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 2400)
            )
        }
    }

    // rotation3：慢速旋转，每 3.0s 增加 360°
    LaunchedEffect(Unit) {
        while (true) {
            val target = rotation3.value + 360f
            rotation3.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 3000)
            )
        }
    }

    // wavePhase：波浪相位（实验性功能），每 1.2s 增加 2π
    LaunchedEffect(Unit) {
        while (true) {
            val target = wavePhase.value + (2 * Math.PI).toFloat()
            wavePhase.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 1200)
            )
        }
    }

    // pulseScale：脉动缩放（实验性功能）
    LaunchedEffect(Unit) {
        while (true) {
            pulseScale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 750)
            )
            pulseScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750)
            )
        }
    }

    // ============ 绘制 ============
    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size
        val centerX = canvasSize.width / 2
        val centerY = canvasSize.height / 2

        val scaleFactor = pulseScale.value
        val baseRadius = (minOf(canvasSize.width, canvasSize.height) / 2
                - strokeWidth.toPx() / 2) * scaleFactor

        // 背景环
        drawCircle(
            color = primary.copy(alpha = 0.1f),
            radius = baseRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth.toPx() * 0.3f)
        )

        // 外层波浪环（最慢旋转，反向）
        drawWaveRing(
            centerX = centerX,
            centerY = centerY,
            radius = baseRadius,
            rotationDegrees = rotation3.value,
            wavePhase = wavePhase.value,
            color = tertiary,
            alpha = 0.4f,
            strokeWidth = strokeWidth.toPx() * 0.4f,
            waveAmplitude = 5f,
            waveFrequency = 4,
            segments = 100
        )

        // 中层波浪环（中速反向旋转）
        drawWaveRing(
            centerX = centerX,
            centerY = centerY,
            radius = baseRadius * 0.78f,
            rotationDegrees = rotation2.value,
            wavePhase = -wavePhase.value * 1.2f,
            color = secondary,
            alpha = 0.6f,
            strokeWidth = strokeWidth.toPx() * 0.5f,
            waveAmplitude = 4f,
            waveFrequency = 5,
            segments = 100
        )

        // 内层波浪环（最快旋转）
        drawWaveRing(
            centerX = centerX,
            centerY = centerY,
            radius = baseRadius * 0.55f,
            rotationDegrees = rotation1.value,
            wavePhase = wavePhase.value * 1.5f,
            color = primary,
            alpha = 1f,
            strokeWidth = strokeWidth.toPx(),
            waveAmplitude = 3f,
            waveFrequency = 6,
            segments = 100
        )

        // 中心点
        val gradientBrush = Brush.radialGradient(
            colors = listOf(
                primary,
                secondary.copy(alpha = 0.5f)
            ),
            center = Offset(centerX, centerY),
            radius = baseRadius * 0.2f
        )

        drawCircle(
            brush = gradientBrush,
            radius = baseRadius * 0.12f,
            center = Offset(centerX, centerY)
        )

        // 外圈光晕
        drawCircle(
            color = primary.copy(alpha = 0.15f),
            radius = baseRadius + strokeWidth.toPx() * 2,
            center = Offset(centerX, centerY),
            style = Stroke(width = strokeWidth.toPx() * 0.8f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveRing(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotationDegrees: Float,
    wavePhase: Float,
    color: androidx.compose.ui.graphics.Color,
    alpha: Float,
    strokeWidth: Float,
    waveAmplitude: Float,
    waveFrequency: Int,
    segments: Int
) {
    val path = androidx.compose.ui.graphics.Path()
    val angleStep = (2 * Math.PI / segments).toFloat()
    val rotationRad = Math.toRadians(rotationDegrees.toDouble()).toFloat()

    for (i in 0..segments) {
        val angle = i * angleStep + rotationRad
        val wave = kotlin.math.sin(angle * waveFrequency + wavePhase) * waveAmplitude
        val r = radius + wave

        val x = centerX + r * cos(angle)
        val y = centerY + r * sin(angle)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.close()

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = strokeWidth)
    )
}
