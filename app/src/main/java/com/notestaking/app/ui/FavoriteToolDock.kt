package com.notestaking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.notestaking.app.R
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.ui.icons.NotesTakingIcons
import com.notestaking.app.ui.theme.toComposeColor

/**
 * Quick access to the real editors' tools and existing configuration popups.
 * Changes here select a tool; the editor still owns its ink, eraser and undo state.
 */
@Composable
internal fun FavoriteToolDock(
    host: ToolPopupHost,
    activeTool: Tool,
    favorites: List<Tool>,
    maxVisible: Int,
    onSelect: (Tool) -> Unit,
) {
    var settingsFor by remember { mutableStateOf<Tool?>(null) }
    val pdfEditor = (host as? Editor)?.takeIf { it.hasPdf }
    // Reserve one slot for Read/Annotate so a narrow floating dock never clips its grip.
    val favoriteSlots = (maxVisible - if (pdfEditor != null) 1 else 0).coerceAtLeast(0)
    val visible = favorites.take(favoriteSlots).ifEmpty { if (pdfEditor == null) listOf(Tool.PEN) else emptyList() }
    val vertical = LocalToolbarLook.current.position.vertical
    val actions: @Composable () -> Unit = {
        for (tool in visible) {
            Box {
                val active = activeTool == tool
                val configurable = tool.isStroke || tool == Tool.ERASER || tool == Tool.SELECT
                val icon: ImageVector = when (tool) {
                    Tool.PEN -> ImageVector.vectorResource(R.drawable.ic_stroke_regular)
                    Tool.DASHED -> ImageVector.vectorResource(R.drawable.ic_stroke_dashed)
                    Tool.CALLIGRAPHY -> ImageVector.vectorResource(R.drawable.ic_stroke_calligraphy)
                    Tool.SPEED -> ImageVector.vectorResource(R.drawable.ic_stroke_speed)
                    Tool.TAPER -> ImageVector.vectorResource(R.drawable.ic_stroke_taper)
                    Tool.HIGHLIGHTER -> ImageVector.vectorResource(R.drawable.ic_stroke_highlighter)
                    Tool.ERASER -> NotesTakingIcons.eraser
                    Tool.LASSO -> NotesTakingIcons.lasso
                    Tool.SELECT -> NotesTakingIcons.select
                    else -> NotesTakingIcons.more
                }
                ToolbarIcon(
                    icon,
                    stringResource(tool.labelRes),
                    active = active,
                    glideKey = tool,
                    hint = if (active && configurable) stringResource(R.string.tip_tap_for_settings) else null,
                ) {
                    if (active && configurable) settingsFor = tool
                    else {
                        settingsFor = null
                        onSelect(tool)
                    }
                }
                if (tool.isStroke) {
                    val config = host.toolConfig(tool)
                    val ink = config.colorOverride
                        ?: host.hostToolbarColors.getOrNull(host.hostActiveColorIndex)
                        ?: config.rgba
                    val sampleWidth = (config.baseWidth / 2.0).toFloat().coerceIn(4f, 12f)
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .padding(bottom = 1.dp)
                            .size(sampleWidth.dp, 4.dp)
                            .background(ink.toComposeColor(), CircleShape),
                    )
                }
                if (settingsFor == tool) {
                    when (tool) {
                        Tool.ERASER -> EraserConfigPopup(host) { settingsFor = null }
                        Tool.SELECT -> SelectConfigPopup(host) { settingsFor = null }
                        else -> ToolConfigPopup(host, tool) { settingsFor = null }
                    }
                }
            }
        }
        pdfEditor?.let { PdfModeButton(it) }
    }
    if (vertical) Column { actions() } else Row { actions() }
}
