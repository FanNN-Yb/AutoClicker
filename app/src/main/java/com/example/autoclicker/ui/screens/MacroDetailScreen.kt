package com.example.autoclicker.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.Role
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
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioTextField
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import com.example.autoclicker.ui.components.GlassMenuPopup
import com.example.autoclicker.ui.components.NumberPickerInlineContent
import com.example.autoclicker.ui.components.numberPickerMenuHeight
import com.example.autoclicker.ui.components.numberPickerVisibleCount
import io.github.neoruaa.glasmio.glass.progressiveBlurBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition
import io.github.neoruaa.glasmio.glass.scrollEdgeContentFade
import com.example.autoclicker.ui.components.RubberBandContainer
import com.example.autoclicker.ui.components.rememberRubberBandState
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import kotlinx.collections.immutable.toImmutableList
import org.json.JSONArray

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
    val dividerColor = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
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

    // Picker 展开（关键帧驱动的玻璃弹窗，见 GlassMenuPopup：矩形四边 + 圆角各自走曲线）
    var showRepeatPicker by remember { mutableStateOf(false) }
    var showSpeedPicker by remember { mutableStateOf(false) }

    // 数值标签动画（原版手感：展开时缩小淡出往下沉，收起时带一点回弹弹回来）
    val repeatLabelAnim = remember { Animatable(1f) }
    val speedLabelAnim = remember { Animatable(1f) }
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

    // 胶囊在窗口里的矩形位置：弹窗用它把首帧种子点校正到胶囊中心。
    // 放在页面级而不是行内，弹窗也就永远不进列表项的布局，胶囊不会被挤动。
    var repeatChipBounds by remember { mutableStateOf(Rect.Zero) }
    var speedChipBounds by remember { mutableStateOf(Rect.Zero) }

    // 弹窗不抢焦点（抢了会让页面动画卡住），返回键在这里关掉
    BackHandler(enabled = showRepeatPicker || showSpeedPicker) {
        showRepeatPicker = false
        showSpeedPicker = false
    }

    // picker 面板高度：和 NumberPicker 的圆柱面几何保持一致（不是 rows × 40dp，首尾行被弧线收进去了）
    val pickerRows = numberPickerVisibleCount(LocalConfiguration.current.screenHeightDp)
    val pickerPanelHeight = numberPickerMenuHeight(pickerRows)
    val pickerMenuWidth = minOf(
        LocalConfiguration.current.screenWidthDp.dp - 32.dp,
        360.dp
    )

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

    // 整页共用一个录制层：picker 和顶栏图标按钮都从这里采样，避免采到空层（黑色/锯齿）
    val pageBackdrop = rememberLayerBackdrop()
    // 橡皮筋状态提升到页面级，供容器外的顶栏模糊读取
    val rubberState = rememberRubberBandState()

    // 遮罩高度
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val titleBarHeightPx = with(density) { 68.dp.toPx() }
    val maskBottomPx = statusBarHeightPx + titleBarHeightPx
    val maskHeightDp = with(density) { maskBottomPx.toDp() }
    // 遮罩高度 = 顶栏下沿（原来 ×1.3，模糊尾巴压到了第一条内容上）
    val maskVisualHeightDp = maskHeightDp

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
        // 页面内容（供 backdrop 录制）：背景画在录制层内部，玻璃/模糊才采得到页面底色
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(pageBackdrop)
                // 菜单/弹窗在独立 window 里，必须登记这层的窗口位置，否则采不到页面（玻璃变纯色）
                .trackBackdropPosition(pageBackdrop)
                .background(bg)
        ) {
        // 🔴 录制层在外面、橡皮筋在里层：拉伸位移会被录进 backdrop，模糊才跟手
        RubberBandContainer(
            state = rubberState,
            modifier = Modifier
                .fillMaxSize()
                // demo 同款：内容在顶栏下沿渐隐
                .scrollEdgeContentFade(
                    progress = 1f,
                    headerHeight = maskHeightDp,
                    headerTop = with(density) { statusBarHeightPx.toDp() }
                )
        ) {
                // 内容
                LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                // 名称（demo 同款卡片 + 库的 Text Field）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SettingsSectionHeader("名称", isLight)
                    Spacer(modifier = Modifier.height(8.dp))
                    GlasmioGroupedList {
                        GlasmioTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            placeholder = "方案名称",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 参数卡片（重复次数、执行速度、点击点数）
                val textGray = Color(0xFF999999)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SettingsSectionHeader("参数", isLight)
                    Spacer(modifier = Modifier.height(8.dp))
                    GlasmioGroupedList {
                    // 重复次数：数值是胶囊按钮，点它展开关键帧玻璃弹窗
                    // （面板从胶囊中心长出来，四边 + 圆角按抓帧曲线各自插值，内容终态尺寸被窗口裁出）
                    GlasmioListRow(
                        title = "重复次数",
                        showTopSeparator = false,
                        modifier = Modifier.demoSeparator(showDividers, dividerColor),
                        trailing = {
                            ValueLabel(
                                value = "$editedRepeat",
                                unit = "次",
                                progress = { repeatLabelAnim.value },
                                textPrimary = textPrimary,
                                textGray = textGray,
                                isLight = isLight,
                                onClick = { showRepeatPicker = true },
                                modifier = Modifier.onGloballyPositioned {
                                    repeatChipBounds = it.boundsInWindow()
                                }
                            )
                        }
                    )

                    // 执行速度
                    GlasmioListRow(
                        title = "执行速度",
                        showTopSeparator = false,
                        modifier = Modifier.demoSeparator(showDividers, dividerColor),
                        trailing = {
                            ValueLabel(
                                value = "$editedSpeed",
                                unit = "倍",
                                progress = { speedLabelAnim.value },
                                textPrimary = textPrimary,
                                textGray = textGray,
                                isLight = isLight,
                                onClick = { showSpeedPicker = true },
                                modifier = Modifier.onGloballyPositioned {
                                    speedChipBounds = it.boundsInWindow()
                                }
                            )
                        }
                    )

                    // 点击点数
                    GlasmioListRow(
                        title = "点击点数",
                        showTopSeparator = false,
                        detail = "${macro.positions.size} 个点"
                    )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                }
                // 动态补位 Spacer：实测视口高 - 内容高 + 1dp，填满屏幕且滚动范围 > 0 触发橡皮筋
                Spacer(modifier = Modifier.height(spacerHeight))
            }
        }
        } // RubberBandContainer
        } // 关闭内层 layerBackdrop Box（遮罩/标题栏放到外层，与 overscroll 各持独立 layer）

        // ===== Gradient overlay =====
        // ===== 顶部渐进式模糊（替代原来的渐变遮罩）=====
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maskVisualHeightDp)
                .zIndex(50f)
                .progressiveBlurBackdrop(
                    backdrop = pageBackdrop,
                    progress = 1f,
                    maxRadius = 10.dp
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
                                GlasmioIconButton(
                    onClick = {
                        if (ThemeSettings.vibrateEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onBack()
                    },
                                        backdrop = pageBackdrop,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                                        // 与库 demo 的返回按钮一致：SfSymbol.ChevronBack + 玻璃按钮自带的前景色
                                        SfIcon(
                        symbol = SfSymbol.ChevronBack,
                        contentDescription = "返回",
                        mirrored = true
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

                GlasmioIconButton(
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
                                        backdrop = pageBackdrop,
                    emphasis = GlassEmphasis.Prominent,
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

        // ===== Picker 弹窗 =====
        // 放在页面根部而不是列表项里：弹窗永远不参与行布局，点开时胶囊不会被挤动。
        GlassMenuPopup(
            expanded = showRepeatPicker,
            onExpandedChange = { showRepeatPicker = it },
            anchorBounds = repeatChipBounds,
            panelWidth = pickerMenuWidth,
            panelHeight = pickerPanelHeight,
            backdrop = pageBackdrop
        ) { childBackdrop ->
            NumberPickerInlineContent(
                value = editedRepeat.toFloat(),
                onValueChange = { editedRepeat = it.toInt() },
                onCancel = { showRepeatPicker = false },
                onConfirm = { showRepeatPicker = false },
                range = (1..1000).map { it.toFloat() },
                label = "重复次数",
                isLight = isLight,
                backdrop = childBackdrop
            )
        }

        GlassMenuPopup(
            expanded = showSpeedPicker,
            onExpandedChange = { showSpeedPicker = it },
            anchorBounds = speedChipBounds,
            panelWidth = pickerMenuWidth,
            panelHeight = pickerPanelHeight,
            backdrop = pageBackdrop
        ) { childBackdrop ->
            NumberPickerInlineContent(
                value = editedSpeed,
                onValueChange = { editedSpeed = it },
                onCancel = { showSpeedPicker = false },
                onConfirm = { showSpeedPicker = false },
                range = generateSequence(0.25f) { it + 0.25f }
                    .takeWhile { it <= 10.0f }.toList(),
                label = "执行速度",
                isLight = isLight,
                backdrop = childBackdrop
            )
        }
    }
}

/**
 * 数值标签（重复次数 / 执行速度）：灰胶囊里放数字，后面跟灰色单位文字。
 *
 * 展开时胶囊缩下去淡出、单位跟着淡出；收起时用 spring 弹回（原版手感）。
 * 用普通控件而不是 GlasmioButton：库的 GlassSurface 一旦落在菜单锚点上会自动套一层
 * `rememberMenuAnchorForeground`（把自己录进 layer 再画出来），和玻璃自身的材质层叠在一起
 * 会成环 → RenderThread 爆栈闪退（实测两次），所以这里就画平面控件。
 */
@Composable
private fun ValueLabel(
    value: String,
    unit: String,
    progress: () -> Float,
    textPrimary: Color,
    textGray: Color,
    isLight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetPx = with(LocalDensity.current) { 24.dp.toPx() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(
            interactionSource = null,
            indication = null,
            role = Role.Button,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val p = progress()
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = p.coerceAtLeast(0.01f)
                    scaleY = p.coerceAtLeast(0.01f)
                    translationY = (1f - p) * offsetPx
                }
                .background(
                    if (isLight) Color(0xFFE5E5E5) else Color(0xFF3A3A3C),
                    RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = unit,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = textGray,
            modifier = Modifier.graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
        )
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
