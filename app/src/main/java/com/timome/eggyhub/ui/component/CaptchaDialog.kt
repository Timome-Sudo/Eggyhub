package com.timome.eggyhub.ui.component

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.timome.eggyhub.data.CaptchaManager
import kotlin.math.roundToInt
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class VerifyStatus {
    PENDING,
    VERIFIED,
    FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptchaDialog(
    show: Boolean,
    onVerified: () -> Unit,
    onCancel: () -> Unit = {},
    forcedType: CaptchaManager.CaptchaType? = null
) {
    var isVisible by remember { mutableStateOf(show) }
    var isAnimatingOut by remember { mutableStateOf(false) }

    var currentType by remember(show) {
        mutableStateOf(forcedType ?: CaptchaManager.randomCaptchaType(null))
    }

    var helpClickCount by remember(show) { mutableIntStateOf(0) }

    var verifyStatus by remember { mutableStateOf(VerifyStatus.PENDING) }

    var refreshCount by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(show) {
        if (show) {
            if (CaptchaManager.isInCooldown()) {
                val remaining = CaptchaManager.getRemainingCooldownSeconds()
                Toast.makeText(
                    context,
                    "请在 ${remaining} 秒后再进行验证",
                    Toast.LENGTH_SHORT
                ).show()
                isAnimatingOut = true
                delay(100)
                isVisible = false
                onCancel()
                return@LaunchedEffect
            }
            isVisible = true
            isAnimatingOut = false
            CaptchaManager.resetHelpClickCount()
            helpClickCount = 0
            verifyStatus = VerifyStatus.PENDING
            refreshCount = 0
        } else {
            if (isVisible) {
                isAnimatingOut = true
                delay(300)
                isVisible = false
            }
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            AnimatedVisibility(
                visible = !isAnimatingOut,
                enter = fadeIn(animationSpec = tween(300, easing = LinearEasing)) +
                        scaleIn(initialScale = 0.85f, animationSpec = tween(300, easing = LinearEasing)),
                exit = fadeOut(animationSpec = tween(300, easing = LinearEasing)) +
                        scaleOut(targetScale = 0.85f, animationSpec = tween(300, easing = LinearEasing))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "人机验证",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        when (verifyStatus) {
                            VerifyStatus.VERIFIED -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "通过",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "验证通过",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            VerifyStatus.FAILED -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "失败",
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "验证失败，请重试",
                                        color = Color(0xFFC62828),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            VerifyStatus.PENDING -> {
                                Text(
                                    text = "请完成以下验证",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        when (currentType) {
                            CaptchaManager.CaptchaType.SLIDER -> {
                                SliderCaptchaContent(
                                    refreshCount = refreshCount,
                                    status = verifyStatus,
                                    onVerified = {
                                        verifyStatus = VerifyStatus.VERIFIED
                                        coroutineScope.launch {
                                            delay(600)
                                            isAnimatingOut = true
                                            onVerified()
                                        }
                                    },
                                    onFailed = {
                                        verifyStatus = VerifyStatus.FAILED
                                    }
                                )
                            }
                            CaptchaManager.CaptchaType.BLIND_SLIDER -> {
                                BlindSliderCaptchaContent(
                                    refreshCount = refreshCount,
                                    status = verifyStatus,
                                    onVerified = {
                                        verifyStatus = VerifyStatus.VERIFIED
                                        coroutineScope.launch {
                                            delay(600)
                                            isAnimatingOut = true
                                            onVerified()
                                        }
                                    },
                                    onFailed = {
                                        verifyStatus = VerifyStatus.FAILED
                                    }
                                )
                            }
                            CaptchaManager.CaptchaType.MATH -> {
                                MathCaptchaContent(
                                    refreshCount = refreshCount,
                                    status = verifyStatus,
                                    onVerified = {
                                        verifyStatus = VerifyStatus.VERIFIED
                                        coroutineScope.launch {
                                            delay(600)
                                            isAnimatingOut = true
                                            onVerified()
                                        }
                                    },
                                    onFailed = {
                                        verifyStatus = VerifyStatus.FAILED
                                    }
                                )
                            }
                            CaptchaManager.CaptchaType.COUNT -> {
                                CountCaptchaContent(
                                    refreshCount = refreshCount,
                                    status = verifyStatus,
                                    onVerified = {
                                        verifyStatus = VerifyStatus.VERIFIED
                                        coroutineScope.launch {
                                            delay(600)
                                            isAnimatingOut = true
                                            onVerified()
                                        }
                                    },
                                    onFailed = {
                                        verifyStatus = VerifyStatus.FAILED
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (verifyStatus != VerifyStatus.VERIFIED) {
                                        refreshCount += 1
                                        verifyStatus = VerifyStatus.PENDING
                                    }
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新题目",
                                tint = if (verifyStatus != VerifyStatus.VERIFIED)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "换一题",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (verifyStatus != VerifyStatus.VERIFIED)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isAnimatingOut = true
                                    CaptchaManager.markCancelled()
                                    onCancel()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "取消",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (helpClickCount < 3) {
                                        CaptchaManager.incrementHelpClickCount()
                                        helpClickCount += 1
                                        currentType = CaptchaManager.randomCaptchaType(currentType)
                                        verifyStatus = VerifyStatus.PENDING
                                    }
                                },
                                enabled = helpClickCount < 3 && verifyStatus != VerifyStatus.VERIFIED,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (helpClickCount < 3) "我不会 (${3 - helpClickCount})" else "已用完",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderCaptchaContent(
    refreshCount: Int,
    status: VerifyStatus,
    onVerified: () -> Unit,
    onFailed: () -> Unit
) {
    var target by remember { mutableIntStateOf(CaptchaManager.generateSliderTarget()) }
    var currentValue by remember { mutableFloatStateOf(0f) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val holdRunnable = remember { Runnable { onVerified() } }
    val timeoutRunnable = remember { Runnable { onFailed() } }

    LaunchedEffect(refreshCount) {
        target = CaptchaManager.generateSliderTarget()
        currentValue = 0f
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, 60_000L)
    }

    LaunchedEffect(status) {
        if (status != VerifyStatus.PENDING) {
            mainHandler.removeCallbacks(holdRunnable)
            mainHandler.removeCallbacks(timeoutRunnable)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            mainHandler.removeCallbacks(holdRunnable)
            mainHandler.removeCallbacks(timeoutRunnable)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = target,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith
                        fadeOut(animationSpec = tween(200)) using
                        SizeTransform(clip = false)
            },
            label = "slider_target"
        ) { currentTarget ->
            Text(
                text = "请将滑块拖动到：${currentTarget}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前值：${currentValue.roundToInt()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(20.dp))

        Slider(
            value = currentValue,
            onValueChange = { value -> currentValue = value },
            onValueChangeFinished = {
                if (status != VerifyStatus.PENDING) return@Slider
                val rounded = currentValue.roundToInt()
                if (rounded == target) {
                    mainHandler.removeCallbacks(holdRunnable)
                    mainHandler.postDelayed(holdRunnable, 1000)
                } else {
                    onFailed()
                }
            },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            enabled = status == VerifyStatus.PENDING
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlindSliderCaptchaContent(
    refreshCount: Int,
    status: VerifyStatus,
    onVerified: () -> Unit,
    onFailed: () -> Unit
) {
    var target by remember { mutableIntStateOf(CaptchaManager.generateSliderTarget()) }
    var currentValue by remember { mutableFloatStateOf(0f) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val holdRunnable = remember { Runnable { onVerified() } }
    val timeoutRunnable = remember { Runnable { onFailed() } }

    LaunchedEffect(refreshCount) {
        target = CaptchaManager.generateSliderTarget()
        currentValue = 0f
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, 60_000L)
    }

    LaunchedEffect(status) {
        if (status != VerifyStatus.PENDING) {
            mainHandler.removeCallbacks(holdRunnable)
            mainHandler.removeCallbacks(timeoutRunnable)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            mainHandler.removeCallbacks(holdRunnable)
            mainHandler.removeCallbacks(timeoutRunnable)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = target,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith
                        fadeOut(animationSpec = tween(200)) using
                        SizeTransform(clip = false)
            },
            label = "blind_slider_target"
        ) { currentTarget ->
            Text(
                text = "请将滑块拖动到：${currentTarget}（允许 ±10 误差）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前值：???",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        Slider(
            value = currentValue,
            onValueChange = { value -> currentValue = value },
            onValueChangeFinished = {
                if (status != VerifyStatus.PENDING) return@Slider
                val diff = kotlin.math.abs(currentValue - target)
                if (diff <= 10f) {
                    mainHandler.removeCallbacks(holdRunnable)
                    mainHandler.postDelayed(holdRunnable, 1000)
                } else {
                    onFailed()
                }
            },
            valueRange = 0f..100f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            enabled = status == VerifyStatus.PENDING
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MathCaptchaContent(
    refreshCount: Int,
    status: VerifyStatus,
    onVerified: () -> Unit,
    onFailed: () -> Unit
) {
    val context = LocalContext.current
    var problem by remember { mutableStateOf(CaptchaManager.generateMathProblem()) }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(refreshCount) {
        input = ""
        problem = CaptchaManager.generateMathProblem()
    }

    LaunchedEffect(status) {
        if (status == VerifyStatus.PENDING) {
            input = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "请计算下列算式的结果：",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = problem,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith
                        fadeOut(animationSpec = tween(200)) using
                        SizeTransform(clip = false)
            },
            label = "math_question"
        ) { targetProblem ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .combinedClickable(
                        onLongClick = {
                            Toast.makeText(
                                context,
                                "小样，这都不会，还想复制给AI？",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onClick = {}
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = targetProblem.displayText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() || c == '-' } },
            label = { Text("结果") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.7f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = status == VerifyStatus.PENDING,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val userAnswer = input.toIntOrNull()
                if (userAnswer != null && userAnswer == problem.answer) {
                    onVerified()
                } else {
                    onFailed()
                }
            },
            enabled = status == VerifyStatus.PENDING && input.isNotEmpty(),
            modifier = Modifier
                .width(160.dp)
                .height(44.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "提交", fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountCaptchaContent(
    refreshCount: Int,
    status: VerifyStatus,
    onVerified: () -> Unit,
    onFailed: () -> Unit
) {
    val context = LocalContext.current
    var problem by remember { mutableStateOf(CaptchaManager.generateCountProblem()) }
    var input by remember { mutableStateOf("") }
    var failCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshCount) {
        input = ""
        problem = CaptchaManager.generateCountProblem()
        failCount = 0
    }

    LaunchedEffect(status) {
        if (status == VerifyStatus.PENDING) {
            input = ""
        }
    }

    LaunchedEffect(failCount) {
        if (failCount > 5) {
            problem = CaptchaManager.generateCountProblem()
            failCount = 0
            CaptchaManager.resetCountFail()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "请统计下方文本中 「${problem.targetChar}」 的出现次数",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = problem,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith
                        fadeOut(animationSpec = tween(200)) using
                        SizeTransform(clip = false)
            },
            label = "count_question"
        ) { targetProblem ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .combinedClickable(
                        onLongClick = {
                            Toast.makeText(
                                context,
                                "小样，这都不会，还想复制给AI？",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onClick = {}
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = targetProblem.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter { c -> c.isDigit() } },
            label = { Text("出现次数") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.5f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = status == VerifyStatus.PENDING,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val userCount = input.toIntOrNull()
                if (userCount != null && userCount == problem.expectedCount) {
                    onVerified()
                } else {
                    failCount += 1
                    CaptchaManager.incrementCountFail()
                    onFailed()
                }
            },
            enabled = status == VerifyStatus.PENDING && input.isNotEmpty(),
            modifier = Modifier
                .width(160.dp)
                .height(44.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = "提交", fontWeight = FontWeight.Medium)
        }
    }
}
