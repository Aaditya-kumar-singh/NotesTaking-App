package com.notestaking.app.core.model

/** Which editable page layers a selective PDF export should include; the normal export stays unchanged. */
data class ExportContent(
    val strokes: Boolean = true,
    val images: Boolean = true,
    val text: Boolean = true,
    val paper: Boolean = true,
    val bookmarks: Boolean = true,
    val markups: Boolean = true,
)

/**
 * Select items without modifying the note. [ExportContent.text] also controls the separate typed
 * flow when the PDF exporter calls [PageLayers.paint]; this list contains only placed text boxes.
 */
fun List<CanvasItem>.forExport(content: ExportContent): List<CanvasItem> {
    if (content.strokes && content.images && content.text) return forExport()
    return filter { item ->
        item.exported && when (item) {
            is Stroke, is ShapeItem, is ConnectorItem -> content.strokes
            is ImageItem -> content.images
            is TextItem -> content.text
            else -> false
        }
    }
}
