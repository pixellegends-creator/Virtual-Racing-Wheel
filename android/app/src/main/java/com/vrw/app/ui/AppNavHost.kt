package com.vrw.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class AppScreen {
    WHEEL, BUILDER, SETTINGS, TELEMETRY, PAIRING, COMMUNITY
}

@Composable
fun AppNavHost() {
    var currentScreen by remember { mutableStateOf(AppScreen.PAIRING) }

    when (currentScreen) {
        AppScreen.PAIRING -> PairingScreen(onPaired = { currentScreen = AppScreen.WHEEL })
        AppScreen.WHEEL -> WheelScreen(
            onOpenBuilder = { currentScreen = AppScreen.BUILDER },
            onOpenSettings = { currentScreen = AppScreen.SETTINGS },
            onOpenTelemetry = { currentScreen = AppScreen.TELEMETRY }
        )
        AppScreen.BUILDER -> ControllerBuilderScreen(onDone = { currentScreen = AppScreen.WHEEL })
        AppScreen.SETTINGS -> SettingsScreen(onDone = { currentScreen = AppScreen.WHEEL })
        AppScreen.TELEMETRY -> TelemetryScreen(onDone = { currentScreen = AppScreen.WHEEL })
        AppScreen.COMMUNITY -> CommunityScreen(onDone = { currentScreen = AppScreen.BUILDER })
    }
}
