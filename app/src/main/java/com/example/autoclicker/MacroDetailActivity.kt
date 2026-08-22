package com.example.autoclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
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
                        saveMacro(updated)
                        finish()
                    }
                )
            }
        }
    }

    private fun loadMacros(): List<MacroConfig> {
        return MacroRepository.loadAll()
    }

    private fun saveMacro(updated: MacroConfig) {
        MacroRepository.updateMacro(updated)
    }
}
