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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timome.eggyhub.ui.component.InfoDialog
import com.timome.eggyhub.ui.component.LoadingDialog
import com.timome.eggyhub.ui.component.StepProgressBar
import com.timome.eggyhub.data.ApiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishShareCodeScreen(
    accessToken: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val mainHandler = Handler(Looper.getMainLooper())

    val totalSteps = 5
    var currentStep by remember { mutableStateOf(1) }

    var name by remember { mutableStateOf("") }
    var firstCode by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eggCodeQuantity by remember { mutableStateOf("1") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var firstCodeError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun getFileName(context: android.content.Context, uri: Uri): String {
        var fileName: String? = null
        val scheme = uri.scheme

        if (scheme == null || scheme == "content") {
            val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                fileName = cursor.getString(columnIndex)
            }
            cursor?.close()
        } else if (scheme == "file") {
            fileName = uri.lastPathSegment
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = "cover_${System.currentTimeMillis()}.jpg"
        }

        return fileName
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                imageUri = data.data
                fileName = getFileName(context, data.data!!)
                Toast.makeText(context, "图片已选择", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun validateFirstCode(firstCode: String): String {
        if (firstCode.isBlank()) {
            return "第一个分享码不能为空"
        }
        if (firstCode.length != 13) {
            return "分享码长度必须为13位"
        }
        val prefix = firstCode.substring(0, 2)
        if (!prefix.equals("2y", ignoreCase = true)) {
            return "分享码必须以2y开头"
        }
        val suffix = firstCode.substring(2)
        for (c in suffix) {
            if (!Character.isLetterOrDigit(c)) {
                return "分享码只能包含字母和数字"
            }
        }
        return "1"
    }

    fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                if (name.isBlank()) {
                    nameError = "请输入分享码名称"
                    false
                } else {
                    nameError = null
                    true
                }
            }
            2 -> {
                if (firstCode.isBlank()) {
                    firstCodeError = "请输入分享码"
                    false
                } else {
                    val codeResult = validateFirstCode(firstCode)
                    if (codeResult != "1") {
                        firstCodeError = codeResult
                        false
                    } else {
                        firstCodeError = null
                        true
                    }
                }
            }
            3 -> {
                if (description.isBlank()) {
                    descriptionError = "请输入描述"
                    false
                } else {
                    descriptionError = null
                    true
                }
            }
            4 -> {
                val quantity = eggCodeQuantity.toIntOrNull()
                if (eggCodeQuantity.isBlank()) {
                    quantityError = "请输入蛋码碎片数量"
                    false
                } else if (quantity == null || quantity < 1) {
                    quantityError = "蛋码碎片数量必须大于0"
                    false
                } else {
                    quantityError = null
                    true
                }
            }
            5 -> {
                if (imageUri == null) {
                    imageError = "请选择封面图片"
                    false
                } else {
                    imageError = null
                    true
                }
            }
            else -> false
        }
    }

    fun uploadShareCode() {
        if (!validateCurrentStep()) return

        isSubmitting = true

        ApiService.publishShareCode(
            accessToken = accessToken,
            name = name.trim(),
            firstCode = firstCode.trim(),
            description = description.trim(),
            eggCodeQuantity = eggCodeQuantity.toInt(),
            imageUri = imageUri!!,
            fileName = fileName,
            context = context,
            onSuccess = {
                mainHandler.post {
                    isSubmitting = false
                    Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                    showSuccessDialog = true
                }
            },
            onFailure = { msg ->
                mainHandler.post {
                    isSubmitting = false
                    errorMessage = msg
                    showErrorDialog = true
                }
            }
        )
    }

    fun onNextClick() {
        if (validateCurrentStep()) {
            if (currentStep < totalSteps) {
                currentStep++
            } else {
                uploadShareCode()
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
                        text = "发布分享码",
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
                        stepLabels = listOf("名称", "分享码", "描述", "数量", "封面"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val stepHint = when (currentStep) {
                        1 -> "请输入分享码名称"
                        2 -> "请输入分享码"
                        3 -> "请输入描述"
                        4 -> "请输入蛋码碎片数量"
                        5 -> "请选择封面图片"
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
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    if (nameError != null) nameError = null
                                },
                                label = { Text("分享码名称") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                isError = nameError != null,
                                supportingText = {
                                    nameError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                        2 -> {
                            OutlinedTextField(
                                value = firstCode,
                                onValueChange = {
                                    if (it.length <= 13) {
                                        firstCode = it
                                    }
                                    if (firstCodeError != null) firstCodeError = null
                                },
                                label = { Text("第一个分享码") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                isError = firstCodeError != null,
                                supportingText = {
                                    firstCodeError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                        3 -> {
                            OutlinedTextField(
                                value = description,
                                onValueChange = {
                                    description = it
                                    if (descriptionError != null) descriptionError = null
                                },
                                label = { Text("描述") },
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                isError = descriptionError != null,
                                supportingText = {
                                    descriptionError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                        4 -> {
                            OutlinedTextField(
                                value = eggCodeQuantity,
                                onValueChange = {
                                    eggCodeQuantity = it
                                    if (quantityError != null) quantityError = null
                                },
                                label = { Text("蛋码碎片数量") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = quantityError != null,
                                supportingText = {
                                    quantityError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                        5 -> {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                    imagePickerLauncher.launch(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "选择图片",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = if (fileName.isNotEmpty()) fileName else "选择封面图片",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            imageError?.let {
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
                                    text = if (currentStep < totalSteps) "下一步" else "发布",
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

    LoadingDialog(
        show = isSubmitting,
        message = "正在上传中...",
        autoDismiss = false,
        showCloseButton = false,
        onDismiss = {}
    )

    InfoDialog(
        show = showSuccessDialog,
        title = "上传成功",
        message = "分享码发布成功",
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