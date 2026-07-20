package com.timome.eggyhub.ui.screen

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.timome.eggyhub.data.ApiService
import com.timome.eggyhub.ui.component.WaitingDialog
import kotlin.math.roundToInt

@Composable
fun FileManageTab(
    accessToken: String,
    onPreviewClick: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var repoInfo by remember { mutableStateOf<ApiService.RepoInfo?>(null) }
    var files by remember { mutableStateOf<List<ApiService.ManageFile>>(emptyList()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showDescDialog by remember { mutableStateOf(false) }
    var showExpandDialog by remember { mutableStateOf(false) }
    var showWaiting by remember { mutableStateOf(false) }
    var waitingText by remember { mutableStateOf("") }

    fun loadData() {
        isLoading = true
        ApiService.fetchUserRepo(
            accessToken = accessToken,
            onSuccess = { response ->
                Handler(Looper.getMainLooper()).post {
                    repoInfo = response.repo
                    files = response.files
                    isLoading = false
                }
            },
            onFailure = { error ->
                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (isLoading) {
        LoadingCenter()
        return
    }

    if (repoInfo == null) {
        EmptyContent(message = "暂无仓库")
        return
    }

    val currentRepo = repoInfo!!
    val usedSizeBytes = files.sumOf { it.fileSizeKb * 1024L }
    val maxSizeBytes = currentRepo.totalSpaceKb * 1024L

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showNameDialog = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "仓库名",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MarqueeText(
                        text = currentRepo.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDescDialog = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "描述",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MarqueeText(
                        text = currentRepo.description.ifBlank { "无描述" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showExpandDialog = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "仓库大小",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MarqueeText(
                        text = formatFileSize(usedSizeBytes) + "/" + formatFileSize(maxSizeBytes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击扩容",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (files.isEmpty()) {
            EmptyContent(message = "暂无文件")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    FileManageItem(
                        file = file,
                        accessToken = accessToken,
                        onPreviewClick = { url, type -> onPreviewClick(url, type) },
                        onDownloadClick = { url, name ->
                            downloadFile(context, url, name, accessToken)
                        }
                    )
                }
            }
        }
    }

    if (showNameDialog) {
        var nameText by remember { mutableStateOf(currentRepo.name) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("修改仓库名", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    showWaiting = true
                    waitingText = "修改中..."
                    ApiService.updateRepo(
                        accessToken = accessToken,
                        repoId = currentRepo.repoId,
                        name = nameText,
                        description = currentRepo.description,
                        onSuccess = {
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        },
                        onFailure = { error ->
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }) {
                    Text("修改")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDescDialog) {
        var descText by remember { mutableStateOf(currentRepo.description) }
        AlertDialog(
            onDismissRequest = { showDescDialog = false },
            title = { Text("修改描述", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDescDialog = false
                    showWaiting = true
                    waitingText = "修改中..."
                    ApiService.updateRepo(
                        accessToken = accessToken,
                        repoId = currentRepo.repoId,
                        name = currentRepo.name,
                        description = descText,
                        onSuccess = {
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        },
                        onFailure = { error ->
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }) {
                    Text("修改")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDescDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showExpandDialog) {
        val initialCapacityMb = 50
        val capacityIncreaseMb = 50
        val currentTotalSpaceMb = currentRepo.totalSpaceKb / 1024
        val expansionCount = (currentTotalSpaceMb - initialCapacityMb) / capacityIncreaseMb
        val nextExpansionCount = expansionCount + 1
        val requiredCoins = 100 * nextExpansionCount
        val nextCapacity = currentTotalSpaceMb + capacityIncreaseMb

        AlertDialog(
            onDismissRequest = { showExpandDialog = false },
            title = { Text("扩容确认", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    text = "当前仓库容量: ${formatFileSize(maxSizeBytes)}\n" +
                            "现在正在进行第 $nextExpansionCount 次扩容，消耗 $requiredCoins 碎片\n" +
                            "扩容后的容量为 ${nextCapacity} MB\n" +
                            "确定进行扩容操作吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExpandDialog = false
                    showWaiting = true
                    waitingText = "扩容中..."
                    ApiService.expandStorage(
                        accessToken = accessToken,
                        onSuccess = {
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, "扩容成功", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        },
                        onFailure = { error ->
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpandDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showWaiting) {
        WaitingDialog(
            show = showWaiting,
            message = waitingText
        )
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes 字节"
        bytes < 1024 * 1024 -> "${(bytes / 1024.0).roundTo(1)} KB"
        else -> "${(bytes / (1024.0 * 1024.0)).roundTo(2)} MB"
    }
}

fun Double.roundTo(decimals: Int): Double {
    val factor = Math.pow(10.0, decimals.toDouble())
    return (this * factor).roundToInt() / factor
}

private fun downloadFile(
    context: Context,
    url: String,
    fileName: String,
    accessToken: String
) {
    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setTitle(fileName)
        request.setDescription("正在下载...")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.addRequestHeader("Authorization", "Bearer $accessToken")
        downloadManager.enqueue(request)
        Toast.makeText(context, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun FileManageItem(
    file: ApiService.ManageFile,
    accessToken: String,
    onPreviewClick: (String, String) -> Unit,
    onDownloadClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fileIcon = getFileIcon(file.fileType)
    val previewUrl = "https://eggyhub.top/${file.fileType}/${file.fileId}"
    val downloadUrl = "https://eggyhub.top/api/files/${file.fileId}/download"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = fileIcon,
                contentDescription = file.originalName,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.originalName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatFileSize(file.fileSizeKb * 1024L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = file.uploadTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            TextButton(
                onClick = { onPreviewClick(previewUrl, file.fileType) },
                modifier = Modifier.size(60.dp, 36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("预览", fontSize = 12.sp)
            }

            TextButton(
                onClick = { onDownloadClick(downloadUrl, file.originalName) },
                modifier = Modifier.size(60.dp, 36.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("下载", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun getFileIcon(fileType: String): ImageVector {
    return when (fileType.lowercase()) {
        "image", "images", "pic", "picture", "jpg", "png", "jpeg", "gif", "bmp", "webp" -> Icons.Default.Image
        "video", "videos", "mp4", "avi", "mkv", "mov", "flv" -> Icons.Default.Movie
        "audio", "music", "mp3", "wav", "flac", "aac" -> Icons.Default.MusicNote
        "pdf" -> Icons.Default.PictureAsPdf
        "text", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> Icons.Default.Description
        else -> Icons.Default.TextSnippet
    }
}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    gradientEdgeColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    delayMillis: Int = 2000,
    animationDurationMillis: Int = 8000
) {
    val offsetX = remember { mutableFloatStateOf(0f) }
    val textLayoutResultState = remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val parentWidthState = remember { mutableIntStateOf(0) }
    val shouldAnimate = remember(text, parentWidthState.intValue, textLayoutResultState.value) {
        textLayoutResultState.value?.let { it.size.width > parentWidthState.intValue } ?: false
    }

    LaunchedEffect(shouldAnimate) {
        if (!shouldAnimate) {
            offsetX.floatValue = 0f
            return@LaunchedEffect
        }
        while (true) {
            delay(delayMillis.toLong())
            val textWidth = textLayoutResultState.value?.size?.width?.toFloat() ?: 0f
            val distance = textWidth + 100f
            val duration = animationDurationMillis
            val startTime = System.currentTimeMillis()
            val startX = offsetX.floatValue
            while (System.currentTimeMillis() - startTime < duration) {
                val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
                offsetX.floatValue = startX - distance * progress
                delay(16)
            }
            offsetX.floatValue = parentWidthState.intValue.toFloat()
            delay(500)
        }
    }

    SubcomposeLayout(modifier = modifier.clipToBounds()) { constraints ->
        val parentWidth = constraints.maxWidth
        parentWidthState.intValue = parentWidth

        val placeable = subcompose("text") {
            Text(
                text = text,
                style = style,
                fontWeight = fontWeight,
                fontSize = fontSize,
                color = color,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { textLayoutResultState.value = it }
            )
        }[0].measure(Constraints(maxWidth = Constraints.Infinity))

        layout(parentWidth, placeable.height) {
            placeable.place(offsetX.floatValue.roundToInt(), 0)
        }
    }
}
