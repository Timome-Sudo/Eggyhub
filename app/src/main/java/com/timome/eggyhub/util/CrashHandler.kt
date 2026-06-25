package com.timome.eggyhub.util

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import com.timome.eggyhub.CrashActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var application: Application

    var crashInfo: CrashInfo? = null

    fun init(app: Application) {
        application = app
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "应用崩溃", throwable)

        val crashTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val stackTrace = getStackTraceString(throwable)
        val logcatContent = getLogcatContent()

        crashInfo = CrashInfo(
            time = crashTime,
            threadName = thread.name,
            exceptionType = throwable.javaClass.name,
            message = throwable.message ?: "未知错误",
            stackTrace = stackTrace,
            logcat = logcatContent
        )

        saveCrashLog(crashInfo!!)

        launchCrashActivity()
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sb = StringBuilder()
        sb.append(throwable.toString()).append("\n")
        for (element in throwable.stackTrace) {
            sb.append("\tat ").append(element.toString()).append("\n")
            if (throwable.cause != null) {
                sb.append("Caused by: ").append(throwable.cause).append("\n")
            }
        }
        return sb.toString()
    }

    private fun getLogcatContent(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            reader.close()
            process.destroy()
            sb.toString()
        } catch (e: Exception) {
            Log.e(TAG, "获取logcat失败", e)
            "获取logcat失败: ${e.message}"
        }
    }

    private fun saveCrashLog(info: CrashInfo) {
        try {
            val fileName = "crash_${System.currentTimeMillis()}.txt"
            val file = File(application.cacheDir, fileName)
            FileWriter(file).use { writer ->
                writer.appendLine("========== 崩溃信息 ==========")
                writer.appendLine("时间: ${info.time}")
                writer.appendLine("线程: ${info.threadName}")
                writer.appendLine("异常类型: ${info.exceptionType}")
                writer.appendLine("错误信息: ${info.message}")
                writer.appendLine()
                writer.appendLine("========== 堆栈跟踪 ==========")
                writer.appendLine(info.stackTrace)
                writer.appendLine("========== Logcat日志 ==========")
                writer.appendLine(info.logcat)
            }
            Log.d(TAG, "崩溃日志已保存到: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志失败", e)
        }
    }

    private fun launchCrashActivity() {
        val intent = Intent(application, CrashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        application.startActivity(intent)
    }

    fun getCrashReport(): String {
        val info = crashInfo ?: return "无崩溃信息"
        val sb = StringBuilder()
        sb.append("========== 崩溃报告 ==========\n")
        sb.append("时间: ${info.time}\n")
        sb.append("线程: ${info.threadName}\n")
        sb.append("异常类型: ${info.exceptionType}\n")
        sb.append("错误信息: ${info.message}\n")
        sb.append("\n")
        sb.append("========== 堆栈跟踪 ==========\n")
        sb.append(info.stackTrace)
        return sb.toString()
    }
}

data class CrashInfo(
    val time: String,
    val threadName: String,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val logcat: String
)