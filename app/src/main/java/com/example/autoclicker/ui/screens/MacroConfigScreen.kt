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
import com.example.autoclicker.ui.components.RubberBandContainer
import com.example.autoclicker.ui.components.rememberRubberBandState
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonLayout
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.glass.progressiveBlurBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition
import io.github.neoruaa.glasmio.glass.scrollEdgeContentFade
import com.example.autoclicker.ui.components.SwipeableActionButton
import com.example.autoclicker.ui.components.rememberSwipeableListState
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.render.drawBackdrop
import io.github.neoruaa.glasmio.glass.render.effects.blur
import io.github.neoruaa.glasmio.glass.render.effects.colorFilter
import io.github.neoruaa.glasmio.glass.render.effects.lens
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import io.github.neoruaa.glasmio.glass.render.highlight.Highlight
import com.kyant.shapes.Capsule
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray

/** 用 ColorMatrix 近似原 Kyant 的 colorControls(brightness, saturation)。 */
private fun brightnessSaturationFilter(brightness: Float, saturation: Float): ColorFilter {
    val matrix = ColorMatrix()
    matrix.setToSaturation(saturation)
    if (brightness != 0f) {
        val scale = 1f + brightness
        matrix[0, 0] *= scale
        matrix[1, 1] *= scale
        matrix[2, 2] *= scale
    }
    return ColorFilter.colorMatrix(matrix)
}

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

    // 橡皮筋状态提升到页面级，供容器外的顶栏模糊读取
    val rubberState = rememberRubberBandState()
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
            else if (titleBottomY <= 0f) {
                if (titleTopY < 0f) 1f else 0f
            }
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
    // 遮罩高度 = 顶栏下沿（原来 ×1.3，模糊尾巴压到了第一条内容上）
    val maskVisualHeightDp = maskHeightDp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        val isLandscape = maxWidth > maxHeight
        val horizontalPadding = if (isLandscape) ((maxWidth - 360.dp) / 2f).coerceAtLeast(24.dp) else 24.dp
        // 页面内容（供 backdrop 录制）：背景画在录制层内部，玻璃才采得到底色
        // trackBackdropPosition：对话框在独立 window 里，必须登记这层的窗口位置，
        // 否则跨窗口采样直接放弃 → 弹窗玻璃只剩底色（颜色不对）
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .trackBackdropPosition(backdrop)
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

        // ===== 顶栏渐进边缘模糊（demo 同款：库原生 progressiveBlurBackdrop，不染色）=====
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maskVisualHeightDp)
                .zIndex(50f)
                .progressiveBlurBackdrop(
                    backdrop = backdrop,
                    progress = 1f,
                    maxRadius = 10.dp
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

        // 删除确认弹窗 —— 换成库的 GlasmioAlertDialog（demo 同款材质 / 尺寸 / 动画）
        // 导航栏遮罩点击 → 关闭弹窗
        LaunchedEffect(navDimDismissTrigger) {
            if (navDimDismissTrigger > 0) macroToDelete = null
        }

        // 库的对话框自带全屏 scrim，导航栏不需要再单独压暗
        LaunchedEffect(Unit) { onDimAlphaChange(0f) }

        // 退场动画期间保留标题/文案，避免文字先消失
        LaunchedEffect(macroToDelete) {
            if (macroToDelete != null) {
                dialogTarget = macroToDelete
            } else {
                kotlinx.coroutines.delay(320)
                dialogTarget = null
            }
        }

        val deleteTarget = macroToDelete ?: dialogTarget
        GlasmioAlertDialog(
            visible = macroToDelete != null,
            onDismissRequest = { macroToDelete = null },
            title = "确认删除",
            message = deleteTarget?.let { target ->
                val displayName = target.name.ifEmpty { "未命名" }
                "确定要删除「${if (displayName.length > 20) displayName.take(20) + "..." else displayName}」吗？\n此操作不可撤销。"
            } ?: "",
            backdrop = backdrop,
            buttonLayout = GlasmioAlertButtonLayout.SideBySide,
            buttons = listOf(
                GlasmioAlertButtonSpec(
                    label = "取消",
                    onClick = { macroToDelete = null },
                    role = GlasmioAlertButtonRole.Cancel
                ),
                GlasmioAlertButtonSpec(
                    label = "删除",
                    onClick = {
                        if (ThemeSettings.vibrateEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val target = deleteTarget
                        macroToDelete = null
                        if (target != null) macroAnimatingOut = target.name
                    },
                    role = GlasmioAlertButtonRole.Destructive
                )
            ),
        )
    }
    }

private fun loadMacros(context: Context): List<MacroConfig> {
    return MacroRepository.loadAll().toImmutableList()
}

private fun saveMacros(context: Context, macros: List<MacroConfig>) {
    MacroRepository.saveAll(macros)
}
