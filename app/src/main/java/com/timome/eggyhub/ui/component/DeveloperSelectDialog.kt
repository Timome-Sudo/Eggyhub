package com.timome.eggyhub.ui.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 收集 logcat 并保存到缓存目录
 * @return logcat 文件的 Uri，失败则返回 null
 */
fun collectLogcat(context: Context): Uri? {
    return try {
        val logcatFile = File(context.cacheDir, "eggyhub_logcat_${System.currentTimeMillis()}.txt")
        val process = Runtime.getRuntime().exec("logcat -d -v time")
        val inputStream = process.inputStream
        val outputStream = logcatFile.outputStream()
        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()
        process.waitFor()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logcatFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 调用系统邮件应用发送邮件
 */
fun sendEmailWithAttachment(
    context: Context,
    toEmail: String,
    subject: String,
    body: String,
    attachmentUri: Uri?
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        if (attachmentUri != null) {
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "发送邮件"))
}

/**
 * 开发者卡片信息
 */
private sealed class DeveloperCard {
    data class DirectEmail(
        val name: String,
        val title: String,
        val email: String
    ) : DeveloperCard()

    data class EmailSelect(
        val name: String,
        val title: String
    ) : DeveloperCard()
}

private val developerCards = listOf(
    DeveloperCard.DirectEmail("timome", "该版本开发者", "timome@qq.com"),
    DeveloperCard.EmailSelect("云云鬼才", "管理员，官方版本开发者"),
    DeveloperCard.DirectEmail("水杨酸酸", "创始人，网站运营者", "3970771550@qq.com")
)

/**
 * 开发者选择弹窗
 * - timome / 水杨酸酸：点击后直接收集 logcat 并发送邮件
 * - 云云鬼才：点击后调用回调，弹出邮箱选择弹窗（不关闭当前弹窗）
 * - 返回箭头：调用 onBack 回调返回上一个弹窗
 * - 不可点击外部关闭
 */
@Composable
fun DeveloperSelectDialog(
    show: Boolean,
    onBack: () -> Unit = {},
    onYunggClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (!show) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCollecting by remember { mutableStateOf(false) }

    /**
     * 直接发送邮件的开发者卡片点击处理
     */
    fun onDirectEmailClick(email: String) {
        isCollecting = true
        coroutineScope.launch(Dispatchers.IO) {
            val attachmentUri = collectLogcat(context)
            isCollecting = false
            val subject = "我遇到了问题！"
            val body = "我遇到了（用户自行填写），附件为 log cat"
            sendEmailWithAttachment(context, email, subject, body, attachmentUri)
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (isCollecting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "正在收集日志...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题行（返回箭头）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onBack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "选择开发者",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 开发者卡片列表
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        developerCards.forEach { card ->
                            when (card) {
                                is DeveloperCard.DirectEmail -> {
                                    OutlinedCard(
                                        onClick = { onDirectEmailClick(card.email) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = card.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = card.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                is DeveloperCard.EmailSelect -> {
                                    OutlinedCard(
                                        onClick = { onYunggClick() },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = card.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = card.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 关闭按钮
                    OutlinedButton(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "关闭",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
