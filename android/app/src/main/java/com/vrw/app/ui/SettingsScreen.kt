package com.vrw.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vrw.app.settings.DisplayUnits

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    var useMph by remember { mutableStateOf(false) }
    var leftHanded by remember { mutableStateOf(false) }
    var lockDegrees by remember { mutableStateOf(180f) }
    var sensitivity by remember { mutableStateOf(1.0f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings")

        Row {
            Text("Use MPH")
            Switch(checked = useMph, onCheckedChange = { useMph = it })
        }
        Text("Preview: 100 km/h = ${DisplayUnits.convert(100f, if (useMph) DisplayUnits.SpeedUnit.MPH else DisplayUnits.SpeedUnit.KMH)} ${if (useMph) "mph" else "km/h"}")

        Row {
            Text("Left-handed mode")
            Switch(checked = leftHanded, onCheckedChange = { leftHanded = it })
        }

        Text("Wheel lock: ${lockDegrees.toInt()}°")
        Text("Sensitivity: $sensitivity")

        Button(onClick = onDone) { Text("Done") }
    }
}
