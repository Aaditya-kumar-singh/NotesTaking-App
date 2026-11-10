package com.notestaking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notestaking.app.R
import com.notestaking.app.core.util.ExportPageRange

internal enum class PageExportScope { THIS, SELECTED, ALL, CUSTOM }
internal enum class PageExportFormat { PDF, PNG }
internal enum class PageExportDestination { SAVE, SHARE }

/** Opening context is captured, so changing the page selection behind the dialog cannot change its export. */
internal data class PageExportStart(
    val pageIndex: Int,
    val selected: List<Int>,
    val scope: PageExportScope,
    val destination: PageExportDestination = PageExportDestination.SAVE,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PageExportDialog(
    start: PageExportStart,
    totalPages: Int,
    onDismiss: () -> Unit,
    onExport: (List<Int>, PageExportFormat, PageExportDestination) -> Unit,
) {
    var scope by remember(start) { mutableStateOf(start.scope) }
    var format by remember(start) { mutableStateOf(PageExportFormat.PDF) }
    var destination by remember(start) { mutableStateOf(start.destination) }
    var range by remember(start) { mutableStateOf("") }
    val selected = remember(start, totalPages) {
        start.selected.filter { it in 0 until totalPages }.distinct().sorted()
    }
    val indices: List<Int>? = when (scope) {
        PageExportScope.THIS -> listOf(start.pageIndex).filter { it in 0 until totalPages }
        PageExportScope.SELECTED -> selected
        PageExportScope.ALL -> (0 until totalPages).toList()
        PageExportScope.CUSTOM -> ExportPageRange.parse(range, totalPages)
    }
    val ready = !indices.isNullOrEmpty()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.page_export_title)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.page_export_format), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = format == PageExportFormat.PDF,
                        onClick = { format = PageExportFormat.PDF },
                        label = { Text(stringResource(R.string.page_export_pdf)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = format == PageExportFormat.PNG,
                        onClick = { format = PageExportFormat.PNG },
                        label = { Text(stringResource(R.string.page_export_png)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                Text(
                    stringResource(if (format == PageExportFormat.PDF) R.string.page_export_pdf_hint else R.string.page_export_png_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                )

                Text(stringResource(R.string.page_export_pages), style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = scope == PageExportScope.THIS,
                        onClick = { scope = PageExportScope.THIS },
                        label = { Text(stringResource(R.string.page_export_this_page, start.pageIndex + 1)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    if (selected.isNotEmpty()) {
                        FilterChip(
                            selected = scope == PageExportScope.SELECTED,
                            onClick = { scope = PageExportScope.SELECTED },
                            label = { Text(stringResource(R.string.page_export_selected, selected.size)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    FilterChip(
                        selected = scope == PageExportScope.ALL,
                        onClick = { scope = PageExportScope.ALL },
                        label = { Text(stringResource(R.string.page_export_all, totalPages)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = scope == PageExportScope.CUSTOM,
                        onClick = { scope = PageExportScope.CUSTOM },
                        label = { Text(stringResource(R.string.page_export_custom)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                if (scope == PageExportScope.CUSTOM) {
                    OutlinedTextField(
                        value = range,
                        onValueChange = { range = it },
                        label = { Text(stringResource(R.string.page_export_range_label)) },
                        placeholder = { Text(stringResource(R.string.page_export_range_example)) },
                        singleLine = true,
                        isError = indices == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (indices == null) {
                        Text(
                            stringResource(R.string.page_export_range_error, totalPages),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Text(stringResource(R.string.page_export_destination), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = destination == PageExportDestination.SAVE,
                        onClick = { destination = PageExportDestination.SAVE },
                        label = { Text(stringResource(R.string.save)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = destination == PageExportDestination.SHARE,
                        onClick = { destination = PageExportDestination.SHARE },
                        label = { Text(stringResource(R.string.share)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                Text(
                    stringResource(R.string.page_export_count, indices?.size ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = ready,
                onClick = { indices?.takeIf { it.isNotEmpty() }?.let { onExport(it, format, destination) } },
            ) {
                Text(stringResource(if (destination == PageExportDestination.SAVE) R.string.save else R.string.share))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
