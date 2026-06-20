package com.timome.eggyhub.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        label = "主页",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home
    )

    object Publish : BottomNavItem(
        route = "publish",
        label = "发布",
        icon = Icons.Outlined.AddCircleOutline,
        selectedIcon = Icons.Filled.AddCircle
    )

    object Task : BottomNavItem(
        route = "task",
        label = "任务",
        icon = Icons.Outlined.CheckCircle,
        selectedIcon = Icons.Filled.CheckCircle
    )

    object Profile : BottomNavItem(
        route = "profile",
        label = "我的",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person
    )
}

object BottomNavItems {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Publish,
        BottomNavItem.Task,
        BottomNavItem.Profile
    )
}

@Composable
fun BottomNavBar(
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        BottomNavItems.items.forEach { item ->
            val selected = selectedItem == item.route

            // 图标颜色渐变动画
            val iconColor by animateColorAsState(
                targetValue = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 250),
                label = "iconColor"
            )

            // 文字颜色渐变动画
            val textColor by animateColorAsState(
                targetValue = if (selected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 250),
                label = "textColor"
            )

            // 选中图标缩放动画
            val selectedIconScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(durationMillis = 250),
                label = "selectedIconScale"
            )

            // 未选中图标缩放动画（反向）
            val unselectedIconScale by animateFloatAsState(
                targetValue = if (selected) 0f else 1f,
                animationSpec = tween(durationMillis = 250),
                label = "unselectedIconScale"
            )

            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item.route) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 未选中图标（渐变淡出）
                            if (unselectedIconScale > 0.01f) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor.copy(alpha = unselectedIconScale)
                                )
                            }
                            // 选中图标（渐变淡入）
                            if (selectedIconScale > 0.01f) {
                                Icon(
                                    imageVector = item.selectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp),
                                    tint = iconColor.copy(alpha = selectedIconScale)
                                )
                            }
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor
                        )
                    }
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = iconColor,
                    unselectedIconColor = iconColor,
                    selectedTextColor = textColor,
                    unselectedTextColor = textColor,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
