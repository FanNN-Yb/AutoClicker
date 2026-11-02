package com.example.autoclicker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import com.example.autoclicker.ThemeSettings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.GlassEmphasis
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize

@Composable
fun HomeScreen(
    onOpenAccessibility: () -> Unit,
    onStartService: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isAccessibilityEnabled: Boolean,
    isServiceRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val isLight = !ThemeSettings.isDarkTheme()
    val bg = if (isLight) Color(0xFFF5F5F7) else Color(0xFF000000)
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0xCC000000) else Color(0xCCFFFFFF)
    val accent = ThemeSettings.getAccentColor()

    val backdrop = rememberLayerBackdrop()

    val density = LocalDensity.current

    // 实测 LazyColumn 视口高度和内容高度，动态计算 Spacer
    // 不用 screenHeightDp（包含状态栏/导航栏，不准），直接量 LazyColumn 自身高度
    var viewportHeightPx by remember { mutableStateOf(0) }
    var contentHeightPx by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 纯色背景层：单独一层只录背景色。
        // 不能把 layerBackdrop 挂在整页根节点上——玻璃按钮在层内部采样自己所在的层，
        // 渲染树会无限递归（RenderThread SIGSEGV）。
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(bg)
        )
        val isLandscape = maxWidth > maxHeight
        val topPadding = if (isLandscape) 80.dp else 220.dp
        val contentMaxWidth = if (isLandscape) 360.dp else androidx.compose.ui.unit.Dp.Unspecified

        val contentPaddingVertical = topPadding + 40.dp
        val viewportContentArea = with(density) { viewportHeightPx.toDp() } - contentPaddingVertical
        val contentHeight = with(density) { contentHeightPx.toDp() }
        val spacerHeight = if (viewportHeightPx > 0 && contentHeightPx > 0) {
            (viewportContentArea - contentHeight + 1.dp).coerceAtLeast(1.dp)
        } else {
            1.dp
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    viewportHeightPx = coords.size.height
                },
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = topPadding, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // 不用 weight：实测内容高度，末尾用一个动态计算的 Spacer 补满视口 + 留过界余量
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .widthIn(max = contentMaxWidth)
                        .onGloballyPositioned { coords ->
                            contentHeightPx = coords.size.height
                        }
                ) {
                Text(
                    text = "宏点击器",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.W600,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.374).sp
                )
                Spacer(modifier = Modifier.height(48.dp))
                // Status cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusCard(
                        title = "无障碍服务",
                        status = if (isAccessibilityEnabled) "已开启" else "未开启",
                        isActive = isAccessibilityEnabled,
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatusCard(
                        title = "悬浮窗",
                        status = if (isServiceRunning) "运行中" else "未启动",
                        isActive = isServiceRunning,
                        isLight = isLight,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                // Primary CTA - Start/Stop (Liquid Glass Button)
                GlasmioButton(
                    onClick = onStartService,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                    size = GlasmioButtonSize.Large,
                    emphasis = GlassEmphasis.Prominent
                ) {
                    Text(
                        text = if (isServiceRunning) "停止服务" else "启动悬浮窗",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W400,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Secondary link - Accessibility settings (Liquid Glass Button)
                GlasmioButton(
                    onClick = onOpenAccessibility,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                    size = GlasmioButtonSize.Large
                ) {
                    Text(
                        text = "打开无障碍设置 >",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W400,
                        color = textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // 提示
                val haptic = LocalHapticFeedback.current
                // 按下立即显示遮罩：默认涟漪在滚动容器内会延迟 ~100ms，跳转动画先行导致看不到；
                // 点击后额外保持 350ms，确保跳转动画全程都能看到反馈
                var hintMaskActive by remember { mutableStateOf(false) }
                val hintInteraction = remember { MutableInteractionSource() }
                val hintPressed by hintInteraction.collectIsPressedAsState()
                LaunchedEffect(hintMaskActive) {
                    if (hintMaskActive) {
                        kotlinx.coroutines.delay(350)
                        hintMaskActive = false
                    }
                }
                val hintMaskColor = if (isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.15f)
                Text(
                    text = "每次关闭应用后无障碍服务会断开？\n前往 设置 → 权限 中「关闭电池优化」和「开启自启动」即可解决",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W400,
                    color = textSecondary.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedRectangle(12.dp, RoundedCornerStyle.Continuous))
                        .background(if (hintPressed || hintMaskActive) hintMaskColor else Color.Transparent)
                        .clickable(interactionSource = hintInteraction, indication = null) {
                            if (ThemeSettings.vibrateEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            hintMaskActive = true
                            onNavigateToSettings()
                        }
                        .padding(8.dp)
                )
                }
                // 动态补位 Spacer：实测视口高 - 内容高 + 1dp，填满屏幕且滚动范围 > 0 触发橡皮筋
                Spacer(modifier = Modifier.height(spacerHeight))
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    status: String,
    isActive: Boolean,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isLight) Color.White else Color(0xFF1C1C1E)
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0xCC000000) else Color(0xCCFFFFFF)
    val borderColor = if (isActive) Color(0xFF34C759) else Color(0xFFFF3B30)

    Column(
        modifier = modifier
            .clip(RoundedRectangle(22.dp, RoundedCornerStyle.Continuous))
            .background(cardBg)
            .border(0.5.dp, borderColor, RoundedRectangle(22.dp, RoundedCornerStyle.Continuous))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.W400,
            color = textSecondary,
            letterSpacing = (-0.12).sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = status,
            fontSize = 17.sp,
            fontWeight = FontWeight.W600,
            color = textPrimary,
            letterSpacing = (-0.374).sp
        )
    }
}
