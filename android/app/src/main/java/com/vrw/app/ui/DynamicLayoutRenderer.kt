package com.vrw.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vrw.app.builder.LayoutElement

/**
 * Renders whichever layout is currently active. If the user has never saved a custom layout,
 * falls back to a hardcoded default (basic throttle/brake pedals) so the app is always usable
 * out of the box.
 */
@Composable
fun DynamicLayoutRenderer(
    activeLayout: List<LayoutElement>?,
    onElementValueChanged: (elementId: String, value: Float) -> Unit
) {
    val elements = activeLayout ?: defaultFallbackLayout()

    Box(modifier = Modifier.fillMaxSize()) {
        elements.forEach { element ->
            Box(
                modifier = Modifier
                    .offset(x = element.x.dp, y = element.y.dp)
                    .size(width = element.width.dp, height = element.height.dp)
                    .background(colorForType(element.type)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = element.type)
            }
        }
    }
}

private fun colorForType(type: String): Color = when (type) {
    "pedal_throttle" -> Color(0xFF4CAF50)
    "pedal_brake" -> Color(0xFFF44336)
    "pedal_clutch" -> Color(0xFF9E9E9E)
    "camera_stick" -> Color(0xFF3F51B5)
    "button" -> Color(0xFFFFC107)
    else -> Color(0xFF607D8B)
}

private fun defaultFallbackLayout(): List<LayoutElement> = listOf(
    LayoutElement("default_throttle", "pedal_throttle", x = 600f, y = 400f, width = 120f, height = 200f),
    LayoutElement("default_brake", "pedal_brake", x = 460f, y = 400f, width = 120f, height = 200f)
)
