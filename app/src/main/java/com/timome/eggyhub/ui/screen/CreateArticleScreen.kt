package com.timome.eggyhub.ui.screen

import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Help
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
fun CreateArticleScreen(
    accessToken: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val mainHandler = Handler(Looper.getMainLooper())

    val totalSteps = 3
    var currentStep by remember { mutableStateOf(1) }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedGroup by remember { mutableStateOf<Int?>(null) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var contentError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var showHelpDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "默认分类" to 1,
        "蛋码基础" to 2,
        "蛋码技能" to 3,
        "蛋码与编程" to 4,
        "网站" to 5
    )

    fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                if (title.isBlank()) {
                    titleError = "请输入文章标题"
                    false
                } else {
                    titleError = null
                    true
                }
            }
            2 -> {
                if (selectedCategory == null) {
                    categoryError = "请选择文章分类"
                    false
                } else {
                    categoryError = null
                    true
                }
            }
            3 -> {
                if (content.isBlank()) {
                    contentError = "请输入文章内容"
                    false
                } else {
                    contentError = null
                    true
                }
            }
            else -> false
        }
    }

    fun publishArticle() {
        if (!validateCurrentStep()) return

        isSubmitting = true

        ApiService.publishArticle(
            accessToken = accessToken,
            title = title.trim(),
            content = content.trim(),
            group = selectedGroup!!,
            onSuccess = { msg ->
                mainHandler.post {
                    isSubmitting = false
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
                publishArticle()
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
                        text = "发布文章",
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
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "书写说明"
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
                        stepLabels = listOf("标题", "分类", "内容"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val stepHint = when (currentStep) {
                        1 -> "请输入文章标题"
                        2 -> "请选择文章分类"
                        3 -> "请输入文章内容"
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
                                value = title,
                                onValueChange = {
                                    title = it
                                    if (titleError != null) titleError = null
                                },
                                label = { Text("文章标题") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                isError = titleError != null,
                                supportingText = {
                                    titleError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                        2 -> {
                            Button(
                                onClick = { showCategoryDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = selectedCategory ?: "请选择分类",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            categoryError?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        3 -> {
                            OutlinedTextField(
                                value = content,
                                onValueChange = {
                                    content = it
                                    if (contentError != null) contentError = null
                                },
                                label = { Text("文章内容") },
                                maxLines = 10,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                isError = contentError != null,
                                supportingText = {
                                    contentError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
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
        message = "正在发布中...",
        autoDismiss = false,
        showCloseButton = false,
        onDismiss = {}
    )

    InfoDialog(
        show = showSuccessDialog,
        title = "发布成功",
        message = "文章发布成功",
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
        title = "发布失败",
        message = errorMessage,
        confirmButtonText = "知道了",
        onConfirm = { showErrorDialog = false },
        onDismiss = { showErrorDialog = false }
    )

    InfoDialog(
        show = showHelpDialog,
        title = "书写教程",
        message = "在这里可以书写教程文章，分享你的经验！文章发布后，再次发布会发出新的一篇文章（标题差的不多，管理员会帮你删以前版本）。\n\n文章格式支持HTML，我会贴心地帮你去掉html里的某些标签哦。\n\n基本HTML结构：\n<h1>标题</h1>\n<p>段落内容</p>\n<p style=\"color:red;\">带样式的段落</p>",
        confirmButtonText = "知道了",
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false }
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { showCategoryDialog = false }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "选择分类",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                categories.forEach { (name, group) ->
                    Button(
                        onClick = {
                            selectedCategory = name
                            selectedGroup = group
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGroup == group) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            contentColor = if (selectedGroup == group) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = name)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedCategory != null) {
                            categoryError = null
                            showCategoryDialog = false
                        } else {
                            Toast.makeText(context, "请选择一个分类", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedCategory != null
                ) {
                    Text(text = "确定", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}