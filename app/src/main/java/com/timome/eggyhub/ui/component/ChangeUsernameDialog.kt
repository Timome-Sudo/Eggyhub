package com.timome.eggyhub.ui.component

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timome.eggyhub.data.ApiService

@Composable
fun ChangeUsernameDialog(
    show: Boolean,
    currentUsername: String = "",
    onDismiss: () -> Unit = {},
    onSuccess: () -> Unit = {},
    isGuestMode: Boolean = false,
    accessToken: String = ""
) {
    if (!show) return

    val context = LocalContext.current

    var newUsername by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showWaitingDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    fun validateUsername(): Boolean {
        if (newUsername.isBlank()) {
            usernameError = "请输入用户名"
            return false
        }
        if (newUsername.length < 2) {
            usernameError = "用户名至少为2位"
            return false
        }
        if (newUsername.length > 20) {
            usernameError = "用户名最多为20位"
            return false
        }
        if (!newUsername.matches(Regex("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$"))) {
            usernameError = "用户名只能包含字母、数字、下划线和中文"
            return false
        }
        usernameError = null
        return true
    }

    Dialog(
        onDismissRequest = {
            if (newUsername.isNotBlank()) {
                showCancelConfirmDialog = true
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "更改用户名",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = newUsername,
                    onValueChange = {
                        newUsername = it
                        if (usernameError != null) usernameError = null
                    },
                    label = { Text("新用户名") },
                    placeholder = { Text(currentUsername) },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (newUsername.isNotBlank()) {
                                showCancelConfirmDialog = true
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .width(100.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "取消",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (validateUsername()) {
                                showConfirmDialog = true
                            }
                        },
                        modifier = Modifier
                            .width(100.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "提交",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmDialog(
            title = "确认更改",
            message = "确定更改用户名为 \"$newUsername\" 吗？",
            onConfirm = {
                showConfirmDialog = false
                showWaitingDialog = true

                val mainHandler = Handler(Looper.getMainLooper())

                if (isGuestMode) {
                    mainHandler.postDelayed({
                        showWaitingDialog = false
                        onSuccess()
                    }, 1500)
                } else {
                    ApiService.changeUsername(
                        accessToken = accessToken,
                        newUsername = newUsername,
                        onSuccess = {
                            mainHandler.post {
                                showWaitingDialog = false
                                onSuccess()
                            }
                        },
                        onFailure = { msg ->
                            mainHandler.post {
                                showWaitingDialog = false
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            },
            onCancel = { showConfirmDialog = false }
        )
    }

    WaitingDialog(
        show = showWaitingDialog,
        message = "提交中..."
    )

    if (showCancelConfirmDialog) {
        ConfirmDialog(
            title = "确认取消",
            message = "确认取消更改吗？所填内容不会保留。",
            onConfirm = {
                showCancelConfirmDialog = false
                onDismiss()
            },
            onCancel = { showCancelConfirmDialog = false }
        )
    }
}