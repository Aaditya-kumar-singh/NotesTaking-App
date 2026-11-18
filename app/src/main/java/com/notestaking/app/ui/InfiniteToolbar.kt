package com.notestaking.app.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.notestaking.app.R
import com.notestaking.app.core.tools.Tool
import com.notestaking.app.core.tools.ToolbarItem
import com.notestaking.app.ui.icons.NotesTakingIcons
import com.notestaking.app.ui.theme.LocalPalette
import com.notestaking.app.ui.theme.toComposeColor

/** Which tool a toolbar item arms, for the items that are simply a tool button. */
private val CANVAS_TOOL_OF: Map<ToolbarItem, Tool> = mapOf(
    ToolbarItem.PEN to Tool.PEN,
    ToolbarItem.DASHED to Tool.DASHED,
    ToolbarItem.CALLIGRAPHY to Tool.CALLIGRAPHY,
    ToolbarItem.SPEED to Tool.SPEED,
    ToolbarItem.TAPER to Tool.TAPER,
    ToolbarItem.HIGHLIGHTER to Tool.HIGHLIGHTER,
    ToolbarItem.ERASER to Tool.ERASER,
    ToolbarItem.PAN to Tool.PAN,
    ToolbarItem.SELECT to Tool.SELECT,
    ToolbarItem.LASSO to Tool.LASSO,
    ToolbarItem.SHAPE to Tool.SHAPE,
    ToolbarItem.TAPE to Tool.TAPE,
)

/**
 * Infinite canvas toolbar. It shares controls with the paged [Toolbar], but keeps a separate
 * saved layout for canvas-specific actions such as waypoints.
 */
@Composable
fun InfiniteToolbar(
    editor: InfiniteEditor,
    onOpenBackstage: () -> Unit,
    onInsertImage: () -> Unit = {},
    onClosePane: (() -> Unit)? = null,
    onInsertAudio: () -> Unit = {},
    onRecordAudio: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    headerChromeVisible: Boolean = false,
) {
    // The stroke tools use the same designed drawables the paged toolbar does, so a pen looks like
    // a pen on either canvas rather than like a generic glyph.
    val toolIcons: Map<Tool, ImageVector> = mapOf(
        Tool.PEN to ImageVector.vectorResource(R.drawable.ic_stroke_regular),
        Tool.DASHED to ImageVector.vectorResource(R.drawable.ic_stroke_dashed),
        Tool.CALLIGRAPHY to ImageVector.vectorResource(R.drawable.ic_stroke_calligraphy),
        Tool.SPEED to ImageVector.vectorResource(R.drawable.ic_stroke_speed),
        Tool.TAPER to ImageVector.vectorResource(R.drawable.ic_stroke_taper),
        Tool.HIGHLIGHTER to ImageVector.vectorResource(R.drawable.ic_stroke_highlighter),
        Tool.PAN to NotesTakingIcons.pan,
        Tool.ERASER to NotesTakingIcons.eraser,
        Tool.SHAPE to NotesTakingIcons.shape,
        Tool.SELECT to NotesTakingIcons.select,
        Tool.LASSO to NotesTakingIcons.lasso,
        Tool.TAPE to NotesTakingIcons.tape,
    )
    var stylesOpen by remember { mutableStateOf(false) }
    var eraserOpen by remember { mutableStateOf(false) }
    var shapeOpen by remember { mutableStateOf(false) }
    var waypointsOpen by remember { mutableStateOf(false) }
    var penOpen by remember { mutableStateOf<Tool?>(null) }
    var selectOpen by remember { mutableStateOf(false) }
    var tapeOpen by remember { mutableStateOf(false) }
    // The selected swatch opens its colour picker when tapped again.
    var switcherIndex by remember { mutableStateOf<Int?>(null) }

    val failureMessage = infiniteCanvasFailureMessage(
        editor.renderFailure != null,
        editor.outOfMemory,
        stringResource(R.string.gl_unavailable),
        stringResource(R.string.canvas_out_of_memory),
    )
    // Keep critical failures visible alongside Close Pane, even when toolbar items scroll.
    ToolbarFrame(
        armed = editor.tool,
        compactControls = { columns, expand -> B101CanvasQuickTools(editor, toolIcons, columns, expand) },
        collapsed = { expand ->
            ToolbarIcon(
                toolIcons[editor.tool] ?: NotesTakingIcons.edit,
                stringResource(editor.tool.labelRes),
                active = true,
                reportSelection = true,
                onClick = expand,
            )
            val selectedInk = editor.toolbarColors.getOrNull(editor.activeColorIndex)
            if (selectedInk != null) Swatch(selectedInk.toComposeColor(), active = true, ink = selectedInk, onClick = expand)
        },

        pinnedButtons = (if (failureMessage != null) 1 else 0) + (if (!headerChromeVisible && onClosePane != null) 1 else 0),
        trailing = { InfiniteCanvasQuickAccess(editor, onInsertImage, null) },
        pinned = {
            failureMessage?.let { InfiniteCanvasWarning(it) }
            if (!headerChromeVisible) onClosePane?.let { ClosePaneButton(it) }
        },
    ) {
        val sections = toolbarSectionsWithoutHeader(
            editor.toolbarLayout.visibleSections.map { it.visibleEntries.map { entry -> entry.item } },
            headerChromeVisible,
        )
        sections.forEachIndexed { index, section ->
            if (index > 0) Separator()
            for (item in section) {
                when (item) {
                    ToolbarItem.HOME -> ToolbarIcon(NotesTakingIcons.prev, stringResource(R.string.toolbar_home)) { onOpenBackstage() }
                    // Show a compact title indicator where a text label cannot fit.
                    ToolbarItem.TITLE -> {
                        val title = if (editor.document.displayName == null && editor.document.path == null) stringResource(R.string.untitled) else editor.title
                        val description = "${stringResource(R.string.toolbar_title)}: $title"
                        if (LocalBar.current.vertical) {
                            Tooltip(description) {
                                Box(
                                    Modifier.size(LocalBar.current.button)
                                        .toolbarFocusOutline(CircleShape)
                                        .semantics { contentDescription = description }
                                        .focusable(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        NotesTakingIcons.file,
                                        contentDescription = null,
                                        tint = LocalPalette.current.textDim.toComposeColor(),
                                        modifier = Modifier.size(LocalBar.current.icon),
                                    )
                                }
                            }
                        } else {
                            Tooltip(description) {
                                Label(
                                    title,
                                    Modifier.padding(end = 4.dp)
                                        .toolbarFocusOutline(MaterialTheme.shapes.extraSmall)
                                        .semantics { contentDescription = description }
                                        .focusable(),
                                )
                            }
                        }
                    }

                    in CANVAS_TOOL_OF -> {
                        val tool = CANVAS_TOOL_OF.getValue(item)
                        val icon = toolIcons[tool]
                        if (icon != null) {
                            Box {
                                // Tapping the armed tool again opens its settings, as the paged bar does.
                                val armed = editor.tool == tool
                                val configurable = tool.isStroke || tool == Tool.ERASER || tool == Tool.SHAPE || tool == Tool.SELECT || tool == Tool.TAPE
                                val name = if (tool == Tool.ERASER) eraserToolbarDescription(editor) else stringResource(tool.labelRes)
                                val description = if (armed && tool.isStroke) strokeToolAnnouncement(editor, tool, name) else name
                                ToolbarIcon(
                                    icon,
                                    description,
                                    active = armed,
                                    glideKey = tool,
                                    hint = if (armed && configurable) stringResource(R.string.tip_tap_for_settings) else null,
                                ) {
                                    when {
                                        tool == Tool.ERASER && editor.tool == tool -> eraserOpen = true
                                        tool == Tool.SHAPE && editor.tool == tool -> shapeOpen = true
                                        tool == Tool.SELECT && editor.tool == tool -> selectOpen = true
                                        tool == Tool.TAPE && editor.tool == tool -> tapeOpen = true
                                        tool.isStroke && editor.tool == tool -> penOpen = tool
                                        else -> editor.armTool(tool)
                                    }
                                }
                                // The very popups the paged toolbar opens, not lookalikes: same
                                // controls, same wording, same ranges, and they cannot drift apart.
                                if (tool == Tool.ERASER && eraserOpen) EraserConfigPopup(editor) { eraserOpen = false }
                                if (tool == Tool.SHAPE && shapeOpen) ShapeConfigPopup(editor) { shapeOpen = false }
                                if (tool == Tool.SELECT && selectOpen) SelectConfigPopup(editor) { selectOpen = false }
                                if (tool == Tool.TAPE && tapeOpen) TapeConfigPopup(editor) { tapeOpen = false }
                                if (penOpen == tool) ToolConfigPopup(editor, tool) { penOpen = null }
                            }
                        }
                    }

                    ToolbarItem.WAND -> ToolbarIcon(
                        NotesTakingIcons.magicWand,
                        stringResource(R.string.tool_wand),
                        active = editor.wandEnabled,
                        reportSelection = true,
                    ) { editor.toggleWand() }

                    ToolbarItem.IMAGE -> InsertButton(
                        owner = editor,
                        imageRows = listOf(
                            InsertRow(NotesTakingIcons.image, stringResource(R.string.insert_image_ellipsis)) { onInsertImage() },
                            InsertRow(NotesTakingIcons.camera, stringResource(R.string.take_photo)) { onTakePhoto() },
                        ),
                        onAudioFromFile = onInsertAudio,
                        onRecord = onRecordAudio,
                    )

                    ToolbarItem.COLORS ->
                        editor.toolbarColors.take(editor.toolbarColorCount).forEachIndexed { i, color ->
                            Box {
                                Swatch(color.toComposeColor(), active = editor.activeColorIndex == i, drag = editor, ink = color) {
                                    if (editor.activeColorIndex == i) switcherIndex = i else editor.pickColor(i)
                                }
                                if (switcherIndex == i) ColorSwitcherPopup(editor, i) { switcherIndex = null }
                            }
                        }

                    ToolbarItem.UNDO ->
                        ToolbarIcon(NotesTakingIcons.undo, stringResource(R.string.undo), enabled = editor.canUndo, shortcut = Keys.UNDO) { editor.undo() }
                    ToolbarItem.REDO ->
                        ToolbarIcon(NotesTakingIcons.redo, stringResource(R.string.redo), enabled = editor.canRedo, shortcut = Keys.REDO) { editor.redo() }

                    // Where the paged bar keeps its page, styles and view menus. Waypoints take the
                    // place of pagination: on an unbounded canvas, a saved view is what a page
                    // number was.
                    ToolbarItem.STYLES -> Box {
                        ToolbarIcon(NotesTakingIcons.sliders, stringResource(R.string.toolbar_styles), active = stylesOpen) { stylesOpen = true }
                        if (stylesOpen) CanvasStylesPopup(editor) { stylesOpen = false }
                    }
                    ToolbarItem.WAYPOINTS -> Box {
                        ToolbarIcon(NotesTakingIcons.bookmark, stringResource(R.string.toolbar_waypoints), active = waypointsOpen) { waypointsOpen = true }
                        if (waypointsOpen) CanvasWaypointsPopup(editor) { waypointsOpen = false }
                    }
                    ToolbarItem.MINIMAP ->
                        ToolbarIcon(NotesTakingIcons.map, stringResource(R.string.toolbar_minimap), active = editor.minimapVisible, reportSelection = true) {
                            editor.toggleMinimap()
                        }

                    ToolbarItem.ZOOM -> {
                        val viewport = editor.viewport
                        val zoomOutEnabled = toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, editor.zoomLocked, zoomIn = false)
                        val zoomInEnabled = toolbarZoomStepEnabled(viewport.zoom, viewport.minZoom, viewport.maxZoom, editor.zoomLocked, zoomIn = true)
                        ToolbarIcon(NotesTakingIcons.zoomOut, stringResource(R.string.zoom_out), enabled = zoomOutEnabled, shortcut = Keys.ZOOM_OUT) {
                            editor.zoomBy(1.0 / InfiniteEditor.ZOOM_STEP)
                        }
                        val zoomDescription = stringResource(
                            R.string.caption_value_percent,
                            stringResource(R.string.title_zoom),
                            editor.zoomPercent,
                        )
                        Label("${editor.zoomPercent}%", Modifier.semantics { contentDescription = zoomDescription })
                        ToolbarIcon(NotesTakingIcons.zoomIn, stringResource(R.string.zoom_in), enabled = zoomInEnabled, shortcut = Keys.ZOOM_IN) {
                            editor.zoomBy(InfiniteEditor.ZOOM_STEP)
                        }
                    }
                    ToolbarItem.FIT ->
                        ToolbarIcon(NotesTakingIcons.fit, stringResource(R.string.fit_all), enabled = !editor.zoomLocked, shortcut = Keys.FIT_ALL) { editor.zoomToFit() }

                    ToolbarItem.ZOOM_LOCK -> ToolbarIcon(
                        if (editor.zoomLocked) NotesTakingIcons.lock else NotesTakingIcons.unlock,
                        stringResource(if (editor.zoomLocked) R.string.unlock_zoom else R.string.lock_zoom),
                        active = editor.zoomLocked,
                        reportSelection = true,
                    ) { editor.toggleZoomLock() }

                    // Everything else belongs to the paged bar; a canvas layout never holds one.
                    else -> Unit
                }
            }
        }

        Spacer(Modifier.padding(horizontal = 2.dp))
    }
}

/** The size control applies only to ink or erasing, not selection and navigation tools. */
internal fun canvasSizeTool(tool: Tool): Tool? = when {
    tool == Tool.ERASER || tool.isStroke -> tool
    else -> null
}

/** Six fixed actions use the editor's live tool configuration; More opens the B100 toolbar. */
@Composable
internal fun B101CanvasQuickTools(
    editor: InfiniteEditor,
    icons: Map<Tool, ImageVector>,
    columns: Int,
    expand: () -> Unit,
    showMore: Boolean = true,
) {
    var subtypeOpen by remember { mutableStateOf(false) }
    var widthOpen by remember { mutableStateOf(false) }
    var penOpen by remember { mutableStateOf(false) }
    var eraserOpen by remember { mutableStateOf(false) }
    var lastStroke by remember(editor) { mutableStateOf(Tool.PEN) }
    LaunchedEffect(editor.tool) { if (editor.tool.isStroke) lastStroke = editor.tool }
    val stroke = editor.tool.takeIf { it.isStroke } ?: lastStroke
    val widthTool = canvasSizeTool(editor.tool)
    val currentWidth = widthTool?.let { editor.toolConfig(it).baseWidth }
    LaunchedEffect(widthTool) { widthOpen = false }
    val penTypes = listOf(Tool.PEN, Tool.DASHED, Tool.CALLIGRAPHY, Tool.SPEED, Tool.TAPER, Tool.HIGHLIGHTER)
    val actions: List<@Composable () -> Unit> = listOf(
        {
            Box {
                ToolbarIcon(
                    icons[Tool.PEN] ?: NotesTakingIcons.edit,
                    stringResource(R.string.tool_pen),
                    active = editor.tool == Tool.PEN,
                    reportSelection = true,
                    hint = stringResource(R.string.tip_tap_for_settings),
                ) {
                    if (editor.tool == Tool.PEN) penOpen = true else editor.armTool(Tool.PEN)
                }
                if (penOpen) ToolConfigPopup(editor, Tool.PEN) { penOpen = false }
            }
        },
        {
            Box {
                ToolbarIcon(
                    NotesTakingIcons.eraser,
                    eraserToolbarDescription(editor),
                    active = editor.tool == Tool.ERASER,
                    reportSelection = true,
                    hint = stringResource(R.string.tip_tap_for_settings),
                ) {
                    if (editor.tool == Tool.ERASER) eraserOpen = true else editor.armTool(Tool.ERASER)
                }
                if (eraserOpen) EraserConfigPopup(editor) { eraserOpen = false }
            }
        },
        {
            Box {
                ToolbarIcon(
                    icons[stroke] ?: NotesTakingIcons.edit,
                    stringResource(stroke.labelRes),
                    active = editor.tool.isStroke,
                    reportSelection = true,
                ) { subtypeOpen = true }
                DropdownMenu(expanded = subtypeOpen, onDismissRequest = { subtypeOpen = false }) {
                    penTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.labelRes)) },
                            onClick = { subtypeOpen = false; editor.armTool(type) },
                        )
                    }
                }
            }
        },
        {
            Box {
                ToolbarIcon(
                    NotesTakingIcons.sliders,
                    if (currentWidth != null) "${stringResource(R.string.caption_size)}: ${"%.1f".format(currentWidth)}"
                    else stringResource(R.string.caption_size),
                    enabled = widthTool != null,
                ) { widthOpen = true }
                Text(
                    currentWidth?.let { "%.1f".format(it) } ?: "",
                    modifier = Modifier.align(Alignment.BottomCenter).clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelSmall,
                )
                if (widthOpen && widthTool != null) {
                    if (widthTool == Tool.ERASER) EraserConfigPopup(editor) { widthOpen = false }
                    else ToolConfigPopup(editor, widthTool) { widthOpen = false }
                }
            }
        },
        {
            ToolbarIcon(
                NotesTakingIcons.undo,
                stringResource(R.string.undo),
                enabled = editor.canUndo,
            ) { editor.undo() }
        },
        {
            ToolbarIcon(NotesTakingIcons.more, stringResource(R.string.canvas_all_tools), onClick = expand)
        },
    )
    val visibleActions = if (showMore) actions else actions.dropLast(1)
    Column {
        visibleActions.indices.chunked(columns.coerceIn(1, 3)).forEach { row ->
            Row { row.forEach { visibleActions[it]() } }
        }
    }
}

/** Fixed Canvas actions for the floating dock; the dock supplies its own More button. */
@Composable
internal fun B101FloatingCanvasQuickTools(editor: InfiniteEditor, columns: Int) {
    val icons = mapOf(
        Tool.PEN to ImageVector.vectorResource(R.drawable.ic_stroke_regular),
        Tool.DASHED to ImageVector.vectorResource(R.drawable.ic_stroke_dashed),
        Tool.CALLIGRAPHY to ImageVector.vectorResource(R.drawable.ic_stroke_calligraphy),
        Tool.SPEED to ImageVector.vectorResource(R.drawable.ic_stroke_speed),
        Tool.TAPER to ImageVector.vectorResource(R.drawable.ic_stroke_taper),
        Tool.HIGHLIGHTER to ImageVector.vectorResource(R.drawable.ic_stroke_highlighter),
    )
    B101CanvasQuickTools(editor, icons, columns, expand = {}, showMore = false)
}

/** Preserve both failure messages while occupying just one pinned toolbar slot. */
internal fun infiniteCanvasFailureMessage(
    glFailed: Boolean,
    lowMemory: Boolean,
    glLabel: String,
    memoryLabel: String,
): String? = when {
    glFailed && lowMemory -> "$glLabel; $memoryLabel"
    glFailed -> glLabel
    lowMemory -> memoryLabel
    else -> null
}

/** Critical render failures must remain discoverable when the tool strip scrolls. */
@Composable
private fun InfiniteCanvasWarning(message: String) {
    Tooltip(message) {
        Box(
            Modifier.size(LocalBar.current.button)
                .toolbarFocusOutline(CircleShape)
                .semantics {
                    contentDescription = message
                    liveRegion = LiveRegionMode.Polite
                }
                .focusable(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                NotesTakingIcons.info,
                contentDescription = null,
                modifier = Modifier.size(LocalBar.current.icon),
                tint = LocalPalette.current.textDim.toComposeColor(),
            )
        }
    }
}
