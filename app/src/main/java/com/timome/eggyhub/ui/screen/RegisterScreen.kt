package com.timome.eggyhub.ui.screen

import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timome.eggyhub.ui.component.CaptchaDialog
import com.timome.eggyhub.ui.component.EmailDuplicateDialog
import com.timome.eggyhub.ui.component.EmailEditDialog
import com.timome.eggyhub.ui.component.InfoDialog
import com.timome.eggyhub.ui.component.LoadingDialog
import com.timome.eggyhub.ui.component.StepProgressBar
import com.timome.eggyhub.ui.component.UsernameDuplicateDialog
import com.timome.eggyhub.ui.component.UsernameEditDialog
import com.timome.eggyhub.data.ApiService

/**
 * 注册账号页面
 *
 * 参考 temp/RegisterActivity.java 实现：
 * - 步骤1：邮箱（必须包含@符号）
 * - 步骤2：用户名
 * - 步骤3：密码
 * - 步骤4：再次确认密码
 * - 步骤5：邀请码
 *
 * 最终提交字段：username, email, password, invite
 * API: POST /api/auth/register
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {}
) {
    val totalSteps = 5
    var currentStep by remember { mutableStateOf(1) }

    // 表单输入
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }

    // 密码可见性
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // 错误状态
    var emailError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var inviteError by remember { mutableStateOf<String?>(null) }

    // 是否正在提交
    var isSubmitting by remember { mutableStateOf(false) }

    // 成功提示对话框
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // 错误提示对话框
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 用户名重复专用弹窗
    var showUsernameDuplicateDialog by remember { mutableStateOf(false) }

    // 修改用户名弹窗
    var showUsernameEditDialog by remember { mutableStateOf(false) }

    // 邮箱重复/错误专用弹窗
    var showEmailDuplicateDialog by remember { mutableStateOf(false) }

    // 修改邮箱弹窗
    var showEmailEditDialog by remember { mutableStateOf(false) }

    // 人机验证弹窗
    var showCaptchaDialog by remember { mutableStateOf(false) }

    val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 邮箱格式校验：xxx@xxx.xxx（xxx部分不做具体内容校验，只要有字符即可）
     */
    fun isValidEmailFormat(email: String): Boolean {
        val atIndex = email.indexOf("@")
        if (atIndex <= 0) return false // @必须存在且不在开头
        val afterAt = email.substring(atIndex + 1)
        val dotIndex = afterAt.indexOf(".")
        if (dotIndex <= 0) return false // @后必须有.且.不能紧跟在@之后
        val afterDot = afterAt.substring(dotIndex + 1)
        return afterDot.isNotEmpty() // .后必须有内容
    }

    /**
     * 判断是否为用户名重复错误
     */
    fun isUsernameDuplicateError(message: String): Boolean {
        val lower = message.lowercase()
        return "用户名" in message && ("重复" in message || "已" in message || "存在" in message ||
                "使用" in message || ("username" in lower &&
                ("duplicate" in lower || "exists" in lower || "taken" in lower || "already" in lower)))
    }

    /**
     * 判断是否为邮箱重复/错误
     */
    fun isEmailDuplicateError(message: String): Boolean {
        val lower = message.lowercase()
        return "邮箱" in message && ("重复" in message || "已" in message || "存在" in message ||
                "使用" in message || "注册" in message || ("email" in lower &&
                ("duplicate" in lower || "exists" in lower || "taken" in lower || "already" in lower || "registered" in lower)))
    }

    fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                if (email.isBlank()) {
                    emailError = "请输入邮箱"
                    false
                } else if (!isValidEmailFormat(email)) {
                    emailError = "邮箱格式不正确，格式应为 xxx@xxx.xxx"
                    false
                } else {
                    emailError = null
                    true
                }
            }
            2 -> {
                if (username.isBlank()) {
                    usernameError = "请输入用户名"
                    false
                } else {
                    usernameError = null
                    true
                }
            }
            3 -> {
                if (password.isBlank()) {
                    passwordError = "请输入密码"
                    false
                } else if (password.length < 6) {
                    passwordError = "密码至少为6位"
                    false
                } else {
                    passwordError = null
                    true
                }
            }
            4 -> {
                if (confirmPassword.isBlank()) {
                    confirmPasswordError = "请再次输入密码"
                    false
                } else if (confirmPassword != password) {
                    confirmPasswordError = "两次输入的密码不一致"
                    false
                } else {
                    confirmPasswordError = null
                    true
                }
            }
            5 -> {
                inviteError = null
                true
            }
            else -> false
        }
    }

    /**
     * 提交注册请求
     */
    fun doRegister() {
        isSubmitting = true

        ApiService.register(
            username = username.trim(),
            email = email.trim(),
            password = password,
            invite = invite.trim(),
            onSuccess = { msg ->
                mainHandler.post {
                    isSubmitting = false
                    successMessage = msg.ifEmpty { "注册成功" }
                    showSuccessDialog = true
                }
            },
            onFailure = { msg ->
                mainHandler.post {
                    isSubmitting = false
                    errorMessage = msg

                    // 根据错误类型选择不同弹窗
                    when {
                        isUsernameDuplicateError(msg) -> {
                            // 用户名重复：使用专用弹窗
                            showUsernameDuplicateDialog = true
                        }
                        isEmailDuplicateError(msg) -> {
                            // 邮箱重复/错误：使用专用弹窗
                            showEmailDuplicateDialog = true
                        }
                        else -> {
                            // 其他错误：使用通用报错弹窗
                            showErrorDialog = true
                        }
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
                // 最后一步：先弹人机验证，验证通过后再提交注册
                showCaptchaDialog = true
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
                        text = "注册账号",
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
                        stepLabels = listOf("邮箱", "用户名", "密码", "确认", "邀请码"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val stepHint = when (currentStep) {
                        1 -> "请输入您的邮箱地址"
                        2 -> "请设置您的用户名"
                        3 -> "请设置您的登录密码（至少6位）"
                        4 -> "请再次输入密码以确认"
                        5 -> "请输入邀请码（选填，可跳过）"
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
                                value = email,
                                onValueChange = {
                                    email = it
                                    if (emailError != null) emailError = null
                                },
                                label = { Text("邮箱") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                isError = emailError != null,
                                supportingText = {
                                    emailError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                        2 -> {
                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    if (usernameError != null) usernameError = null
                                },
                                label = { Text("用户名") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                isError = usernameError != null,
                                supportingText = {
                                    usernameError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                        3 -> {
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    if (passwordError != null) passwordError = null
                                },
                                label = { Text("密码") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = passwordError != null,
                                supportingText = {
                                    passwordError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                trailingIcon = {
                                    Button(
                                        onClick = { passwordVisible = !passwordVisible },
                                        colors = ButtonDefaults.textButtonColors()
                                    ) {
                                        Text(if (passwordVisible) "隐藏" else "显示")
                                    }
                                }
                            )
                        }
                        4 -> {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    if (confirmPasswordError != null) confirmPasswordError = null
                                },
                                label = { Text("再次确认密码") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                isError = confirmPasswordError != null,
                                supportingText = {
                                    confirmPasswordError?.let {
                                        Text(text = it, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                trailingIcon = {
                                    Button(
                                        onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                        colors = ButtonDefaults.textButtonColors()
                                    ) {
                                        Text(if (confirmPasswordVisible) "隐藏" else "显示")
                                    }
                                }
                            )
                        }
                        5 -> {
                            OutlinedTextField(
                                value = invite,
                                onValueChange = {
                                    invite = it
                                    if (inviteError != null) inviteError = null
                                },
                                label = { Text("邀请码（选填）") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                isError = inviteError != null,
                                supportingText = {
                                    inviteError?.let {
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
                                    text = if (currentStep < totalSteps) "下一步" else "完成",
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

    // 加载弹窗
    LoadingDialog(
        show = isSubmitting,
        message = "正在注册中...",
        autoDismiss = false,
        showCloseButton = false,
        onDismiss = {}
    )

    // 成功提示对话框 - 点击后跳回登录页
    InfoDialog(
        show = showSuccessDialog,
        title = "注册成功",
        message = successMessage,
        confirmButtonText = "去登录",
        onConfirm = {
            showSuccessDialog = false
            onRegisterSuccess()
        },
        onDismiss = {
            showSuccessDialog = false
            onRegisterSuccess()
        }
    )

    // 错误提示
    InfoDialog(
        show = showErrorDialog,
        title = "注册失败",
        message = errorMessage,
        confirmButtonText = "知道了",
        onConfirm = { showErrorDialog = false },
        onDismiss = { showErrorDialog = false }
    )

    // 用户名重复专用弹窗
    UsernameDuplicateDialog(
        show = showUsernameDuplicateDialog,
        message = errorMessage,
        onModify = {
            showUsernameDuplicateDialog = false
            showUsernameEditDialog = true
        },
        onDismiss = { showUsernameDuplicateDialog = false }
    )

    // 修改用户名弹窗 - 确认后直接提交完整注册请求（只替换用户名字段）
    UsernameEditDialog(
        show = showUsernameEditDialog,
        initialUsername = username,
        onConfirm = { newUsername ->
            username = newUsername
            showUsernameEditDialog = false
            doRegister()
        },
        onDismiss = { showUsernameEditDialog = false }
    )

    // 邮箱重复/错误专用弹窗
    EmailDuplicateDialog(
        show = showEmailDuplicateDialog,
        message = errorMessage,
        onModify = {
            showEmailDuplicateDialog = false
            showEmailEditDialog = true
        },
        onDismiss = { showEmailDuplicateDialog = false }
    )

    // 修改邮箱弹窗 - 确认后直接提交完整注册请求（只替换邮箱字段）
    EmailEditDialog(
        show = showEmailEditDialog,
        initialEmail = email,
        onConfirm = { newEmail ->
            email = newEmail
            showEmailEditDialog = false
            doRegister()
        },
        onDismiss = { showEmailEditDialog = false }
    )

    // 人机验证弹窗（最后一步验证通过后才真正提交注册请求）
    CaptchaDialog(
        show = showCaptchaDialog,
        onVerified = {
            showCaptchaDialog = false
            doRegister()
        },
        onCancel = {
            showCaptchaDialog = false
        }
    )
}
