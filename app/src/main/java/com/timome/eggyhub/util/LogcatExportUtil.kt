package com.timome.eggyhub.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.timome.eggyhub.ui.component.DataCollectionConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logcat导出工具类
 * 负责收集设备信息、应用版本信息和logcat日志
 * 直接写入文件，避免内存溢出
 */
object LogcatExportUtil {

    private const val TAG = "LogcatExportUtil"
    private const val REQUEST_CODE_WRITE_STORAGE = 1001

    /**
     * 检查是否有写入存储权限
     */
    fun hasWriteStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上不需要存储权限
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 请求写入存储权限
     */
    fun requestWriteStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_WRITE_STORAGE
            )
        }
    }

    /**
     * 导出完整日志并直接写入文件（避免内存溢出）
     * 不限制日志行数，直接流式写入文件
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun exportLogToFile(context: Context): File? {
        return exportLogToFile(context, DataCollectionConfig(
            deviceInfo = mapOf(
                "时间" to true,
                "设备品牌" to true,
                "设备型号" to true,
                "设备制造商" to true,
                "系统版本" to true,
                "设备名称" to true,
                "产品名称" to true,
                "硬件名称" to true,
                "显示分辨率" to true,
                "屏幕密度" to true
            ),
            appInfo = mapOf(
                "应用名称" to true,
                "应用包名" to true,
                "应用版本号" to true,
                "应用版本代码" to true,
                "最低支持API版本" to true,
                "目标API版本" to true,
                "安装时间" to true,
                "更新时间" to true
            ),
            logcatTypes = mapOf(
                "警告" to true,
                "信息" to true,
                "错误" to true
            )
        ))
    }

    /**
     * 导出完整日志并直接写入文件（避免内存溢出）
     * 根据用户选择收集数据
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun exportLogToFile(context: Context, config: DataCollectionConfig): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "eggyhub_log_$timestamp.txt"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val logFile = File(downloadsDir, fileName)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            // 直接写入文件，避免内存溢出
            logFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                // 写入标题
                writer.appendLine("========================================")
                writer.appendLine("       EggyHub 应用日志导出")
                writer.appendLine("========================================")
                writer.appendLine()

                // 彩蛋模式
                if (config.exportEgg) {
                    writer.appendLine("蛋")
                    writer.appendLine()
                    writer.appendLine("========================================")
                    writer.appendLine("       日志导出完成")
                    writer.appendLine("========================================")
                    Log.d(TAG, "日志已保存到: ${logFile.absolutePath}")
                    return logFile
                }

                // 写入设备信息
                writer.appendLine("========== 设备信息 ==========")
                if (config.deviceInfo["时间"] == true) {
                    writer.appendLine("时间: ${dateFormat.format(Date())}")
                } else {
                    writer.appendLine("时间: 用户未同意收集此信息")
                }
                if (config.deviceInfo["设备品牌"] == true) {
                    writer.appendLine("设备品牌: ${Build.BRAND}")
                } else {
                    writer.appendLine("设备品牌: 用户未同意收集此信息")
                }
                if (config.deviceInfo["设备型号"] == true) {
                    writer.appendLine("设备型号: ${Build.MODEL}")
                } else {
                    writer.appendLine("设备型号: 用户未同意收集此信息")
                }
                if (config.deviceInfo["设备制造商"] == true) {
                    writer.appendLine("设备制造商: ${Build.MANUFACTURER}")
                } else {
                    writer.appendLine("设备制造商: 用户未同意收集此信息")
                }
                if (config.deviceInfo["系统版本"] == true) {
                    writer.appendLine("系统版本: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                } else {
                    writer.appendLine("系统版本: 用户未同意收集此信息")
                }
                if (config.deviceInfo["设备名称"] == true) {
                    writer.appendLine("设备名称: ${Build.DEVICE}")
                } else {
                    writer.appendLine("设备名称: 用户未同意收集此信息")
                }
                if (config.deviceInfo["产品名称"] == true) {
                    writer.appendLine("产品名称: ${Build.PRODUCT}")
                } else {
                    writer.appendLine("产品名称: 用户未同意收集此信息")
                }
                if (config.deviceInfo["硬件名称"] == true) {
                    writer.appendLine("硬件名称: ${Build.HARDWARE}")
                } else {
                    writer.appendLine("硬件名称: 用户未同意收集此信息")
                }
                if (config.deviceInfo["显示分辨率"] == true) {
                    writer.appendLine("显示分辨率: ${context.resources.displayMetrics.widthPixels}x${context.resources.displayMetrics.heightPixels}")
                } else {
                    writer.appendLine("显示分辨率: 用户未同意收集此信息")
                }
                if (config.deviceInfo["屏幕密度"] == true) {
                    writer.appendLine("屏幕密度: ${context.resources.displayMetrics.densityDpi} dpi")
                } else {
                    writer.appendLine("屏幕密度: 用户未同意收集此信息")
                }
                writer.appendLine("===============================")
                writer.appendLine()

                // 写入应用信息
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)

                    writer.appendLine("========== 应用信息 ==========")
                    if (config.appInfo["应用名称"] == true) {
                        writer.appendLine("应用名称: ${context.packageManager.getApplicationLabel(appInfo)}")
                    } else {
                        writer.appendLine("应用名称: 用户未同意收集此信息")
                    }
                    if (config.appInfo["应用包名"] == true) {
                        writer.appendLine("应用包名: ${context.packageName}")
                    } else {
                        writer.appendLine("应用包名: 用户未同意收集此信息")
                    }
                    if (config.appInfo["应用版本号"] == true) {
                        writer.appendLine("应用版本号: ${packageInfo.versionName ?: "未知"}")
                    } else {
                        writer.appendLine("应用版本号: 用户未同意收集此信息")
                    }
                    if (config.appInfo["应用版本代码"] == true) {
                        writer.appendLine("应用版本代码: ${packageInfo.longVersionCode}")
                    } else {
                        writer.appendLine("应用版本代码: 用户未同意收集此信息")
                    }
                    if (config.appInfo["最低支持API版本"] == true) {
                        writer.appendLine("最低支持API版本: ${appInfo.minSdkVersion}")
                    } else {
                        writer.appendLine("最低支持API版本: 用户未同意收集此信息")
                    }
                    if (config.appInfo["目标API版本"] == true) {
                        writer.appendLine("目标API版本: ${appInfo.targetSdkVersion}")
                    } else {
                        writer.appendLine("目标API版本: 用户未同意收集此信息")
                    }
                    if (config.appInfo["安装时间"] == true) {
                        writer.appendLine("安装时间: ${dateFormat.format(Date(packageInfo.firstInstallTime))}")
                    } else {
                        writer.appendLine("安装时间: 用户未同意收集此信息")
                    }
                    if (config.appInfo["更新时间"] == true) {
                        writer.appendLine("更新时间: ${dateFormat.format(Date(packageInfo.lastUpdateTime))}")
                    } else {
                        writer.appendLine("更新时间: 用户未同意收集此信息")
                    }
                    writer.appendLine("===============================")
                    writer.appendLine()
                } catch (e: Exception) {
                    writer.appendLine("========== 应用信息 ==========")
                    writer.appendLine("获取应用信息失败: ${e.message}")
                    writer.appendLine("===============================")
                    writer.appendLine()
                }

                // 直接写入logcat日志（不限制行数，流式写入避免内存溢出）
                writer.appendLine("========== Logcat日志 ==========")
                // 检查是否选择了任何logcat类型
                val anyLogcatTypeSelected = config.logcatTypes.any { it.value }
                if (!anyLogcatTypeSelected) {
                    // 未勾选任何logcat类型，不输出日志
                    writer.appendLine("用户未同意收集任何日志类型")
                } else {
                    try {
                        val process = Runtime.getRuntime().exec("logcat -d -v time")
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?

                        while (reader.readLine().also { line = it } != null) {
                            // 根据用户选择过滤日志类型
                            // 未勾选的类型对应的日志不输出
                            val shouldInclude = when {
                                config.logcatTypes["错误"] == true && line?.contains("E/") == true -> true
                                config.logcatTypes["警告"] == true && line?.contains("W/") == true -> true
                                config.logcatTypes["信息"] == true && line?.contains("I/") == true -> true
                                else -> false // 未勾选类型的日志不输出
                            }

                            if (shouldInclude) {
                                writer.appendLine(line)
                            }
                        }

                        reader.close()
                        process.destroy()
                    } catch (e: Exception) {
                        writer.appendLine("收集logcat失败: ${e.message}")
                        e.printStackTrace()
                    }
                }
                writer.appendLine("===============================")
                writer.appendLine()

                // 写入结尾
                writer.appendLine("========================================")
                writer.appendLine("       日志导出完成")
                writer.appendLine("========================================")
            }

            Log.d(TAG, "日志已保存到: ${logFile.absolutePath}")
            logFile
        } catch (e: Exception) {
            Log.e(TAG, "保存日志失败", e)
            null
        }
    }

    /**
     * 清除logcat缓冲区
     */
    fun clearLogcat() {
        try {
            Runtime.getRuntime().exec("logcat -c")
            Log.d(TAG, "logcat缓冲区已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除logcat失败", e)
        }
    }
}