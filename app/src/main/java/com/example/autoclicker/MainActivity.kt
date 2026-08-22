package com.example.autoclicker

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import io.github.fannn_yb.rubberband.overscroll.rememberOffsetOverscrollFactory
import com.example.autoclicker.ui.LiquidBottomTab
import com.example.autoclicker.ui.LiquidBottomTabs
import com.example.autoclicker.ui.LiquidSideTab
import com.example.autoclicker.ui.LiquidSideTabs
import com.example.autoclicker.ui.screens.HomeScreen
import com.example.autoclicker.ui.screens.MacroConfigScreen
import com.example.autoclicker.ui.screens.SettingsScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule

class MainActivity : ComponentActivity() {

    private var refreshAccessibility: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 浅色模式：状态栏/导航栏图标为黑色；深色模式：白色
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            var refreshKey by remember { mutableIntStateOf(0) }
            val accessibilityEnabled = remember(refreshKey) { isAccessibilityServiceEnabled() }
            val serviceRunning = remember(refreshKey) { isFloatingWindowServiceRunning() }

            // Expose refresh to Activity
            this@MainActivity.refreshAccessibility = { refreshKey++ }

            val isDark = ThemeSettings.isDarkTheme()
            val colorScheme = remember(isDark) {
                if (isDark) {
                    darkColorScheme(
                        background = Color(0xFF000000),
                        surface = Color(0xFF000000),
                        onBackground = Color.White,
                        onSurface = Color.White
                    )
                } else {
                    lightColorScheme(
                        background = Color(0xFFF5F5F7),
                        surface = Color(0xFFF5F5F7),
                        onBackground = Color(0xFF1D1D1F),
                        onSurface = Color(0xFF1D1D1F)
                    )
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                val overscrollFactory = rememberOffsetOverscrollFactory(Orientation.Vertical)
                CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    MainScreen(
                        onOpenAccessibility = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                        },
                        onStartService = {
                            // 如果服务已在运行，则停止它
                            if (FloatingWindowService.instance != null) {
                                val serviceIntent = Intent(
                                    this@MainActivity,
                                    FloatingWindowService::class.java
                                )
                                stopService(serviceIntent)
                                // 延迟刷新 UI，等待 Service onDestroy 完成
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    refreshAccessibility?.invoke()
                                }, 200)
                                return@MainScreen
                            }

                            if (!Settings.canDrawOverlays(this@MainActivity)) {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.overlay_permission_required,
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                                return@MainScreen
                            }

                            if (!isAccessibilityServiceEnabled()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.accessibility_not_enabled,
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@MainScreen
                            }

                            val serviceIntent = Intent(
                                this@MainActivity,
                                FloatingWindowService::class.java
                            )
                            startForegroundService(serviceIntent)
                            moveTaskToBack(true)
                        },
                        onOpenBatteryOptimization = {
                            val pm = getSystemService(POWER_SERVICE) as PowerManager
                            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = android.net.Uri.parse("package:$packageName")
                                }
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "已关闭电池优化",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onOpenAutoStart = {
                            AutoStartHelper.openAutoStartSettings(this@MainActivity)
                        },
                        isAccessibilityEnabled = accessibilityEnabled,
                        isServiceRunning = serviceRunning
                    )
                }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAccessibility?.invoke()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val target = ComponentName(this, AutoClickerService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { entry ->
            val cn = ComponentName.unflattenFromString(entry) ?: return@any false
            cn.packageName == target.packageName && cn.className == target.className
        }
    }

    /** 检查 FloatingWindowService 是否确实在运行 */
    private fun isFloatingWindowServiceRunning(): Boolean {
        return FloatingWindowService.instance != null
    }
}

// ==================== 转场动画定义 ====================

// 快进慢出曲线：前半段快速响应，后半段柔和停下
private val iosEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

// ==================== 主屏幕 ====================

@Composable
fun MainScreen(
    onOpenAccessibility: () -> Unit,
    onStartService: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenAutoStart: () -> Unit,
    isAccessibilityEnabled: Boolean,
    isServiceRunning: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var macroDimAlpha by remember { mutableFloatStateOf(0f) }
    var navDimDismissTrigger by remember { mutableIntStateOf(0) }
    // 从首页提示文字跳转设置页时触发"权限"区块高亮闪烁
    var permissionHighlightTrigger by remember { mutableIntStateOf(0) }

    val isLightTheme = !ThemeSettings.isDarkTheme()
    val contentColor = remember(isLightTheme) { if (isLightTheme) Color.Black else Color.White }
    val contentDimColor = remember(isLightTheme) { if (isLightTheme) Color.Black else Color.White }
    val bgColor = remember(isLightTheme) { if (isLightTheme) Color(0xFFF5F5F7) else Color(0xFF000000) }
    val backdrop = rememberLayerBackdrop()

    val context = androidx.compose.ui.platform.LocalContext.current

    // 返回键：非首页时回到首页
    androidx.activity.compose.BackHandler(enabled = selectedTabIndex != 0) {
        selectedTabIndex = 0
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val navIsLandscape = maxWidth > maxHeight
        // 记录已访问过的 tab，首次访问后常驻内存不销毁
        val visitedTabs = remember { mutableStateOf(setOf(0)) }
        LaunchedEffect(selectedTabIndex) {
            visitedTabs.value = visitedTabs.value + selectedTabIndex
        }

        val screenW = LocalConfiguration.current.screenWidthDp.dp
        val slideSpec = tween<androidx.compose.ui.unit.Dp>(250, easing = iosEaseOut)

        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(bgColor)
        ) {
            // 页面 0：首页（最左边）
            if (0 in visitedTabs.value) {
                val isActive = selectedTabIndex == 0
                val offsetX by animateDpAsState(
                    if (isActive) 0.dp else if (selectedTabIndex > 0) -screenW else screenW,
                    slideSpec, label = "slide0"
                )
                Box(
                    Modifier.fillMaxSize()
                        .zIndex(if (isActive) 1f else 0f)
                        .graphicsLayer { translationX = offsetX.toPx() }
                        .background(bgColor)
                ) {
                    // 每个页面独立提供 overscroll 工厂：RubberBand 的 effect 是单例、渲染节点只能挂一个列表，
                    // 三个页面共用一个工厂会导致位移只画在其中一个列表上（见 RubberBand OffsetOverscrollFactory）。
                    CompositionLocalProvider(LocalOverscrollFactory provides rememberOffsetOverscrollFactory(Orientation.Vertical)) {
                        HomeScreen(
                            onOpenAccessibility = onOpenAccessibility,
                            onStartService = { onStartService() },
                            onNavigateToSettings = {
                                selectedTabIndex = 2
                                permissionHighlightTrigger++
                            },
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            isServiceRunning = isServiceRunning
                        )
                    }
                }
            }

            // 页面 1：宏配置（中间）
            if (1 in visitedTabs.value) {
                val isActive = selectedTabIndex == 1
                val offsetX by animateDpAsState(
                    if (isActive) 0.dp else if (selectedTabIndex > 1) -(screenW) else (screenW),
                    slideSpec, label = "slide1"
                )
                Box(
                    Modifier.fillMaxSize()
                        .zIndex(if (isActive) 1f else 0f)
                        .graphicsLayer { translationX = offsetX.toPx() }
                        .background(bgColor)
                ) {
                    CompositionLocalProvider(LocalOverscrollFactory provides rememberOffsetOverscrollFactory(Orientation.Vertical)) {
                        MacroConfigScreen(
                            onDimAlphaChange = { macroDimAlpha = it },
                            navDimDismissTrigger = navDimDismissTrigger
                        )
                    }
                }
            }

            // 页面 2：设置（最右边）
            if (2 in visitedTabs.value) {
                val isActive = selectedTabIndex == 2
                val offsetX by animateDpAsState(
                    if (isActive) 0.dp else if (selectedTabIndex > 2) -(screenW) else (screenW),
                    slideSpec, label = "slide2"
                )
                Box(
                    Modifier.fillMaxSize()
                        .zIndex(if (isActive) 1f else 0f)
                        .graphicsLayer { translationX = offsetX.toPx() }
                        .background(bgColor)
                ) {
                    CompositionLocalProvider(LocalOverscrollFactory provides rememberOffsetOverscrollFactory(Orientation.Vertical)) {
                        SettingsScreen(
                            onOpenBatteryOptimization = onOpenBatteryOptimization,
                            onOpenAutoStart = onOpenAutoStart,
                            onNavigateToTheme = {
                                context.startActivity(
                                    android.content.Intent(context, ThemeActivity::class.java)
                                )
                            },
                            onNavigateToCredits = {
                                context.startActivity(
                                    android.content.Intent(context, CreditsActivity::class.java)
                                )
                            },
                            permissionHighlightTrigger = permissionHighlightTrigger
                        )
                    }
                }
            }
        }

        // Navigation
        if (navIsLandscape) {
            // 横屏：右侧竖向导航
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidSideTabs(
                    selectedTabIndex = { selectedTabIndex },
                    onTabSelected = { selectedTabIndex = it },
                    backdrop = backdrop,
                    tabsCount = 3
                ) {
                    LiquidSideTab({ selectedTabIndex = 0 }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_house),
                            contentDescription = "首页",
                            modifier = Modifier.size(24.dp),
                            tint = if (selectedTabIndex == 0) contentColor
                            else contentDimColor
                        )
                        Text(
                            "首页",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTabIndex == 0) contentColor
                            else contentDimColor
                        )
                    }

                    LiquidSideTab({ selectedTabIndex = 1 }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_slider),
                            contentDescription = "宏配置",
                            modifier = Modifier.size(24.dp),
                            tint = if (selectedTabIndex == 1) contentColor
                            else contentDimColor
                        )
                        Text(
                            "宏配置",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTabIndex == 1) contentColor
                            else contentDimColor
                        )
                    }

                    LiquidSideTab({ selectedTabIndex = 2 }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_gear),
                            contentDescription = "设置",
                            modifier = Modifier.size(24.dp),
                            tint = if (selectedTabIndex == 2) contentColor
                            else contentDimColor
                        )
                        Text(
                            "设置",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTabIndex == 2) contentColor
                            else contentDimColor
                        )
                    }
                }
            }

            // 弹窗时侧边栏遮罩
            if (macroDimAlpha > 0.05f) {
                val dimColor = if (isLightTheme) Color(0xFF29293A).copy(0.1f) else Color(0xFF121212).copy(0.56f)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer { alpha = macroDimAlpha }
                            .width(64.dp)
                            .fillMaxHeight(0.75f)
                            .clip(Capsule())
                            .background(dimColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { navDimDismissTrigger++ }
                    )
                }
            }
        } else {
            // 竖屏：底部横向导航
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedTabIndex },
                    onTabSelected = { selectedTabIndex = it },
                    backdrop = backdrop,
                    tabsCount = 3,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                ) {
                    LiquidBottomTab({ selectedTabIndex = 0 }) {

                        Icon(
                            painter = painterResource(R.drawable.ic_house),
                            contentDescription = "首页",
                            modifier = Modifier.size(28.dp),
                            tint = if (selectedTabIndex == 0) contentColor
                            else contentDimColor
                        )
                        Text(
                            "首页",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTabIndex == 0) contentColor
                            else contentDimColor
                        )
                    }

                    LiquidBottomTab({ selectedTabIndex = 1 }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_slider),
                            contentDescription = "宏配置",
                            modifier = Modifier.size(28.dp),
                            tint = if (selectedTabIndex == 1) contentColor
                            else contentDimColor
                        )
                        Text(
                            "宏配置",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTabIndex == 1) contentColor
                            else contentDimColor
                        )
                    }

                    LiquidBottomTab({ selectedTabIndex = 2 }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_gear),
                            contentDescription = "设置",
                            modifier = Modifier.size(28.dp),
                            tint = if (selectedTabIndex == 2) contentColor
                            else contentDimColor
                        )
                        Text(
                            "设置",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTabIndex == 2) contentColor
                            else contentDimColor
                        )
                    }
                }
            }

            // 弹窗时底部标签栏遮罩（和 LiquidBottomTabs 相同尺寸、相同位置）
            if (macroDimAlpha > 0.05f) {
                val dimColor = if (isLightTheme) Color(0xFF29293A).copy(0.1f) else Color(0xFF121212).copy(0.56f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                        .graphicsLayer { alpha = macroDimAlpha }
                        .height(64.dp)
                        .fillMaxWidth()
                        .padding(0.dp)
                        .clip(Capsule())
                        .background(dimColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { navDimDismissTrigger++ }
                )
            }
        }
    }
}