package com.example.autoclicker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.res.painterResource
import com.example.autoclicker.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.example.autoclicker.ThemeSettings
import kotlin.math.absoluteValue
import kotlin.math.sin

/**
 * iOS 风格的 3D 轮子数字选择器
 */
@Composable
fun NumberPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: List<Float>,
    label: String,
    isLight: Boolean,
    modifier: Modifier = Modifier,
    backdropParam: com.kyant.backdrop.Backdrop? = null
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var hasEmittedInitialSelection by remember { mutableStateOf(false) }

    val textPrimary = if (isLight) Color(0xFF1D1D1F) else Color.White
    val textSecondary = Color(0xFF999999)

    val items = remember(range) {
        range.map { v ->
            if (v == v.toInt().toFloat()) {
                v.toInt().toString()
            } else {
                String.format("%.2f", v).trimEnd('0').trimEnd('.')
            }
        }
    }

    val currentIndex = remember(value, range) {
        range.indexOf(value).coerceAtLeast(0)
    }

    // 矮屏（横屏手机等）减少可见行数，避免弹窗总高超出屏幕
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val visibleCount = if (screenHeightDp < 480) 5 else 7
    val halfCount = visibleCount / 2
    val itemHeight = 40.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    val maxRotationDeg = 90f
    val maxAngleRad = Math.toRadians(maxRotationDeg.toDouble()).toFloat()
    val geometricHalfCount = halfCount + 1
    val wheelExtentPx = (geometricHalfCount * itemHeightPx).coerceAtLeast(1f)
    val radiusPx = wheelExtentPx / maxAngleRad

    fun distanceToAngleRad(distanceToCenter: Float): Float {
        val normalized = (distanceToCenter / wheelExtentPx).coerceIn(-1f, 1f)
        return normalized * maxAngleRad
    }

    fun mapToWheelY(distanceToCenter: Float): Float {
        val angle = distanceToAngleRad(distanceToCenter)
        return sin(angle) * radiusPx
    }

    val visibleHalfLinearPx = halfCount * itemHeightPx
    val visibleHalfArcPx = mapToWheelY(visibleHalfLinearPx).absoluteValue
    val wheelContainerHeight = with(density) {
        (itemHeightPx + (visibleHalfArcPx * 2f)).toDp()
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centeredIndex by remember(listState, items) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf currentIndex
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            layoutInfo.visibleItemsInfo
                .minByOrNull { visible ->
                    val itemCenter = visible.offset + visible.size / 2f
                    mapToWheelY(itemCenter - viewportCenter).absoluteValue
                }
                ?.index
                ?.let { index -> index.coerceIn(0, items.lastIndex) }
                ?: currentIndex
        }
    }

    LaunchedEffect(centeredIndex) {
        if (hasEmittedInitialSelection) {
            onValueChange(range[centeredIndex])
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else {
            hasEmittedInitialSelection = true
        }
    }

    LaunchedEffect(currentIndex, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        val targetIndex = currentIndex.coerceIn(0, items.lastIndex)
        if (targetIndex != centeredIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    val indicatorColor = if (isLight) Color(0xFFE5E5EA) else Color(0xFF2C2C2E)

    Box(
        modifier = modifier
            .height(wheelContainerHeight)
            .clipToBounds()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                        .drawWithContent {
                            drawContent()
                            val outline = cardShape.createOutline(size, layoutDirection, this)
                            drawOutline(outline, indicatorColor)
                            drawRoundRect(
                                color = indicatorColor.copy(alpha = 0.15f),
                                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                                style = Fill
                            )
                        }
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.requiredHeight((visibleCount * itemHeight.value).dp),
            contentPadding = PaddingValues(vertical = (halfCount * itemHeight.value).dp)
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val layoutInfo = listState.layoutInfo
                            val visibleInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                            if (visibleInfo != null) {
                                val itemCenterY = visibleInfo.offset + (visibleInfo.size / 2f)
                                val viewportCenterY = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val distanceToCenter = itemCenterY - viewportCenterY

                                val angleRad = distanceToAngleRad(distanceToCenter)
                                val wheelY = mapToWheelY(distanceToCenter)
                                val falloff = (angleRad / maxAngleRad).absoluteValue

                                translationY = wheelY - distanceToCenter
                                rotationX = -Math.toDegrees(angleRad.toDouble()).toFloat()

                                scaleX = 1f - (falloff * 0.15f)
                                scaleY = 1f - (falloff * 0.15f)
                                alpha = 1f - falloff
                                cameraDistance = 4f * density.density
                            } else {
                                alpha = 0f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = centeredIndex == index
                    Text(
                        text = items[index],
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isSelected) textPrimary else textSecondary
                    )
                }
            }
        }
    }
}

/**
 * 内联数字选择器（展开在选项下方，非弹窗）
 */
@Composable
fun NumberPickerInline(
    value: Float,
    onValueChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    range: List<Float>,
    label: String,
    isLight: Boolean,
    modifier: Modifier = Modifier,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    cornerAnimProgress: Float = 0f,
    scaleAnim: Float = 1f,
    bounceAnim: Float = 1f
) {
    val cardBg = if (isLight) Color.White else Color(0xFF1C1C1E)
    val pickerDensity = LocalDensity.current
    val targetRadiusPx = with(pickerDensity) { 32.dp.toPx() }
    val startRadiusPx = with(pickerDensity) { 180.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = {
                            RoundedRectangle(
                                with(pickerDensity) { lerp(targetRadiusPx, startRadiusPx, cornerAnimProgress).toDp() },
                                RoundedCornerStyle.Continuous
                            )
                        },
                        effects = {
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = { drawRect(cardBg.copy(alpha = 0.5f)) }
                    )
                } else {
                    Modifier
                        .clip(RoundedRectangle(32.dp, RoundedCornerStyle.Continuous))
                        .background(cardBg)
                }
            )
            .padding(16.dp)
            .graphicsLayer {
                val invScale = if (scaleAnim > 0.01f) 1f / scaleAnim else 1f
                scaleX = invScale * bounceAnim
                scaleY = invScale * bounceAnim
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NumberPickerInlineContent(
            value = value,
            onValueChange = onValueChange,
            onCancel = onCancel,
            onConfirm = onConfirm,
            range = range,
            label = label,
            isLight = isLight,
            backdrop = backdrop ?: com.kyant.backdrop.backdrops.emptyBackdrop()
        )
    }
}

/**
 * Picker 内部内容（无玻璃外壳），供 LiquidGlassPopup 使用。
 */
@Composable
fun NumberPickerInlineContent(
    value: Float,
    onValueChange: (Float) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    range: List<Float>,
    label: String,
    isLight: Boolean,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val accentColor = ThemeSettings.getAccentColor()
    val textSecondary = if (isLight) Color(0xFF1D1D1F) else Color.White
    var pendingValue by remember(value) { mutableFloatStateOf(value) }

    val buttonSize = 44.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),  // 卡片左右边距 = 圆角44dp - 按钮半宽22dp
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头部：SpaceBetween 模式下按钮自然位于左右两端
        // 22dp 的 Column padding 使按钮中心正好在 44dp(=圆角半径)处，与圆角同心
        // padding(top=18dp) 将按钮垂直下移使其竖直中心对齐圆角圆心（44dp - 4dp - 22dp = 18dp）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 关闭按钮 — 左上角
            LiquidGlassIconButton(
                onClick = onCancel,
                backdrop = backdrop,
                modifier = Modifier.size(buttonSize)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_xmark),
                    contentDescription = "取消",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(textSecondary)
                )
            }

            // 标题 — 居中
            Text(
                text = label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLight) Color(0xFF1D1D1F) else Color.White
            )

            // 确定按钮 — 右上角
            LiquidGlassIconButton(
                onClick = { onValueChange(pendingValue); onConfirm() },
                backdrop = backdrop,
                tint = accentColor,
                modifier = Modifier.size(buttonSize)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_checkmark),
                    contentDescription = "确定",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NumberPicker(
            value = pendingValue,
            onValueChange = { pendingValue = it },
            range = range,
            label = label,
            isLight = isLight
        )
    }
}
