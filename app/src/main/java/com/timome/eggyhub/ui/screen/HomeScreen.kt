package com.timome.eggyhub.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.timome.eggyhub.ui.component.BottomNavBar
import com.timome.eggyhub.ui.component.BottomNavItem
import com.timome.eggyhub.ui.component.CircleRevealOverlay
import com.timome.eggyhub.ui.component.DataCollectionConfig
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String,
    userId: String,
    email: String,
    description: String,
    avatarUrl: String,
    role: String,
    sponser: String,
    eggyid: String,
    contact: String,
    accessToken: String,
    password: String,
    isGuestMode: Boolean,
    onLogout: suspend () -> Unit,
    onExportRequested: (DataCollectionConfig, (Boolean) -> Unit) -> Unit = { _, _ -> }
) {
    val itemOrder = remember {
        listOf(
            BottomNavItem.Home.route,
            BottomNavItem.Task.route,
            BottomNavItem.Profile.route
        )
    }

    // 当前选中的页面索引（0-3），与 BottomNavBar 联动
    var selectedIndex by remember { mutableIntStateOf(0) }
    // 用于动画方向判断：记录上一个索引
    var previousIndexForAnimation by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // ========== 开发者模式页面显示状态 ==========
    var showDevMode by remember { mutableStateOf(false) }
    // 开发者选项开关状态（全局）
    var devOptionsEnabled by remember { mutableStateOf(false) }
    // 关于应用页面显示状态
    var showAbout by remember { mutableStateOf(false) }
    // 账户设置页面显示状态
    var showAccountSettings by remember { mutableStateOf(false) }
    // 更改密码页面显示状态
    var showChangePassword by remember { mutableStateOf(false) }

    // 发布界面显示状态
    var showCreateArticle by remember { mutableStateOf(false) }
    var showPublishVideo by remember { mutableStateOf(false) }
    var showPublishShareCode by remember { mutableStateOf(false) }
    var showPublishFile by remember { mutableStateOf(false) }

    // 内容管理相关
    var showContentManage by remember { mutableStateOf(false) }
    var showShareCodeDetail by remember { mutableStateOf(false) }
    var currentShareCodeId by remember { mutableIntStateOf(0) }
    var showFilePreview by remember { mutableStateOf(false) }
    var previewUrl by remember { mutableStateOf("") }
    var previewFileName by remember { mutableStateOf("") }

    val context = LocalContext.current

    // ========== 圆形扩散动画状态 ==========
    var revealVisible by remember { mutableStateOf(false) }
    var revealCenterX by remember { mutableFloatStateOf(0f) }
    var revealCenterY by remember { mutableFloatStateOf(0f) }
    var revealInitialSize by remember { mutableFloatStateOf(48f) }
    var revealColor by remember { mutableStateOf(Color(0xFFF5F5F5)) }
    var clickedIconIndex by remember { mutableStateOf<Int?>(null) }

    // 容器绝对位置（用于将图标相对容器的坐标转换为相对 CircleRevealOverlay 的坐标）
    var containerAbsLeft by remember { mutableFloatStateOf(0f) }
    var containerAbsTop by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }

    // 手势滑动累积距离（用于判断是否触发页面切换）
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    // FAB 展开状态
    var isFabExpanded by remember { mutableStateOf(false) }
    // FAB 旋转角度动画
    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fabRotation"
    )
    // FAB 是否显示在屏幕内（用于控制平移动画，支持先收回再移动）
    var fabShown by remember { mutableStateOf(true) }
    // FAB 水平平移动画（向右移动到屏幕外）
    val fabTranslateX by animateFloatAsState(
        targetValue = if (fabShown) 0f else 300f,
        animationSpec = tween(durationMillis = 300),
        label = "fabTranslateX"
    )
    // 监听页面切换，实现先收回再移动的序列动画
    LaunchedEffect(selectedIndex) {
        if (selectedIndex == 0) {
            // 切回主页：先移入，然后不自动展开
            fabShown = true
        } else {
            // 切换到非主页
            if (isFabExpanded) {
                // 如果FAB展开，先收回小按钮，等待动画完成后再移出
                isFabExpanded = false
                kotlinx.coroutines.delay(300)
            }
            fabShown = false
        }
    }

    // 关于应用页面
    if (showAbout) {
        AboutScreen(
            onBack = { showAbout = false },
            devOptionsEnabled = devOptionsEnabled,
            onEnterDevMode = {
                showAbout = false
                showDevMode = true
            }
        )
        return
    }

    // 开发者模式页面
    if (showDevMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "开发者模式",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showDevMode = false }) {
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
                    DevModeScreen(
                        initialEnabled = devOptionsEnabled,
                        onEnabledChange = { devOptionsEnabled = it },
                        onExportRequested = onExportRequested
                    )
                }
            }
        )
        return
    }

    // 更改密码页面
    if (showChangePassword) {
        ChangePasswordScreen(
            onBack = { showChangePassword = false },
            onSuccess = {
                showChangePassword = false
                Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show()
            },
            accessToken = accessToken,
            oldPassword = password,
            isGuestMode = isGuestMode
        )
        return
    }

    // 账户设置页面
    if (showAccountSettings) {
        AccountSettingsScreen(
            onBack = { showAccountSettings = false },
            onChangePassword = { showChangePassword = true },
            onDeleteAccount = {
                Toast.makeText(context, "注销账户功能开发中", Toast.LENGTH_SHORT).show()
            },
            eggyid = eggyid,
            description = description,
            contact = contact,
            username = username,
            isGuestMode = accessToken.isEmpty(),
            accessToken = accessToken
        )
        return
    }

    // 发布文章页面
    if (showCreateArticle) {
        CreateArticleScreen(
            accessToken = accessToken,
            onBack = { showCreateArticle = false }
        )
        return
    }

    // 发布视频页面
    if (showPublishVideo) {
        PublishVideoScreen(
            accessToken = accessToken,
            onBack = { showPublishVideo = false }
        )
        return
    }

    // 发布分享码页面
    if (showPublishShareCode) {
        PublishShareCodeScreen(
            accessToken = accessToken,
            onBack = { showPublishShareCode = false }
        )
        return
    }

    // 发布文件页面
    if (showPublishFile) {
        PublishFileScreen(
            accessToken = accessToken,
            onBack = { showPublishFile = false }
        )
        return
    }

    // 内容管理页面
    if (showContentManage) {
        ContentManageScreen(
            accessToken = accessToken,
            onBack = { showContentManage = false },
            onShareCodeClick = { id ->
                currentShareCodeId = id
                showShareCodeDetail = true
            }
        )
        return
    }

    // 分享码详情设置页面
    if (showShareCodeDetail) {
        ShareCodeDetailScreen(
            accessToken = accessToken,
            giftId = currentShareCodeId,
            onBack = { showShareCodeDetail = false }
        )
        return
    }

    // 文件预览页面
    if (showFilePreview) {
        FilePreviewScreen(
            url = previewUrl,
            fileName = previewFileName,
            onBack = { showFilePreview = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 页面内容区域：使用 AnimatedContent 实现类似 tab 的切换动画
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    // 记录容器的绝对位置和尺寸，用于 CircleRevealOverlay 的坐标计算
                    val positionInRoot = layoutCoordinates.localToRoot(
                        androidx.compose.ui.geometry.Offset.Zero
                    )
                    containerAbsLeft = positionInRoot.x
                    containerAbsTop = positionInRoot.y
                    containerWidth = layoutCoordinates.size.width.toFloat()
                }
                // 手势检测：左右滑动切换页面
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount
                        },
                        onDragEnd = {
                            val threshold = 80.dp.toPx()
                            if (abs(dragAccumulator) > threshold) {
                                if (dragAccumulator < 0f && selectedIndex < itemOrder.size - 1) {
                                    // 向左滑 → 下一页
                                    previousIndexForAnimation = selectedIndex
                                    selectedIndex += 1
                                } else if (dragAccumulator > 0f && selectedIndex > 0) {
                                    // 向右滑 → 上一页
                                    previousIndexForAnimation = selectedIndex
                                    selectedIndex -= 1
                                }
                            }
                            dragAccumulator = 0f
                        },
                        onDragCancel = {
                            dragAccumulator = 0f
                        }
                    )
                }
        ) {
            // AnimatedContent：根据方向决定左滑入/右滑入，类似 Tab 的直接切换
            AnimatedContent(
                targetState = selectedIndex,
                transitionSpec = {
                    // 方向判断：目标索引 > 初始索引 → 向左切换（新页从右滑入）
                    val forward = targetState > initialState
                    val slideIn = slideInHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetX = { width -> if (forward) width else -width }
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))

                    val slideOut = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        targetOffsetX = { width -> if (forward) -width else width }
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))

                    ContentTransform(slideIn, slideOut)
                },
                label = "pageSwitch"
            ) { targetIndex ->
                // 根据当前页索引渲染对应页面内容
                when (itemOrder[targetIndex]) {
                    BottomNavItem.Home.route -> {
                        HomePageContent(
                            username = username,
                            onIconClick = { info ->
                                clickedIconIndex = info.iconIndex
                                revealCenterX = info.centerX - containerAbsLeft
                                revealCenterY = info.centerY - containerAbsTop
                                revealColor = info.color
                                revealVisible = true
                            },
                            clickedIconIndex = clickedIconIndex
                        )
                    }
                    BottomNavItem.Task.route -> {
                        TaskPageContent()
                    }
                    BottomNavItem.Profile.route -> {
                        ProfilePageContent(
                            username = username,
                            userId = userId,
                            email = email,
                            description = description,
                            avatarUrl = avatarUrl,
                            role = role,
                            sponser = sponser,
                            eggyid = eggyid,
                            contact = contact,
                            onLogoutClick = {
                                // 退出登录并返回登录页面
                                coroutineScope.launch {
                                    onLogout()
                                }
                            },
                            onAboutClick = { showAbout = true },
                            onAccountSettingsClick = { showAccountSettings = true },
                            onContentManageClick = { showContentManage = true }
                        )
                    }
                }
            }
        }

        // FAB 按钮区域（只有在主页时显示，其他页面向右移动到屏幕外）
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
                .graphicsLayer {
                    translationX = fabTranslateX
                }
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 文章按钮（向上展开，向下收回）
                AnimatedContent(
                    targetState = isFabExpanded,
                    transitionSpec = {
                        (fadeIn() + slideInVertically(initialOffsetY = { 50 })) togetherWith
                        (fadeOut() + slideOutVertically(targetOffsetY = { 50 }))
                    },
                    label = "articleButton"
                ) { visible ->
                    if (visible) {
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                showCreateArticle = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Article, contentDescription = "文章")
                        }
                    }
                }

                // 视频按钮（向上展开，向下收回）
                AnimatedContent(
                    targetState = isFabExpanded,
                    transitionSpec = {
                        (fadeIn() + slideInVertically(initialOffsetY = { 50 })) togetherWith
                        (fadeOut() + slideOutVertically(targetOffsetY = { 50 }))
                    },
                    label = "videoButton"
                ) { visible ->
                    if (visible) {
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                showPublishVideo = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = "视频")
                        }
                    }
                }

                // 分享码按钮（向上展开，向下收回）
                AnimatedContent(
                    targetState = isFabExpanded,
                    transitionSpec = {
                        (fadeIn() + slideInVertically(initialOffsetY = { 50 })) togetherWith
                        (fadeOut() + slideOutVertically(targetOffsetY = { 50 }))
                    },
                    label = "shareCodeButton"
                ) { visible ->
                    if (visible) {
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                showPublishShareCode = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = "分享码")
                        }
                    }
                }

                // 文件按钮（向上展开，向下收回）
                AnimatedContent(
                    targetState = isFabExpanded,
                    transitionSpec = {
                        (fadeIn() + slideInVertically(initialOffsetY = { 50 })) togetherWith
                        (fadeOut() + slideOutVertically(targetOffsetY = { 50 }))
                    },
                    label = "fileButton"
                ) { visible ->
                    if (visible) {
                        FloatingActionButton(
                            onClick = {
                                isFabExpanded = false
                                showPublishFile = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.FileCopy, contentDescription = "文件")
                        }
                    }
                }

                // 主 FAB 按钮
                FloatingActionButton(
                    onClick = {
                        isFabExpanded = !isFabExpanded
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "添加",
                        modifier = Modifier.rotate(fabRotation)
                    )
                }
            }
        }

        // BottomNavBar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            BottomNavBar(
                    selectedItem = itemOrder[selectedIndex],
                    onItemSelected = { route ->
                        val targetIndex = itemOrder.indexOf(route)
                        if (targetIndex != selectedIndex) {
                            // 记录上一个索引用于判断动画方向
                            previousIndexForAnimation = selectedIndex
                            // 直接切换到目标页（类似 tab 逻辑，不经过中间页面）
                            selectedIndex = targetIndex
                        }
                    }
                )
        }

        // 圆形扩散动画覆盖层（置于最上层，zIndex 保证覆盖所有内容）
        CircleRevealOverlay(
            visible = revealVisible,
            centerX = revealCenterX,
            centerY = revealCenterY,
            initialSize = revealInitialSize,
            color = revealColor,
            containerWidth = containerWidth,
            durationMillis = 1000,
            onExpandFinished = {
                // 放大完成后根据点击的图标索引执行对应操作
                when (clickedIconIndex) {
                    6 -> {
                        // 官方网址 - 打开浏览器
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://eggyhub.top")
                        )
                        context.startActivity(intent)
                    }
                }
            },
            onFinished = {
                clickedIconIndex = null
                revealVisible = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        )
    }
}
