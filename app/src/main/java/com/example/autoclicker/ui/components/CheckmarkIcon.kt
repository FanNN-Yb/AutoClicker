package com.example.autoclicker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * SF Pro 风格对勾图标（来自 Cupertino Icons checkmark.svg）
 * viewBox: 0 0 17.6199 17.4086
 * Path 在 remember 中缓存，仅在尺寸变化时重建，避免每帧重复创建。
 */
@Composable
fun CheckmarkIcon(
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val path = remember { Path() }
    var lastWidth by remember { mutableFloatStateOf(-1f) }
    var lastHeight by remember { mutableFloatStateOf(-1f) }

    Canvas(modifier = modifier) {
        if (size.width != lastWidth || size.height != lastHeight) {
            lastWidth = size.width
            lastHeight = size.height
            path.reset()
            val scaleX = size.width / 17.6199f
            val scaleY = size.height / 17.4086f
            path.apply {
                moveTo(6.60448f * scaleX, 17.4086f * scaleY)
                cubicTo(
                    7.11229f * scaleX, 17.4086f * scaleY,
                    7.51287f * scaleX, 17.1967f * scaleY,
                    7.79119f * scaleX, 16.7721f * scaleY
                )
                lineTo(16.9705f * scaleX, 2.46229f * scaleY)
                cubicTo(
                    17.1762f * scaleX, 2.14042f * scaleY,
                    17.2586f * scaleX, 1.87109f * scaleY,
                    17.2586f * scaleX, 1.60605f * scaleY
                )
                cubicTo(
                    17.2586f * scaleX, 0.933592f * scaleY,
                    16.7857f * scaleX, 0.469534f * scaleY,
                    16.1062f * scaleX, 0.469534f * scaleY
                )
                cubicTo(
                    15.6242f * scaleX, 0.469534f * scaleY,
                    15.3443f * scaleX, 0.631642f * scaleY,
                    15.052f * scaleX, 1.09062f * scaleY
                )
                lineTo(6.56542f * scaleX, 14.5731f * scaleY)
                lineTo(2.19217f * scaleX, 8.95f * scaleY)
                cubicTo(
                    1.9076f * scaleX, 8.57305f * scaleY,
                    1.60897f * scaleX, 8.41524f * scaleY,
                    1.1871f * scaleX, 8.41524f * scaleY
                )
                cubicTo(
                    0.489839f * scaleX, 8.41524f * scaleY,
                    0f, 8.90332f * scaleY,
                    0f, 9.57754f * scaleY
                )
                cubicTo(
                    0f, 9.86718f * scaleY,
                    0.108203f * scaleX, 10.1598f * scaleY,
                    0.351755f * scaleX, 10.4557f * scaleY
                )
                lineTo(5.41133f * scaleX, 16.7859f * scaleY)
                cubicTo(
                    5.74824f * scaleX, 17.2072f * scaleY,
                    6.11249f * scaleX, 17.4086f * scaleY,
                    6.60448f * scaleX, 17.4086f * scaleY
                )
                close()
            }
        }
        drawPath(path = path, color = color)
    }
}
