package com.vrw.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vrw.app.builder.LayoutElement
import com.vrw.app.settings.LayoutRepository

@Composable
fun WheelScreen(
    onOpenBuilder: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTelemetry: () -> Unit
) {
    val activeLayout = remember { LayoutRepository.getActiveLayout() }

    Box(modifier = Modifier.fillMaxSize()) {
        DynamicLayoutRenderer(
            activeLayout = activeLayout,
            onElementValueChanged = { _, _ -> /* wired to ControlUdpClient in the real app */ }
        )

        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Button(onClick = onOpenBuilder) { Text("Edit Layout") }
                Button(onClick = onOpenSettings) { Text("Settings") }
                Button(onClick = onOpenTelemetry) { Text("Telemetry") }
            }
        }
    }
}
