package com.timome.eggyhub.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DataCollectionDialog(
    show: Boolean,
    onExport: (DataCollectionConfig) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    var deviceExpanded by remember { mutableStateOf(true) }
    var appExpanded by remember { mutableStateOf(true) }
    var logcatExpanded by remember { mutableStateOf(true) }

    var deviceTime by remember { mutableStateOf(false) }
    var deviceBrand by remember { mutableStateOf(false) }
    var deviceModel by remember { mutableStateOf(false) }
    var deviceManufacturer by remember { mutableStateOf(false) }
    var deviceSystemVersion by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf(false) }
    var deviceProduct by remember { mutableStateOf(false) }
    var deviceHardware by remember { mutableStateOf(false) }
    var deviceResolution by remember { mutableStateOf(false) }
    var deviceDensity by remember { mutableStateOf(false) }

    var appName by remember { mutableStateOf(false) }
    var appPackage by remember { mutableStateOf(false) }
    var appVersionName by remember { mutableStateOf(false) }
    var appVersionCode by remember { mutableStateOf(false) }
    var appMinSdk by remember { mutableStateOf(false) }
    var appTargetSdk by remember { mutableStateOf(false) }
    var appInstallTime by remember { mutableStateOf(false) }
    var appUpdateTime by remember { mutableStateOf(false) }

    var logcatWarning by remember { mutableStateOf(false) }
    var logcatInfo by remember { mutableStateOf(false) }
    var logcatError by remember { mutableStateOf(false) }
    var logcatDebug by remember { mutableStateOf(false) }
    var logcatVerbose by remember { mutableStateOf(false) }

    val deviceInfoMap = mapOf(
        "时间" to deviceTime,
        "设备品牌" to deviceBrand,
        "设备型号" to deviceModel,
        "设备制造商" to deviceManufacturer,
        "系统版本" to deviceSystemVersion,
        "设备名称" to deviceName,
        "产品名称" to deviceProduct,
        "硬件名称" to deviceHardware,
        "显示分辨率" to deviceResolution,
        "屏幕密度" to deviceDensity
    )

    val appInfoMap = mapOf(
        "应用名称" to appName,
        "应用包名" to appPackage,
        "应用版本号" to appVersionName,
        "应用版本代码" to appVersionCode,
        "最低支持API版本" to appMinSdk,
        "目标API版本" to appTargetSdk,
        "安装时间" to appInstallTime,
        "更新时间" to appUpdateTime
    )

    val logcatMap = mapOf(
        "警告" to logcatWarning,
        "信息" to logcatInfo,
        "错误" to logcatError,
        "调试" to logcatDebug,
        "详细" to logcatVerbose
    )

    val allUnchecked = !deviceTime && !deviceBrand && !deviceModel && !deviceManufacturer &&
                       !deviceSystemVersion && !deviceName && !deviceProduct && !deviceHardware &&
                       !deviceResolution && !deviceDensity && !appName && !appPackage &&
                       !appVersionName && !appVersionCode && !appMinSdk && !appTargetSdk &&
                       !appInstallTime && !appUpdateTime && !logcatWarning && !logcatInfo && 
                       !logcatError && !logcatDebug && !logcatVerbose

    val deviceCheckedCount = listOf(deviceTime, deviceBrand, deviceModel, deviceManufacturer,
        deviceSystemVersion, deviceName, deviceProduct, deviceHardware,
        deviceResolution, deviceDensity).count { it }

    val appCheckedCount = listOf(appName, appPackage, appVersionName, appVersionCode,
        appMinSdk, appTargetSdk, appInstallTime, appUpdateTime).count { it }

    val logcatCheckedCount = listOf(logcatWarning, logcatInfo, logcatError, logcatDebug, logcatVerbose).count { it }

    val deviceAllChecked = deviceCheckedCount == 10
    val devicePartial = deviceCheckedCount > 0 && deviceCheckedCount < 10

    val appAllChecked = appCheckedCount == 8
    val appPartial = appCheckedCount > 0 && appCheckedCount < 8

    val logcatAllChecked = logcatCheckedCount == 5
    val logcatPartial = logcatCheckedCount > 0 && logcatCheckedCount < 5

    val allChecked = deviceAllChecked && appAllChecked && logcatAllChecked
    val allPartial = (deviceAllChecked || devicePartial) &&
                     (appAllChecked || appPartial) &&
                     (logcatAllChecked || logcatPartial) &&
                     !allChecked

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "你要收集什么数据？",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
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

                    TriStateCheckbox(
                        state = when {
                            allChecked -> androidx.compose.ui.state.ToggleableState.On
                            allPartial -> androidx.compose.ui.state.ToggleableState.Indeterminate
                            else -> androidx.compose.ui.state.ToggleableState.Off
                        },
                        onClick = {
                            val newState = when {
                                allChecked -> false
                                else -> true
                            }
                            deviceTime = newState
                            deviceBrand = newState
                            deviceModel = newState
                            deviceManufacturer = newState
                            deviceSystemVersion = newState
                            deviceName = newState
                            deviceProduct = newState
                            deviceHardware = newState
                            deviceResolution = newState
                            deviceDensity = newState
                            appName = newState
                            appPackage = newState
                            appVersionName = newState
                            appVersionCode = newState
                            appMinSdk = newState
                            appTargetSdk = newState
                            appInstallTime = newState
                            appUpdateTime = newState
                            logcatWarning = newState
                            logcatInfo = newState
                            logcatError = newState
                            logcatDebug = newState
                            logcatVerbose = newState
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            uncheckedColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            checkmarkColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                ExpandableSectionCard(
                    title = "设备信息",
                    expanded = deviceExpanded,
                    onExpandChange = { deviceExpanded = it },
                    allChecked = deviceAllChecked,
                    partial = devicePartial,
                    onToggleAll = { checked ->
                        deviceTime = checked
                        deviceBrand = checked
                        deviceModel = checked
                        deviceManufacturer = checked
                        deviceSystemVersion = checked
                        deviceName = checked
                        deviceProduct = checked
                        deviceHardware = checked
                        deviceResolution = checked
                        deviceDensity = checked
                    }
                ) {
                    CheckboxRow("时间", deviceTime) { deviceTime = it }
                    CheckboxRow("设备品牌", deviceBrand) { deviceBrand = it }
                    CheckboxRow("设备型号", deviceModel) { deviceModel = it }
                    CheckboxRow("设备制造商", deviceManufacturer) { deviceManufacturer = it }
                    CheckboxRow("系统版本", deviceSystemVersion) { deviceSystemVersion = it }
                    CheckboxRow("设备名称", deviceName) { deviceName = it }
                    CheckboxRow("产品名称", deviceProduct) { deviceProduct = it }
                    CheckboxRow("硬件名称", deviceHardware) { deviceHardware = it }
                    CheckboxRow("显示分辨率", deviceResolution) { deviceResolution = it }
                    CheckboxRow("屏幕密度", deviceDensity) { deviceDensity = it }
                }

                ExpandableSectionCard(
                    title = "应用信息",
                    expanded = appExpanded,
                    onExpandChange = { appExpanded = it },
                    allChecked = appAllChecked,
                    partial = appPartial,
                    onToggleAll = { checked ->
                        appName = checked
                        appPackage = checked
                        appVersionName = checked
                        appVersionCode = checked
                        appMinSdk = checked
                        appTargetSdk = checked
                        appInstallTime = checked
                        appUpdateTime = checked
                    }
                ) {
                    CheckboxRow("应用名称", appName) { appName = it }
                    CheckboxRow("应用包名", appPackage) { appPackage = it }
                    CheckboxRow("应用版本号", appVersionName) { appVersionName = it }
                    CheckboxRow("应用版本代码", appVersionCode) { appVersionCode = it }
                    CheckboxRow("最低支持API版本", appMinSdk) { appMinSdk = it }
                    CheckboxRow("目标API版本", appTargetSdk) { appTargetSdk = it }
                    CheckboxRow("安装时间", appInstallTime) { appInstallTime = it }
                    CheckboxRow("更新时间", appUpdateTime) { appUpdateTime = it }
                }

                ExpandableSectionCard(
                    title = "应用logcat",
                    expanded = logcatExpanded,
                    onExpandChange = { logcatExpanded = it },
                    allChecked = logcatAllChecked,
                    partial = logcatPartial,
                    onToggleAll = { checked ->
                        logcatWarning = checked
                        logcatInfo = checked
                        logcatError = checked
                        logcatDebug = checked
                        logcatVerbose = checked
                    }
                ) {
                    CheckboxRow("警告", logcatWarning) { logcatWarning = it }
                    CheckboxRow("信息", logcatInfo) { logcatInfo = it }
                    CheckboxRow("错误", logcatError) { logcatError = it }
                    CheckboxRow("调试", logcatDebug) { logcatDebug = it }
                    CheckboxRow("详细", logcatVerbose) { logcatVerbose = it }
                }

                Button(
                    onClick = {
                        onExport(
                            DataCollectionConfig(
                                deviceInfo = deviceInfoMap,
                                appInfo = appInfoMap,
                                logcatTypes = logcatMap,
                                exportEgg = allUnchecked
                            )
                        )
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
}

@Composable
private fun ExpandableSectionCard(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    allChecked: Boolean,
    partial: Boolean,
    onToggleAll: (Boolean) -> Unit,
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
                        .clickable(
                            onClick = { onExpandChange(!expanded) },
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { onExpandChange(!expanded) },
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                TriStateCheckbox(
                    state = when {
                        allChecked -> androidx.compose.ui.state.ToggleableState.On
                        partial -> androidx.compose.ui.state.ToggleableState.Indeterminate
                        else -> androidx.compose.ui.state.ToggleableState.Off
                    },
                    onClick = { onToggleAll(!allChecked) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}

data class DataCollectionConfig(
    val deviceInfo: Map<String, Boolean>,
    val appInfo: Map<String, Boolean>,
    val logcatTypes: Map<String, Boolean>,
    val exportEgg: Boolean = false
)