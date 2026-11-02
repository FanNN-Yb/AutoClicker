package com.example.autoclicker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.example.autoclicker.ui.AppGlasmioTheme
import com.example.autoclicker.ui.screens.MacroDetailScreen
import io.github.fannn_yb.rubberband.overscroll.rememberOffsetOverscrollFactory

class MacroDetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MACRO_NAME = "macro_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val macroName = intent.getStringExtra(EXTRA_MACRO_NAME) ?: run {
            finish()
            return
        }

        // 从 MMKV 加载宏配置
        val macros = loadMacros()
        val macro = macros.find { it.name == macroName }
        if (macro == null) {
            finish()
            return
        }

        setContent {
            AppGlasmioTheme {
            val overscrollFactory = rememberOffsetOverscrollFactory(
                orientation = Orientation.Vertical
            )
            CompositionLocalProvider(
                LocalOverscrollFactory provides overscrollFactory
            ) {
                MacroDetailScreen(
                    macro = macro,
                    onBack = { finish() },
                    onSave = { updated ->
                        // 必须用**原名**定位：改名后新名字在库里还不存在
                        val saved = MacroRepository.updateMacro(macroName, updated)
                        if (saved != null && saved.name != updated.name.trim()) {
                            // 重名被自动编号，得告诉用户，不然下次找不到自己起的名字
                            Toast.makeText(
                                this@MacroDetailActivity,
                                "名称重复，已保存为「${saved.name}」",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                    }
                )
            }
            }
        }
    }

    private fun loadMacros(): List<MacroConfig> {
        return MacroRepository.loadAll()
    }
}
