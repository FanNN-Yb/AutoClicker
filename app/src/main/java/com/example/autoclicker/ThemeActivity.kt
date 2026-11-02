package com.example.autoclicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.example.autoclicker.ui.AppGlasmioTheme
import com.example.autoclicker.ui.screens.ThemePreviewScreen
import io.github.fannn_yb.rubberband.overscroll.rememberOffsetOverscrollFactory

class ThemeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            AppGlasmioTheme {
            val overscrollFactory = rememberOffsetOverscrollFactory(
                orientation = Orientation.Vertical
            )
            CompositionLocalProvider(
                LocalOverscrollFactory provides overscrollFactory
            ) {
                ThemePreviewScreen(
                    onBack = { finish() }
                )
            }
            }
        }
    }
}
