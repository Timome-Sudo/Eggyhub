package com.timome.eggyhub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.timome.eggyhub.data.CaptchaManager
import com.timome.eggyhub.ui.component.CaptchaDialog
import com.timome.eggyhub.ui.component.LoadingDialog
import com.timome.eggyhub.ui.component.ExportLogcatWarningDialog
import com.timome.eggyhub.ui.component.ExportLogcatProgressDialog
import com.timome.eggyhub.ui.component.DataCollectionDialog
import com.timome.eggyhub.util.LogcatExportUtil
import com.timome.eggyhub.ui.component.DataCollectionConfig
import kotlinx.coroutines.launch

/**
 * 开发者模式页面
 *
 * - 顶部卡片：左侧"开发者选项"文字，右侧开关，默认关闭
 * - 开关状态控制下方所有按钮的启用/禁用
 * - "等待进度条测试"按钮：点击弹出等待弹窗，需要手动点击关闭按钮才能关闭
 */
@Composable
fun DevModeScreen(
    initialEnabled: Boolean = false,
    onEnabledChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 开发者选项开关状态
    var devOptionsEnabled by remember { mutableStateOf(initialEnabled) }

    // 等待进度条弹窗显示状态
    var showLoadingDialog by remember { mutableStateOf(false) }

    // ==================== 导出logcat ====================
    var showExportWarningDialog by remember { mutableStateOf(false) }
    var showExportProgressDialog by remember { mutableStateOf(false) }
    var showDataCollectionDialog by remember { mutableStateOf(false) }

    // ==================== 人机验证测试 ====================
    // 类型选择弹窗显示状态
    var showCaptchaTypePicker by remember { mutableStateOf(false) }
    // 实际验证弹窗显示状态
    var showCaptchaTestDialog by remember { mutableStateOf(false) }
    // 用户选择的验证类型（null = 正常/随机）
    var selectedCaptchaType by remember { mutableStateOf<CaptchaManager.CaptchaType?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== 顶部：开发者选项开关卡片 ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "开发者选项",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Switch(
                        checked = devOptionsEnabled,
                        onCheckedChange = {
                            devOptionsEnabled = it
                            onEnabledChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ========== 下方：开发者测试按钮列表 ==========
            Text(
                text = "开发者测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 等待进度条测试按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "等待进度条测试",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "点击弹出等待弹窗，需手动关闭",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { showLoadingDialog = true },
                        enabled = devOptionsEnabled,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(
                            text = "测试",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 人机验证测试按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "人机验证测试",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "选择验证类型后弹出对应验证弹窗",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { showCaptchaTypePicker = true },
                        enabled = devOptionsEnabled,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(
                            text = "选择",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 预留位置：其他开发者测试按钮（可后续扩展）
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "其他测试",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "保留给后续扩展",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 导出logcat按钮
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "导出logcat",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "导出应用日志用于调试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = { showExportWarningDialog = true },
                        enabled = devOptionsEnabled,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Text(
                            text = "导出",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ========== 等待进度条弹窗（开发者测试模式） ==========
        LoadingDialog(
            show = showLoadingDialog,
            message = "正在测试中...",
            autoDismiss = false,
            showCloseButton = true,
            onDismiss = { showLoadingDialog = false }
        )

        // ========== 导出logcat警告弹窗 ==========
        ExportLogcatWarningDialog(
            show = showExportWarningDialog,
            onConfirm = {
                showExportWarningDialog = false
                showDataCollectionDialog = true
            },
            onDismiss = {
                showExportWarningDialog = false
            }
        )

        // ========== 数据收集选择弹窗 ==========
        DataCollectionDialog(
            show = showDataCollectionDialog,
            onExport = { config ->
                showDataCollectionDialog = false
                showExportProgressDialog = true

                // 在协程中执行导出操作
                coroutineScope.launch {
                    try {
                        // 检查权限
                        if (!LogcatExportUtil.hasWriteStoragePermission(context)) {
                            android.widget.Toast.makeText(
                                context,
                                "需要存储权限才能导出日志",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            showExportProgressDialog = false
                            return@launch
                        }

                        // 导出日志
                        val logFile = LogcatExportUtil.exportLogToFile(context, config)

                        if (logFile != null) {
                            android.widget.Toast.makeText(
                                context,
                                "日志已导出到: ${logFile.absolutePath}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "日志导出失败",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(
                            context,
                            "日志导出失败: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        showExportProgressDialog = false
                    }
                }
            },
            onDismiss = {
                showDataCollectionDialog = false
            }
        )

        // ========== 导出logcat进度弹窗 ==========
        ExportLogcatProgressDialog(
            show = showExportProgressDialog,
            onDismiss = {
                showExportProgressDialog = false
            }
        )

        // ========== 人机验证类型选择弹窗（开发者测试模式） ==========
        if (showCaptchaTypePicker) {
            Dialog(
                onDismissRequest = { showCaptchaTypePicker = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "选择验证类型",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 正常验证（随机）
                        CaptchaTypeOptionItem(
                            title = "正常验证",
                            subtitle = "随机选择一种验证类型",
                            onClick = {
                                selectedCaptchaType = null
                                showCaptchaTypePicker = false
                                showCaptchaTestDialog = true
                            }
                        )

                        // 滑条验证
                        CaptchaTypeOptionItem(
                            title = CaptchaManager.CaptchaType.SLIDER.displayName,
                            subtitle = "拖动滑块到指定数值",
                            onClick = {
                                selectedCaptchaType = CaptchaManager.CaptchaType.SLIDER
                                showCaptchaTypePicker = false
                                showCaptchaTestDialog = true
                            }
                        )

                        // 盲猜滑条验证
                        CaptchaTypeOptionItem(
                            title = CaptchaManager.CaptchaType.BLIND_SLIDER.displayName,
                            subtitle = "盲猜滑块位置（±10范围内）",
                            onClick = {
                                selectedCaptchaType = CaptchaManager.CaptchaType.BLIND_SLIDER
                                showCaptchaTypePicker = false
                                showCaptchaTestDialog = true
                            }
                        )

                        // 计算验证
                        CaptchaTypeOptionItem(
                            title = CaptchaManager.CaptchaType.MATH.displayName,
                            subtitle = "解答随机生成的算术题",
                            onClick = {
                                selectedCaptchaType = CaptchaManager.CaptchaType.MATH
                                showCaptchaTypePicker = false
                                showCaptchaTestDialog = true
                            }
                        )

                        // 计数验证
                        CaptchaTypeOptionItem(
                            title = CaptchaManager.CaptchaType.COUNT.displayName,
                            subtitle = "统计文本中特定字符的出现次数",
                            onClick = {
                                selectedCaptchaType = CaptchaManager.CaptchaType.COUNT
                                showCaptchaTypePicker = false
                                showCaptchaTestDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 取消按钮
                        TextButton(
                            onClick = { showCaptchaTypePicker = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("取消")
                        }
                    }
                }
            }
        }

        // ========== 人机验证测试弹窗（开发者模式） ==========
        CaptchaDialog(
            show = showCaptchaTestDialog,
            onVerified = { showCaptchaTestDialog = false },
            onCancel = { showCaptchaTestDialog = false },
            forcedType = selectedCaptchaType
        )
    }
}

/**
 * 类型选择弹窗中的单条选项（点击即选中）
 */
@Composable
private fun CaptchaTypeOptionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextButton(onClick = onClick) {
                Text("选择")
            }
        }
    }
}
