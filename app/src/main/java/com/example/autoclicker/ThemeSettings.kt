package com.example.autoclicker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.tencent.mmkv.MMKV

/**
 * 主题设置管理器，使用 MMKV 持久化
 */
object ThemeSettings {
    private val mmkv: MMKV get() = MMKV.defaultMMKV()

    /** 预设主题颜色 */
    data class ThemeColor(val name: String, val color: Color)

    val presetColors = listOf(
        ThemeColor("蓝色", Color(0xFF007AFF)),
        ThemeColor("橙色", Color(0xFFFF9500)),
        ThemeColor("绿色", Color(0xFF34C759)),
        ThemeColor("黄色", Color(0xFFFFCC00)),
        ThemeColor("红色", Color(0xFFFF3B30))
    )

    /** 0=浅色, 1=深色 */
    var themeMode by mutableIntStateOf(0)
        private set

    /** 是否跟随系统 */
    var themeAuto by mutableStateOf(true)
        private set

    /** 是否使用自定义主题色 */
    var useCustomTheme by mutableStateOf(false)
        private set

    /** 选中的预设颜色索引 */
    var selectedColorIndex by mutableIntStateOf(0)
        private set

    /** 震动反馈 */
    var vibrateEnabled by mutableStateOf(true)
        private set

    /** 显示分割线 */
    var showDividers by mutableStateOf(true)
        private set

    /** 悬浮窗不透明度 (0.3 ~ 1.0) */
    var floatingWindowOpacity by mutableFloatStateOf(0.80f)
        private set

    /** 默认强调色 */
    private val defaultAccent = Color(0xFF0071E3)

    /** 获取当前强调色 */
    fun getAccentColor(): Color {
        return if (useCustomTheme) {
            presetColors.getOrElse(selectedColorIndex) { ThemeColor("蓝色", defaultAccent) }.color
        } else {
            defaultAccent
        }
    }

    fun init() {
        themeMode = mmkv.decodeInt("theme_mode", 0)
        themeAuto = mmkv.decodeBool("theme_auto", true)
        useCustomTheme = mmkv.decodeBool("use_custom_theme", false)
        selectedColorIndex = mmkv.decodeInt("selected_color_index", 0)
        vibrateEnabled = mmkv.decodeBool("vibrate_enabled", true)
        showDividers = mmkv.decodeBool("show_dividers", true)
        floatingWindowOpacity = mmkv.decodeFloat("floating_window_opacity", 0.80f)
    }

    /** 根据设置返回是否应该使用深色模式 */
    @Composable
    fun isDarkTheme(): Boolean {
        return if (themeAuto) {
            isSystemInDarkTheme()
        } else {
            themeMode == 1
        }
    }

    fun updateThemeMode(mode: Int) {
        themeMode = mode
        mmkv.encode("theme_mode", mode)
    }

    fun updateThemeAuto(auto: Boolean) {
        themeAuto = auto
        mmkv.encode("theme_auto", auto)
    }

    fun updateUseCustomTheme(use: Boolean) {
        useCustomTheme = use
        mmkv.encode("use_custom_theme", use)
    }

    fun updateSelectedColorIndex(index: Int) {
        selectedColorIndex = index
        mmkv.encode("selected_color_index", index)
    }

    fun updateVibrateEnabled(enabled: Boolean) {
        vibrateEnabled = enabled
        mmkv.encode("vibrate_enabled", enabled)
    }

    fun updateShowDividers(show: Boolean) {
        showDividers = show
        mmkv.encode("show_dividers", show)
    }

    fun updateFloatingWindowOpacity(opacity: Float) {
        floatingWindowOpacity = opacity.coerceIn(0.3f, 1.0f)
        mmkv.encode("floating_window_opacity", floatingWindowOpacity)
    }
}
