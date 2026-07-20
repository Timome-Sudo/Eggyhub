package com.timome.eggyhub.ui.screen

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timome.eggyhub.data.ApiService
import com.timome.eggyhub.ui.component.NetworkImage
import com.timome.eggyhub.ui.component.WaitingDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCodeDetailScreen(
    accessToken: String,
    giftId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var detail by remember { mutableStateOf<ApiService.ShareCodeDetail?>(null) }
    var categories by remember { mutableStateOf<List<ApiService.CategoryItem>>(emptyList()) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showDescDialog by remember { mutableStateOf(false) }
    var showAddCodeDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteCodeDialog by remember { mutableStateOf(false) }
    var codeToDelete by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableIntStateOf(0) }
    var descText by remember { mutableStateOf("") }
    var addCodeText by remember { mutableStateOf("") }
    var showWaiting by remember { mutableStateOf(false) }
    var waitingText by remember { mutableStateOf("") }

    fun loadData() {
        isLoading = true
        ApiService.fetchShareCodeDetail(
            accessToken = accessToken,
            giftId = giftId,
            onSuccess = { data ->
                Handler(Looper.getMainLooper()).post {
                    detail = data
                    selectedGroupId = data.grid
                    descText = data.description
                    isLoading = false
                }
            },
            onFailure = { error ->
                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        loadData()
        ApiService.fetchGiftCategories(
            onSuccess = { list ->
                Handler(Looper.getMainLooper()).post {
                    categories = list
                }
            },
            onFailure = {
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.name ?: "分享码详情",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            LoadingCenter()
        } else if (detail != null) {
            val currentDetail = detail!!
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    NetworkImage(
                        url = "https://eggyhub.top/${currentDetail.cover}",
                        contentDescription = currentDetail.name,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "描述",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentDetail.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val categoryName = categories.find { it.id == currentDetail.grid }?.name ?: "未知分类"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "分类",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGroupDialog = true },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "修改分组",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "修改",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            descText = currentDetail.description
                            showDescDialog = true
                        },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "修改描述",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "修改",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "补充分享码",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { showAddCodeDialog = true },
                                modifier = Modifier.size(80.dp, 36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "补充",
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "库存：${currentDetail.stock}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentDetail.codes.isEmpty()) {
                            Text(
                                text = "暂无分享码",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            currentDetail.codes.forEach { code ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                codeToDelete = code
                                                showDeleteCodeDialog = true
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                text = "长按分享码可删除",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除分享码")
                }
            }
        }
    }

    if (showGroupDialog && categories.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = {
                Text(
                    text = "修改分组",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedGroupId == category.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedGroupId = category.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGroupId == category.id,
                                onClick = { selectedGroupId = category.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showGroupDialog = false
                    showWaiting = true
                    waitingText = "修改中..."
                    ApiService.updateShareCodeGroup(
                        accessToken = accessToken,
                        giftId = giftId,
                        groupId = selectedGroupId,
                        onSuccess = {
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        },
                        onFailure = { error ->
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }) {
                    Text("修改")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDescDialog) {
        var tempDesc by remember { mutableStateOf(descText) }
        AlertDialog(
            onDismissRequest = { showDescDialog = false },
            title = {
                Text(
                    text = "修改描述",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = tempDesc,
                    onValueChange = { tempDesc = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDescDialog = false
                    showWaiting = true
                    waitingText = "修改中..."
                    ApiService.updateShareCodeDescription(
                        accessToken = accessToken,
                        giftId = giftId,
                        description = tempDesc,
                        onSuccess = {
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        },
                        onFailure = { error ->
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }) {
                    Text("修改")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDescDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAddCodeDialog) {
        var tempCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCodeDialog = false },
            title = {
                Text(
                    text = "补充分享码",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text = "请输入分享码（以2y开头，可一次输入多个，以2y为分界线）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = tempCode,
                        onValueChange = { tempCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 6,
                        shape = RoundedCornerShape(8.dp),
                        placeholder = {
                            Text(
                                text = "2yxxxx\n2yyyyy\n...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempCode.isBlank()) {
                        Toast.makeText(context, "请输入分享码", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    showAddCodeDialog = false
                    showWaiting = true
                    waitingText = "上传中..."
                    ApiService.addShareCodes(
                        accessToken = accessToken,
                        giftId = giftId,
                        codes = tempCode,
                        onSuccess = {
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, "补充成功", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        },
                        onFailure = { error ->
                            Handler(Looper.getMainLooper()).post {
                                showWaiting = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCodeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "删除分享码",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "确定删除这个分享码吗，此操作不可逆！",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        showWaiting = true
                        waitingText = "删除中..."
                        ApiService.deleteShareCode(
                            accessToken = accessToken,
                            giftId = giftId,
                            onSuccess = {
                                Handler(Looper.getMainLooper()).post {
                                    showWaiting = false
                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            },
                            onFailure = { error ->
                                Handler(Looper.getMainLooper()).post {
                                    showWaiting = false
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                ) {
                    Text(
                        text = "确定",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteCodeDialog && codeToDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteCodeDialog = false },
            title = {
                Text(
                    text = "删除分享码",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "确定删除此分享码吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteCodeDialog = false
                        showWaiting = true
                        waitingText = "删除中..."
                        ApiService.deleteSingleShareCode(
                            accessToken = accessToken,
                            giftId = giftId,
                            code = codeToDelete,
                            onSuccess = {
                                Handler(Looper.getMainLooper()).post {
                                    showWaiting = false
                                    Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                    codeToDelete = ""
                                    loadData()
                                }
                            },
                            onFailure = { error ->
                                Handler(Looper.getMainLooper()).post {
                                    showWaiting = false
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                ) {
                    Text(
                        text = "确定",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCodeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showWaiting) {
        WaitingDialog(show = showWaiting, message = waitingText)
    }
}
