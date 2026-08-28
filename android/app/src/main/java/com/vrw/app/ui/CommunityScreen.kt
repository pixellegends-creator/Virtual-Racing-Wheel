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
import com.vrw.app.community.CommunityFeedClient
import com.vrw.app.settings.LayoutRepository

@Composable
fun CommunityScreen(onDone: () -> Unit) {
    var feedUrl by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LayoutRepository.NamedLayout>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Community Layouts (read-only feed, no accounts)")
        Text("Feed URL: $feedUrl")

        Button(onClick = {
            results = CommunityFeedClient.fetchFeed(feedUrl)
        }) { Text("Fetch Feed") }

        results.forEach { layout ->
            Column(modifier = Modifier.padding(4.dp)) {
                Text("${layout.name} (${layout.elements.size} elements)")
                Button(onClick = { LayoutRepository.saveLayout(layout) }) { Text("Import") }
            }
        }

        Button(onClick = onDone) { Text("Back") }
    }
}
