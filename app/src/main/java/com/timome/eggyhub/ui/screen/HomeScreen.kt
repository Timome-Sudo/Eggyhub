package com.timome.eggyhub.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.timome.eggyhub.ui.component.BottomNavBar
import com.timome.eggyhub.ui.component.BottomNavItem
import com.timome.eggyhub.ui.component.CircleRevealOverlay
import com.timome.eggyhub.ui.component.DataCollectionConfig
import com.timome.eggyhub.ui.component.ChangeInfoDialog
import com.timome.eggyhub.ui.component.ChangeUsernameDialog
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
            BottomNavItem.Publish.route,
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
    // 更改详细信息弹窗显示状态
    var showChangeInfo by remember { mutableStateOf(false) }
    // 更改用户名弹窗显示状态
    var showChangeUsername by remember { mutableStateOf(false) }

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
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // 手势滑动累积距离（用于判断是否触发页面切换）
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

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
            onChangeInfo = { showChangeInfo = true },
            onChangeUsername = { showChangeUsername = true }
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
                    containerHeight = layoutCoordinates.size.height.toFloat()
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
                    BottomNavItem.Publish.route -> {
                        PublishPageContent()
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
                            onAccountSettingsClick = { showAccountSettings = true }
                        )
                    }
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
            containerHeight = containerHeight,
            durationMillis = 550,
            onFinished = {
                clickedIconIndex = null
                revealVisible = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        )
    }

    // 更改详细信息弹窗
    ChangeInfoDialog(
        show = showChangeInfo,
        eggyid = eggyid,
        description = description,
        contact = contact,
        onDismiss = { showChangeInfo = false },
        onSuccess = {
            showChangeInfo = false
            Toast.makeText(context, "资料更新成功", Toast.LENGTH_SHORT).show()
        },
        isGuestMode = accessToken.isEmpty(),
        accessToken = accessToken
    )

    // 更改用户名弹窗
    ChangeUsernameDialog(
        show = showChangeUsername,
        currentUsername = username,
        onDismiss = { showChangeUsername = false },
        onSuccess = {
            showChangeUsername = false
            Toast.makeText(context, "用户名修改成功", Toast.LENGTH_SHORT).show()
        },
        isGuestMode = accessToken.isEmpty(),
        accessToken = accessToken
    )
}
