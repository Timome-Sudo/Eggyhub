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
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val mainHandler = Handler(Looper.getMainLooper())

    val totalSteps = 2
    var currentStep by remember { mutableStateOf(1) }

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var uploadedBytes by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf(0L) }
    var uploadSpeed by remember { mutableStateOf(0.0) }
    var uploadStatus by remember { mutableStateOf("等待上传") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                fileUri = data.data
                fileName = getFileName(context, data.data!!)
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
        if (validateCurrentStep()) {
            if (currentStep < totalSteps) {
                currentStep++
            } else {
                uploadFile()
            }
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
}