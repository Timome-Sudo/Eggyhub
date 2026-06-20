package com.timome.eggyhub.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timome.eggyhub.R

/**
 * 图标点击信息：用于传递给圆形扩散动画
 */
data class IconClickInfo(
    val centerX: Float,
    val centerY: Float,
    val size: Float,
    val color: Color,
    val iconIndex: Int
)

@Composable
fun HomePageContent(
    username: String = "",
    onIconClick: (IconClickInfo) -> Unit = {},
    clickedIconIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (username.isNotBlank()) "你好，$username" else "欢迎回到 eggyhub",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "探索精彩内容",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "今日精选",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "发现最新最热的内容推荐",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // ========== 功能图标网格：8个图标，2行4列 ==========
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "快捷功能",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val iconItems = listOf(
                    Triple("分享码", R.drawable.share_code, Color(0xFFE3F2FD)),
                    Triple("邮箱", R.drawable.email, Color(0xFFEBEDFF)),
                    Triple("文章", R.drawable.article, Color(0xFFF2F2F2)),
                    Triple("视频", R.drawable.video, Color(0xFFF0EBFF)),
                    Triple("仓库", R.drawable.files, Color(0xFFFFEFC9)),
                    Triple("比赛", R.drawable.competition, Color(0xFFFFE7D9)),
                    Triple("官方网址", R.drawable.jump_website, Color(0xFFDBE9FF)),
                    Triple("白鹤星球", R.drawable.bhxq, Color(0xFFF5F5F5))
                )

                for (row in 0 until 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until 4) {
                            val index = row * 4 + col
                            val (label, iconId, bgColor) = iconItems[index]

                            IconGridItem(
                                label = label,
                                iconResId = iconId,
                                bgColor = bgColor,
                                iconIndex = index,
                                isClicked = clickedIconIndex == index,
                                onClick = onIconClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "热门动态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                repeat(3) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = when (index) {
                                        0 -> MaterialTheme.colorScheme.primary
                                        1 -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    },
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "用户 ${index + 1}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "这是一条精彩的动态内容...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < 2) {
                        Divider(thickness = 1.dp)
                    }
                }
            }
        }
    }
}

/**
 * 单个图标网格项
 * 背景色圆形和图标图片分离为两个独立的层级，方便独立控制
 */
@Composable
private fun IconGridItem(
    label: String,
    iconResId: Int,
    bgColor: Color,
    iconIndex: Int,
    isClicked: Boolean,
    onClick: (IconClickInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    // 记录图标圆形背景的中心位置（相对于根容器）和大小
    var circleCenterX by remember { mutableFloatStateOf(0f) }
    var circleCenterY by remember { mutableFloatStateOf(0f) }
    var circleSize by remember { mutableFloatStateOf(48.dp.value * 2.75f) } // 48dp 初始估算

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标区域外层：记录位置 + 可点击
        Box(
            modifier = Modifier
                .size(48.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    // 计算圆形中心相对于根布局的位置
                    val positionInRoot = layoutCoordinates.positionInRoot()
                    val size = layoutCoordinates.size
                    circleCenterX = positionInRoot.x + size.width / 2f
                    circleCenterY = positionInRoot.y + size.height / 2f
                    circleSize = size.width.toFloat()
                }
                .clip(CircleShape)
                .clickable {
                    onClick(
                        IconClickInfo(
                            centerX = circleCenterX,
                            centerY = circleCenterY,
                            size = circleSize,
                            color = bgColor,
                            iconIndex = iconIndex
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // ========== 层级 1：背景色圆形（独立层级）==========
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = bgColor)
            )

            // ========== 层级 2：图标图片（独立层级，堆叠在背景之上）==========
            // 被点击时隐藏图标（圆形扩散动画接管视觉）
            if (!isClicked) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = label,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}
