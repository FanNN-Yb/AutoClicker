package com.example.autoclicker.ui.screens

import android.content.Context
import com.example.autoclicker.MacroRepository
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.autoclicker.MacroConfig
import com.example.autoclicker.R
import com.example.autoclicker.ThemeSettings
import com.example.autoclicker.ui.components.GlasenseSwipeable
import io.github.fannn_yb.rubberband.RubberBandContainer
import com.example.autoclicker.ui.components.SwipeableActionButton
import com.example.autoclicker.ui.components.rememberSwipeableListState
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.Capsule
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray

@Composable
fun MacroConfigScreen(
    modifier: Modifier = Modifier,
    onDimAlphaChange: (Float) -> Unit = {},
    navDimDismissTrigger: Int = 0
) {
        val isLight = !ThemeSettings.isDarkTheme()
    val bg = if (isLight) Color(0xFFF5F5F7) else Color(0xFF000000)
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0xCC000000) else Color(0xCCFFFFFF)
    val cardBg = if (isLight) Color.White else Color(0xFF1C1C1E)
    val deleteColor = Color(0xFFFF3B30)
    val cardBorder = if (isLight) Color(0xFFE5E5E5) else Color(0xFF3A3A3C)
    val accentColor = ThemeSettings.getAccentColor()

    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
                var macros by remember { mutableStateOf(loadMacros(context)) }
    // 从详情页返回后重新加载数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                macros = loadMacros(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var macroToDelete by remember { mutableStateOf<MacroConfig?>(null) }
    var dialogTarget by remember { mutableStateOf<MacroConfig?>(null) }
    val swipeableListState = rememberSwipeableListState()
    var macroAnimatingOut by remember { mutableStateOf<String?>(null) }

    // 删除动画：卡片滑出后移除
    LaunchedEffect(macroAnimatingOut) {
        val name = macroAnimatingOut ?: return@LaunchedEffect
        kotlinx.coroutines.delay(300)
        macros = macros.filter { it.name != name }.toImmutableList()
        saveMacros(context, macros)
        macroAnimatingOut = null
    }

    val backdrop = rememberLayerBackdrop()
    val listState = rememberLazyListState()

    // ===== 大标题位置追踪 =====
    var titleTopY by remember { mutableFloatStateOf(0f) }
    var titleBottomY by remember { mutableFloatStateOf(0f) }

    // 遮罩底部边界（状态栏 + 标题栏）
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val titleBarHeightPx = with(density) { 68.dp.toPx() }
    val maskBottomPx = statusBarHeightPx + titleBarHeightPx

    // 小标题渐显：大标题顶部滚入遮罩时开始渐显
    val rawTitleHidden by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else if (titleBottomY <= 0f) 0f
            else {
                val titleHeight = (titleBottomY - titleTopY).coerceAtLeast(1f)
                val hidden = (maskBottomPx - titleTopY) / titleHeight
                hidden.coerceIn(0f, 1f)
            }
        }
    }
    val titleHidden by animateFloatAsState(
        targetValue = rawTitleHidden,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "titleHidden"
    )

    // 遮罩高度 dp
    val maskHeightDp = with(density) { maskBottomPx.toDp() }
    val maskVisualHeightDp = with(density) { (maskBottomPx * 1.3f).toDp() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        val isLandscape = maxWidth > maxHeight
        val horizontalPadding = if (isLandscape) ((maxWidth - 360.dp) / 2f).coerceAtLeast(24.dp) else 24.dp
        // 页面内容（供 backdrop 录制）
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
        RubberBandContainer {
        // ===== LazyColumn =====
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = maskHeightDp + 12.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (macros.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(200.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_slider),
                            contentDescription = "暂无方案",
                            modifier = Modifier.size(64.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                textSecondary.copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无方案",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W500,
                            color = textSecondary.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "点击录制按钮创建你的第一个宏",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W400,
                            color = textSecondary.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(0.dp))
                }
            } else {
                // ===== 大标题（随滚动渐隐）=====
                item(key = "_big_title_") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .graphicsLayer {
                                alpha = (1f - titleHidden * 2f).coerceAtLeast(0f)
                            }
                            .onGloballyPositioned { coords ->
                                titleTopY = coords.positionInRoot().y
                                titleBottomY = coords.positionInRoot().y + coords.size.height
                            }
                    ) {
                        Text(
                            text = "宏配置",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "${macros.size} 个方案",
                            fontSize = 14.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // ===== 宏列表 =====
                itemsIndexed(macros, key = { _, macro -> macro.name }, contentType = { _, _ -> "macro_item" }) { index, macro ->
                    val cardShape = remember { RoundedRectangle(22.dp, RoundedCornerStyle.Continuous) }

                    val isAnimatingOut = macroAnimatingOut == macro.name
                    val slideOffset by animateFloatAsState(
                        targetValue = if (isAnimatingOut) -1.5f else 0f,
                        animationSpec = tween(300),
                        label = "slideOffset"
                    )

                                        GlasenseSwipeable(
                        key = macro.name,
                        listState = swipeableListState,
                        actions = remember { persistentListOf(SwipeableActionButton(index = 0, color = deleteColor, isDestructive = false)) },
                        onAction = { _ ->
                            macroToDelete = macro
                        },
                        modifier = Modifier
                            .animateItem()
                            .graphicsLayer {
                                translationX = slideOffset * size.width
                                alpha = if (isAnimatingOut) 0f else 1f
                            }
                    ) {
                                                Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(cardBg)
                                .then(if (isLight) Modifier else Modifier.border(0.25.dp, cardBorder, cardShape))
                                .clickable {
                                    if (ThemeSettings.vibrateEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    val intent = android.content.Intent(context, com.example.autoclicker.MacroDetailActivity::class.java)
                                    intent.putExtra(com.example.autoclicker.MacroDetailActivity.EXTRA_MACRO_NAME, macro.name)
                                    context.startActivity(intent)
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = macro.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.W600,
                                    color = textPrimary,
                                    letterSpacing = (-0.374).sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${macro.positions.size} 个点",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.W400,
                                    color = textSecondary,
                                    letterSpacing = (-0.224).sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "重复 ${macro.repeatCount} 次",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                                Text(
                                    text = "速度 ${macro.speed}x",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
        } // RubberBandContainer
        } // 关闭内层 layerBackdrop Box（只录 LazyColumn；遮罩/标题栏放到外层，与 overscroll 各持独立 layer）

        // ===== Gradient overlay at top =====（backdrop 的兄弟节点）
        val maskVisualHeightPx = with(density) { maskVisualHeightDp.toPx() }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maskVisualHeightDp)
                .zIndex(50f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            bg,
                            bg.copy(alpha = 0.85f),
                            bg.copy(alpha = 0f)
                        ),
                        startY = 0f,
                        endY = maskVisualHeightPx
                    )
                )
        )

        // ===== 顶部标题栏：遮罩上的小标题 =====
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
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                // 遮罩上的小标题（大标题渐隐完成后渐显）
                Column(
                    modifier = Modifier
                        .alpha(((titleHidden - 0.5f) * 2f).coerceIn(0f, 1f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "宏配置",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${macros.size} 个方案",
                        fontSize = 11.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 0.dp)
                    )
                }
            }
        }

        // 删除确认弹窗 — 液态玻璃样式，不透明底衬防止透出页面文字
        val dialogAlphaAnim = remember { Animatable(0f) }
        val dialogScaleAnim = remember { Animatable(0.3f) }
        val dimAlpha = remember { Animatable(0f) }
        var dialogVisible by remember { mutableStateOf(false) }

        // 导航栏遮罩点击 → 关闭弹窗
        LaunchedEffect(navDimDismissTrigger) {
            if (navDimDismissTrigger > 0) macroToDelete = null
        }

        // 同步 dimAlpha 动画值到 MainActivity 用于导航栏遮罩
        LaunchedEffect(dialogVisible) {
            if (dialogVisible) {
                snapshotFlow { dimAlpha.value }.collect { onDimAlphaChange(it) }
            } else {
                onDimAlphaChange(0f)
            }
        }

        val dimColor = if (isLight) Color(0xFF29293A).copy(0.23f) else Color(0xFF121212).copy(0.56f)
        val containerColor = if (isLight) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)

        LaunchedEffect(macroToDelete) {
            if (macroToDelete != null) {
                dialogTarget = macroToDelete
                dialogVisible = true
                dialogScaleAnim.snapTo(0.3f)
                dialogAlphaAnim.snapTo(0f)
                dimAlpha.snapTo(0f)
                coroutineScope {
                    launch { dimAlpha.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 400f)) }
                    launch { dialogAlphaAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 400f)) }
                    dialogScaleAnim.animateTo(1f, spring(dampingRatio = 1f, stiffness = 550f))
                }
            } else if (dialogTarget != null) {
                coroutineScope {
                    launch { dimAlpha.animateTo(0f, tween(200)) }
                    launch { dialogAlphaAnim.animateTo(0f, tween(200)) }
                    dialogScaleAnim.animateTo(0.3f, spring(dampingRatio = 0.7f, stiffness = 390f))
                }
                dialogVisible = false
                dialogTarget = null
            }
        }

        if (dialogVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .graphicsLayer { alpha = dimAlpha.value }
                    .background(dimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { macroToDelete = null },
                contentAlignment = Alignment.Center
            ) {
                    // 动画容器：内含不透明底衬 + 液态玻璃卡片
                    // 横屏时与页面卡片同宽（360dp），父容器 Center 自动水平居中
                    Box(
                        modifier = Modifier
                            .then(
                                if (isLandscape) {
                                    Modifier.width(360.dp)
                                } else {
                                    Modifier.padding(horizontal = 40.dp).fillMaxWidth()
                                }
                            )
                            .graphicsLayer {
                                alpha = dialogAlphaAnim.value
                                scaleX = dialogScaleAnim.value
                                scaleY = dialogScaleAnim.value
                            }
                    ) {
                        // 不透明底衬（挡住页面原文，只让 backdrop 录制的模糊内容显示）
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedRectangle(48.dp, RoundedCornerStyle.Continuous))
                                .background(bg)
                        )

                        // 液态玻璃卡片
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(48f.dp) },
                                    effects = {
                                        colorControls(
                                            brightness = if (isLight) 0.2f else 0f,
                                            saturation = 1.5f
                                        )
                                        blur(if (isLight) 16f.dp.toPx() else 8f.dp.toPx())
                                        lens(24f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                                    },
                                    highlight = { Highlight.Plain },
                                    onDrawSurface = { drawRect(containerColor) }
                                )
                        ) {
                            Text(
                                "确认删除",
                                Modifier.padding(28.dp, 24.dp, 28.dp, 12.dp),
                                style = TextStyle(textPrimary, 24.sp, FontWeight.Medium)
                            )
                            val displayName = dialogTarget!!.name.ifEmpty { "未命名" }
                            Text(
                                "确定要删除「${if (displayName.length > 20) displayName.take(20) + "..." else displayName}」吗？\n此操作不可撤销。",
                                Modifier.padding(24.dp, 12.dp, 24.dp, 12.dp)
                                    .then(
                                        if (isLight) Modifier
                                        else Modifier.graphicsLayer(blendMode = BlendMode.Plus)
                                    ),
                                style = TextStyle(textSecondary, 15.sp),
                                maxLines = 5
                            )
                            Row(
                                Modifier
                                    .padding(24.dp, 12.dp, 24.dp, 24.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    Modifier
                                        .clip(Capsule())
                                        .background(containerColor.copy(alpha = 0.2f))
                                        .border(0.5.dp, textPrimary.copy(alpha = 0.15f), Capsule())
                                        .clickable { macroToDelete = null }
                                        .height(48.dp)
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("取消", style = TextStyle(textPrimary, 16.sp))
                                }
                                Row(
                                    Modifier
                                        .clip(Capsule())
                                        .background(deleteColor)
                                        .clickable {
                                            if (ThemeSettings.vibrateEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            val target = dialogTarget!!
                                            macroToDelete = null
                                            macroAnimatingOut = target.name
                                        }
                                        .height(48.dp)
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("删除", style = TextStyle(Color.White, 16.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

private fun loadMacros(context: Context): List<MacroConfig> {
    return MacroRepository.loadAll().toImmutableList()
}

private fun saveMacros(context: Context, macros: List<MacroConfig>) {
    MacroRepository.saveAll(macros)
}
