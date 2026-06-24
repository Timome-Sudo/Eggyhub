package com.timome.eggyhub.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Composable
fun ProfilePageContent(
    username: String = "",
    userId: String = "",
    email: String = "",
    description: String = "",
    avatarUrl: String = "",
    role: String = "",
    sponser: String = "",
    eggyid: String = "",
    contact: String = "",
    onLogoutClick: () -> Unit,
    onAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAvatarLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(avatarUrl) {
        if (avatarUrl.isBlank()) {
            isAvatarLoaded = false
            loadedBitmap = null
            return@LaunchedEffect
        }
        isAvatarLoaded = false
        val bitmap = loadAvatarBitmap(avatarUrl)
        if (bitmap != null) {
            loadedBitmap = bitmap
            isAvatarLoaded = true
        }
    }

    val userTypeDisplay = remember(role, sponser) {
        when {
            role == "admin" -> "管理员"
            role == "user" && sponser != "0" && sponser != "0.0" -> "赞助用户"
            role == "user" -> "标准用户"
            else -> "未知"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { isExpanded = !isExpanded },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isAvatarLoaded && loadedBitmap != null,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 500)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 300))
                        },
                        label = "avatarSwitch"
                    ) { targetState ->
                        if (targetState && loadedBitmap != null) {
                            val circular = remember(loadedBitmap) { makeCircleBitmap(loadedBitmap!!) }
                            androidx.compose.foundation.Image(
                                bitmap = circular.asImageBitmap(),
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val displayName = username.ifBlank { "用户" }
                            val firstLetter = displayName.firstOrNull()?.uppercase() ?: "U"
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstLetter,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = username.ifBlank { "用户" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val emailText = if (email.isNotBlank()) email else "未设置"
                        Text(
                            text = "邮箱：$emailText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.combinedClickable(
                                onLongClick = {
                                    if (email.isNotBlank()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("email", email)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "已复制到剪切板！", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClick = {}
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                AnimatedContent(
                    targetState = isExpanded,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            fadeOut(animationSpec = tween(durationMillis = 200))
                    },
                    label = "expandedContent"
                ) { expanded ->
                    if (expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            val rawItems = listOfNotNull(
                                Triple("ID", if (userId.isNotBlank()) userId else "未设置", userId),
                                Triple("用户类型", userTypeDisplay, userTypeDisplay),
                                Triple("蛋仔昵称", if (eggyid.isNotBlank()) eggyid else "未设置", eggyid),
                                Triple("简介", if (description.isNotBlank()) description else "未设置", description),
                                Triple("联系方式", if (contact.isNotBlank()) contact else "未设置", contact)
                            )

                            rawItems.forEachIndexed { index, (label, value, copyValue) ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$label：",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 13.sp,
                                        modifier = Modifier.combinedClickable(
                                            onLongClick = {
                                                if (copyValue.isNotBlank()) {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText(label, copyValue)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "已复制到剪切板！", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onClick = {}
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.size(0.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val settingsItems = listOf(
            "账户设置" to null,
            "通知设置" to null,
            "隐私设置" to null,
            "帮助与反馈" to null,
            "关于应用" to onAboutClick
        )

        settingsItems.forEach { (item, onClick) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .then(
                            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("退出登录")
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "是否要退出登录？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogoutClick()
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private suspend fun loadAvatarBitmap(url: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val inputStream: InputStream? = response.body?.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap
            }
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

private fun makeCircleBitmap(source: Bitmap): Bitmap {
    val size = minOf(source.width, source.height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint()
    val rect = Rect(0, 0, size, size)

    paint.isAntiAlias = true
    paint.isFilterBitmap = true
    paint.isDither = true

    canvas.drawARGB(0, 0, 0, 0)
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(
        (size / 2).toFloat(),
        (size / 2).toFloat(),
        (size / 2).toFloat(),
        paint
    )
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(source, rect, rect, paint)
    return output
}
