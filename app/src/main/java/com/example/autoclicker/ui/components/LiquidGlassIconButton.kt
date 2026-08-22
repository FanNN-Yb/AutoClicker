package com.example.autoclicker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.autoclicker.ThemeSettings
import com.example.autoclicker.ui.utils.InteractiveHighlight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 液态玻璃圆形图标按钮 — 与 AndroidLiquidGlass 2.0.0 库 LiquidButton 完全一致
 * modifier 顺序：drawBackdrop → clickable → ih.modifier → ih.gestureModifier
 */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = !ThemeSettings.isDarkTheme()
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val containerColor = if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f)

    // 严格按库中 LiquidButton 的 modifier 顺序
    Box(
        modifier = modifier
            .size(44.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(22.dp) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height

                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)

                                        val maxOffset = size.minDimension * 0.4f
                                                            val initialDerivative = 0.04f
                    val offset = interactiveHighlight.offset
                                        translationX = (maxOffset * tanh(initialDerivative * offset.x / maxOffset)).toFloat()
                    translationY = (maxOffset * tanh(initialDerivative * offset.y / maxOffset)).toFloat()

                    val maxDragScale = 4f.dp.toPx() / height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX =
                        scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).coerceAtMost(1f)
                    scaleY =
                        scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).coerceAtMost(1f)
                },
                                                                                                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint)
                    } else {
                        drawRect(if (isLight) Color.White else Color.Black)
                    }
                }
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                enabled = enabled
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
        content = content
    )
}
