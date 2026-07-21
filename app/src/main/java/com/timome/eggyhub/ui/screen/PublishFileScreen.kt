package com.timome.eggyhub.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timome.eggyhub.ui.component.InfoDialog
import com.timome.eggyhub.ui.component.StepProgressBar
import com.timome.eggyhub.ui.component.UploadProgressDialog
import com.timome.eggyhub.data.ApiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishFileScreen(
    accessToken: String,
    isGuestMode: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val mainHandler = Handler(Looper.getMainLooper())

    val totalSteps = 2
    var currentStep by rememberSaveable { mutableStateOf(1) }

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var fileSize by rememberSaveable { mutableStateOf(0L) }
    var fileError by rememberSaveable { mutableStateOf<String?>(null) }
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    var uploadedBytes by rememberSaveable { mutableStateOf(0L) }
    var totalBytes by rememberSaveable { mutableStateOf(0L) }
    var uploadSpeed by rememberSaveable { mutableStateOf(0.0) }
    var uploadStatus by rememberSaveable { mutableStateOf("等待上传") }

    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var showFileTypeErrorDialog by rememberSaveable { mutableStateOf(false) }
    var showFileSizeErrorDialog by rememberSaveable { mutableStateOf(false) }
    var showGuestErrorDialog by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val allowedExtensions = listOf("jpg", "png", "jpeg", "gif", "mp3", "wav", "txt", "lua", "json", "md")
    val maxFileSize = 20 * 1024 * 1024L // 20MB

    fun isFileTypeAllowed(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in allowedExtensions
    }

    fun getFileName(context: android.content.Context, uri: Uri): String {
        var fileName: String? = null
        val scheme = uri.scheme

        if (scheme == null || scheme == "content") {
            val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                fileName = cursor.getString(columnIndex)
            }
            cursor?.close()
        } else if (scheme == "file") {
            fileName = uri.lastPathSegment
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = "file_${System.currentTimeMillis()}.dat"
        }

        return fileName
    }

    fun getFileSize(context: android.content.Context, uri: Uri): Long {
        return try {
            val scheme = uri.scheme
            if (scheme == null || scheme == "content") {
                val projection = arrayOf(MediaStore.Files.FileColumns.SIZE)
                val cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val size = cursor.getLong(columnIndex)
                    cursor.close()
                    size
                } else {
                    cursor?.close()
                    0L
                }
            } else if (scheme == "file") {
                val file = java.io.File(uri.path ?: "")
                if (file.exists()) file.length() else 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                fileUri = data.data
                fileName = getFileName(context, data.data!!)
                fileSize = getFileSize(context, data.data!!)
                fileError = null
                Toast.makeText(context, "文件已选择", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                if (fileUri == null) {
                    fileError = "请选择要上传的文件"
                    false
                } else {
                    fileError = null
                    true
                }
            }
            2 -> {
                if (fileUri == null) {
                    fileError = "请选择要上传的文件"
                    false
                } else {
                    fileError = null
                    true
                }
            }
            else -> false
        }
    }

    fun uploadFile() {
        if (!validateCurrentStep()) return

        isSubmitting = true
        uploadedBytes = 0L
        totalBytes = 0L
        uploadSpeed = 0.0
        uploadStatus = "等待上传"

        ApiService.publishFile(
            accessToken = accessToken,
            fileUri = fileUri!!,
            fileName = fileName,
            context = context,
            onSuccess = {
                mainHandler.post {
                    uploadStatus = "上传完成"
                    isSubmitting = false
                    Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                    showSuccessDialog = true
                }
            },
            onFailure = { msg ->
                mainHandler.post {
                    uploadStatus = "上传失败"
                    isSubmitting = false
                    errorMessage = msg
                    showErrorDialog = true
                }
            },
            onProgress = { uploaded, total, speed ->
                mainHandler.post {
                    uploadedBytes = uploaded
                    totalBytes = total
                    uploadSpeed = speed
                    uploadStatus = if (total > 0) {
                        val percent = (uploaded.toDouble() / total.toDouble() * 100).toInt()
                        "上传中 $percent%"
                    } else {
                        "上传中..."
                    }
                }
            }
        )
    }

    fun onNextClick() {
        if (!validateCurrentStep()) return

        if (currentStep == 1 && fileUri != null) {
            if (!isFileTypeAllowed(fileName)) {
                showFileTypeErrorDialog = true
                return
            }
            if (fileSize > maxFileSize) {
                showFileSizeErrorDialog = true
                return
            }
        }

        if (currentStep == totalSteps) {
            if (isGuestMode) {
                showGuestErrorDialog = true
                return
            }
        }

        if (currentStep < totalSteps) {
            currentStep++
        } else {
            uploadFile()
        }
    }

    fun onPrevClick() {
        if (currentStep > 1) {
            currentStep--
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "发布文件",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onPrevClick() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        content = { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    StepProgressBar(
                        currentStep = currentStep,
                        totalSteps = totalSteps,
                        stepLabels = listOf("选择文件", "确认上传"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val stepHint = when (currentStep) {
                        1 -> "请选择要上传的文件"
                        2 -> "请确认上传此文件"
                        else -> ""
                    }
                    Text(
                        text = stepHint,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "第 $currentStep / $totalSteps 步",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    when (currentStep) {
                        1 -> {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                    filePickerLauncher.launch(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "选择文件",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = if (fileName.isNotEmpty()) fileName else "选择要上传的文件",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            fileError?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "即将上传的文件：",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = fileName.ifEmpty { "未选择文件" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            fileError?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onNextClick() },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .width(100.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                Text(text = "...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            } else {
                                Text(
                                    text = if (currentStep < totalSteps) "下一步" else "上传",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    UploadProgressDialog(
        show = isSubmitting,
        uploadedBytes = uploadedBytes,
        totalBytes = totalBytes,
        speedBytesPerSec = uploadSpeed,
        status = uploadStatus,
        onDismiss = {}
    )

    InfoDialog(
        show = showSuccessDialog,
        title = "上传成功",
        message = "文件上传成功",
        confirmButtonText = "知道了",
        onConfirm = {
            showSuccessDialog = false
            onBack()
        },
        onDismiss = {
            showSuccessDialog = false
            onBack()
        }
    )

    InfoDialog(
        show = showErrorDialog,
        title = "上传失败",
        message = errorMessage,
        confirmButtonText = "知道了",
        onConfirm = { showErrorDialog = false },
        onDismiss = { showErrorDialog = false }
    )

    InfoDialog(
        show = showFileTypeErrorDialog,
        title = "文件类型不支持",
        message = "上传文件仅支持以下后缀：jpg、png、jpeg、gif、mp3、wav、txt、lua、json、md",
        confirmButtonText = "确定",
        onConfirm = {
            showFileTypeErrorDialog = false
            currentStep = 1
            fileUri = null
            fileName = ""
        },
        onDismiss = {
            showFileTypeErrorDialog = false
            currentStep = 1
            fileUri = null
            fileName = ""
        }
    )

    InfoDialog(
        show = showFileSizeErrorDialog,
        title = "文件过大",
        message = "文件大小超过20MB了！要撑死服务器啊！",
        confirmButtonText = "确定",
        onConfirm = {
            showFileSizeErrorDialog = false
            currentStep = 1
            fileUri = null
            fileName = ""
            fileSize = 0L
        },
        onDismiss = {
            showFileSizeErrorDialog = false
            currentStep = 1
            fileUri = null
            fileName = ""
            fileSize = 0L
        }
    )

    InfoDialog(
        show = showGuestErrorDialog,
        title = "无法上传",
        message = "访客模式不支持上传操作，请先登录。",
        confirmButtonText = "确定",
        onConfirm = {
            showGuestErrorDialog = false
        },
        onDismiss = {
            showGuestErrorDialog = false
        }
    )
}