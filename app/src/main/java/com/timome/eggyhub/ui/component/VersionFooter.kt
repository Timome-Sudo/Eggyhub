package com.timome.eggyhub.ui.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 版本号底部组件
 *
 * - 实时从 PackageInfo 获取 versionName
 * - 连续点击 7 次（两次点击间隔 < 1.5s）触发 onSevenClicks 回调
 *
 * @param onSevenClicks 连续点击 7 次后的回调（用于进入开发者模式）
 */
@Composable
fun VersionFooter(
    onSevenClicks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val versionName: String = remember(context) { getVersionName(context) }

    // 记录点击次数和上次点击时间
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "版本 $versionName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier
                .clickable(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime < 1500L) {
                            // 1.5s 内的连续点击
                            clickCount += 1
                            if (clickCount >= 7) {
                                clickCount = 0
                                onSevenClicks()
                            }
                        } else {
                            // 超过 1.5s，重新计数
                            clickCount = 1
                        }
                        lastClickTime = now
                    },
                    indication = null,
                    interactionSource = null
                )
                .padding(vertical = 8.dp)
        )
    }
}

/**
 * 从 PackageManager 获取 versionName
 */
private fun getVersionName(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
}
