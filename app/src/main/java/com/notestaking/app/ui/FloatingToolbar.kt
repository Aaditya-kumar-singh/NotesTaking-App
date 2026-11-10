package com.notestaking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.absoluteOffset
import com.notestaking.app.R
import com.notestaking.app.settings.ToolbarLook
import com.notestaking.app.ui.icons.NotesTakingIcons
import com.notestaking.app.ui.theme.LocalPalette
import com.notestaking.app.ui.theme.toComposeColor
import kotlin.math.roundToInt

/**
 * The existing floating toolbar, plus an isolated drag grip. Only the grip consumes drag events;
 * pen, eraser, swatches, menus and the underlying canvas keep their original input handlers.
 */
@Composable
internal fun MovableFloatingToolbar(
    look: ToolbarLook,
    bar: @Composable () -> Unit,
    onDrop: (Float, Float) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val edgePx = with(density) { 8.dp.roundToPx() }
        var measured by remember { mutableStateOf(IntSize.Zero) }
        val travelX = (constraints.maxWidth - measured.width - 2 * edgePx).coerceAtLeast(0).toFloat()
        val travelY = (constraints.maxHeight - measured.height - 2 * edgePx).coerceAtLeast(0).toFloat()
        val initial = look.floatingFractions
        var x by remember(look.floatingX, look.floatingY, look.position) { mutableFloatStateOf(initial.first) }
        var y by remember(look.floatingX, look.floatingY, look.position) { mutableFloatStateOf(initial.second) }
        var dragStartX by remember { mutableFloatStateOf(x) }
        var dragStartY by remember { mutableFloatStateOf(y) }
        val commit by rememberUpdatedState(onDrop)

        fun nudge(dx: Float, dy: Float): Boolean {
            if ((dx != 0f && travelX == 0f) || (dy != 0f && travelY == 0f)) return false
            val nextX = (x + dx).coerceIn(0f, 1f)
            val nextY = (y + dy).coerceIn(0f, 1f)
            if (x == nextX && y == nextY) return false
            x = nextX
            y = nextY
            commit(x, y)
            return true
        }

        val dragLabel = stringResource(R.string.toolbar_drag_handle)
        val leftLabel = stringResource(R.string.toolbar_move_left)
        val rightLabel = stringResource(R.string.toolbar_move_right)
        val upLabel = stringResource(R.string.toolbar_move_up)
        val downLabel = stringResource(R.string.toolbar_move_down)
        val resetLabel = stringResource(R.string.toolbar_move_reset)
        val palette = LocalPalette.current
        val shape = MaterialTheme.shapes.extraLarge
        val grip: @Composable () -> Unit = {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(palette.panel.toComposeColor())
                    .border(1.dp, palette.border.toComposeColor(), shape)
                    .semantics {
                        contentDescription = dragLabel
                        customActions = listOf(
                            CustomAccessibilityAction(leftLabel) { nudge(-0.1f, 0f) },
                            CustomAccessibilityAction(rightLabel) { nudge(0.1f, 0f) },
                            CustomAccessibilityAction(upLabel) { nudge(0f, -0.1f) },
                            CustomAccessibilityAction(downLabel) { nudge(0f, 0.1f) },
                            CustomAccessibilityAction(resetLabel) {
                                val defaults = look.copy(floatingX = null, floatingY = null).floatingFractions
                                x = defaults.first
                                y = defaults.second
                                commit(x, y)
                                true
                            },
                        )
                    }
                    .pointerInput(travelX, travelY) {
                        detectDragGestures(
                            onDragStart = { dragStartX = x; dragStartY = y },
                            onDragEnd = { if (x != dragStartX || y != dragStartY) commit(x, y) },
                            onDragCancel = { x = dragStartX; y = dragStartY },
                        ) { change, delta ->
                            change.consume()
                            if (travelX > 0f) x = (x + delta.x / travelX).coerceIn(0f, 1f)
                            if (travelY > 0f) y = (y + delta.y / travelY).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(NotesTakingIcons.grip, contentDescription = null, modifier = Modifier.size(20.dp),
                    tint = palette.textDim.toComposeColor())
            }
        }

        Box(
            Modifier
                .absoluteOffset {
                    IntOffset(
                        edgePx + (travelX * x).roundToInt(),
                        edgePx + (travelY * y).roundToInt(),
                    )
                }
                .onSizeChanged { measured = it },
        ) {
            if (look.position.vertical) {
                Column(
                    Modifier.heightIn(max = (maxHeight - 16.dp).coerceIn(48.dp, 600.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grip()
                    bar()
                }
            } else {
                Row(
                    Modifier.widthIn(max = (maxWidth - 16.dp).coerceIn(48.dp, 480.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grip()
                    bar()
                }
            }
        }
    }
}
