package com.timome.eggyhub.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 圆形扩散覆盖动画组件
 *
 * 动画流程：
 * 1. 点击图标 → 圆形从图标位置开始平滑放大
 * 2. 圆形持续放大直到完全超出整个屏幕（直径 = 屏幕对角线 × 1.5）
 * 3. 放大完成后 → 左上角显示向左的箭头图标
 * 4. 点击箭头 → 动画倒放（圆形缩小回初始图标大小），箭头立即消失
 * 5. 缩小完成 → 调用 onFinished，整体消失
 *
 * @param visible 是否启动/显示动画
 * @param centerX 圆心在容器中的 X 坐标（像素）
 * @param centerY 圆心在容器中的 Y 坐标（像素）
 * @param initialSize 圆形元素初始大小（像素）
 * @param color 圆形元素背景色
 * @param containerWidth 容器宽度（像素）
 * @param containerHeight 容器高度（像素）
 * @param durationMillis 动画时长（毫秒）
 * @param onFinished 动画完成回调
 */
@Composable
fun CircleRevealOverlay(
    visible: Boolean,
    centerX: Float,
    centerY: Float,
    initialSize: Float,
    color: Color,
    containerWidth: Float,
    containerHeight: Float,
    durationMillis: Int = 500,
    onFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 控制大小动画的 Animatable（值以像素为单位）
    val sizeAnim = remember { Animatable(initialSize) }

    // 放大是否已完成（控制箭头显示）
    var isExpanded by remember { mutableStateOf(false) }

    // 是否正在倒放（缩小）
    var isReversing by remember { mutableStateOf(false) }

    // ────────────────────────────────────────────────
    // 关键：计算最终目标直径，确保圆形完全超出整个屏幕
    //  1. 先计算圆心到4个角的距离，取最大值（保证所有角都被覆盖）
    //  2. 目标直径 = 2 × 最大距离 × 1.5（额外 1.5 倍确保完全超出屏幕）
    // ────────────────────────────────────────────────
    val d1 = sqrt(centerX * centerX + centerY * centerY)
    val d2 = sqrt((containerWidth - centerX) * (containerWidth - centerX) + centerY * centerY)
    val d3 = sqrt(centerX * centerX + (containerHeight - centerY) * (containerHeight - centerY))
    val d4 = sqrt(
        (containerWidth - centerX) * (containerWidth - centerX) +
                (containerHeight - centerY) * (containerHeight - centerY)
    )
    val maxDistance = max(max(d1, d2), max(d3, d4))
    // ×1.5 确保圆形边界完全超出屏幕，而不是刚好覆盖
    val targetSize = 2f * maxDistance * 1.5f

    // ────────────────────────────────────────────────
    // 放大动画：visible 从 false → true 时触发
    // ────────────────────────────────────────────────
    LaunchedEffect(visible) {
        if (visible) {
            isExpanded = false
            isReversing = false
            sizeAnim.snapTo(initialSize)
            sizeAnim.animateTo(
                targetValue = targetSize,
                animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
            )
            // 放大完成 → 显示箭头
            isExpanded = true
        }
    }

    // ────────────────────────────────────────────────
    // 倒放（缩小）动画：isReversing 从 false → true 时触发
    // ────────────────────────────────────────────────
    LaunchedEffect(isReversing) {
        if (isReversing) {
            // 开始缩小，立即隐藏箭头（箭头在缩小过程中不可见）
            isExpanded = false
            sizeAnim.animateTo(
                targetValue = initialSize,
                animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
            )
            // 缩小完成
            onFinished()
            isReversing = false
            isExpanded = false
        }
    }

    if (visible) {
        Box(modifier = modifier.fillMaxSize()) {
            // ────────────────────────────────────────────────
            // 扩散圆形：透明度固定为 100%，圆心保持不变
            // 大小使用像素单位（不通过 .dp 转换），通过 onSizeChanged/Modifier.sizeIn 无法直接设置像素
            // 解决方法：使用自定义尺寸布局 - 但 Compose Modifier.size() 需要 Dp
            // 正确做法：用 density 转换像素为 Dp
            // ────────────────────────────────────────────────
            val density = androidx.compose.ui.platform.LocalDensity.current
            val currentSizeDp = with(density) { sizeAnim.value.toDp() }
            val offsetXDp = with(density) { (centerX - sizeAnim.value / 2f).toDp() }
            val offsetYDp = with(density) { (centerY - sizeAnim.value / 2f).toDp() }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = offsetXDp,
                        y = offsetYDp
                    )
                    .size(currentSizeDp)
                    .background(color = color, shape = CircleShape)
            )

            // ────────────────────────────────────────────────
            // 放大完成后显示左上角的返回箭头
            // 注意：箭头只会在放大完成 & 未开始倒放时显示
            // 一旦开始倒放（isReversing=true），isExpanded 立即设为 false，箭头消失
            // ────────────────────────────────────────────────
            if (isExpanded && !isReversing) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp, start = 16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color = Color.White.copy(alpha = 0.9f))
                                .clickable {
                                    // 点击箭头 → 触发倒放动画
                                    isReversing = true
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
