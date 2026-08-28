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

@Composable
fun PairingScreen(onPaired: () -> Unit) {
    var status by remember { mutableStateOf("Not connected") }
    var pendingPin by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pair with PC")
        Text("Status: $status")

        Button(onClick = {
            // In the real app this calls PairingClient.discover(...) and reacts to the result.
            status = "Searching for companion on LAN..."
            pendingPin = "4821"
        }) { Text("Search") }

        pendingPin?.let { pin ->
            Text("Confirm this PIN matches the one on your PC: $pin")
            Button(onClick = {
                status = "Paired"
                pendingPin = null
                onPaired()
            }) { Text("Confirm") }
        }

        Button(onClick = onPaired) { Text("Skip (already paired)") }
    }
}
