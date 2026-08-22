package com.example.autoclicker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * 引导用户开启自启动权限，覆盖主流国产 ROM。
 * 无障碍服务被关闭的根因通常是系统杀进程后没有自启动权限导致服务无法恢复。
 */
object AutoStartHelper {

    /**
     * 尝试打开厂商的自启动管理页面，失败则跳转到应用详情页。
     */
    fun openAutoStartSettings(context: Context) {
        val intent = getManufacturerIntent(context) ?: getFallbackIntent(context)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // 最终降级：跳转应用详情
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
                Toast.makeText(context, "请手动前往设置 > 应用 > 权限中开启自启动", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getManufacturerIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                // MIUI / HyperOS
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                // EMUI / HarmonyOS / MagicOS
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                }
            }
            manufacturer.contains("oppo") || manufacturer.contains("oneplus") || manufacturer.contains("realme") -> {
                // ColorOS / OxygenOS / realmeUI
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.coloros.oppoguardelf",
                        "com.coloros.oppoguardelf.AutoStartAppListActivity"
                    )
                }
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                // Funtouch OS / OriginOS
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
            }
            manufacturer.contains("samsung") -> {
                // One UI — 电池优化页面（三星没有独立的"自启动"入口）
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            }
            manufacturer.contains("meizu") -> {
                // Flyme
                Intent().apply {
                    component = android.content.ComponentName(
                        "com.meizu.safe",
                        "com.meizu.safe.permission.PermissionMainActivity"
                    )
                }
            }
            else -> null // 原生 Android / 未知厂商 → 降级
        }
    }

    private fun getFallbackIntent(context: Context): Intent {
        // 降级：先尝试通用的电池优化页面
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
    }
}
