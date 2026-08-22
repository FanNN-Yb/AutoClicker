# AutoClicker 悬浮窗自动点击器

基于 Android 无障碍服务（AccessibilityService）手势派发的自动点击工具：录制屏幕坐标生成宏，通过悬浮窗一键回放，支持循环执行、变速与多方案管理。

## 功能特性

- 悬浮窗常驻，随时录制 / 回放点击宏
- 支持重复次数、执行速度调节
- 录制点可视化标注，宏方案统一管理
- iOS 风格液态玻璃 UI（Jetpack Compose + Backdrop）
- 浅色 / 深色主题自适应，自定义强调色
- Baseline Profile 优化冷启动

## 系统要求

- Android 8.0（API 26）及以上
- 需要授予无障碍服务权限与悬浮窗权限

## 下载安装

前往 [Releases](https://github.com/FanNN-Yb/AutoClicker/releases) 下载最新 APK。

## 本地构建

1. 使用 Android Studio（JDK 21）打开项目
2. Debug 包直接运行；Release 包需在项目根目录创建 `keystore.properties`（已被 .gitignore 排除）：

   ```properties
   storeFile=release-key.jks
   storePassword=你的密码
   keyAlias=你的别名
   keyPassword=你的密码
   ```

3. 执行 `./gradlew assembleRelease`，产物位于 `app/build/outputs/apk/release/`

## 免责声明

本工具仅供学习交流与个人效率辅助使用，请勿用于破坏其他应用或服务的正常运行、游戏作弊等违反规则的场景，由此产生的一切后果由使用者自行承担。

## 开源许可

本项目基于 [GPL-3.0](LICENSE) 协议开源。

你可以自由地使用、学习、修改和分发本项目，但任何基于本项目的衍生作品必须同样以 GPL-3.0 协议开源并保留原作者署名。

Copyright (c) 2026 FanNN-Yb
