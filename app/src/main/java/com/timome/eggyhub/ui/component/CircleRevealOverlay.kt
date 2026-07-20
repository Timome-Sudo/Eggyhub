package com.timome.eggyhub.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

/**
 * 圆形扩散动画覆盖组件
 *
 * 使用 Canvas 绘制圆形，实现从点击位置向外扩散的圆形揭露动画效果。
 *
 * 动画流程：
 * 1. 点击图标 → 在点击位置创建一个圆形
 * 2. 圆形背景色与被点击的 XML 图标底色一致
 * 3. 等比放大（半径增大）
 * 4. 当半径等于屏幕宽度时，停止缩放，内容界面淡入
 * 5. 点击返回按钮 → 内容界面淡出，然后圆形动画倒放
 * 6. 缩小完成 → 调用 onFinished，整体消失
 *
 * @param visible 是否启动/显示动画
 * @param centerX 圆心在容器中的 X 坐标（像素）
 * @param centerY 圆心在容器中的 Y 坐标（像素）
 * @param initialSize 圆形初始大小（直径，像素）
 * @param color 圆形背景色
 * @param containerWidth 容器宽度（像素），用于计算目标半径
 * @param durationMillis 动画时长（毫秒）
 * @param showContent 是否显示内容界面
 * @param onExpandFinished 放大动画完成回调
 * @param onBackClick 返回按钮点击回调
 * @param onFinished 倒放完成（整体消失）回调
 * @param content 展开后显示的内容界面
 */
@Composable
fun CircleRevealOverlay(
    visible: Boolean,
    centerX: Float,
    centerY: Float,
    initialSize: Float,
    color: Color,
    containerWidth: Float,
    durationMillis: Int = 1000,
    showContent: Boolean = false,
    onExpandFinished: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val radiusAnim = remember { Animatable(initialSize / 2f) }

    var isExpanded by remember { mutableStateOf(false) }
    var isReversing by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    val targetRadius = containerWidth * 2

    LaunchedEffect(visible) {
        if (visible) {
            isExpanded = false
            isReversing = false
            contentVisible = false
            radiusAnim.snapTo(initialSize / 2f)
            radiusAnim.animateTo(
                targetValue = targetRadius,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            isExpanded = true
            onExpandFinished()
            if (showContent) {
                contentVisible = true
            }
        }
    }

    LaunchedEffect(showContent) {
        if (visible && isExpanded && !isReversing) {
            contentVisible = showContent
        }
    }

    LaunchedEffect(isReversing) {
        if (isReversing) {
            contentVisible = false
            kotlinx.coroutines.delay(300)
            isExpanded = false
            radiusAnim.animateTo(
                targetValue = initialSize / 2f,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            onFinished()
            isReversing = false
            isExpanded = false
        }
    }

    if (visible) {
        Box(modifier = modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = color,
                    radius = radiusAnim.value,
                    center = Offset(centerX, centerY),
                    style = Fill
                )
            }

            AnimatedContent(
                targetState = contentVisible,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 300))
                },
                label = "contentFade"
            ) { visible ->
                if (visible) {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            content()

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
                                            onBackClick()
                                            isReversing = true
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    }
}
