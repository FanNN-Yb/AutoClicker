package com.example.autoclicker.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import com.example.autoclicker.FloatingWindowService
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.autoclicker.ui.components.LiquidGlassIconButton
import com.example.autoclicker.R
import com.example.autoclicker.ThemeSettings
import com.example.autoclicker.ui.components.LiquidToggle
import com.example.autoclicker.ui.components.CheckmarkIcon

@Composable
fun ThemePreviewScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isLight = !ThemeSettings.isDarkTheme()
    val bg = if (isLight) Color(0xFFF2F2F7) else Color(0xFF000000)
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0x99000000) else Color(0x99FFFFFF)
    val cardBg = if (isLight) Color.White else Color(0xFF1C1C1E)
    val dividerColor = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    val cardBorder = if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)

    val backdrop = rememberLayerBackdrop()
    val density = LocalDensity.current

    // 实测 LazyColumn 视口高度和内容高度，动态计算 Spacer
    var viewportHeightPx by remember { mutableStateOf(0) }
    var contentHeightPx by remember { mutableStateOf(0) }

    val themeMode = ThemeSettings.themeMode
    val themeAuto = ThemeSettings.themeAuto
    val useCustomTheme = ThemeSettings.useCustomTheme
    val selectedColorIndex = ThemeSettings.selectedColorIndex

    // 遮罩高度
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val titleBarHeightPx = with(density) { 56.dp.toPx() }
    val maskBottomPx = statusBarHeightPx + titleBarHeightPx
    val maskHeightDp = with(density) { maskBottomPx.toDp() }
    val maskVisualHeightDp = with(density) { (maskBottomPx * 1.3f).toDp() }

    val contentPaddingVertical = (maskHeightDp - 21.dp) + 165.dp
    val viewportContentArea = with(density) { viewportHeightPx.toDp() } - contentPaddingVertical
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
    ) {
        val isLandscape = maxWidth > maxHeight
        val horizontalPadding = if (isLandscape) ((maxWidth - 360.dp) / 2f).coerceAtLeast(20.dp) else 20.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .onGloballyPositioned { coords ->
                    viewportHeightPx = coords.size.height
                },
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = maskHeightDp - 21.dp,
                bottom = 165.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        contentHeightPx = coords.size.height
                    }
                ) {
                // 预览卡片
                SettingsCard(cardBg, cardBorder) {
                    ThemePreviewSection(
                        selectedMode = themeMode,
                        isAuto = themeAuto,
                        onSelectMode = {
                            if (!themeAuto) {
                                ThemeSettings.updateThemeMode(it)
                                // 实时更新悬浮窗主题
                                FloatingWindowService.instance?.updateWindowTheme()
                            }
                        },
                        onAutoToggle = {
                            ThemeSettings.updateThemeAuto(it)
                            // 实时更新悬浮窗主题
                            FloatingWindowService.instance?.updateWindowTheme()
                        },
                        isLight = isLight,
                        dividerColor = dividerColor,
                        showDividers = com.example.autoclicker.ThemeSettings.showDividers,
                        backdrop = backdrop
                    )
                }

                // 说明
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "自动模式将跟随系统深浅色设置切换主题。",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    color = textSecondary.copy(alpha = 0.5f),
                    letterSpacing = (-0.12).sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // 自定义主题色开关
                Spacer(modifier = Modifier.height(24.dp))
                SettingsCard(cardBg, cardBorder) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "自定义主题色",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W400,
                            color = textPrimary,
                            letterSpacing = (-0.374).sp
                        )
                        LiquidToggle(
                            selected = { useCustomTheme },
                            onSelect = { ThemeSettings.updateUseCustomTheme(it) },
                            backdrop = backdrop
                        )
                    }
                }

                // 颜色选择卡片
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.alpha(if (useCustomTheme) 1f else 0.4f)) {
                    SettingsCard(cardBg, cardBorder) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 标题行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "颜色",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.W400,
                                    color = textPrimary,
                                    letterSpacing = (-0.374).sp
                                )
                                Text(
                                    text = ThemeSettings.presetColors[selectedColorIndex].name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.W400,
                                    color = textSecondary,
                                    letterSpacing = (-0.2).sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 颜色圆点行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThemeSettings.presetColors.forEachIndexed { index, themeColor ->
                                    val isSelected = index == selectedColorIndex
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(themeColor.color)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(3.dp, if (isLight) Color(0xFFF2F2F7) else textPrimary, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable(enabled = useCustomTheme) { ThemeSettings.updateSelectedColorIndex(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            CheckmarkIcon(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
                // 动态补位 Spacer：实测视口高 - 内容高 + 1dp，填满屏幕且滚动范围 > 0 触发橡皮筋
                Spacer(modifier = Modifier.height(spacerHeight))
            }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp)
                .zIndex(60f),
            contentAlignment = Alignment.Center
        ) {
                        LiquidGlassIconButton(
                onClick = onBack,
                backdrop = backdrop,
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
                text = "主题",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                letterSpacing = (-0.2).sp
            )
        }
    }
}
