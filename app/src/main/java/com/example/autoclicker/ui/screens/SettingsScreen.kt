package com.example.autoclicker.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Icon

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.autoclicker.R
import com.example.autoclicker.ThemeSettings
import com.example.autoclicker.FloatingWindowService
import com.example.autoclicker.ui.components.CheckmarkIcon
import com.example.autoclicker.ui.components.GlassSlider
import com.example.autoclicker.ui.components.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import io.github.fannn_yb.rubberband.RubberBandContainer

@Composable
fun SettingsScreen(
    backdrop: Backdrop? = null,
    onOpenBatteryOptimization: () -> Unit = {},
    onOpenAutoStart: () -> Unit = {},
    onNavigateToTheme: () -> Unit = {},
    onNavigateToCredits: () -> Unit = {},
    permissionHighlightTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val effectiveBackdrop = backdrop ?: emptyBackdrop()
    val isLight = !ThemeSettings.isDarkTheme()
   val bg = if (isLight) Color(0xFFF2F2F7) else Color(0xFF000000)
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0x99000000) else Color(0x99FFFFFF)
    val cardBg = if (isLight) Color.White else Color(0xFF1C1C1E)
    val accent = Color(0xFF0071E3)
    val dividerColor =
        if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    val cardBorder =
        if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)

    // 设置从 ThemeSettings 读取
    val vibrateEnabled = ThemeSettings.vibrateEnabled
    val showDividers = ThemeSettings.showDividers
    val themeMode = ThemeSettings.themeMode
    val themeAuto = ThemeSettings.themeAuto

    val density = LocalDensity.current
    val listState = rememberLazyListState()

    // ===== 权限区块高亮闪烁（从首页提示文字跳转过来时触发）=====
    // "权限" section 是 LazyColumn 的 item 1（item 0 是大标题）
    val permissionSectionIndex = 1
    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(permissionHighlightTrigger) {
        if (permissionHighlightTrigger <= 0) return@LaunchedEffect
        // 先滚动到权限区块，再闪烁两次灰色遮罩
        listState.animateScrollToItem(permissionSectionIndex)
        repeat(2) {
            flashAlpha.animateTo(1f, tween(durationMillis = 140, easing = FastOutSlowInEasing))
            flashAlpha.animateTo(0f, tween(durationMillis = if (it == 0) 160 else 420, easing = FastOutSlowInEasing))
        }
    }
    val flashColor = if (isLight) Color.Black else Color.White
    val flashTargetAlpha = if (isLight) 0.12f else 0.18f

    // ===== 大标题位置追踪 =====
    var titleTopY by remember { mutableFloatStateOf(0f) }
    var titleBottomY by remember { mutableFloatStateOf(0f) }

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val titleBarHeightPx = with(density) { 68.dp.toPx() }
    val maskBottomPx = statusBarHeightPx + titleBarHeightPx

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

    val maskHeightDp = with(density) { maskBottomPx.toDp() }
    val maskVisualHeightDp = with(density) { (maskBottomPx * 1.3f).toDp() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        val isLandscape = maxWidth > maxHeight
        val horizontalPadding = if (isLandscape) ((maxWidth - 360.dp) / 2f).coerceAtLeast(20.dp) else 20.dp
        RubberBandContainer {
        LazyColumn(
            state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
                start = horizontalPadding, end = horizontalPadding, top = maskHeightDp + 12.dp, bottom = 120.dp
            ), verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ===== 大标题 =====
            item(key = "_big_title_") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 28.dp)
                        .graphicsLayer {
                            alpha = (1f - titleHidden * 2f).coerceAtLeast(0f)
                        }
                        .onGloballyPositioned { coords ->
                            titleTopY = coords.positionInRoot().y
                            titleBottomY = coords.positionInRoot().y + coords.size.height
                        }) {
                    Text(
                        text = "设置",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "自定义应用行为",
                        fontSize = 14.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // ===== Section: 权限 =====
            item(key = "section_permission") {
                SettingsSectionHeader("权限", isLight)
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    SettingsCard(cardBg, cardBorder) {
                        SettingsActionRow(
                            title = "关闭电池优化",
                            description = "防止系统在后台关闭无障碍服务",
                            isLight = isLight,
                            showDivider = showDividers,
                            dividerColor = dividerColor,
                            onClick = onOpenBatteryOptimization
                        )
                        SettingsActionRow(
                            title = "开启自启动",
                            description = "允许应用在后台自动恢复无障碍服务",
                            isLight = isLight,
                            showDivider = false,
                            dividerColor = dividerColor,
                            onClick = onOpenAutoStart
                        )
                    }
                    // 跳转高亮：灰色遮罩闪烁（形状与卡片圆角一致）
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedRectangle(22.dp, RoundedCornerStyle.Continuous))
                            .graphicsLayer { alpha = flashAlpha.value }
                            .background(flashColor.copy(alpha = flashTargetAlpha))
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "关闭应用后无障碍服务若被系统关闭，请确保已开启以上两项权限。国产手机通常需要在手机管家中同时开启。",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    color = textSecondary.copy(alpha = 0.5f),
                    letterSpacing = (-0.12).sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                )
            }

            item(key = "spacer_1") { Spacer(modifier = Modifier.height(24.dp)) }

            // ===== Section: 通用 =====
            item(key = "section_general") {
                SettingsSectionHeader("通用", isLight)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard(cardBg, cardBorder) {
                    SettingsActionRow(
                        title = "主题",
                        description = if (themeAuto) "自动" else if (themeMode == 0) "浅色" else "深色",
                        isLight = isLight,
                        showDivider = showDividers,
                        dividerColor = dividerColor,
                        onClick = { onNavigateToTheme() })
                    SettingsRow(
                        title = "震动反馈",
                        description = "点击时提供触觉反馈",
                        isLight = isLight,
                        showDivider = showDividers,
                        dividerColor = dividerColor
                    ) {
                        LiquidToggle(
                            selected = { vibrateEnabled },
                            onSelect = { ThemeSettings.updateVibrateEnabled(it) },
                            backdrop = effectiveBackdrop
                        )
                    }
                    SettingsRow(
                        title = "显示分割线",
                        description = "在项目之间显示分割线",
                        isLight = isLight,
                        showDivider = false,
                        dividerColor = dividerColor
                    ) {
                        LiquidToggle(
                            selected = { showDividers },
                            onSelect = { ThemeSettings.updateShowDividers(it) },
                            backdrop = effectiveBackdrop
                        )
                    }
                    // 悬浮窗不透明度
                    if (showDividers) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(0.5.dp)
                                .background(dividerColor)
                        )
                    }
                    FloatingOpacityRow(
                        isLight = isLight,
                        backdrop = effectiveBackdrop
                    )
                }
            }

            item(key = "spacer_2") { Spacer(modifier = Modifier.height(24.dp)) }

            // ===== Section: 关于 =====
            item(key = "section_about") {
                SettingsSectionHeader("关于", isLight)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard(cardBg, cardBorder) {
                    SettingsInfoRow(
                        title = "版本",
                        value = "1.0.0",
                        isLight = isLight,
                        showDivider = showDividers,
                        dividerColor = dividerColor
                    )
                                        SettingsInfoRow(
                        title = "开发者",
                        value = "江雾未央ʸ°",
                        isLight = isLight,
                        showDivider = showDividers,
                        dividerColor = dividerColor
                    )
                    SettingsActionRow(
                        title = "致谢",
                        description = "",
                        isLight = isLight,
                        showDivider = false,
                        dividerColor = dividerColor,
                        onClick = onNavigateToCredits
                    )
                }
            }

            item(key = "spacer_bottom") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        } // RubberBandContainer

        // ===== Gradient overlay =====
        val maskVisualHeightPx = with(density) { maskVisualHeightDp.toPx() }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maskVisualHeightDp)
                .zIndex(1f)
               .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(bg, bg.copy(alpha = 0.85f), bg.copy(alpha = 0f)),
                        startY = 0f,
                        endY = maskVisualHeightPx
                    )
                )
        )

        // ===== 顶部小标题 =====
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(2f)
       ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.alpha(((titleHidden - 0.5f) * 2f).coerceIn(0f, 1f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "设置",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "自定义应用行为",
                        fontSize = 11.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 0.dp)
                    )
                }
            }
        }
    }
}

// ==================== Card wrapper ====================

@Composable
internal fun SettingsCard(
    cardBg: Color, borderColor: Color, content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(22.dp, RoundedCornerStyle.Continuous))
            .background(cardBg)
            .border(0.5.dp, borderColor, RoundedRectangle(22.dp, RoundedCornerStyle.Continuous))
    ) {
        content()
    }
}

// ==================== Section Header ====================

@Composable
internal fun SettingsSectionHeader(
    title: String, isLight: Boolean, modifier: Modifier = Modifier
) {
    val textSecondary = if (isLight) Color(0x99000000) else Color(0x99FFFFFF)
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.W400,
        color = textSecondary,
        letterSpacing = (-0.08).sp,
        modifier = modifier.padding(start = 16.dp, bottom = 0.dp)
    )
}

// ==================== Row: text + switch ====================

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    isLight: Boolean,
    showDivider: Boolean,
    dividerColor: Color,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(16.dp.toPx(), size.height),
                    end = Offset(size.width - 16.dp.toPx(), size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.W400,
                color = if (isLight) Color(0xFF1D1D1F) else Color.White,
                letterSpacing = (-0.374).sp
            )
            Text(
                text = description,
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                color = if (isLight) Color(0xFF6E6E73) else Color(0xFF8E8E93),
                letterSpacing = (-0.224).sp
            )
        }
        trailing()
    }
}

// ==================== Row: clickable action ====================

@Composable
private fun SettingsActionRow(
    title: String,
    description: String = "",
    isLight: Boolean,
    showDivider: Boolean,
    dividerColor: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(16.dp.toPx(), size.height),
                    end = Offset(size.width - 16.dp.toPx(), size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            } else Modifier)
            .clickable {
                if (ThemeSettings.vibrateEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.W400,
                color = if (isLight) Color(0xFF1D1D1F) else Color.White,
                letterSpacing = (-0.374).sp
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                    color = if (isLight) Color(0xFF6E6E73) else Color(0xFF8E8E93),
                    letterSpacing = (-0.224).sp
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = if (isLight) Color(0xFF1D1D1F) else Color.Unspecified,
            modifier = Modifier.size(width = 8.dp, height = 12.dp)
        )
    }
}

// ==================== Row: title + value ====================

@Composable
internal fun SettingsInfoRow(
    title: String, value: String, isLight: Boolean, showDivider: Boolean, dividerColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(16.dp.toPx(), size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.W400,
            color = if (isLight) Color(0xFF1D1D1F) else Color.White,
            letterSpacing = (-0.374).sp
        )
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.W400,
            color = Color.Gray,
            letterSpacing = (-0.374).sp
        )
    }
}

// ==================== Theme Preview Section ====================

@Composable
internal fun ThemePreviewSection(
    selectedMode: Int,
    isAuto: Boolean,
    onSelectMode: (Int) -> Unit,
    onAutoToggle: (Boolean) -> Unit,
    isLight: Boolean,
    dividerColor: Color,
    backdrop: Backdrop,
    showDividers: Boolean = true
) {
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0x99000000) else Color(0x99FFFFFF)

    Column(modifier = Modifier.fillMaxWidth()) {
        // ===== 浅色 & 深色预览卡片 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 浅色预览
            ThemePreviewCard(
                modifier = Modifier.weight(1f),
                isDark = false,
                isSelected = selectedMode == 0,
                enabled = !isAuto,
                label = "浅色",
                isLight = isLight,
                onSelect = { onSelectMode(0) })
            // 深色预览
            ThemePreviewCard(
                modifier = Modifier.weight(1f),
                isDark = true,
                isSelected = selectedMode == 1,
                enabled = !isAuto,
                label = "深色",
                isLight = isLight,
                onSelect = { onSelectMode(1) })
        }

        // ===== 分割线 =====
        if (showDividers) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = dividerColor,
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                .height(1.dp))
        }

        // ===== 自动开关行 =====
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { onAutoToggle(!isAuto) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "自动",
                fontSize = 17.sp,
                fontWeight = FontWeight.W400,
                color = textPrimary,
                letterSpacing = (-0.374).sp
            )
            LiquidToggle(
                selected = { isAuto }, onSelect = { onAutoToggle(it) }, backdrop = backdrop
            )
        }
    }
}

// ==================== Theme Preview Card ====================

@Composable
private fun ThemePreviewCard(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    label: String,
    isLight: Boolean,
    onSelect: () -> Unit
) {
    val cardBg = if (isDark) Color.Black else Color(0xFFF2F2F7)
    val surfaceColor = if (isDark) Color(0xFF2C2C2E) else Color.White
    val blockColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFC7C7CC)
    val accentBlue = ThemeSettings.getAccentColor()

    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== 手机预览图 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .clickable(enabled = enabled, onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            // 模拟手机界面
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 顶部状态栏模拟
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(blockColor.copy(alpha = 0.8f))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(blockColor.copy(alpha = 0.8f))
                        )
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(blockColor.copy(alpha = 0.8f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 内容块
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(blockColor.copy(alpha = 0.8f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(blockColor.copy(alpha = 0.7f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(surfaceColor)
                )
                Spacer(modifier = Modifier.weight(1f))
                // 底部导航栏模拟
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentBlue)
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(blockColor.copy(alpha = 0.8f))
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(blockColor.copy(alpha = 0.8f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== 标签 =====
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.W400,
            color = if (isLight) Color(0xFF1D1D1F) else Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ===== 选择圆圈 =====
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isSelected) Modifier.background(accentBlue)
                    else Modifier.border(
                        2.dp,
                        if (isLight) Color(0xFF8E8E93) else Color.White.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                )
                .clickable(enabled = enabled, onClick = onSelect),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                CheckmarkIcon(
                    color = Color.White, modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ==================== Floating Opacity Row ====================

@Composable
private fun FloatingOpacityRow(
    isLight: Boolean,
    backdrop: Backdrop
) {
    var opacity by remember { mutableFloatStateOf(ThemeSettings.floatingWindowOpacity) }
    val debounceHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "悬浮窗不透明度",
                fontSize = 17.sp,
                fontWeight = FontWeight.W400,
                color = if (isLight) Color(0xFF1D1D1F) else Color.White,
                letterSpacing = (-0.374).sp
            )
            Text(
                text = "${(opacity * 100).toInt()}%",
                fontSize = 17.sp,
                fontWeight = FontWeight.W400,
                color = if (isLight) Color(0xFF6E6E73) else Color(0xFF8E8E93),
                letterSpacing = (-0.374).sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GlassSlider(
                value = { opacity },
                onValueChange = { newOpacity ->
                    val clamped = newOpacity.coerceIn(0.3f, 1.0f)
                    opacity = clamped
                    FloatingWindowService.instance?.updateWindowOpacity(clamped)
                    // 防抖：延迟 150ms 再写 MMKV，拖动中间不写
                    debounceHandler.removeCallbacksAndMessages(null)
                    debounceHandler.postDelayed({ ThemeSettings.updateFloatingWindowOpacity(clamped) }, 150)
                },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}