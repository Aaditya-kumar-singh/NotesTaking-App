package com.notestaking.app.settings

import com.notestaking.app.core.tools.Tool

/** How the editor toolbar is drawn; one setting for both the paged and the canvas bar. */
data class ToolbarLook(
    val position: ToolbarPosition = ToolbarPosition.AUTO,
    val size: ToolbarSize = ToolbarSize.REGULAR,
    /** A rounded pill over the canvas instead of a strip along the pane's edge. */
    val floating: Boolean = false,
    /** Normalized position within the pane's free space. Null follows [position]. */
    val floatingX: Float? = null,
    val floatingY: Float? = null,
    /** Quick dock order; its first four tools are used in phone-sized panes. */
    val favoriteTools: List<Tool> = DEFAULT_FAVORITES,
) {
    val floatingFractions: Pair<Float, Float>
        get() = if (floatingX != null && floatingY != null) floatingX to floatingY else when (position) {
            ToolbarPosition.TOP, ToolbarPosition.AUTO -> 0.5f to 0f
            ToolbarPosition.BOTTOM -> 0.5f to 1f
            ToolbarPosition.LEFT -> 0f to 0.5f
            ToolbarPosition.RIGHT -> 1f to 0.5f
        }

    companion object {
        const val MAX_FAVORITES = 8
        val FAVORITE_CHOICES: List<Tool> = listOf(
            Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER, Tool.LASSO,
            Tool.DASHED, Tool.CALLIGRAPHY, Tool.SPEED, Tool.TAPER, Tool.SELECT,
        )
        val DEFAULT_FAVORITES: List<Tool> = FAVORITE_CHOICES.take(MAX_FAVORITES)

        fun sanitizeFavorites(tools: List<Tool>): List<Tool> = tools
            .filter { it in FAVORITE_CHOICES }.distinct().take(MAX_FAVORITES)
            .ifEmpty { DEFAULT_FAVORITES }

        /** Keep the grip and expand button reachable at every phone, tablet or split-pane size. */
        fun visibleFavoriteCount(shortSideDp: Int, axisDp: Int, buttonDp: Int): Int {
            val limit = if (shortSideDp >= 600) MAX_FAVORITES else 4
            val slot = buttonDp + 8
            val available = axisDp.coerceAtMost(600) // The compact dock's maximum edge length.
            return ((available - 16 - 48 - 8 - slot) / slot).coerceIn(1, limit)
        }
    }
}

/** Which edge of its pane the toolbar runs along. */
enum class ToolbarPosition(val id: String) {
    AUTO("auto"),
    TOP("top"),
    BOTTOM("bottom"),
    LEFT("left"),
    RIGHT("right");

    val vertical: Boolean get() = this == LEFT || this == RIGHT

    /** Resolve against the active window, so a tablet in split-screen uses phone placement. */
    fun resolve(windowShortSideDp: Int): ToolbarPosition = when {
        this != AUTO -> this
        windowShortSideDp >= 600 -> TOP
        else -> BOTTOM
    }

    companion object {
        fun fromId(id: String): ToolbarPosition = entries.find { it.id == id } ?: TOP
    }
}

/** How big the toolbar's buttons are. */
enum class ToolbarSize(val id: String) {
    COMPACT("compact"),
    REGULAR("regular"),
    COMFORTABLE("comfortable");

    companion object {
        fun fromId(id: String): ToolbarSize = entries.find { it.id == id } ?: REGULAR
    }
}
