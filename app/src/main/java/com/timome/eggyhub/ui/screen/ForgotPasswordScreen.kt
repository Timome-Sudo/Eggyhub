package com.timome.eggyhub.ui.screen

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timome.eggyhub.ui.component.CaptchaDialog
import com.timome.eggyhub.ui.component.InfoDialog
import com.timome.eggyhub.ui.component.LoadingDialog
import com.timome.eggyhub.data.ApiService

/**
 * 找回密码页面
 *
 * 参考 temp/ForgotPasswordActivity.java 实现：
 * - 用户输入注册邮箱
 * - 点击"重置密码"按钮后调用 API: POST /api/password/forgot
 * - 成功后提示"重置密码链接已发送至邮箱"，并返回登录页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    onGoToLogin: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 人机验证弹窗状态
    var showCaptchaDialog by remember { mutableStateOf(false) }

    val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 邮箱格式校验：xxx@xxx.xxx（xxx部分不做具体内容校验，只要有字符即可
     */
    fun isValidEmailFormat(email: String): Boolean {
        val atIndex = email.indexOf("@")
        if (atIndex <= 0) return false
        val afterAt = email.substring(atIndex + 1)
        val dotIndex = afterAt.indexOf(".")
        if (dotIndex <= 0) return false
        val afterDot = afterAt.substring(dotIndex + 1)
        return afterDot.isNotEmpty()
    }

    /**
     * 点击确认按钮：先做邮箱格式校验，通过后弹出人机验证
     */
    fun doSubmit() {
        // 校验邮箱
        if (email.isBlank()) {
            emailError = "请输入邮箱"
            return
        }
        if (!isValidEmailFormat(email)) {
            emailError = "邮箱格式不正确，格式应为 xxx@xxx.xxx"
            return
        }
        emailError = null

        // 基础校验通过 -> 弹出人机验证
        showCaptchaDialog = true
    }

    /**
     * 人机验证通过后，真正向服务器发送找回密码请求
     */
    fun sendForgotPasswordRequest() {
        // 调用 API
        isSubmitting = true
        ApiService.forgotPassword(
            email = email.trim(),
            onSuccess = {
                mainHandler.post {
                    isSubmitting = false
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "找回密码",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "请输入您注册时使用的邮箱",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "我们会将重置密码链接发送到该邮箱",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) {
                                emailError = null
                            }
                        },
                        label = { Text("注册邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailError != null,
                        supportingText = {
                            emailError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { doSubmit() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "确认",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    )

    // 加载弹窗
    LoadingDialog(
        show = isSubmitting,
        message = "正在发送找回密码邮件...",
        autoDismiss = false,
        showCloseButton = false,
        onDismiss = {}
    )

    // 成功提示 - 点击后返回登录页
    InfoDialog(
        show = showSuccessDialog,
        title = "发送成功！",
        message = "如未找到请检查垃圾邮件或黑名单",
        confirmButtonText = "确定",
        onConfirm = {
            showSuccessDialog = false
            onGoToLogin()
        },
        onDismiss = {
            showSuccessDialog = false
            onGoToLogin()
        }
    )

    // 错误提示
    InfoDialog(
        show = showErrorDialog,
        title = "发送失败",
        message = errorMessage,
        confirmButtonText = "确定",
        onConfirm = { showErrorDialog = false },
        onDismiss = { showErrorDialog = false }
    )

    // 人机验证弹窗（验证通过后才真正发送找回密码请求）
    CaptchaDialog(
        show = showCaptchaDialog,
        onVerified = {
            showCaptchaDialog = false
            sendForgotPasswordRequest()
        },
        onCancel = {
            showCaptchaDialog = false
        }
    )
}
