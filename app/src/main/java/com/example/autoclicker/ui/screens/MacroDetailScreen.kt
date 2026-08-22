package com.example.autoclicker.ui.screens

import android.content.Context
import com.example.autoclicker.MacroRepository
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.example.autoclicker.R
import com.example.autoclicker.MacroConfig
import com.example.autoclicker.ThemeSettings
import com.example.autoclicker.ui.components.LiquidGlassIconButton
import com.example.autoclicker.ui.components.LiquidGlassPopup
import com.example.autoclicker.ui.components.NumberPickerInline
import com.example.autoclicker.ui.components.NumberPickerInlineContent
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.collections.immutable.toImmutableList
import org.json.JSONArray
import kotlin.math.roundToInt

@Composable
fun MacroDetailScreen(
    macro: MacroConfig,
    onBack: () -> Unit,
    onSave: (MacroConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = !ThemeSettings.isDarkTheme()
    val bg = if (isLight) Color(0xFFF5F5F7) else Color(0xFF000000)
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0xCC000000) else Color(0xCCFFFFFF)
    val cardBg = if (isLight) Color.White else Color(0xFF1C1C1E)
    val dimColor = Color.Black.copy(alpha = if (isLight) 0.45f else 0.6f)
    val accentColor = ThemeSettings.getAccentColor()

                                                val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val showDividers = ThemeSettings.showDividers

    // 编辑状态
    var editedName by remember { mutableStateOf(macro.name) }
    var editedRepeat by remember { mutableStateOf(macro.repeatCount) }
    var editedSpeed by remember { mutableFloatStateOf(macro.speed) }

    // Picker 展开（动画由 LiquidGlassPopup 内部处理）
    var showRepeatPicker by remember { mutableStateOf(false) }
    var showSpeedPicker by remember { mutableStateOf(false) }

    // 分割线在 root 坐标系中的 Y 位置
    var divider1Y by remember { mutableIntStateOf(0) }
    var divider2Y by remember { mutableIntStateOf(0) }

    // 数字标签动画（展开时渐隐下移缩小，收起时放大弹跳复位）
    val repeatLabelAnim = remember { Animatable(1f) }
    val speedLabelAnim = remember { Animatable(1f) }
    val labelOffsetPx = with(density) { 24.dp.toPx() }

    LaunchedEffect(showRepeatPicker) {
        if (showRepeatPicker) {
            repeatLabelAnim.animateTo(0f, tween(durationMillis = 250, easing = FastOutSlowInEasing))
        } else {
            repeatLabelAnim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
        }
    }

    LaunchedEffect(showSpeedPicker) {
        if (showSpeedPicker) {
            speedLabelAnim.animateTo(0f, tween(durationMillis = 250, easing = FastOutSlowInEasing))
        } else {
            speedLabelAnim.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
        }
    }

    // 标题渐变动画
    val scrollState = androidx.compose.foundation.lazy.rememberLazyListState()

    // 实测 LazyColumn 视口高度和内容高度，动态计算 Spacer
    var viewportHeightPx by remember { mutableStateOf(0) }
    var contentHeightPx by remember { mutableStateOf(0) }
    val scrollOffset by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex * 100f + scrollState.firstVisibleItemScrollOffset
        }
    }
    val titleHidden by animateFloatAsState(
        targetValue = if (scrollOffset > 50) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "titleHidden"
    )

    val childBackdrop = rememberLayerBackdrop()
    val pickerBackdrop = rememberLayerBackdrop()

    // 遮罩高度
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val titleBarHeightPx = with(density) { 68.dp.toPx() }
    val maskBottomPx = statusBarHeightPx + titleBarHeightPx
    val maskHeightDp = with(density) { maskBottomPx.toDp() }
    val maskVisualHeightDp = with(density) { (maskBottomPx * 1.3f).toDp() }

    val contentPaddingTop = maskHeightDp - 30.dp
    val viewportContentArea = with(density) { viewportHeightPx.toDp() } - contentPaddingTop
    val contentHeight = with(density) { contentHeightPx.toDp() }
    val spacerHeight = if (viewportHeightPx > 0 && contentHeightPx > 0) {
        (viewportContentArea - contentHeight + 1.dp).coerceAtLeast(1.dp)
    } else {
        1.dp
    }

    BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(bg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {
        val isLandscape = maxWidth > maxHeight
        val contentMaxWidth = if (isLandscape) 360.dp else androidx.compose.ui.unit.Dp.Unspecified
                // 内容
                LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .layerBackdrop(pickerBackdrop)
                .onGloballyPositioned { coords ->
                    viewportHeightPx = coords.size.height
                },
            contentPadding = PaddingValues(top = maskHeightDp - 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .onGloballyPositioned { coords ->
                        contentHeightPx = coords.size.height
                    }
                ) {
                // 名称
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "名称",
                        fontSize = 13.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
                    )
                                                            Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedRectangle(32.dp, RoundedCornerStyle.Continuous))
                            .background(cardBg)
                            .border(0.5.dp, if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f), RoundedRectangle(32.dp, RoundedCornerStyle.Continuous))
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        BasicTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 18.sp, color = textPrimary),
                            cursorBrush = SolidColor(accentColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 参数卡片（重复次数、执行速度、点击点数）
                val textGray = Color(0xFF999999)
                                                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedRectangle(32.dp, RoundedCornerStyle.Continuous))
                        .background(cardBg)
                        .border(0.5.dp, if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f), RoundedRectangle(32.dp, RoundedCornerStyle.Continuous))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 重复次数
                    DetailRow(label = "重复次数") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                .clickable(interactionSource = null, indication = null) { showRepeatPicker = !showRepeatPicker }
                        ) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        val p = repeatLabelAnim.value
                                        alpha = p.coerceIn(0f, 1f)
                                        scaleX = p.coerceAtLeast(0.01f)
                                        scaleY = p.coerceAtLeast(0.01f)
                                        translationY = (1f - p) * labelOffsetPx
                                    }
                                    .background(if (isLight) Color(0xFFE5E5E5) else Color(0xFF3A3A3C), RoundedCornerShape(22.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$editedRepeat",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "次",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = textGray,
                                modifier = Modifier.graphicsLayer {
                                    alpha = repeatLabelAnim.value.coerceIn(0f, 1f)
                                }
                            )
                        }
                    }

                    // 分割线 1（重复次数 / 执行速度 之间）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (showDividers) 0.5.dp else 0.dp)
                            .then(
                                if (showDividers) Modifier.background(if (isLight) Color(0xFFE5E5E5) else Color(0xFF38383A))
                                else Modifier
                            )
                            .onGloballyPositioned { coords ->
                                divider1Y = coords.positionInRoot().y.roundToInt()
                            }
                    )

                    // 执行速度
                    DetailRow(label = "执行速度", verticalPadding = 20.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                .clickable(interactionSource = null, indication = null) { showSpeedPicker = !showSpeedPicker }
                        ) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        val p = speedLabelAnim.value
                                        alpha = p.coerceIn(0f, 1f)
                                        scaleX = p.coerceAtLeast(0.01f)
                                        scaleY = p.coerceAtLeast(0.01f)
                                        translationY = (1f - p) * labelOffsetPx
                                    }
                                    .background(if (isLight) Color(0xFFE5E5E5) else Color(0xFF3A3A3C), RoundedCornerShape(22.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${editedSpeed}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "倍",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = textGray,
                                modifier = Modifier.graphicsLayer {
                                    alpha = speedLabelAnim.value.coerceIn(0f, 1f)
                                }
                            )
                        }
                    }

                    // 分割线 2（执行速度 / 点击点数 之间）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (showDividers) 0.5.dp else 0.dp)
                            .then(
                                if (showDividers) Modifier.background(if (isLight) Color(0xFFE5E5E5) else Color(0xFF38383A))
                                else Modifier
                            )
                            .onGloballyPositioned { coords ->
                                divider2Y = coords.positionInRoot().y.roundToInt()
                            }
                    )

                    // 点击点数
                    DetailRow(label = "点击点数") {
                        Text(
                            text = "${macro.positions.size} 个点",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = textGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                }
                // 动态补位 Spacer：实测视口高 - 内容高 + 1dp，填满屏幕且滚动范围 > 0 触发橡皮筋
                Spacer(modifier = Modifier.height(spacerHeight))
            }
        }

                // ===== Picker 覆盖层 =====
        // 重复次数 picker — 使用 LiquidGlassPopup
        LiquidGlassPopup(
            visible = showRepeatPicker,
            onDismiss = { showRepeatPicker = false },
            backdrop = pickerBackdrop,
            fillMaxWidth = true,
            absoluteOffsetPx = divider1Y,
            pivotX = 0.86f,
            pivotY = -0.15f
        ) {
            NumberPickerInlineContent(
                value = editedRepeat.toFloat(),
                onValueChange = { editedRepeat = it.toInt() },
                onCancel = { showRepeatPicker = false },
                onConfirm = { showRepeatPicker = false },
                range = (1..1000).map { it.toFloat() },
                label = "重复次数",
                isLight = isLight,
                backdrop = pickerBackdrop
            )
        }

        // 执行速度 picker — 使用 LiquidGlassPopup
        LiquidGlassPopup(
            visible = showSpeedPicker,
            onDismiss = { showSpeedPicker = false },
            backdrop = pickerBackdrop,
            fillMaxWidth = true,
            absoluteOffsetPx = divider2Y,
            pivotX = 0.86f,
            pivotY = -0.15f
            ) {
                NumberPickerInlineContent(
                    value = editedSpeed,
                    onValueChange = { editedSpeed = it },
                    onCancel = { showSpeedPicker = false },
                    onConfirm = { showSpeedPicker = false },
                    range = generateSequence(0.25f) { it + 0.25f }.takeWhile { it <= 10.0f }.toList(),
                    label = "执行速度",
                    isLight = isLight,
                    backdrop = pickerBackdrop
                )
        }

        // ===== Gradient overlay =====
        val maskVisualHeightPx = with(density) { maskVisualHeightDp.toPx() }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maskVisualHeightDp)
                .zIndex(50f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(bg, bg, bg.copy(alpha = 0.80f), bg.copy(alpha = 0f)),
                        startY = 0f,
                        endY = maskVisualHeightPx
                    )
                )
        )

        // 顶部标题栏
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(60f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                                LiquidGlassIconButton(
                    onClick = {
                        if (ThemeSettings.vibrateEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onBack()
                    },
                                        backdrop = childBackdrop,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                                        Image(
                        painter = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = "返回",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(if (isLight) Color.Black else Color.White)
                    )
                }

                Text(
                    text = "宏详情",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )

                LiquidGlassIconButton(
                    onClick = {
                        if (ThemeSettings.vibrateEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val updated = macro.copy(
                            name = editedName,
                            repeatCount = editedRepeat,
                            speed = editedSpeed
                        )
                        onSave(updated)
                    },
                                        backdrop = childBackdrop,
                    tint = accentColor,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_checkmark),
                        contentDescription = "保存",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                                                                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    verticalPadding: androidx.compose.ui.unit.Dp = 17.dp,
    content: @Composable () -> Unit
) {
    val isLight = !ThemeSettings.isDarkTheme()
    val labelColor = if (isLight) Color.Black else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            color = labelColor
        )
        content()
    }
}

private fun loadMacros(context: Context): List<MacroConfig> {
    return MacroRepository.loadAll().toImmutableList()
}

private fun saveMacros(context: Context, macros: List<MacroConfig>) {
    MacroRepository.saveAll(macros)
}
