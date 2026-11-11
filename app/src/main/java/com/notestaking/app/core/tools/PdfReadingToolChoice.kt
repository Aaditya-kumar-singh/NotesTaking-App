package com.notestaking.app.core.tools

/** Remembers the last PDF annotation tool while Pan is used for reading. Not serialized. */
internal class PdfReadingToolChoice {
    private var annotationTool = Tool.PEN

    fun remember(tool: Tool) {
        if (tool != Tool.PAN && tool != Tool.SCREENSHOT && tool != Tool.IMAGE) annotationTool = tool
    }

    fun toolFor(reading: Boolean): Tool = if (reading) Tool.PAN else annotationTool
}
