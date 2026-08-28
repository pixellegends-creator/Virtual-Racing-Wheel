package com.vrw.app.ui

import androidx.compose.foundation.layout.Column
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
import com.vrw.app.telemetry.TelemetryFrame

@Composable
fun TelemetryScreen(onDone: () -> Unit) {
    var frame by remember { mutableStateOf<TelemetryFrame?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Telemetry")
        if (frame == null) {
            Text("Waiting for data from companion (or run --simulate-telemetry on PC)...")
        } else {
            val f = frame!!
            Text("Speed: ${f.speedKmh} km/h")
            Text("RPM: ${f.rpm.toInt()} / ${f.maxRpm.toInt()}")
            Text("Gear: ${f.gear}")
            Text("Fuel: ${f.fuelLiters} L")
            Text("Lap: ${f.lapTimeSeconds}s")
        }
        Button(onClick = onDone) { Text("Back") }
    }
}
