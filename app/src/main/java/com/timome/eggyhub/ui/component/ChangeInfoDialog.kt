package com.timome.eggyhub.ui.component

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timome.eggyhub.data.ApiService

@Composable
fun ChangeInfoDialog(
    show: Boolean,
    eggyid: String = "",
    description: String = "",
    contact: String = "",
    onDismiss: () -> Unit = {},
    onSuccess: () -> Unit = {},
    isGuestMode: Boolean = false,
    accessToken: String = ""
) {
    if (!show) return

    val context = LocalContext.current

    var eggyidChecked by remember { mutableStateOf(false) }
    var descriptionChecked by remember { mutableStateOf(false) }
    var contactChecked by remember { mutableStateOf(false) }

    var eggyidExpanded by remember { mutableStateOf(false) }
    var descriptionExpanded by remember { mutableStateOf(false) }
    var contactExpanded by remember { mutableStateOf(false) }

    var eggyidValue by remember { mutableStateOf(eggyid) }
    var descriptionValue by remember { mutableStateOf(description) }
    var contactValue by remember { mutableStateOf(contact) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showWaitingDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    fun hasContentChanged(): Boolean {
        return (eggyidChecked && eggyidValue != eggyid) ||
                (descriptionChecked && descriptionValue != description) ||
                (contactChecked && contactValue != contact)
    }

    Dialog(
        onDismissRequest = {
            if (hasContentChanged()) {
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
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "更改详细信息",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "请选择要修改的内容",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                InfoCard(
                    title = "蛋仔昵称",
                    checked = eggyidChecked,
                    expanded = eggyidExpanded,
                    value = eggyidValue,
                    onCheckedChange = { eggyidChecked = it },
                    onExpandChange = { eggyidExpanded = it },
                    onValueChange = { eggyidValue = it }
                )

                InfoCard(
                    title = "简介",
                    checked = descriptionChecked,
                    expanded = descriptionExpanded,
                    value = descriptionValue,
                    onCheckedChange = { descriptionChecked = it },
                    onExpandChange = { descriptionExpanded = it },
                    onValueChange = { descriptionValue = it }
                )

                InfoCard(
                    title = "联系方式",
                    checked = contactChecked,
                    expanded = contactExpanded,
                    value = contactValue,
                    onCheckedChange = { contactChecked = it },
                    onExpandChange = { contactExpanded = it },
                    onValueChange = { contactValue = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (hasContentChanged()) {
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
                        onClick = { showConfirmDialog = true },
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
            message = "确定更改详细信息吗？",
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
                    ApiService.updateUserProfile(
                        accessToken = accessToken,
                        eggyid = if (eggyidChecked) eggyidValue else null,
                        description = if (descriptionChecked) descriptionValue else null,
                        contact = if (contactChecked) contactValue else null,
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

@Composable
private fun InfoCard(
    title: String,
    checked: Boolean,
    expanded: Boolean,
    value: String,
    onCheckedChange: (Boolean) -> Unit,
    onExpandChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = {
                            onCheckedChange(!checked)
                            onExpandChange(!expanded)
                        })
                )

                Checkbox(
                    checked = checked,
                    onCheckedChange = {
                        onCheckedChange(it)
                        onExpandChange(it)
                    }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text(title) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}