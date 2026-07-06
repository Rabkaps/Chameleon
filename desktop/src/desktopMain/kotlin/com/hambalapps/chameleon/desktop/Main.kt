package com.hambalapps.chameleon.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.hambalapps.chameleon.theme.ChameleonTheme
import com.hambalapps.chameleon.desktop.ui.MainScreen

fun main() = application {
    val windowState = rememberWindowState(
        width = 1000.dp,
        height = 700.dp
    )
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Chameleon",
        icon = painterResource("icon.png")
    ) {
        ChameleonTheme {
            MainScreen()
        }
    }
}
