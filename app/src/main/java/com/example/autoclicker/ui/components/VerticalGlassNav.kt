package com.example.autoclicker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.render.Backdrop
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver
import io.github.neoruaa.glasmio.icons.LocalGlasmioIconResolver
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBar
import io.github.neoruaa.glasmio.ui.GlasmioTabItem

/** 空图标：让库自己那层（会跟着旋转的）内容不画东西 */
private val EmptyIcon: ImageVector = ImageVector.Builder(
    name = "empty",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).build()

private val EmptyIconResolver = GlasmioIconResolver { EmptyIcon }

// 以下三个数值照抄库里 GlassBottomBar 的展开态：
// ExpandedNavigationIconSize = 28.dp、symbol fontSize 18.sp / Medium、
// NavigationLabelStyle = caption.copy(fontSize = 10.sp, lineHeight = 12.sp)。
private val NavIconSize = 28.dp
private val NavLabelStyle = TextStyle(fontSize = 10.sp, lineHeight = 12.sp)
private val RailWidth = 62.dp

/**
 * 横屏用的竖向导航栏。
 *
 * 栏本体是原版 `GlasmioNavigationBar`，整体顺时针转 90°；玻璃、滑块、按下放大、
 * 拖动拉伸跟手全部是库自己的代码在跑。
 *
 * 图标文字由库的 `contentOverlay` 插槽画 —— 这个插槽在库内部位于**玻璃之后、滑块之前**，
 * 正是原版里文字所在的层级。库的内容本身传空（空 label + `EmptyIconResolver`），因为
 * 它会跟着整条栏一起转 90°。
 *
 * 插槽在栏的局部坐标系里是横的（宽度 = 栏长、高 62dp），所以套一层 `rotationZ = -90f`
 * 把它转回正立；这一层的旋转中心就是栏的中心，和整条栏 +90° 的旋转中心重合，两次旋转
 * 正好抵消，不会有位移。
 *
 * 另外同一份内容还要再传一份给 `lensOverlay`：滑块是不透的，它内部显示的是自己折射出来
 * 的那份内容（库会给它整体染强调色），不喂的话选中那格的字会被滑块盖没。
 */
@Composable
fun VerticalGlassNav(
    items: List<GlasmioTabItem<Int>>,
    selectedKey: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val colors = LocalGlassColors.current
    // contentOverlay/lensOverlay 是在下面 GlasmioIconProvider(EmptyIconResolver) 作用域里
    // 被库回调的，SfIcon 走 LocalGlasmioIconResolver 会被解析成空图标，
    // 所以先抓外层（AppGlasmioTheme 提供的）resolver，在 overlay 里再包一层还原。
    val appIconResolver = LocalGlasmioIconResolver.current
    // 传给库的 item：标签留空、图标由 EmptyIconResolver 吃掉
    val hiddenItems = remember(items) {
        items.map {
            GlasmioTabItem(
                key = it.key,
                label = "",
                symbol = it.symbol,
                contentDescription = it.contentDescription,
            )
        }
    }
    val compactProgress = remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = modifier.width(RailWidth)) {
        val density = LocalDensity.current
        val railHeightDp = with(density) { constraints.maxHeight.toDp() }
        val cellHeightDp = railHeightDp / items.size

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .requiredSize(width = railHeightDp, height = RailWidth)
                    .graphicsLayer { rotationZ = 90f },
            ) {
                GlasmioIconProvider(resolver = EmptyIconResolver) {
                    GlasmioNavigationBar(
                        items = hiddenItems,
                        selectedKey = selectedKey,
                        onSelected = onSelected,
                        onExpand = {},
                        compactProgress = compactProgress,
                        compactSize = 48.dp,
                        modifier = Modifier.fillMaxSize(),
                        backdrop = backdrop,
                        // 玻璃之上、滑块之下：原版里图标文字所在的层级
                        contentOverlay = {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer { rotationZ = -90f },
                                contentAlignment = Alignment.Center,
                            ) {
                                GlasmioIconProvider(resolver = appIconResolver) {
                                    RailContent(items, railHeightDp, cellHeightDp, colors.content)
                                }
                            }
                        },
                        // 同一份内容再喂给滑块的折射源（库会整体染成强调色）
                        lensOverlay = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationZ = -90f },
                                contentAlignment = Alignment.Center,
                            ) {
                                GlasmioIconProvider(resolver = appIconResolver) {
                                    RailContent(items, railHeightDp, cellHeightDp, colors.content)
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/** 竖排的图标 + 文字，尺寸按栏的格子分 */
@Composable
private fun RailContent(
    items: List<GlasmioTabItem<Int>>,
    railHeightDp: Dp,
    cellHeightDp: Dp,
    color: Color,
) {
    Column(
        modifier = Modifier.requiredSize(width = RailWidth, height = railHeightDp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SfIcon(
                    symbol = item.symbol,
                    contentDescription = item.contentDescription,
                    tint = color,
                    size = NavIconSize,
                    fontSize = 18.sp,
                    weight = FontWeight.Medium,
                    fitGlyphToBounds = false,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = item.label,
                    color = color,
                    style = NavLabelStyle,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 1.dp, bottom = 4.dp),
                )
            }
        }
    }
}
