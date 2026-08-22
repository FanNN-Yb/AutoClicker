package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutoClickerService : AccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, but required
    }

    override fun onInterrupt() {
        // Not used, but required
    }

    /**
     * 返回 true 以支持 onRebind，确保服务断开后系统可以重新绑定。
     * 这是无障碍服务"关闭后不需要重新打开"的关键。
     */
    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return true // 允许 onRebind
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /**
     * 执行一次点击手势。
     */
    fun performClick(x: Int, y: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 使用单个点的 Path 触发点击手势
            val path = Path().apply {
                moveTo(x.toFloat(), y.toFloat())
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 1L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        } else {
            Log.w("AutoClicker", "performClick requires API 24+, current: ${Build.VERSION.SDK_INT}")
        }
    }

    companion object {
        var instance: AutoClickerService? = null
            private set
    }
}
