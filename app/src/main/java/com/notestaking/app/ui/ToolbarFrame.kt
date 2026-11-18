package com.notestaking.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notestaking.app.R
import com.notestaking.app.ui.icons.NotesTakingIcons
import androidx.compose.ui.res.stringResource
import com.notestaking.app.settings.ToolbarLook
import com.notestaking.app.settings.ToolbarPosition
import com.notestaking.app.settings.ToolbarSize
import com.notestaking.app.ui.theme.LocalPalette
import com.notestaking.app.ui.theme.toComposeColor

/** The toolbar look from Preferences, provided once above both panes. */
val LocalToolbarLook = staticCompositionLocalOf { ToolbarLook() }

/** What every toolbar widget sizes itself by, and which way the bar runs. */
internal data class BarMetrics(
    val button: Dp,
    val circle: Dp,
    val icon: Dp,
    val swatch: Dp,
    val rule: Dp,
    val label: TextUnit,
    val vertical: Boolean = false,
) {
    /** Across the bar: one button with a little air either side. */
    val thickness: Dp get() = button + 8.dp
}

internal fun barMetrics(size: ToolbarSize): BarMetrics = when (size) {
    ToolbarSize.COMPACT -> BarMetrics(48.dp, 40.dp, 20.dp, 28.dp, 22.dp, 12.sp)
    ToolbarSize.REGULAR -> BarMetrics(48.dp, 40.dp, 24.dp, 28.dp, 26.dp, 12.sp)
    ToolbarSize.COMFORTABLE -> BarMetrics(56.dp, 48.dp, 26.dp, 36.dp, 30.dp, 13.sp)
}

internal val LocalBar = staticCompositionLocalOf { barMetrics(ToolbarSize.REGULAR) }

/** Where a menu opens from its anchor; a side rail sets it so its menus open beside the rail. */
internal val LocalMenuOffset = compositionLocalOf { DpOffset.Zero }

/** How far a floating bar sits in from the canvas edge. */
private val FLOAT_MARGIN = 8.dp

/** Count padded icons and pinned actions so split panes scroll before buttons collide. */
internal fun toolbarScrollsTrailing(
    floating: Boolean,
    availableAxisDp: Float = Float.POSITIVE_INFINITY,
    buttonDp: Float = 48f,
    pinnedButtons: Int = 1,
): Boolean {
    val reserved = (buttonDp + 8f) * (6 + pinnedButtons.coerceAtLeast(0))
    return floating || availableAxisDp < reserved + (if (pinnedButtons > 0) 1f else 0f)
}

/** Selecting a different tool dismisses the palette; reselecting the same one opens its settings. */
internal fun drawingDrawerDismissOnSelection(previous: Any?, selected: Any?): Boolean = previous != selected

/** What a floating bar covers of the canvas, for overlays that must stay out from under it. */
internal val LocalToolbarCover = compositionLocalOf { PaddingValues(0.dp) }

/** Floating toolbar asks the existing bar to dismiss when its armed tool changes. */
internal val LocalDrawingToolSelected = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Lays a pane out with its [bar] along the edge Preferences chose and [content] in the rest. A
 * floating bar is handed to [content] to lay over its canvas instead, and [onCover] learns how many
 * px of each edge (left, top, right, bottom) it covers so the canvas keeps its pages clear of it.
 */
@Composable
internal fun ColumnScope.ToolbarAround(
    bar: @Composable () -> Unit,
    onCover: (Double, Double, Double, Double) -> Unit,
    onFloatingDrop: (Float, Float) -> Unit,
    onGripFocusChange: (Boolean) -> Unit = {},
    favorites: @Composable (Int) -> Unit,
    content: @Composable (floatingBar: @Composable BoxScope.() -> Unit) -> Unit,
) {
    val look = LocalToolbarLook.current
    val rest = Modifier.weight(1f).fillMaxWidth()
    if (!look.floating) {
        SideEffect {
            onCover(0.0, 0.0, 0.0, 0.0)
            onGripFocusChange(false)
        }
        val docked: @Composable BoxScope.() -> Unit = {}
        // Left and right are the screen's, as the canvas insets are, whatever the language.
        val across = Arrangement.Absolute.Left
        when (look.position) {
            ToolbarPosition.TOP, ToolbarPosition.AUTO -> { bar(); Box(rest) { content(docked) } }
            ToolbarPosition.BOTTOM -> { Box(rest) { content(docked) }; bar() }
            ToolbarPosition.LEFT -> Row(rest, across) { bar(); Box(Modifier.weight(1f).fillMaxHeight()) { content(docked) } }
            ToolbarPosition.RIGHT -> Row(rest, across) { Box(Modifier.weight(1f).fillMaxHeight()) { content(docked) }; bar() }
        }
        return
    }
    val depth = barMetrics(look.size).thickness + FLOAT_MARGIN
    val px = with(LocalDensity.current) { depth.toPx().toDouble() }
    val position = look.position
    SideEffect {
        onCover(
            if (position == ToolbarPosition.LEFT) px else 0.0,
            if (position == ToolbarPosition.TOP || position == ToolbarPosition.AUTO) px else 0.0,
            if (position == ToolbarPosition.RIGHT) px else 0.0,
            if (position == ToolbarPosition.BOTTOM) px else 0.0,
        )
    }
    val cover = when (position) {
        ToolbarPosition.TOP, ToolbarPosition.AUTO -> PaddingValues(top = depth)
        ToolbarPosition.BOTTOM -> PaddingValues(bottom = depth)
        ToolbarPosition.LEFT -> PaddingValues.Absolute(left = depth)
        ToolbarPosition.RIGHT -> PaddingValues.Absolute(right = depth)
    }
    CompositionLocalProvider(LocalToolbarCover provides cover) {
        Box(rest) { content { MovableFloatingToolbar(look, bar, favorites, onFloatingDrop, onGripFocusChange) } }
    }
}

/**
 * Shared toolbar strip: horizontal at top/bottom and vertical at either side.
 * Tool actions scroll; [trailing] actions also scroll in narrow panes or floating bars.
 * [pinned] actions remain reachable at the end of the strip.
 */
@Composable
internal fun ToolbarFrame(
    armed: Any?,
    trailing: (@Composable () -> Unit)? = null,
    pinned: (@Composable () -> Unit)? = null,
    pinnedButtons: Int = 1,
    collapsed: (@Composable (expand: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val look = LocalToolbarLook.current
    val bar = barMetrics(look.size).copy(vertical = look.position.vertical)
    val glide = remember { ToolGlide() }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var previousArmed by remember { mutableStateOf(armed) }
    val dismissFloatingDrawer = LocalDrawingToolSelected.current
    LaunchedEffect(armed) {
        if (drawingDrawerDismissOnSelection(previousArmed, armed)) {
            drawerOpen = false
            dismissFloatingDrawer?.invoke()
            previousArmed = armed
        }
    }
    BackHandler(enabled = !look.floating && collapsed != null && drawerOpen) { drawerOpen = false }
    val drawingStrip: @Composable () -> Unit = {
        if (collapsed == null || look.floating) {
            content()
        } else if (drawerOpen) {
            ToolbarIcon(NotesTakingIcons.chevronDown, stringResource(R.string.toolbar_collapse_to_favorites)) { drawerOpen = false }
            content()
        } else {
            ToolbarIcon(NotesTakingIcons.chevronUp, stringResource(R.string.toolbar_expand_all_tools)) { drawerOpen = true }
            collapsed { drawerOpen = true }
        }
    }
    var availableAxisPx by remember(look.position) { mutableIntStateOf(0) }
    var paneBounds by remember(look.position, look.floating) { mutableStateOf<IntRect?>(null) }
    // The docked strip's layout parent spans its owning editor pane, even for side rails.
    val measurePane = if (look.floating) Modifier else Modifier.onGloballyPositioned { c ->
        c.parentCoordinates?.tooltipPaneRect()?.let { if (it != paneBounds) paneBounds = it }
    }
    val availableAxisDp = with(LocalDensity.current) { availableAxisPx.toDp().value }
    val scrollTrailing = toolbarScrollsTrailing(look.floating, availableAxisDp, bar.button.value, pinnedButtons)
    val palette = LocalPalette.current
    val shape = MaterialTheme.shapes.extraLarge
    val divider = palette.border.toComposeColor()
    // Docked, the strip fills its edge; floating, a pill only as long as its tools, up to that edge.
    val surface = if (!look.floating) {
        Modifier.background(palette.panel.toComposeColor()).drawBehind {
            val stroke = 1.dp.toPx()
            if (bar.vertical) {
                val x = if (look.position == ToolbarPosition.LEFT) size.width - stroke / 2 else stroke / 2
                drawLine(divider, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
            } else {
                val y = if (look.position == ToolbarPosition.TOP) size.height - stroke / 2 else stroke / 2
                drawLine(divider, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
        }
    } else {
        Modifier
            .shadow(6.dp, shape)
            .clip(shape)
            .background(palette.panel.toComposeColor())
            .border(1.dp, palette.border.toComposeColor(), shape)
    }
    val stretch = when {
        look.floating -> Modifier
        bar.vertical -> Modifier.fillMaxHeight()
        else -> Modifier.fillMaxWidth()
    }
    // Material places a menu below its anchor; shifting it one bar across and one button up puts it
    // beside the rail, and on a right rail material mirrors it to the left side on its own.
    val menuOffset = if (bar.vertical) DpOffset(bar.thickness, -bar.button) else DpOffset.Zero
    val tipSide = when (look.position) {
        ToolbarPosition.TOP, ToolbarPosition.AUTO -> TipSide.BELOW
        ToolbarPosition.BOTTOM -> TipSide.ABOVE
        ToolbarPosition.LEFT -> TipSide.RIGHT
        ToolbarPosition.RIGHT -> TipSide.LEFT
    }
    val tipBounds = if (look.floating) LocalTipPaneBounds.current else paneBounds
    CompositionLocalProvider(
        LocalToolGlide provides glide, LocalBar provides bar,
        LocalMenuOffset provides menuOffset, LocalTipSide provides tipSide,
        LocalTipPaneBounds provides tipBounds,
    ) {
        if (bar.vertical) {
            Column(
                stretch.width(bar.thickness).then(surface).onSizeChanged { availableAxisPx = it.height }.then(measurePane),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .weight(1f, fill = !look.floating)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                        .toolGlide(glide, armed),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    drawingStrip()
                    if (scrollTrailing && (drawerOpen || collapsed == null || look.floating)) trailing?.invoke()
                }
                if (!scrollTrailing && (drawerOpen || collapsed == null || look.floating)) trailing?.invoke()
                pinned?.invoke()
            }
        } else {
            Row(
                stretch.height(bar.thickness).then(surface).onSizeChanged { availableAxisPx = it.width }.then(measurePane),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f, fill = !look.floating)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp)
                        .toolGlide(glide, armed),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    drawingStrip()
                    if (scrollTrailing && (drawerOpen || collapsed == null || look.floating)) trailing?.invoke()
                }
                if (!scrollTrailing && (drawerOpen || collapsed == null || look.floating)) trailing?.invoke()
                pinned?.invoke()
            }
        }
    }
}
