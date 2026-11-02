package com.example.autoclicker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.LocalGlassAppearance
import io.github.neoruaa.glasmio.glass.NavigationGlassMaterial
import io.github.neoruaa.glasmio.glass.navigationGlassBackground
import io.github.neoruaa.glasmio.glass.rememberNavigationInnerShadows
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.glass.render.ProvideGlassMaterialContext
import io.github.neoruaa.glasmio.glass.render.rememberGlassMaterialContext
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * 36 号参考动画的 Compose 复刻（抓帧文件：桌面/菜单动画说明.html、液态玻璃菜单-四角按钮.html）。
 *
 * 和库里 GlasmioMenu 的入场动画（scale 0.2→1 + 整体模糊）完全不是一回事：
 * 这里动的是**矩形本身**——左/上/右/下四条边和圆角各自按关键帧走，
 * 内容全程保持终态尺寸，只是被这个「越来越大的窗口」裁出来。
 * 所以永远不能用 scale() 去缩放面板：那样圆角比例会一起缩，圆角就「不动」了。
 *
 * 关键帧来自 62 帧真机抓取，open 453ms / close 454ms，close **不是** open 的倒放，
 * 它自己有一套独立的曲线，所以两张表都要照抄。
 */

private const val OpenDurationMs = 453f
private const val CloseDurationMs = 454f

/** 面板终态圆角（和库的 menuShape 一致），关键帧里的 r/F.r 是它的倍数。 */
private val PanelBaseRadius = 34.dp

/** 内容与面板内边距（库的 GlasmioMenu 也是 10dp）。 */
private val PanelContentInset = 10.dp

/**
 * 一帧的面板几何。
 */
private class PanelFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val progress: Float,
)

/**
 * 面板终态矩形 + 锚点。所有量都是像素，坐标系 = 窗口坐标（本 App 是 edge-to-edge，窗口 = 屏幕）。
 */
private class PanelLayout(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val baseRadius: Float,
    /** 种子点（首帧中心）→ 锚点中心的校正量，p=1 时归零。 */
    val anchorDeltaX: Float,
    val anchorDeltaY: Float,
    /** 面板在锚点下方：顶边钉住、内容顶部对齐。 */
    val flipY: Boolean,
    /** 面板在锚点左侧：左边钉住。 */
    val flipX: Boolean,
)

/**
 * 玻璃菜单弹窗：页面内的全屏浮层（**不开独立窗口**），用关键帧驱动面板矩形。
 *
 * @param expanded 目标状态。true 播 open，false 播 close，close 播完才真正移除窗口。
 * @param anchorBounds 锚点（数值胶囊）在**窗口坐标**里的矩形，用来做种子点校正。
 * @param content 面板内容；`Backdrop` 是面板自己导出的玻璃层，内部玻璃按钮采它才和面板同色。
 */
@Composable
fun GlassMenuPopup(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    anchorBounds: Rect,
    panelWidth: Dp,
    panelHeight: Dp,
    backdrop: Backdrop,
    content: @Composable (Backdrop) -> Unit,
) {
    var alive by remember { mutableStateOf(false) }
    var opening by remember { mutableStateOf(true) }
    var timeMs by remember { mutableFloatStateOf(0f) }

    // 目标状态 → 阶段。关闭动画播完前不能把窗口拆掉，否则会「啪」一下消失。
    LaunchedEffect(expanded) {
        if (expanded) {
            alive = true
            opening = true
            timeMs = 0f
        } else if (alive) {
            opening = false
            timeMs = 0f
        }
    }

    // 逐帧推时间轴：直接采样关键帧，不走任何插值器（曲线的过冲全在表里）。
    LaunchedEffect(alive, opening) {
        if (!alive) return@LaunchedEffect
        val duration = if (opening) OpenDurationMs else CloseDurationMs
        var startNs = 0L
        var done = false
        while (!done) {
            withFrameNanos { now ->
                if (startNs == 0L) startNs = now
                val elapsed = (now - startNs) / 1_000_000f
                timeMs = elapsed.coerceAtMost(duration)
                if (elapsed >= duration) done = true
            }
        }
        if (!opening) alive = false
    }

    if (!alive) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 安全区用真实系统栏 inset，别写死：横屏时状态栏只有二十几 dp，写死 52 会把面板往下挤
    val statusTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    val layout = remember(
        anchorBounds, panelWidth, panelHeight, screenWidthPx, screenHeightPx,
        statusTopPx, navBottomPx,
    ) {
        resolveLayout(
            anchorBounds = anchorBounds,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            panelWidthPx = with(density) { panelWidth.toPx() },
            panelHeightPx = with(density) { panelHeight.toPx() },
            baseRadiusPx = with(density) { PanelBaseRadius.toPx() },
            gapPx = with(density) { 10.dp.toPx() },
            marginPx = with(density) { 12.dp.toPx() },
            safeTopPx = statusTopPx + with(density) { 8.dp.toPx() },
            safeBottomPx = navBottomPx + with(density) { 8.dp.toPx() },
        )
    }

    val frame = frameAt(if (opening) openFrames else closeFrames, timeMs, layout)
    val panelAlpha = (frame.progress / 0.10f).coerceIn(0f, 1f)
    // 内容静止在终态位置：面板窗口怎么动、怎么长，都不影响它
    val insetPx = with(density) { PanelContentInset.toPx() }
    val contentLeft = layout.left + insetPx
    val contentTop = layout.top + insetPx

    // 面板玻璃：和 GlasmioMenu 用的是同一套材质，只是形状/尺寸由关键帧给。
    // 不再开独立窗口，直接采页面那一层（同窗口采样最准，也少一层矩阵换算）。
    val samplingBackdrop = backdrop
    val panelBackdrop = rememberLayerBackdrop()
    val materialContext = rememberGlassMaterialContext(inheritLuminance = false)
    val innerShadows = rememberNavigationInnerShadows(true)
    val appearance = LocalGlassAppearance.current
    val colors = materialContext.colors

    // 用页面内的浮层，**不开 Popup 窗口**：独立窗口一出现，页面那一侧的帧时钟就停了，
    // 胶囊的「下沉淡出」于是卡在半路（实测：面板自己动得好好的，数字停在中间不动）。
    // 现在面板和胶囊共用同一个帧时钟，两条动画一起走。返回键由页面里的 BackHandler 负责。
    var originInWindow by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            // 必须压在标题栏之上：页面顶部那条标题栏/遮罩是 zIndex 50/60 的，
            // 横屏窗口只有 332dp 高，面板顶边会钻到它底下被切掉一截。
            .zIndex(100f)
            .onGloballyPositioned { originInWindow = it.localToWindow(Offset.Zero) }
    ) {
        Box(Modifier.fillMaxSize()) {
            // 点击面板外收起。放在面板前面 = 在面板下层，不会抢面板里的手势。
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null) {
                        onExpandedChange(false)
                    }
            )

            if (panelAlpha <= 0.002f) return@Box

            val widthPx = frame.width.coerceAtLeast(1f)
            val heightPx = frame.height.coerceAtLeast(1f)
            val radiusDp = with(density) { frame.radius.toDp() }
            val shape = ContinuousRoundedRectangle(radiusDp)

            Box(
                Modifier
                    // 浮层不在窗口原点（外层可能有 padding），坐标要减掉它自己的窗口位置
                    .offset {
                        IntOffset(
                            (frame.left - originInWindow.x).roundToInt(),
                            (frame.top - originInWindow.y).roundToInt(),
                        )
                    }
                    .size(
                        width = with(density) { widthPx.toDp() },
                        height = with(density) { heightPx.toDp() },
                    )
                    .graphicsLayer { alpha = panelAlpha }
                    // 🔴 这里**不能**再包一层 drawWithContent 裁剪：
                    // 那刀会把库画在形状外侧的外阴影 / 内环一起裁掉，面板就变成贴在页面上的平板，
                    // 和「确认删除」弹窗（GlasmioAlertSurface，不做外部裁剪）的玻璃观感不一致。
                    // 轮廓只交给 shape 参数，库自己会沿着圆角画；内容溢出由下面那层单独裁。
                    .navigationGlassBackground(
                        backdrop = samplingBackdrop,
                        exportedBackdrop = panelBackdrop,
                        shape = { shape },
                        containerColor = colors.container,
                        navigationMaterial = true,
                        innerShadows = innerShadows,
                        isDark = colors.isDark,
                        appearance = appearance,
                        materialContext = materialContext,
                        // 用回原来的 RegularMedium（5dp 霜化 + 外阴影 + 内环）。
                        // Clear 那一档太透，面板边界几乎看不见，用户看着不对，已换回。
                        blurRadius = NavigationGlassMaterial.RegularMedium.frostRadius,
                        material = NavigationGlassMaterial.RegularMedium,
                        shadowAlpha = 1f,
                        highlightAlphaMultiplier = 1f,
                    )
            ) {
                // 内容层：只裁这一层。动画中面板窗口比内容小，不裁的话内容和雾面会溢出圆角；
                // 玻璃本体不参与这刀，外阴影才能像删除弹窗那样留在形状外侧。
                Box(
                    Modifier
                        .matchParentSize()
                        .drawWithContent { clipToShape(shape) { this@drawWithContent.drawContent() } }
                ) {
                    // 雾面：进度越低越白，把刚长出来的小窗口盖成一片奶白，而不是一块窗玻璃。
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val haze = 0.07f * (1f - frame.progress)
                                if (haze > 0.004f) {
                                    drawRect(Color(0xFFE4E8F0).copy(alpha = haze))
                                }
                            }
                    )

                    // 内容：**终态尺寸 + 终态位置**，全程不动，只是被面板窗口逐渐裁出来。
                    // 关键是 requiredSize：普通 size() 会被父容器（此刻只有十几 dp）的约束挤压，
                    // 内容于是每帧重新布局一次，看起来就是“内容在不停重建位置”。
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    (contentLeft - frame.left).roundToInt(),
                                    (contentTop - frame.top).roundToInt(),
                                )
                            }
                            .requiredSize(
                                width = panelWidth - PanelContentInset * 2,
                                height = panelHeight - PanelContentInset * 2,
                            )
                            .graphicsLayer {
                                val rest = 1f - frame.progress
                                // 微缩放：内容起步比终态大 35%，三次方收敛，最后 20% 基本不动
                                val zoom = 1f + 0.35f * rest * rest * rest
                                scaleX = zoom
                                scaleY = zoom
                                transformOrigin = TransformOrigin(0.5f, if (layout.flipY) 0f else 1f)
                                // 内容在 p≈2% 才突然出现，6 个点内淡满
                                alpha = ((frame.progress - 0.02f) / 0.06f).coerceIn(0f, 1f)
                                // 虚化随窗口一起收干净；模糊半径按面板尺寸缩过，参考里是 18px/217px 宽
                                val blurPx = with(density) { 12.dp.toPx() } * rest.pow(1.3f)
                                renderEffect = if (blurPx > 0.8f) {
                                    BlurEffect(blurPx, blurPx, TileMode.Clamp)
                                } else {
                                    null
                                }
                            }
                    ) {
                        ProvideGlassMaterialContext(materialContext) {
                            content(panelBackdrop)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 按形状裁剪。形状是在**绘制**时读的，所以动画里每帧的圆角都会真实生效；
 * 库自己的玻璃材质只保证材质画在形状里，轮廓还需要这一刀。
 */
private fun DrawScope.clipToShape(shape: Shape, block: DrawScope.() -> Unit) {
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> clipRect(
            left = outline.rect.left,
            top = outline.rect.top,
            right = outline.rect.right,
            bottom = outline.rect.bottom,
            block = block,
        )

        is Outline.Rounded -> clipPath(
            path = Path().apply { addRoundRect(outline.roundRect) },
            block = block,
        )

        is Outline.Generic -> clipPath(path = outline.path, block = block)
    }
}

/**
 * 面板终态矩形 + 种子点校正。
 *
 * 参考里的定位规则：非镜像时右边缘、底边钉在锚点附近，镜像时改钉左边/顶边；
 * 首帧那小块「种子」的中心再被整体平移到锚点中心（校正量随 p 线性归零）。
 */
private fun resolveLayout(
    anchorBounds: Rect,
    screenWidthPx: Float,
    screenHeightPx: Float,
    panelWidthPx: Float,
    panelHeightPx: Float,
    baseRadiusPx: Float,
    gapPx: Float,
    marginPx: Float,
    safeTopPx: Float,
    safeBottomPx: Float,
): PanelLayout {
    val left = ((screenWidthPx - panelWidthPx) / 2f)
        .coerceIn(marginPx, (screenWidthPx - panelWidthPx - marginPx).coerceAtLeast(marginPx))

    val aboveTop = anchorBounds.top - gapPx - panelHeightPx
    val belowTop = anchorBounds.bottom + gapPx
    val flipY: Boolean
    val rawTop: Float
    if (aboveTop >= safeTopPx) {
        rawTop = aboveTop
        flipY = false
    } else if (belowTop + panelHeightPx <= screenHeightPx - safeBottomPx) {
        rawTop = belowTop
        flipY = true
    } else {
        // 两边都塞不下（小屏 / 横屏）：夹在安全区里居中，靠种子校正把起点拉到胶囊上
        rawTop = ((safeTopPx + (screenHeightPx - safeBottomPx) - panelHeightPx) / 2f)
            .coerceAtLeast(safeTopPx)
        flipY = false
    }

    // 兜底：不管走哪条分支，整块面板都必须完整落在屏幕里。
    // 横屏高度只有 400dp 左右，以前这里不夹底边，面板下沿会被裁掉一截。
    val maxTop = (screenHeightPx - safeBottomPx - panelHeightPx).coerceAtLeast(0f)
    val top = rawTop.coerceIn(0f, maxTop)

    val flipX = anchorBounds.center.x < left + panelWidthPx / 2f

    // 种子点 = open 首帧；它的中心被平移到锚点中心，p→1 时偏移归零
    val seed = sampleFrame(openFrames, 0f)
    val placement = seedRect(
        ox = seed[1], oy = seed[2], w = seed[3], h = seed[4],
        left = left, top = top, width = panelWidthPx, height = panelHeightPx,
        flipX = flipX, flipY = flipY,
    )
    val deltaX = anchorBounds.center.x - (placement[0] + placement[1]) / 2f
    val deltaY = anchorBounds.center.y - (placement[2] + placement[3]) / 2f

    return PanelLayout(
        left = left,
        top = top,
        width = panelWidthPx,
        height = panelHeightPx,
        baseRadius = baseRadiusPx,
        anchorDeltaX = deltaX,
        anchorDeltaY = deltaY,
        flipY = flipY,
        flipX = flipX,
    )
}

/** 返回 [left, right, top, bottom]。 */
private fun seedRect(
    ox: Float,
    oy: Float,
    w: Float,
    h: Float,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    flipX: Boolean,
    flipY: Boolean,
): FloatArray {
    val l: Float
    val r: Float
    if (flipX) {
        l = left - ox * width
        r = l + w * width
    } else {
        r = left + width + ox * width
        l = r - w * width
    }
    val t: Float
    val b: Float
    if (flipY) {
        t = top - oy * height
        b = t + h * height
    } else {
        b = top + height + oy * height
        t = b - h * height
    }
    return floatArrayOf(l, r, t, b)
}

private fun frameAt(frames: FloatArray, timeMs: Float, layout: PanelLayout): PanelFrame {
    val v = sampleFrame(frames, timeMs)
    val rect = seedRect(
        ox = v[1], oy = v[2], w = v[3], h = v[4],
        left = layout.left, top = layout.top,
        width = layout.width, height = layout.height,
        flipX = layout.flipX, flipY = layout.flipY,
    )
    val correction = (1f - v[6]).coerceIn(0f, 1f)
    val dx = layout.anchorDeltaX * correction
    val dy = layout.anchorDeltaY * correction
    return PanelFrame(
        left = rect[0] + dx,
        top = rect[2] + dy,
        width = rect[1] - rect[0],
        height = rect[3] - rect[2],
        radius = layout.baseRadius * v[5],
        progress = v[6],
    )
}

/**
 * 在关键帧表里采样。每行 7 个：t(ms)、左偏移、底偏移、宽比、高比、圆角倍数、进度。
 * 表里已经归一化过（偏移/尺寸都除以终态矩形），所以换面板尺寸只改乘法，不用重抓。
 */
private fun sampleFrame(frames: FloatArray, timeMs: Float): FloatArray {
    val count = frames.size / 7
    var lo = 0
    var hi = count - 1
    if (timeMs <= frames[0]) {
        hi = 0
    } else if (timeMs >= frames[(count - 1) * 7]) {
        lo = count - 1
        hi = count - 1
    } else {
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (frames[mid * 7] <= timeMs) lo = mid else hi = mid
        }
    }
    val a = lo * 7
    val b = hi * 7
    val span = frames[b] - frames[a]
    val u = if (span <= 0f) 0f else ((timeMs - frames[a]) / span).coerceIn(0f, 1f)
    val out = FloatArray(7)
    out[0] = timeMs
    for (i in 1 until 7) {
        out[i] = frames[a + i] + (frames[b + i] - frames[a + i]) * u
    }
    return out
}

private val openFrames = floatArrayOf(
    0.0000f, 0.0547f, -0.0452f, 0.0552f, 0.0384f, 0.1747f, 0.0080f, 5.0000f, 0.0539f, -0.0613f, 0.0693f, 0.0528f, 0.2195f, 0.0300f, 11.0000f, 0.0526f, -0.0823f, 0.0917f, 0.0756f, 0.2904f, 0.0620f, 17.0000f, 0.0509f, -0.1050f, 0.1209f, 0.1053f, 0.3829f, 0.1020f,
    24.0000f, 0.0489f, -0.1271f, 0.1557f, 0.1407f, 0.4929f, 0.1480f, 29.0000f, 0.0467f, -0.1471f, 0.1937f, 0.1794f, 0.6132f, 0.1960f, 35.0000f, 0.0443f, -0.1648f, 0.2345f, 0.2209f, 0.7422f, 0.2440f, 41.0000f, 0.0418f, -0.1804f, 0.2784f, 0.2656f, 0.8813f, 0.2950f,
    48.0000f, 0.0392f, -0.1935f, 0.3230f, 0.3110f, 1.0225f, 0.3440f, 55.0000f, 0.0365f, -0.2045f, 0.3690f, 0.3578f, 1.1680f, 0.3930f, 61.0000f, 0.0340f, -0.2133f, 0.4136f, 0.4032f, 1.3092f, 0.4390f, 66.0000f, 0.0314f, -0.2202f, 0.4585f, 0.4489f, 1.4493f, 0.4830f,
    73.0000f, 0.0288f, -0.2241f, 0.5027f, 0.4939f, 1.5679f, 0.5260f, 78.0000f, 0.0263f, -0.2250f, 0.5460f, 0.5379f, 1.6632f, 0.5660f, 85.0000f, 0.0239f, -0.2233f, 0.5871f, 0.5797f, 1.7357f, 0.6030f, 91.0000f, 0.0216f, -0.2194f, 0.6273f, 0.6207f, 1.7908f, 0.6390f,
    97.0000f, 0.0194f, -0.2138f, 0.6647f, 0.6588f, 1.8279f, 0.6710f, 103.0000f, 0.0173f, -0.2066f, 0.7008f, 0.6955f, 1.8510f, 0.7010f, 109.0000f, 0.0154f, -0.1984f, 0.7341f, 0.7294f, 1.8612f, 0.7290f, 115.0000f, 0.0136f, -0.1893f, 0.7653f, 0.7611f, 1.8608f, 0.7540f,
    120.0000f, 0.0119f, -0.1794f, 0.7947f, 0.7910f, 1.8512f, 0.7770f, 127.0000f, 0.0103f, -0.1692f, 0.8217f, 0.8186f, 1.8340f, 0.7990f, 133.0000f, 0.0089f, -0.1585f, 0.8470f, 0.8442f, 1.8101f, 0.8190f, 139.0000f, 0.0075f, -0.1478f, 0.8698f, 0.8675f, 1.7812f, 0.8360f,
    145.0000f, 0.0063f, -0.1372f, 0.8905f, 0.8886f, 1.7485f, 0.8530f, 151.0000f, 0.0053f, -0.1267f, 0.9093f, 0.9077f, 1.7130f, 0.8670f, 157.0000f, 0.0043f, -0.1164f, 0.9264f, 0.9251f, 1.6747f, 0.8810f, 163.0000f, 0.0034f, -0.1064f, 0.9416f, 0.9406f, 1.6352f, 0.8930f,
    169.0000f, 0.0026f, -0.0966f, 0.9554f, 0.9547f, 1.5940f, 0.9040f, 175.0000f, 0.0019f, -0.0874f, 0.9675f, 0.9669f, 1.5534f, 0.9140f, 181.0000f, 0.0013f, -0.0787f, 0.9779f, 0.9775f, 1.5134f, 0.9230f, 187.0000f, 0.0007f, -0.0703f, 0.9872f, 0.9870f, 1.4731f, 0.9310f,
    194.0000f, 0.0003f, -0.0627f, 0.9951f, 0.9950f, 1.4349f, 0.9380f, 199.0000f, -0.0001f, -0.0554f, 1.0019f, 1.0020f, 1.3970f, 0.9450f, 205.0000f, -0.0004f, -0.0486f, 1.0077f, 1.0078f, 1.3611f, 0.9500f, 212.0000f, -0.0007f, -0.0423f, 1.0125f, 1.0128f, 1.3264f, 0.9560f,
    218.0000f, -0.0010f, -0.0365f, 1.0166f, 1.0168f, 1.2932f, 0.9600f, 224.0000f, -0.0011f, -0.0313f, 1.0196f, 1.0200f, 1.2629f, 0.9650f, 229.0000f, -0.0013f, -0.0265f, 1.0221f, 1.0225f, 1.2340f, 0.9680f, 236.0000f, -0.0014f, -0.0221f, 1.0240f, 1.0245f, 1.2064f, 0.9720f,
    242.0000f, -0.0015f, -0.0183f, 1.0253f, 1.0258f, 1.1817f, 0.9750f, 248.0000f, -0.0015f, -0.0148f, 1.0261f, 1.0266f, 1.1586f, 0.9780f, 254.0000f, -0.0015f, -0.0116f, 1.0266f, 1.0271f, 1.1371f, 0.9800f, 260.0000f, -0.0015f, -0.0089f, 1.0266f, 1.0271f, 1.1177f, 0.9820f,
    266.0000f, -0.0015f, -0.0065f, 1.0264f, 1.0269f, 1.1001f, 0.9840f, 272.0000f, -0.0015f, -0.0044f, 1.0259f, 1.0264f, 1.0845f, 0.9860f, 278.0000f, -0.0015f, -0.0025f, 1.0252f, 1.0257f, 1.0699f, 0.9880f, 284.0000f, -0.0014f, -0.0010f, 1.0243f, 1.0248f, 1.0572f, 0.9890f,
    289.0000f, -0.0014f, 0.0003f, 1.0233f, 1.0237f, 1.0460f, 0.9900f, 296.0000f, -0.0013f, 0.0014f, 1.0222f, 1.0226f, 1.0360f, 0.9910f, 301.0000f, -0.0012f, 0.0023f, 1.0210f, 1.0214f, 1.0273f, 0.9920f, 307.0000f, -0.0012f, 0.0030f, 1.0198f, 1.0201f, 1.0196f, 0.9930f,
    314.0000f, -0.0011f, 0.0035f, 1.0185f, 1.0188f, 1.0131f, 0.9940f, 320.0000f, -0.0010f, 0.0039f, 1.0172f, 1.0175f, 1.0076f, 0.9950f, 327.0000f, -0.0009f, 0.0042f, 1.0159f, 1.0162f, 1.0029f, 0.9950f, 332.0000f, -0.0009f, 0.0044f, 1.0147f, 1.0149f, 0.9991f, 0.9960f,
    344.0000f, -0.0007f, 0.0045f, 1.0122f, 1.0124f, 0.9934f, 0.9970f, 356.0000f, -0.0006f, 0.0043f, 1.0099f, 1.0101f, 0.9902f, 0.9970f, 368.0000f, -0.0005f, 0.0039f, 1.0079f, 1.0080f, 0.9888f, 0.9980f, 386.0000f, -0.0003f, 0.0031f, 1.0053f, 1.0054f, 0.9890f, 0.9990f,
    410.0000f, -0.0002f, 0.0019f, 1.0026f, 1.0027f, 0.9921f, 0.9990f, 453.0000f, 0.0000f, 0.0000f, 1.0000f, 1.0000f, 1.0000f, 1.0000f,
)

private val closeFrames = floatArrayOf(
    0.0000f, 0.0002f, -0.0095f, 0.9969f, 0.9968f, 1.0191f, 0.9910f, 6.0000f, 0.0006f, -0.0271f, 0.9891f, 0.9889f, 1.0369f, 0.9690f, 13.0000f, 0.0014f, -0.0495f, 0.9768f, 0.9764f, 1.0645f, 0.9360f, 18.0000f, 0.0023f, -0.0738f, 0.9607f, 0.9600f, 1.1011f, 0.8960f,
    25.0000f, 0.0034f, -0.0976f, 0.9414f, 0.9403f, 1.1449f, 0.8510f, 30.0000f, 0.0046f, -0.1196f, 0.9196f, 0.9182f, 1.1941f, 0.8040f, 36.0000f, 0.0061f, -0.1403f, 0.8948f, 0.8930f, 1.2503f, 0.7530f, 42.0000f, 0.0076f, -0.1582f, 0.8686f, 0.8663f, 1.3098f, 0.7040f,
    47.0000f, 0.0092f, -0.1740f, 0.8407f, 0.8379f, 1.3729f, 0.6540f, 54.0000f, 0.0109f, -0.1879f, 0.8112f, 0.8079f, 1.4398f, 0.6060f, 61.0000f, 0.0127f, -0.1996f, 0.7811f, 0.7772f, 1.5081f, 0.5590f, 66.0000f, 0.0145f, -0.2096f, 0.7503f, 0.7459f, 1.5778f, 0.5140f,
    73.0000f, 0.0162f, -0.2173f, 0.7197f, 0.7147f, 1.6471f, 0.4730f, 79.0000f, 0.0181f, -0.2233f, 0.6883f, 0.6828f, 1.7182f, 0.4330f, 86.0000f, 0.0199f, -0.2276f, 0.6569f, 0.6509f, 1.7893f, 0.3950f, 91.0000f, 0.0217f, -0.2302f, 0.6259f, 0.6193f, 1.8596f, 0.3610f,
    97.0000f, 0.0234f, -0.2315f, 0.5953f, 0.5881f, 1.8844f, 0.3280f, 104.0000f, 0.0252f, -0.2315f, 0.5652f, 0.5575f, 1.7893f, 0.2980f, 109.0000f, 0.0269f, -0.2304f, 0.5356f, 0.5274f, 1.6954f, 0.2710f, 115.0000f, 0.0285f, -0.2283f, 0.5068f, 0.4981f, 1.6042f, 0.2450f,
    121.0000f, 0.0302f, -0.2255f, 0.4789f, 0.4697f, 1.5161f, 0.2220f, 127.0000f, 0.0318f, -0.2218f, 0.4511f, 0.4414f, 1.4281f, 0.2000f, 133.0000f, 0.0333f, -0.2176f, 0.4250f, 0.4149f, 1.3455f, 0.1810f, 139.0000f, 0.0347f, -0.2129f, 0.3999f, 0.3892f, 1.2659f, 0.1630f,
    146.0000f, 0.0362f, -0.2076f, 0.3750f, 0.3640f, 1.1873f, 0.1470f, 151.0000f, 0.0375f, -0.2021f, 0.3517f, 0.3402f, 1.1133f, 0.1320f, 157.0000f, 0.0388f, -0.1963f, 0.3293f, 0.3174f, 1.0423f, 0.1190f, 163.0000f, 0.0400f, -0.1903f, 0.3084f, 0.2961f, 0.9762f, 0.1070f,
    170.0000f, 0.0412f, -0.1840f, 0.2876f, 0.2749f, 0.9104f, 0.0960f, 176.0000f, 0.0423f, -0.1778f, 0.2686f, 0.2556f, 0.8502f, 0.0860f, 181.0000f, 0.0434f, -0.1714f, 0.2502f, 0.2368f, 0.7919f, 0.0770f, 188.0000f, 0.0444f, -0.1651f, 0.2332f, 0.2196f, 0.7382f, 0.0690f,
    193.0000f, 0.0454f, -0.1587f, 0.2167f, 0.2028f, 0.6862f, 0.0620f, 200.0000f, 0.0462f, -0.1525f, 0.2015f, 0.1873f, 0.6379f, 0.0550f, 206.0000f, 0.0471f, -0.1461f, 0.1868f, 0.1724f, 0.5914f, 0.0490f, 213.0000f, 0.0478f, -0.1402f, 0.1736f, 0.1590f, 0.5496f, 0.0440f,
    217.0000f, 0.0486f, -0.1342f, 0.1609f, 0.1460f, 0.5094f, 0.0390f, 225.0000f, 0.0493f, -0.1284f, 0.1492f, 0.1341f, 0.4724f, 0.0350f, 229.0000f, 0.0499f, -0.1228f, 0.1382f, 0.1229f, 0.4375f, 0.0310f, 235.0000f, 0.0505f, -0.1173f, 0.1281f, 0.1126f, 0.4054f, 0.0280f,
    242.0000f, 0.0510f, -0.1121f, 0.1187f, 0.1030f, 0.3756f, 0.0250f, 248.0000f, 0.0516f, -0.1070f, 0.1098f, 0.0940f, 0.3477f, 0.0220f, 254.0000f, 0.0520f, -0.1021f, 0.1019f, 0.0859f, 0.3225f, 0.0200f, 260.0000f, 0.0524f, -0.0974f, 0.0943f, 0.0783f, 0.2987f, 0.0180f,
    266.0000f, 0.0528f, -0.0930f, 0.0876f, 0.0714f, 0.2774f, 0.0160f, 273.0000f, 0.0532f, -0.0887f, 0.0814f, 0.0651f, 0.2576f, 0.0140f, 279.0000f, 0.0535f, -0.0847f, 0.0757f, 0.0593f, 0.2397f, 0.0120f, 284.0000f, 0.0538f, -0.0808f, 0.0705f, 0.0540f, 0.2232f, 0.0110f,
    290.0000f, 0.0541f, -0.0773f, 0.0659f, 0.0493f, 0.2087f, 0.0100f, 296.0000f, 0.0543f, -0.0738f, 0.0617f, 0.0451f, 0.1954f, 0.0090f, 302.0000f, 0.0545f, -0.0707f, 0.0580f, 0.0413f, 0.1836f, 0.0080f, 309.0000f, 0.0547f, -0.0676f, 0.0546f, 0.0378f, 0.1729f, 0.0070f,
    315.0000f, 0.0549f, -0.0648f, 0.0516f, 0.0347f, 0.1633f, 0.0060f, 321.0000f, 0.0551f, -0.0621f, 0.0489f, 0.0320f, 0.1532f, 0.0050f, 327.0000f, 0.0552f, -0.0597f, 0.0466f, 0.0296f, 0.1417f, 0.0050f, 332.0000f, 0.0553f, -0.0573f, 0.0446f, 0.0275f, 0.1317f, 0.0040f,
    345.0000f, 0.0555f, -0.0532f, 0.0412f, 0.0242f, 0.1154f, 0.0030f, 357.0000f, 0.0557f, -0.0496f, 0.0388f, 0.0217f, 0.1038f, 0.0030f, 369.0000f, 0.0557f, -0.0466f, 0.0372f, 0.0201f, 0.0959f, 0.0020f, 386.0000f, 0.0558f, -0.0430f, 0.0359f, 0.0187f, 0.0895f, 0.0010f,
    411.0000f, 0.0558f, -0.0396f, 0.0357f, 0.0185f, 0.0887f, 0.0010f, 454.0000f, 0.0557f, -0.0363f, 0.0378f, 0.0207f, 0.0991f, 0.0000f,
)
