package com.notestaking.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notestaking.app.R
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.ui.icons.NotesTakingIcons
import com.notestaking.app.ui.theme.toComposeColor

/** The floating shell pins More; these actions fill the compact paged-note dock. */
internal enum class PagedFloatingAction { PEN, ERASER, PEN_TYPE, SIZE, UNDO, PDF_MODE }

internal fun pagedFloatingActions(hasPdf: Boolean): List<PagedFloatingAction> =
    listOf(
        PagedFloatingAction.PEN,
        PagedFloatingAction.ERASER,
        PagedFloatingAction.PEN_TYPE,
        PagedFloatingAction.SIZE,
        PagedFloatingAction.UNDO,
    ) + if (hasPdf) listOf(PagedFloatingAction.PDF_MODE) else emptyList()

/** Use the same compact tools as Infinity Canvas without changing the paged editor's model. */
@Composable
private fun PagedFloatingQuickTools(
    editor: Editor,
    activeTool: Tool,
    columns: Int,
    onSelect: (Tool) -> Unit,
) {
    var popupAction by remember(editor) { mutableStateOf<PagedFloatingAction?>(null) }
    var subtypeOpen by remember(editor) { mutableStateOf(false) }
    var lastStroke by remember(editor) { mutableStateOf(Tool.PEN) }
    LaunchedEffect(activeTool) {
        if (activeTool.isStroke) lastStroke = activeTool
        popupAction = null
        subtypeOpen = false
    }
    val widthTool = canvasSizeTool(activeTool)
    val currentWidth = widthTool?.let { editor.toolConfig(it).baseWidth }
    val penTypes = listOf(Tool.PEN, Tool.DASHED, Tool.CALLIGRAPHY, Tool.SPEED, Tool.TAPER, Tool.HIGHLIGHTER)
    val strokeIcon = when (lastStroke) {
        Tool.DASHED -> ImageVector.vectorResource(R.drawable.ic_stroke_dashed)
        Tool.CALLIGRAPHY -> ImageVector.vectorResource(R.drawable.ic_stroke_calligraphy)
        Tool.SPEED -> ImageVector.vectorResource(R.drawable.ic_stroke_speed)
        Tool.TAPER -> ImageVector.vectorResource(R.drawable.ic_stroke_taper)
        Tool.HIGHLIGHTER -> ImageVector.vectorResource(R.drawable.ic_stroke_highlighter)
        else -> ImageVector.vectorResource(R.drawable.ic_stroke_regular)
    }

    Column {
        pagedFloatingActions(editor.hasPdf).chunked(columns.coerceIn(1, 3)).forEach { actions ->
            Row {
                actions.forEach { action ->
                    Box {
                        when (action) {
                            PagedFloatingAction.PEN -> {
                                ToolbarIcon(
                                    ImageVector.vectorResource(R.drawable.ic_stroke_regular),
                                    stringResource(R.string.tool_pen),
                                    active = activeTool == Tool.PEN,
                                    reportSelection = true,
                                    hint = stringResource(R.string.tip_tap_for_settings),
                                ) {
                                    if (activeTool == Tool.PEN) popupAction = action else onSelect(Tool.PEN)
                                }
                                if (popupAction == action) ToolConfigPopup(editor, Tool.PEN) { popupAction = null }
                            }
                            PagedFloatingAction.ERASER -> {
                                ToolbarIcon(
                                    NotesTakingIcons.eraser,
                                    eraserToolbarDescription(editor),
                                    active = activeTool == Tool.ERASER,
                                    reportSelection = true,
                                    hint = stringResource(R.string.tip_tap_for_settings),
                                ) {
                                    if (activeTool == Tool.ERASER) popupAction = action else onSelect(Tool.ERASER)
                                }
                                if (popupAction == action) EraserConfigPopup(editor) { popupAction = null }
                            }
                            PagedFloatingAction.PEN_TYPE -> {
                                ToolbarIcon(
                                    strokeIcon,
                                    stringResource(R.string.toolbar_pen_type, stringResource(lastStroke.labelRes)),
                                    active = activeTool.isStroke,
                                    reportSelection = true,
                                ) { subtypeOpen = true }
                                DropdownMenu(expanded = subtypeOpen, onDismissRequest = { subtypeOpen = false }) {
                                    penTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(type.labelRes)) },
                                            onClick = {
                                                subtypeOpen = false
                                                lastStroke = type
                                                onSelect(type)
                                            },
                                        )
                                    }
                                }
                            }
                            PagedFloatingAction.SIZE -> {
                                ToolbarIcon(
                                    NotesTakingIcons.sliders,
                                    currentWidth?.let { stringResource(R.string.caption_size) + ": " + "%.1f".format(it) }
                                        ?: stringResource(R.string.caption_size),
                                    enabled = widthTool != null,
                                ) { popupAction = action }
                                Text(
                                    currentWidth?.let { "%.1f".format(it) } ?: "",
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                        .width(LocalBar.current.button).clearAndSetSemantics {},
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                if (popupAction == action && widthTool != null) {
                                    if (widthTool == Tool.ERASER) EraserConfigPopup(editor) { popupAction = null }
                                    else ToolConfigPopup(editor, widthTool) { popupAction = null }
                                }
                            }
                            PagedFloatingAction.UNDO -> {
                                ToolbarIcon(
                                    NotesTakingIcons.undo,
                                    stringResource(R.string.undo),
                                    enabled = editor.canUndo,
                                ) { editor.undo() }
                            }
                            PagedFloatingAction.PDF_MODE -> PdfModeButton(editor)
                        }
                    }
                }
            }
        }
    }
}


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
    if (host is InfiniteEditor) {
        B101FloatingCanvasQuickTools(host, LocalFloatingCanvasColumns.current)
        return
    }
    if (host is Editor) {
        PagedFloatingQuickTools(host, activeTool, LocalFloatingCanvasColumns.current, onSelect)
        return
    }
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
