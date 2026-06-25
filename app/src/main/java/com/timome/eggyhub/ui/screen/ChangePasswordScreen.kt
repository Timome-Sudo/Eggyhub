package com.timome.eggyhub.ui.screen

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.timome.eggyhub.data.ApiService
import com.timome.eggyhub.ui.component.InfoDialog
import com.timome.eggyhub.ui.component.LoadingDialog
import com.timome.eggyhub.ui.component.StepProgressBar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
    accessToken: String = "",
    oldPassword: String = "",
    isGuestMode: Boolean = false
) {
    val totalSteps = 3
    var currentStep by remember { mutableStateOf(1) }

    var inputOldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }

    var oldPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val mainHandler = Handler(Looper.getMainLooper())

    fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> {
                if (isGuestMode) {
                    oldPasswordError = null
                    true
                } else if (inputOldPassword.isBlank()) {
                    oldPasswordError = "请输入旧密码"
                    false
                } else if (inputOldPassword != oldPassword) {
                    oldPasswordError = "旧密码不正确"
                    false
                } else {
                    oldPasswordError = null
                    true
                }
            }
            2 -> {
                if (newPassword.isBlank()) {
                    newPasswordError = "请输入新密码"
                    false
                } else if (newPassword.length < 6) {
                    newPasswordError = "密码至少为6位"
                    false
                } else if (!isGuestMode && newPassword == oldPassword) {
                    newPasswordError = "新密码不能与旧密码相同"
                    false
                } else {
                    newPasswordError = null
                    true
                }
            }
            3 -> {
                if (confirmPassword.isBlank()) {
                    confirmPasswordError = "请再次输入新密码"
                    false
                } else if (confirmPassword != newPassword) {
                    confirmPasswordError = "两次输入的密码不一致"
                    false
                } else {
                    confirmPasswordError = null
                    true
                }
            }
            else -> false
        }
    }

    fun doChangePassword() {
        isSubmitting = true

        ApiService.changePassword(
            accessToken = accessToken,
            oldPassword = oldPassword,
            newPassword = newPassword,
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

    fun onNextClick() {
        if (validateCurrentStep()) {
            if (currentStep < totalSteps) {
                currentStep++
            } else {
                doChangePassword()
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
                        text = "更改密码",
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
                        stepLabels = listOf("旧密码", "新密码", "确认密码"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val stepHint = when (currentStep) {
                        1 -> "请输入您的当前密码"
                        2 -> "请设置新密码（至少6位）"
                        3 -> "请再次输入新密码以确认"
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

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(animationSpec = tween(300))
                        },
                        label = "stepTransition"
                    ) { step ->
                        when (step) {
                            1 -> {
                                OutlinedTextField(
                                    value = inputOldPassword,
                                    onValueChange = {
                                        inputOldPassword = it
                                        if (oldPasswordError != null) oldPasswordError = null
                                    },
                                    label = { Text("旧密码") },
                                    placeholder = { Text(if (isGuestMode) "访客模式：任意输入" else "") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    isError = oldPasswordError != null,
                                    supportingText = {
                                        oldPasswordError?.let {
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
                            2 -> {
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = {
                                        newPassword = it
                                        if (newPasswordError != null) newPasswordError = null
                                    },
                                    label = { Text("新密码") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    isError = newPasswordError != null,
                                    supportingText = {
                                        newPasswordError?.let {
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
                            3 -> {
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        if (confirmPasswordError != null) confirmPasswordError = null
                                    },
                                    label = { Text("确认新密码") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    isError = confirmPasswordError != null,
                                    supportingText = {
                                        confirmPasswordError?.let {
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
                                    text = if (currentStep < totalSteps) "下一步" else "确认",
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
        message = "正在修改密码...",
        autoDismiss = false,
        showCloseButton = false,
        onDismiss = {}
    )

    InfoDialog(
        show = showSuccessDialog,
        title = "修改成功",
        message = "密码已成功修改",
        confirmButtonText = "确定",
        onConfirm = {
            showSuccessDialog = false
            onSuccess()
        },
        onDismiss = {
            showSuccessDialog = false
            onSuccess()
        }
    )

    InfoDialog(
        show = showErrorDialog,
        title = "修改失败",
        message = errorMessage,
        confirmButtonText = "知道了",
        onConfirm = { showErrorDialog = false },
        onDismiss = { showErrorDialog = false }
    )
}