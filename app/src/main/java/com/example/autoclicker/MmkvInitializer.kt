package com.example.autoclicker

import android.content.Context
import androidx.startup.Initializer
import com.tencent.mmkv.MMKV

class MmkvInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        MMKV.initialize(context)
        ThemeSettings.init()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
