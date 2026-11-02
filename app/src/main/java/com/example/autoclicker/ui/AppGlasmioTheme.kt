package com.example.autoclicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.autoclicker.R
import com.example.autoclicker.ThemeSettings
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.glasmioColorScheme
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver
import io.github.neoruaa.glasmio.icons.MaterialGlasmioIconResolver

/**
 * 全应用统一的 Glasmio 主题入口（每个 Activity 都要包一层）。
 *
 * 两点很关键：
 * 1. `GlasmioTheme` 必须存在。缺了它 `LocalGlassColors` 会退回到库里的默认配色
 *    （accent = #FF3B30 红色），于是 Prominent 按钮、图标按钮的玻璃全被染成红色。
 * 2. accent 显式传 `glasmioColorScheme(isDark, 主题色)`，让染色玻璃跟随 App 主题色。
 *
 * 图标：库里的 SfSymbol 通过 resolver 映射回项目自己的矢量图（ic_house / ic_slider /
 * ic_gear / ic_chevron_left），保持原来的视觉，不用 Material 图标顶替。
 */
@Composable
fun AppGlasmioTheme(content: @Composable () -> Unit) {
    val isDark = ThemeSettings.isDarkTheme()
    val accent = ThemeSettings.getAccentColor()

    val houseIcon = ImageVector.vectorResource(R.drawable.ic_house)
    val sliderIcon = ImageVector.vectorResource(R.drawable.ic_slider)
    val gearIcon = ImageVector.vectorResource(R.drawable.ic_gear)
    val backIcon = ImageVector.vectorResource(R.drawable.ic_chevron_left)

    GlasmioIconProvider(
        resolver = GlasmioIconResolver { systemName ->
            when {
                systemName.startsWith("house") -> houseIcon
                systemName.startsWith("square.3.layers") || systemName.startsWith("slider") -> sliderIcon
                systemName.startsWith("gear") -> gearIcon
                systemName.startsWith("chevron.left") || systemName.startsWith("chevron.backward") -> backIcon
                else -> MaterialGlasmioIconResolver.resolve(systemName)
            }
        }
    ) {
        GlasmioTheme(
            isDark = isDark,
            colorScheme = glasmioColorScheme(isDark, accent)
        ) {
            content()
        }
    }
}
