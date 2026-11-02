package com.example.autoclicker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.autoclicker.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.autoclicker.ThemeSettings
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.glass.progressiveBlurBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.layerBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.example.autoclicker.ui.components.RubberBandContainer
import com.example.autoclicker.ui.components.rememberRubberBandState
import io.github.neoruaa.glasmio.glass.scrollEdgeContentFade

/**
 * 致谢条目，包含名称和描述，点击可打开对应链接。
 */
private data class CreditItem(
    val name: String,
    val description: String,
    val url: String
)

private val creditItems = listOf(
    // 开源库
    CreditItem("AndroidX Core", "提供基础兼容层", "https://developer.android.com/jetpack/androidx"),
    CreditItem("Jetpack Compose", "声明式 UI 框架", "https://developer.android.com/compose"),
    CreditItem("Material3", "Material Design 3 组件", "https://m3.material.io"),
    CreditItem("MMKV (Tencent)", "高性能键值存储", "https://github.com/Tencent/MMKV"),
    CreditItem("Glasmio", "iOS 27 液态玻璃组件库", "https://github.com/NEORUAA/Glasmio"),
    CreditItem("AndroidLiquidGlass", "液态玻璃折射与光学实现", "https://github.com/Kyant0/AndroidLiquidGlass"),
    CreditItem("Shapes (Kyant)", "连续圆角形状", "https://github.com/kyant0/shapes"),
    CreditItem("Kotlin Coroutines", "异步编程框架", "https://github.com/Kotlin/kotlinx.coroutines"),
    CreditItem("Kotlin Immutable", "不可变集合", "https://github.com/Kotlin/kotlinx.collections.immutable"),
    CreditItem("AppCompat", "Android 向后兼容", "https://developer.android.com/jetpack/androidx/releases/appcompat"),
    CreditItem("Navigation Compose", "Compose 导航组件", "https://developer.android.com/guide/navigation"),
    CreditItem("App Startup", "优化组件初始化", "https://developer.android.com/topic/libraries/app-startup"),
    CreditItem("Profile Installer", "加速冷启动", "https://developer.android.com/topic/performance/baselineprofiles"),
)

@Composable
fun CreditsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLight = !ThemeSettings.isDarkTheme()
    val bg = if (isLight) Color(0xFFF2F2F7) else Color(0xFF000000)
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0x99000000) else Color(0x99FFFFFF)
    val cardBg = if (isLight) Color.White else Color(0xFF1C1C1E)
    val dividerColor = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    val cardBorder = if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
    val accentColor = ThemeSettings.getAccentColor()

    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    // 内容层：给顶部渐进式模糊采样
    val contentBackdrop = rememberLayerBackdrop()
    val rubberState = rememberRubberBandState()
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    var titleTopY by remember { mutableFloatStateOf(0f) }
    var titleBottomY by remember { mutableFloatStateOf(0f) }

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()
    val titleBarHeightPx = with(density) { 68.dp.toPx() }
    val maskBottomPx = statusBarHeightPx + titleBarHeightPx

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

    val maskHeightDp = with(density) { maskBottomPx.toDp() }
    // 遮罩高度 = 顶栏下沿（原来 ×1.3，模糊尾巴压到了第一条内容上）
    val maskVisualHeightDp = maskHeightDp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 纯色背景层：只录背景，返回按钮在层外采样（层内含采样者会渲染递归崩溃）
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(bg)
        )
        val isLandscape = maxWidth > maxHeight
        val horizontalPadding = if (isLandscape) ((maxWidth - 360.dp) / 2f).coerceAtLeast(20.dp) else 20.dp
        RubberBandContainer(
            state = rubberState,
            modifier = Modifier
                .fillMaxSize()
                // 与宏配置页一致：录制层里既要有内容、也要有背景色
                .layerBackdrop(contentBackdrop)
                .background(bg)
                .scrollEdgeContentFade(
                    progress = 1f,
                    headerHeight = maskHeightDp,
                    headerTop = with(density) { statusBarHeightPx.toDp() }
                )
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = maskHeightDp + 12.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item(key = "_big_title_") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 28.dp)
                        .graphicsLayer {
                            alpha = (1f - titleHidden * 2f).coerceIn(0f, 1f)
                        }
                        .onGloballyPositioned { coords ->
                            titleTopY = coords.positionInRoot().y
                            titleBottomY = coords.positionInRoot().y + coords.size.height
                        }
                ) {
                    Text(
                        text = "致谢",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "贡献者与开源库",
                        fontSize = 14.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            item(key = "section_credits") {
                GlasmioGroupedList {
                    val showDiv = ThemeSettings.showDividers
                    creditItems.forEachIndexed { index, item ->
                        GlasmioListRow(
                            title = item.name,
                            showTopSeparator = showDiv && index != 0,
                            // 箭头换回 App 原来的图标和大小
                            showDisclosure = false,
                            trailing = { OriginalChevron(isLight) },
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    // 无法打开链接时静默忽略
                                }
                            }
                        )
                    }
                }
            }

            item(key = "spacer_bottom") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        } // RubberBandContainer

        // 顶部渐进式模糊（替代原来的渐变遮罩）
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(maskVisualHeightDp)
                .zIndex(1f)
                .progressiveBlurBackdrop(
                    backdrop = contentBackdrop,
                    progress = 1f,
                    maxRadius = 10.dp
                )
        )

        // 顶部小标题 + 返回按钮
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .zIndex(2f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlasmioIconButton(
                onClick = onBack,
                backdrop = backdrop,
                modifier = Modifier.size(44.dp)
            ) {
                // 与库 demo 的返回按钮一致：SfSymbol.ChevronBack + 玻璃按钮自带的前景色
                SfIcon(
                    symbol = SfSymbol.ChevronBack,
                    contentDescription = "返回",
                    mirrored = true
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.alpha(((titleHidden - 0.5f) * 2f).coerceIn(0f, 1f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "致谢",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "贡献者与开源库",
                        fontSize = 11.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 0.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun CreditRow(
    name: String,
    description: String,
    isLight: Boolean,
    showDivider: Boolean,
    dividerColor: Color,
    onClick: () -> Unit
) {
    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = if (isLight) Color(0x99000000) else Color(0x99FFFFFF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = name,
            fontSize = 17.sp,
            fontWeight = FontWeight.W400,
            color = textPrimary,
            letterSpacing = (-0.374).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            fontWeight = FontWeight.W400,
            color = textSecondary
        )
    }

    if (showDivider) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(0.5.dp)
                .background(dividerColor)
        )
    }
}
