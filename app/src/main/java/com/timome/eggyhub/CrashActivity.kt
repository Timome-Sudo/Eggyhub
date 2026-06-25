package com.timome.eggyhub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.timome.eggyhub.ui.component.DataCollectionConfig
import com.timome.eggyhub.ui.screen.CrashScreen
import com.timome.eggyhub.ui.theme.EggyhubTheme
import com.timome.eggyhub.util.CrashHandler
import com.timome.eggyhub.util.LogcatExportUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EggyhubTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val crashInfo = remember { CrashHandler.crashInfo }
                    CrashScreen(
                        crashInfo = crashInfo,
                        onRestart = {
                            val intent = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        },
                        onCopy = { text ->
                            (getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).apply {
                                setPrimaryClip(
                                    android.content.ClipData.newPlainText("崩溃信息", text)
                                )
                            }
                            android.widget.Toast.makeText(this, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onContactDeveloper = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/q/4HHCFlN9M4"))
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            } else {
                                android.widget.Toast.makeText(this, "未找到浏览器应用", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExportLogcat = { config, onProgress ->
                            @OptIn(DelicateCoroutinesApi::class)
                            GlobalScope.launch(Dispatchers.IO) {
                                val success = LogcatExportUtil.exportLogToFile(this@CrashActivity, config) != null
                                withContext(Dispatchers.Main) {
                                    onProgress(false)
                                    if (success) {
                                        android.widget.Toast.makeText(
                                            this@CrashActivity,
                                            "日志导出成功",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        android.widget.Toast.makeText(
                                            this@CrashActivity,
                                            "日志导出失败",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        onClose = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }
}
