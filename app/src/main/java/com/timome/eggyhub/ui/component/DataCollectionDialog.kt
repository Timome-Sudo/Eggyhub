package com.timome.eggyhub.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 数据收集选择弹窗
 * 允许用户选择要收集的数据类型和具体数据项
 */
@Composable
fun DataCollectionDialog(
    show: Boolean,
    onExport: (DataCollectionConfig) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    // 设备信息展开状态
    var deviceInfoExpanded by remember { mutableStateOf(true) }
    // 应用信息展开状态
    var appInfoExpanded by remember { mutableStateOf(true) }
    // 应用logcat展开状态
    var appLogcatExpanded by remember { mutableStateOf(true) }

    // 设备信息数据项
    val deviceInfoItems = remember {
        mutableStateListOf(
            DataItem("时间", true),
            DataItem("设备品牌", true),
            DataItem("设备型号", true),
            DataItem("设备制造商", true),
            DataItem("系统版本", true),
            DataItem("设备名称", true),
            DataItem("产品名称", true),
            DataItem("硬件名称", true),
            DataItem("显示分辨率", true),
            DataItem("屏幕密度", true)
        )
    }

    // 应用信息数据项
    val appInfoItems = remember {
        mutableStateListOf(
            DataItem("应用名称", true),
            DataItem("应用包名", true),
            DataItem("应用版本号", true),
            DataItem("应用版本代码", true),
            DataItem("最低支持API版本", true),
            DataItem("目标API版本", true),
            DataItem("安装时间", true),
            DataItem("更新时间", true)
        )
    }

    // 应用logcat日志类型
    val logcatTypes = remember {
        mutableStateListOf(
            DataItem("警告", true),
            DataItem("信息", true),
            DataItem("错误", true)
        )
    }

    // 彩蛋弹窗状态
    var showEggDialog by remember { mutableStateOf(false) }

    // 计算是否所有选项都未勾选
    val allUnchecked = deviceInfoItems.none { it.checked } &&
                       appInfoItems.none { it.checked } &&
                       logcatTypes.none { it.checked }

    // 计算每个大卡片的选择状态
    val deviceInfoChecked = deviceInfoItems.all { it.checked }
    val deviceInfoPartial = deviceInfoItems.any { it.checked } && !deviceInfoChecked

    val appInfoChecked = appInfoItems.all { it.checked }
    val appInfoPartial = appInfoItems.any { it.checked } && !appInfoChecked

    val logcatChecked = logcatTypes.all { it.checked }
    val logcatPartial = logcatTypes.any { it.checked } && !logcatChecked

    // 计算全选状态
    val allCardsChecked = deviceInfoChecked && appInfoChecked && logcatChecked
    val allCardsPartial = (deviceInfoChecked || deviceInfoPartial) &&
                          (appInfoChecked || appInfoPartial) &&
                          (logcatChecked || logcatPartial) &&
                          !allCardsChecked

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "你要收集什么数据？",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 全选卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "全选",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // 全选三态勾选框
                        TriStateCheckbox(
                            checked = when {
                                allCardsChecked -> CheckboxState.CHECKED
                                allCardsPartial -> CheckboxState.PARTIAL
                                else -> CheckboxState.UNCHECKED
                            },
                            onCheckedChange = { newState ->
                                val shouldCheck = newState == CheckboxState.CHECKED
                                // 联动所有卡片和子选项
                                deviceInfoItems.forEach { it.checked = shouldCheck }
                                appInfoItems.forEach { it.checked = shouldCheck }
                                logcatTypes.forEach { it.checked = shouldCheck }
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 设备信息卡片
                    DataCollectionCard(
                        title = "设备信息",
                        expanded = deviceInfoExpanded,
                        checked = when {
                            deviceInfoChecked -> CheckboxState.CHECKED
                            deviceInfoPartial -> CheckboxState.PARTIAL
                            else -> CheckboxState.UNCHECKED
                        },
                        onExpandClick = { deviceInfoExpanded = !deviceInfoExpanded },
                        onCheckClick = { newState ->
                            val shouldCheck = newState == CheckboxState.CHECKED
                            deviceInfoItems.forEach { it.checked = shouldCheck }
                        }
                    ) {
                        AnimatedVisibility(
                            visible = deviceInfoExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                deviceInfoItems.forEach { item ->
                                    DataItemRow(
                                        item = item,
                                        onCheckedChange = { item.checked = it }
                                    )
                                }
                            }
                        }
                    }

                    // 应用信息卡片
                    DataCollectionCard(
                        title = "应用信息",
                        expanded = appInfoExpanded,
                        checked = when {
                            appInfoChecked -> CheckboxState.CHECKED
                            appInfoPartial -> CheckboxState.PARTIAL
                            else -> CheckboxState.UNCHECKED
                        },
                        onExpandClick = { appInfoExpanded = !appInfoExpanded },
                        onCheckClick = { newState ->
                            val shouldCheck = newState == CheckboxState.CHECKED
                            appInfoItems.forEach { it.checked = shouldCheck }
                        }
                    ) {
                        AnimatedVisibility(
                            visible = appInfoExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                appInfoItems.forEach { item ->
                                    DataItemRow(
                                        item = item,
                                        onCheckedChange = { item.checked = it }
                                    )
                                }
                            }
                        }
                    }

                    // 应用logcat卡片
                    DataCollectionCard(
                        title = "应用logcat",
                        expanded = appLogcatExpanded,
                        checked = when {
                            logcatChecked -> CheckboxState.CHECKED
                            logcatPartial -> CheckboxState.PARTIAL
                            else -> CheckboxState.UNCHECKED
                        },
                        onExpandClick = { appLogcatExpanded = !appLogcatExpanded },
                        onCheckClick = { newState ->
                            val shouldCheck = newState == CheckboxState.CHECKED
                            logcatTypes.forEach { it.checked = shouldCheck }
                        }
                    ) {
                        AnimatedVisibility(
                            visible = appLogcatExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                logcatTypes.forEach { item ->
                                    DataItemRow(
                                        item = item,
                                        onCheckedChange = { item.checked = it }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 导出按钮
                Button(
                    onClick = {
                        if (allUnchecked) {
                            showEggDialog = true
                        } else {
                            onExport(
                                DataCollectionConfig(
                                    deviceInfoItems.map { it.name to it.checked }.toMap(),
                                    appInfoItems.map { it.name to it.checked }.toMap(),
                                    logcatTypes.map { it.name to it.checked }.toMap()
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "导出",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // 彩蛋弹窗
    EggDialog(
        show = showEggDialog,
        onExportEgg = {
            showEggDialog = false
            onExport(
                DataCollectionConfig(
                    deviceInfoItems.map { it.name to false }.toMap(),
                    appInfoItems.map { it.name to false }.toMap(),
                    logcatTypes.map { it.name to false }.toMap(),
                    exportEgg = true
                )
            )
        },
        onDismiss = { showEggDialog = false }
    )
}

/**
 * 数据收集卡片
 */
@Composable
private fun DataCollectionCard(
    title: String,
    expanded: Boolean,
    checked: CheckboxState,
    onExpandClick: () -> Unit,
    onCheckClick: (CheckboxState) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 展开图标
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(300),
                    label = "rotation"
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation)
                        .clickable { onExpandClick() },
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 三态勾选框
                TriStateCheckbox(
                    checked = checked,
                    onCheckedChange = onCheckClick
                )
            }

            // 展开内容
            content()
        }
    }
}

/**
 * 数据项行
 */
@Composable
private fun DataItemRow(
    item: DataItem,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(36.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Checkbox(
            checked = item.checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 三态勾选框（使用 M3 原生组件）
 */
@Composable
private fun TriStateCheckbox(
    checked: CheckboxState,
    onCheckedChange: (CheckboxState) -> Unit
) {
    androidx.compose.material3.TriStateCheckbox(
        state = when (checked) {
            CheckboxState.UNCHECKED -> androidx.compose.ui.state.ToggleableState.Off
            CheckboxState.PARTIAL -> androidx.compose.ui.state.ToggleableState.Indeterminate
            CheckboxState.CHECKED -> androidx.compose.ui.state.ToggleableState.On
        },
        onClick = {
            onCheckedChange(
                when (checked) {
                    CheckboxState.UNCHECKED -> CheckboxState.CHECKED
                    CheckboxState.PARTIAL -> CheckboxState.CHECKED
                    CheckboxState.CHECKED -> CheckboxState.UNCHECKED
                }
            )
        },
        colors = androidx.compose.material3.CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            indeterminateColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * 二态勾选框（Material You 3 风格，用于子选项）
 */
@Composable
private fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.material3.Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = androidx.compose.material3.CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            disabledCheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        ),
        modifier = Modifier.size(24.dp)
    )
}

/**
 * 勾选框状态
 */
enum class CheckboxState {
    UNCHECKED,   // 未选中
    PARTIAL,     // 部分选中
    CHECKED      // 全选
}

/**
 * 彩蛋弹窗
 */
@Composable
private fun EggDialog(
    show: Boolean,
    onExportEgg: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "你导出个蛋啊",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportEgg,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "我不管",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "确定",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 数据项
 */
data class DataItem(
    val name: String,
    var checked: Boolean
)

/**
 * 数据收集配置
 */
data class DataCollectionConfig(
    val deviceInfo: Map<String, Boolean>,
    val appInfo: Map<String, Boolean>,
    val logcatTypes: Map<String, Boolean>,
    val exportEgg: Boolean = false
)