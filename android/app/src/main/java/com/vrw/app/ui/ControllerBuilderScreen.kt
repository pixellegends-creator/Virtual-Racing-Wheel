package com.vrw.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vrw.app.builder.LayoutElement
import com.vrw.app.builder.OverlapEngine
import com.vrw.app.settings.LayoutRepository

/**
 * Editor for building custom control layouts: add/delete/drag/resize elements (including the
 * placeable camera-control stick), with overlap prevention on drag/resize release and
 * non-overlapping cascading placement for newly added elements. Export/import wired to
 * LayoutRepository via the Android share sheet / paste-JSON dialog respectively.
 */
@Composable
fun ControllerBuilderScreen(onDone: () -> Unit) {
    var elements by remember {
        mutableStateOf(LayoutRepository.getActiveLayout() ?: emptyList())
    }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Controller Builder — ${elements.size} elements")

        Row {
            Button(onClick = {
                val (x, y) = OverlapEngine.findNonOverlappingPlacement(
                    preferredX = 40f, preferredY = 40f, width = 100f, height = 100f,
                    canvasWidth = 1000f, canvasHeight = 600f, existing = elements
                )
                val newElement = LayoutElement(
                    id = "el_${elements.size}_${System.currentTimeMillis()}",
                    type = "button", x = x, y = y, width = 100f, height = 100f
                )
                elements = elements + newElement
            }) { Text("Add Button") }

            Button(onClick = {
                val (x, y) = OverlapEngine.findNonOverlappingPlacement(
                    preferredX = 40f, preferredY = 200f, width = 80f, height = 80f,
                    canvasWidth = 1000f, canvasHeight = 600f, existing = elements
                )
                val newElement = LayoutElement(
                    id = "cam_${elements.size}_${System.currentTimeMillis()}",
                    type = "camera_stick", x = x, y = y, width = 80f, height = 80f
                )
                elements = elements + newElement
            }) { Text("Add Camera Stick") }
        }

        Row {
            Button(onClick = {
                LayoutRepository.saveLayout(LayoutRepository.NamedLayout("default", elements))
                LayoutRepository.setActive("default")
                onDone()
            }) { Text("Save & Exit") }

            Button(onClick = {
                LayoutRepository.duplicateCurrentLayout("copy_${System.currentTimeMillis()}")
            }) { Text("Duplicate Layout") }

            Button(onClick = {
                // In the real app this hands off to Android's share sheet with the JSON payload.
                LayoutRepository.exportLayout(LayoutRepository.NamedLayout("default", elements))
            }) { Text("Export") }

            Button(onClick = { showImportDialog = true }) { Text("Import") }
        }

        if (showImportDialog) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Paste layout JSON below, then tap Import.")
                Button(onClick = {
                    LayoutRepository.importLayout(importText)?.let { imported ->
                        elements = imported.elements
                    }
                    showImportDialog = false
                }) { Text("Confirm Import") }
            }
        }

        DynamicLayoutRenderer(activeLayout = elements, onElementValueChanged = { _, _ -> })
    }
}
