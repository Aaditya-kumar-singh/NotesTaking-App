package com.notestaking.app.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B100 keyboard/drawer integration source contracts. These cannot substitute for Compose,
 * hardware Back, focus traversal, accessibility speech, or screenshot tests.
 */
class ToolbarDrawerKeyboardContractTest {
    private fun source(name: String): String {
        val path = "src/main/java/com/notestaking/app/ui/$name"
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.isFile }
        requireNotNull(file) { "Missing source: $path" }
        return file.readText(Charsets.UTF_8)
    }

    private fun between(source: String, start: String, end: String): String {
        val from = source.indexOf(start)
        require(from >= 0) { "Missing start: $start" }
        val to = source.indexOf(end, from + start.length)
        require(to > from) { "Missing end: $end" }
        return source.substring(from, to)
    }

    @Test fun dockedDrawerBackPrecedesPaneCloseAndUsesExistingToolActions() {
        val frame = source("ToolbarFrame.kt")
        val drawer = between(frame, "internal fun ToolbarFrame(", "val surface =")
        assertTrue(drawer.contains("rememberSaveable { mutableStateOf(false) }"))
        val backHandler = drawer.lineSequence().first { it.contains("BackHandler(enabled =") }
        assertTrue(backHandler.contains("!look.floating"))
        assertTrue(backHandler.contains("collapsed != null || compactControls != null"))
        assertTrue(backHandler.contains("&& drawerOpen"))
        assertTrue(backHandler.contains("{ drawerOpen = false }"))
        assertTrue(drawer.contains("if (drawingDrawerDismissOnSelection(previousArmed, armed))"))
        assertTrue(drawer.contains("drawerOpen = false"))
        assertTrue(drawer.contains("content()"))
        assertFalse(drawer.contains("editor.selectTool("))
        assertFalse(drawer.contains("editor.undo("))
        val paged = source("Toolbar.kt")
        val canvas = source("InfiniteToolbar.kt")
        assertTrue(paged.contains("if (!headerChromeVisible) onClosePane?.let { ClosePaneButton(it) }"))
        assertTrue(canvas.contains("if (!headerChromeVisible) onClosePane?.let { ClosePaneButton(it) }"))
    }

    @Test fun floatingDrawerHasItsOwnBackHandlerAndDoesNotReplaceTheCanvas() {
        val floating = source("FloatingToolbar.kt")
        val drawer = between(floating, "var expanded by remember", "val length =")
        assertTrue(drawer.contains("BackHandler(enabled = expanded) { expanded = false }"))
        assertTrue(floating.contains("CompositionLocalProvider(LocalDrawingToolSelected provides { expanded = false })"))
        assertTrue(floating.contains("ToolbarIcon(NotesTakingIcons.chevronUp, stringResource(R.string.toolbar_expand_all_tools))"))
        assertTrue(floating.contains("ToolbarIcon(NotesTakingIcons.chevronDown, stringResource(R.string.toolbar_collapse_to_favorites))"))
        assertTrue(floating.contains("if (showFavorites) favorites(count)"))
        assertTrue(floating.contains("if (look.position.vertical)"))
        assertFalse(floating.contains("AndroidView("))
    }

    @Test fun chevronsAndCurrentToolUseExistingAccessibleButtonSemantics() {
        val toolbar = source("Toolbar.kt")
        val frame = source("ToolbarFrame.kt")
        val canvas = source("InfiniteToolbar.kt")
        assertTrue(frame.contains("ToolbarIcon(NotesTakingIcons.chevronUp, stringResource(R.string.toolbar_expand_all_tools))"))
        assertTrue(frame.contains("ToolbarIcon(NotesTakingIcons.chevronDown, stringResource(R.string.toolbar_collapse_to_favorites))"))
        assertTrue(toolbar.contains("stringResource(editor.tool.labelRes)"))
        assertTrue(canvas.contains("stringResource(editor.tool.labelRes)"))
        val icon = between(toolbar, "internal fun ToolbarIcon(", "/** Closes this pane")
        assertTrue(icon.contains(".toolbarFocusOutline(CircleShape)"))
        assertTrue(icon.contains("role = Role.Button"))
        assertTrue(icon.contains("Icon(icon, contentDescription = contentDescription"))
        assertTrue(icon.contains(".size(bar.button)"))
    }

    // MainActivity keyboard routing belongs to the coordinator. No test may require
    // its current preview interception to remain in place after a legitimate fix.
}
