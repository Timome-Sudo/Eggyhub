package com.timome.eggyhub.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.timome.eggyhub.ui.component.LoadingDialog
import com.timome.eggyhub.ui.component.LoginErrorDialog
import com.timome.eggyhub.ui.component.PasswordErrorDialog
import com.timome.eggyhub.data.ApiService
import kotlinx.coroutines.launch
import okhttp3.Call

/**
 * 判断错误信息是否与密码相关
 * 关键词：密码、password、Password、PASSWORD
 */
private fun isPasswordError(message: String): Boolean {
    val lower = message.lowercase()
    return "密码" in message || "password" in lower || "pwd" in lower || "wrong" in lower
}

/**
 * 判断错误信息是否与邮箱/用户相关
 */
private fun isEmailOrUserError(message: String): Boolean {
    val lower = message.lowercase()
    return "邮箱" in message || "email" in lower || "账户" in message ||
            "用户" in message || "不存在" in message || "not found" in lower ||
            "未注册" in message || "未找到" in message
}

/**
 * 登录界面
 *
 * 参考 temp/LoginActivity.java 实现：
 * 1. 用户输入邮箱/密码
 * 2. 将 email, password, timestamp 构建成 JSON
 * 3. RSA加密后转为十六进制字符串
 * 4. POST { "auth": hexString } 到 /api/auth/login
 * 5. 成功：保存 access_token、用户信息
 * 6. 失败：
 *    - 密码错误 → 密码错误专用弹窗（含"找回密码"按钮）+ 密码输入框报错
 *    - 其他错误 → 通用登录错误弹窗（含"复制错误""联系开发者"按钮）
 */
@Composable
fun LoginScreen(
    context: Context,
    onLoginSuccess: suspend (
        accessToken: String,
        username: String,
        email: String,
        password: String,
        userId: Int,
        role: String,
        sponser: String,
        avatar: String,
        contact: String,
        description: String,
        eggyid: String
    ) -> Unit,
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // 错误弹窗状态
    var showPasswordErrorDialog by remember { mutableStateOf(false) }
    var showLoginErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 人机验证弹窗状态
    var showCaptchaDialog by remember { mutableStateOf(false) }

    // 当前登录请求引用，用于点击"取消"按钮时取消请求
    var currentLoginCall by remember { mutableStateOf<Call?>(null) }

    val coroutineScope = rememberCoroutineScope()

    /**
     * 点击登录按钮：
     * 1. 先做基础校验（邮箱/密码非空）
     * 2. 校验通过 -> 弹出人机验证弹窗
     * 3. 人机验证通过 -> 真正发送登录请求
     */
    fun doLogin() {
        val emailInput = email.trim()
        val passwordInput = password

        // 基础校验
        var valid = true
        if (emailInput.isBlank()) {
            emailError = "未填写邮箱/账户名"
            valid = false
        } else {
            emailError = null
        }
        if (passwordInput.isBlank()) {
            passwordError = "未填写密码"
            valid = false
        } else {
            passwordError = null
        }
        if (!valid) return

        // 基础校验通过 -> 弹出人机验证弹窗
        showCaptchaDialog = true
    }

    /**
     * 用户点击加载弹窗"取消"按钮时
     * 取消当前登录请求并关闭加载弹窗
     */
    fun onCancelLoginClick() {
        currentLoginCall?.cancel()
        currentLoginCall = null
        isLoading = false
    }

    /**
     * 人机验证通过后，真正向服务器发送登录请求
     * 加载弹窗中会显示"取消"按钮，用户可手动取消请求
     */
    fun sendLoginRequest() {
        val emailInput = email.trim()
        val passwordInput = password

        // 显示加载弹窗
        isLoading = true

        // 调用 API
        currentLoginCall = ApiService.login(
            email = emailInput,
            password = passwordInput,
            onSuccess = { response ->
                if (!isLoading) return@login
                Toast.makeText(
                    context,
                    "登录成功",
                    Toast.LENGTH_SHORT
                ).show()

                // 登录成功后，立即获取用户资料
                ApiService.fetchUserProfile(
                    accessToken = response.accessToken,
                    onSuccess = { profile ->
                        isLoading = false
                        currentLoginCall = null
                        coroutineScope.launch {
                            onLoginSuccess(
                                response.accessToken,
                                profile.username.ifBlank { response.user.username },
                                profile.email.ifBlank { response.user.email },
                                passwordInput,
                                if (profile.id > 0) profile.id else response.user.id,
                                profile.role.ifBlank { response.user.role },
                                profile.sponser.ifBlank { response.user.sponser },
                                profile.avatar,
                                profile.contact,
                                profile.description,
                                profile.eggyid
                            )
                        }
                    },
                    onFailure = {
                        // 获取用户资料失败，仍用登录返回的基础信息继续
                        isLoading = false
                        currentLoginCall = null
                        coroutineScope.launch {
                            onLoginSuccess(
                                response.accessToken,
                                response.user.username,
                                response.user.email,
                                passwordInput,
                                response.user.id,
                                response.user.role,
                                response.user.sponser,
                                "",
                                "",
                                "",
                                ""
                            )
                        }
                    }
                )
            },
            onFailure = { msg ->
                // 如果请求被取消（用户点击取消按钮），则不处理此回调
                if (!isLoading) return@login
                isLoading = false
                currentLoginCall = null
                errorMessage = msg

                // 根据错误类型选择不同的弹窗
                when {
                    isPasswordError(msg) -> {
                        passwordError = "密码错误，请重新输入"
                        showPasswordErrorDialog = true
                    }
                    isEmailOrUserError(msg) -> {
                        emailError = msg
                        showLoginErrorDialog = true
                    }
                    else -> {
                        showLoginErrorDialog = true
                    }
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "登录",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) {
                        emailError = null
                    }
                },
                label = { Text("邮箱/账户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) {
                        passwordError = null
                    }
                },
                label = { Text("密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordError != null,
                supportingText = {
                    passwordError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "隐藏" else "显示")
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 登录按钮
            Button(
                onClick = { doLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "登录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 注册账号按钮
            OutlinedButton(
                onClick = { onRegisterClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "注册账号",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 找回密码按钮
            OutlinedButton(
                onClick = { onForgotPasswordClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "找回密码",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // 加载弹窗（显示取消按钮，用户可手动取消登录请求）
    LoadingDialog(
        show = isLoading,
        message = "正在登录中...",
        autoDismiss = false,
        showCloseButton = true,
        closeButtonText = "取消",
        onDismiss = { onCancelLoginClick() }
    )

    // 密码错误专用弹窗
    PasswordErrorDialog(
        show = showPasswordErrorDialog,
        message = errorMessage,
        onConfirm = {
            showPasswordErrorDialog = false
        },
        onForgotPassword = {
            showPasswordErrorDialog = false
            onForgotPasswordClick()
        },
        onDismiss = {
            showPasswordErrorDialog = false
        }
    )

    // 通用登录错误弹窗
    LoginErrorDialog(
        show = showLoginErrorDialog,
        title = "登录失败",
        message = errorMessage,
        onConfirm = {
            showLoginErrorDialog = false
        },
        onDismiss = {
            showLoginErrorDialog = false
        }
    )

    // 人机验证弹窗（验证通过后发起真正的登录请求）
    CaptchaDialog(
        show = showCaptchaDialog,
        onVerified = {
            showCaptchaDialog = false
            sendLoginRequest()
        },
        onCancel = {
            showCaptchaDialog = false
        }
    )
}
