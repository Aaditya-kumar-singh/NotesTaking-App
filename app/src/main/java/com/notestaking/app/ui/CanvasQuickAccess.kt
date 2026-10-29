package com.notestaking.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notestaking.app.R
import com.notestaking.app.core.tools.EraseMode
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.ui.icons.NotesTakingIcons

/**
 * A stable home for essential canvas actions outside ToolbarFrame's scrolling strip.
 * Keeps the existing user-customised toolbar untouched; the menu still reaches tools that
 * a user hid in Preferences. Actions always delegate to the real editor.
 */
@Composable
internal fun PagedCanvasQuickAccess(
    editor: Editor,
    onInsertImage: () -> Unit,
    onImportTemplate: () -> Unit,
    onClosePane: (() -> Unit)?,
) {
    if (LocalBar.current.vertical) {
        Column {
            PagedQuickButtons(editor, onInsertImage, onImportTemplate)
            onClosePane?.let { ClosePaneButton(it) }
        }
    } else {
        Row {
            PagedQuickButtons(editor, onInsertImage, onImportTemplate)
            onClosePane?.let { ClosePaneButton(it) }
        }
    }
}

/** Paged canvas: direct eraser and + Page; the overflow always stays on-screen. */
@Composable
private fun PagedQuickButtons(editor: Editor, onInsertImage: () -> Unit, onImportTemplate: () -> Unit) {
    var viewOpen by remember { mutableStateOf(false) }
    var stylesOpen by remember { mutableStateOf(false) }
    PinnedEraser(editor, editor.tool == Tool.ERASER) { editor.selectTool(Tool.ERASER) }
    PinnedPageInsertion(editor)
    CanvasAllTools(
        host = editor,
        selected = editor.tool,
        tools = Tool.entries.filter { it != Tool.IMAGE && (it != Tool.MARKUP || editor.hasPdf) },
        select = editor::selectTool,
        canUndo = editor.canUndo,
        canRedo = editor.canRedo,
        undo = editor::undo,
        redo = editor::redo,
        extras = { dismiss ->
            HorizontalDivider()
            QuickActionRow(stringResource(R.string.pages)) {
                dismiss()
                editor.sidePanelTab = SidePanelTab.PAGES
                if (!editor.sidebarVisible) editor.toggleSidebar()
            }
            QuickActionRow(stringResource(R.string.tool_image)) { dismiss(); onInsertImage() }
            QuickActionRow(stringResource(R.string.tool_wand)) { dismiss(); editor.toggleWand() }
            QuickActionRow(stringResource(R.string.tool_ruler)) { dismiss(); editor.toggleRuler() }
            QuickActionRow(stringResource(R.string.toolbar_styles)) { dismiss(); stylesOpen = true }
            QuickActionRow(stringResource(R.string.toolbar_view)) { dismiss(); viewOpen = true }
        },
    )
    if (viewOpen) Box { ViewMenuPopup(editor) { viewOpen = false } }
    if (stylesOpen) Box { StylesPopup(editor, onImportTemplate = onImportTemplate) { stylesOpen = false } }
}

/** Infinite canvas shares the same quick tool access, but page actions are never shown. */
@Composable
internal fun InfiniteCanvasQuickAccess(
    editor: InfiniteEditor,
    onInsertImage: () -> Unit,
    onClosePane: (() -> Unit)?,
) {
    if (LocalBar.current.vertical) {
        Column {
            InfiniteQuickButtons(editor, onInsertImage)
            onClosePane?.let { ClosePaneButton(it) }
        }
    } else {
        Row {
            InfiniteQuickButtons(editor, onInsertImage)
            onClosePane?.let { ClosePaneButton(it) }
        }
    }
}

@Composable
private fun InfiniteQuickButtons(editor: InfiniteEditor, onInsertImage: () -> Unit) {
    var stylesOpen by remember { mutableStateOf(false) }
    var waypointsOpen by remember { mutableStateOf(false) }
    PinnedEraser(editor, editor.tool == Tool.ERASER) { editor.armTool(Tool.ERASER) }
    CanvasAllTools(
        host = editor,
        selected = editor.tool,
        tools = listOf(
            Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO, Tool.SELECT, Tool.PAN,
            Tool.DASHED, Tool.CALLIGRAPHY, Tool.SPEED, Tool.TAPER, Tool.SHAPE, Tool.TAPE,
        ),
        select = editor::armTool,
        canUndo = editor.canUndo,
        canRedo = editor.canRedo,
        undo = editor::undo,
        redo = editor::redo,
        extras = { dismiss ->
            HorizontalDivider()
            QuickActionRow(stringResource(R.string.tool_image)) { dismiss(); onInsertImage() }
            QuickActionRow(stringResource(R.string.tool_wand)) { dismiss(); editor.toggleWand() }
            QuickActionRow(stringResource(R.string.toolbar_fit)) { dismiss(); editor.zoomToFit() }
            QuickActionRow(stringResource(R.string.toolbar_minimap)) { dismiss(); editor.toggleMinimap() }
            QuickActionRow(stringResource(R.string.toolbar_waypoints)) { dismiss(); waypointsOpen = true }
            QuickActionRow(stringResource(R.string.toolbar_styles)) { dismiss(); stylesOpen = true }
        },
    )
    if (stylesOpen) Box { CanvasStylesPopup(editor) { stylesOpen = false } }
    if (waypointsOpen) Box { CanvasWaypointsPopup(editor) { waypointsOpen = false } }
}

@Composable
private fun PinnedEraser(
    host: ToolPopupHost,
    active: Boolean,
    selectEraser: () -> Unit,
) {
    var settingsOpen by remember { mutableStateOf(false) }
    val area = host.toolConfig(Tool.ERASER).eraseMode == EraseMode.AREA
    val name = stringResource(R.string.tool_eraser)
    val mode = stringResource(if (area) R.string.eraser_area else R.string.eraser_stroke)
    Box {
        ToolbarIcon(
            NotesTakingIcons.eraser,
            "$name: $mode",
            active = active,
            hint = stringResource(R.string.canvas_eraser_tool_hint),
        ) {
            if (active) settingsOpen = true else selectEraser()
        }
        if (settingsOpen) EraserConfigPopup(host) { settingsOpen = false }
    }
}

@Composable
private fun PinnedPageInsertion(editor: Editor) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolbarIcon(
            NotesTakingIcons.plus,
            stringResource(R.string.add_page),
            active = open,
        ) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            QuickActionRow(stringResource(R.string.canvas_page_after)) {
                open = false
                editor.addPage()
            }
            QuickActionRow(stringResource(R.string.canvas_page_before), enabled = !editor.currentPageIsCover) {
                open = false
                editor.addPageBeforeCurrent()
            }
            QuickActionRow(stringResource(R.string.canvas_page_end)) {
                open = false
                editor.addPageAtEndAndOpen()
            }
        }
    }
}

@Composable
private fun CanvasAllTools(
    host: ToolPopupHost,
    selected: Tool,
    tools: List<Tool>,
    select: (Tool) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    undo: () -> Unit,
    redo: () -> Unit,
    extras: @Composable ((() -> Unit) -> Unit),
) {
    var open by remember { mutableStateOf(false) }
    var eraserOpen by remember { mutableStateOf(false) }
    var penOpen by remember { mutableStateOf(false) }
    val penForConfig = selected.takeIf { it.isStroke } ?: Tool.PEN
    Box {
        ToolbarIcon(
            NotesTakingIcons.more,
            stringResource(R.string.canvas_all_tools),
            active = open,
        ) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(androidx.compose.ui.Modifier.width(248.dp)) {
                Text(
                    stringResource(R.string.canvas_all_tools),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    modifier = androidx.compose.ui.Modifier.width(248.dp),
                )
                tools.forEach { tool ->
                    QuickActionRow(stringResource(tool.labelRes)) {
                        open = false
                        select(tool)
                    }
                }
                HorizontalDivider()
                QuickActionRow(stringResource(R.string.canvas_eraser_settings)) {
                    open = false
                    select(Tool.ERASER)
                    eraserOpen = true
                }
                QuickActionRow(stringResource(R.string.canvas_pen_settings)) {
                    open = false
                    select(penForConfig)
                    penOpen = true
                }
                QuickActionRow(stringResource(R.string.undo), enabled = canUndo) { open = false; undo() }
                QuickActionRow(stringResource(R.string.redo), enabled = canRedo) { open = false; redo() }
                extras { open = false }
            }
        }
        if (eraserOpen) EraserConfigPopup(host) { eraserOpen = false }
        if (penOpen) ToolConfigPopup(host, penForConfig) { penOpen = false }
    }
}

@Composable
private fun QuickActionRow(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(text = { Text(label) }, enabled = enabled, onClick = onClick)
}
