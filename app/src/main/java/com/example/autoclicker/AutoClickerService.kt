package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
     * 派发一次点击，**等它真的走完**才返回。
     *
     * 为什么不 fire-and-forget：`dispatchGesture` 每派发一次新手势，系统会把上一个还没走完的手势
     * 直接取消（回调 onCancelled）。执行循环原来不等回调，speed 调高、间隔压到十几毫秒之后，
     * 后一次点击就会把前一次掐死 —— 用户看到的就是「速度越快、漏点越多」。
     *
     * @return true = 手势完整执行；false = 没派发出去，或被系统/别的服务取消（调用方可重试）
     */
    suspend fun performClickAwait(x: Int, y: Int): Boolean {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, CLICK_DURATION_MS)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return suspendCancellableCoroutine { cont ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(false)
                }
            }
            val dispatched = try {
                dispatchGesture(gesture, callback, null)
            } catch (e: Exception) {
                false
            }
            if (!dispatched) {
                // 没派发成功就不会有回调，必须自己收尾，否则协程永久挂起
                if (cont.isActive) cont.resume(false)
            }
        }
    }

    companion object {
        /** 单次点击的按压时长：1ms 是系统允许的下限，短按不会触发长按 */
        private const val CLICK_DURATION_MS = 1L

        var instance: AutoClickerService? = null
            private set
    }
}
