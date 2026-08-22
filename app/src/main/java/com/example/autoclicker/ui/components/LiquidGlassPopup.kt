package com.example.autoclicker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.autoclicker.ThemeSettings
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur as kyBlur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 通用液态玻璃弹出容器（适配自 Beiwanglu 项目）。
 *
 * 支持两种定位方式：
 * - menuAlignment + menuOffset：相对父 Box 对齐（默认）
 * - absoluteOffsetPx：绝对像素偏移（用于 picker 等固定位置场景）
 */
@Composable
fun LiquidGlassPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    menuAlignment: Alignment = Alignment.TopStart,
    menuOffset: DpOffset = DpOffset(16.dp, 8.dp),
    pivotX: Float = 0.145f,
    pivotY: Float = 0.13f,
    width: Dp = 220.dp,
    fillMaxWidth: Boolean = false,
    horizontalMargin: Dp = 16.dp,
    absoluteOffsetPx: Int? = null,
    bounceVertical: Boolean = false,
    content: @Composable () -> Unit
) {
    val isLight = !ThemeSettings.isDarkTheme()
    val glassOverlay = if (isLight) Color(0xFFFFFFFF) else Color(0xFF151515)
    val glassOverlayAlpha = 0.85f
    val blurDp = 3f
    val lensH = 20f
    val lensV = 36f

    val animScale = remember { Animatable(0f) }
    val animCornerRadius = remember { Animatable(1f) }
    val animTextBounce = remember { Animatable(1.5f) }
    val animPivotX = remember { Animatable(0f) }
    val animPivotY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(0f) }
    var contentVisible by remember { mutableStateOf(false) }

    // 实测弹窗和容器高度，用于把超出屏幕底部的弹窗上移，保证完整可见
    var menuHeightPx by remember { mutableStateOf(0) }
    var overlayHeightPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val finalCornerRadiusPx = with(density) { 44.dp.toPx() }
    val startCornerRadiusPx = with(density) { 200.dp.toPx() }
    val safeMarginPx = with(density) { 12.dp.toPx() }

    // 正向：圆形液态玻璃从中心扩散，文字弹跳回弹
    suspend fun playForward() = coroutineScope {
        animScale.snapTo(0.3f)
        animCornerRadius.snapTo(1f)
        animTextBounce.snapTo(1.5f)
        animPivotX.snapTo(pivotX)
        animPivotY.snapTo(pivotY)
        animAlpha.snapTo(1f)

        launch { animScale.animateTo(1f, spring(dampingRatio = 1f, stiffness = 550f)) }
        launch { animTextBounce.animateTo(1f, spring(dampingRatio = 1f, stiffness = 550f)) }
        launch { animPivotX.animateTo(0.5f, spring(dampingRatio = 0.7f, stiffness = 550f)) }
        launch { animPivotY.animateTo(0.5f, spring(dampingRatio = 0.85f, stiffness = 550f)) }

        delay(60)
        animCornerRadius.animateTo(0f, spring(dampingRatio = 1f, stiffness = 450f))
    }

    // 反向：原点移回按钮位置，面板缩回
    suspend fun playReverse() = coroutineScope {
        if (bounceVertical) {
            animTextBounce.snapTo(1f)
            launch { animTextBounce.animateTo(1.6f, spring(dampingRatio = 0.3f, stiffness = 400f)) }
        } else {
            animTextBounce.snapTo(1f)
        }
        launch { animCornerRadius.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 360f)) }
        launch { animPivotX.animateTo(pivotX, spring(dampingRatio = 0.7f, stiffness = 390f)) }
        launch { animPivotY.animateTo(pivotY, spring(dampingRatio = 0.7f, stiffness = 390f)) }
        launch { animAlpha.animateTo(0f, tween(durationMillis = 400)) }

        delay(40)
        animScale.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 390f))
    }

    LaunchedEffect(visible) {
        if (visible) {
            contentVisible = true
            playForward()
        } else {
            playReverse()
            contentVisible = false
        }
    }

    if (contentVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { overlayHeightPx = it.height }
        ) {
            // 半透明遮罩层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            // 菜单本体
            Box(
                modifier = Modifier
                    .then(
                        if (absoluteOffsetPx != null) {
                            Modifier.offset {
                                // 底部超出屏幕时上移，保证完整可见（横屏等矮屏场景）
                                val maxY = (overlayHeightPx - menuHeightPx - safeMarginPx).toInt().coerceAtLeast(0)
                                IntOffset(0, absoluteOffsetPx.coerceIn(0, maxY))
                            }
                        } else {
                            Modifier
                                .align(menuAlignment)
                                .offset(x = menuOffset.x, y = menuOffset.y)
                        }
                    )
                    .then(
                        if (fillMaxWidth) {
                            val isLandscape = LocalConfiguration.current.orientation ==
                                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
                            if (isLandscape) {
                                // 横屏：与页面卡片同宽（MacroDetailScreen 横屏卡片容器 360dp），水平居中
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .width(360.dp)
                                    .padding(horizontal = horizontalMargin)
                            } else {
                                Modifier.fillMaxWidth().padding(horizontal = horizontalMargin)
                            }
                        } else {
                            Modifier.width(width)
                        }
                    )
                    .onSizeChanged { menuHeightPx = it.height }
                    .graphicsLayer {
                        this.transformOrigin = TransformOrigin(animPivotX.value, animPivotY.value)
                        scaleX = animScale.value
                        scaleY = animScale.value
                        alpha = animAlpha.value
                    }
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            glassOverlay,
                            RoundedRectangle(
                                with(density) {
                                    lerp(finalCornerRadiusPx, startCornerRadiusPx, animCornerRadius.value).toDp()
                                },
                                RoundedCornerStyle.Continuous
                            )
                        )
                        .drawBackdrop(
                            backdrop,
                            shape = {
                                RoundedRectangle(
                                    with(density) {
                                        lerp(finalCornerRadiusPx, startCornerRadiusPx, animCornerRadius.value).toDp()
                                    },
                                    RoundedCornerStyle.Continuous
                                )
                            },
                            effects = {
                                kyBlur(blurDp.dp.toPx())
                                vibrancy()
                                lens(lensH.dp.toPx(), lensV.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(
                                    color = glassOverlay,
                                    alpha = glassOverlayAlpha
                                )
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .graphicsLayer {
                                val invScale = if (animScale.value > 0.01f) 1f / animScale.value else 1f
                                if (bounceVertical) {
                                    scaleX = invScale
                                    scaleY = invScale * animTextBounce.value
                                } else {
                                    scaleX = invScale * animTextBounce.value
                                    scaleY = invScale * animTextBounce.value
                                }
                            }
                            .blur((30f * (1f - animScale.value * animScale.value)).dp)
                            .padding(vertical = 4.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
