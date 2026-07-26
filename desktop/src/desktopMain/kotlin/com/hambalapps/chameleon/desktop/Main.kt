package com.hambalapps.chameleon.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.hambalapps.chameleon.desktop.cli.CliHandler
import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.theme.ExpressiveDesktopTheme
import com.hambalapps.chameleon.desktop.ui.MainScreen
import com.hambalapps.chameleon.desktop.vpn.SingboxManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun main(args: Array<String>) {
    // Check CLI command execution first
    if (CliHandler.handleCli(args)) {
        return
    }

    // Launch Compose Desktop GUI application
    application {
        val settingsManager = remember { SettingsManager() }
        val settings by settingsManager.settings.collectAsState()
        
        var isWindowVisible by remember { mutableStateOf(true) }
        val windowState = rememberWindowState(
            width = 1100.dp,
            height = 750.dp
        )
        val trayState = rememberTrayState()
        val scope = rememberCoroutineScope()

        val vpnState by SingboxManager.vpnState.collectAsState()

        // System Tray Integration
        Tray(
            state = trayState,
            icon = painterResource("icon.ico"),
            tooltip = "Chameleon Desktop ($vpnState)",
            onAction = { isWindowVisible = !isWindowVisible }, // Left-click toggles window
            menu = {
                Item("Show / Hide Window") {
                    isWindowVisible = !isWindowVisible
                }
                Separator()
                Item(if (vpnState == "CONNECTED") "Disconnect VPN" else "Connect VPN") {
                    scope.launch {
                        if (vpnState == "CONNECTED" || vpnState == "CONNECTING") {
                            SingboxManager.stop()
                        } else {
                            val profileToStart = if (settings.activeProfile.isNotEmpty()) settings.activeProfile else {
                                val combined = settings.allSubscriptionServers + "\n" + settings.manualServers
                                combined.lines().firstOrNull { it.trim().isNotEmpty() } ?: ""
                            }
                            SingboxManager.start(profileToStart, settingsManager)
                        }
                    }
                }
                Item("Mode: ${if (settings.enableTun) "TUN Mode" else "System Proxy"}") {
                    settingsManager.setEnableTun(!settings.enableTun)
                }
                Separator()
                Item("Exit Chameleon") {
                    SingboxManager.stop()
                    exitApplication()
                }
            }
        )

        if (isWindowVisible) {
            Window(
                onCloseRequest = {
                    if (settings.minimizeToTray) {
                        isWindowVisible = false
                    } else {
                        SingboxManager.stop()
                        exitApplication()
                    }
                },
                state = windowState,
                title = "Chameleon Desktop",
                icon = painterResource("icon.ico")
            ) {
                ExpressiveDesktopTheme(settingsManager = settingsManager) {
                    MainScreen(settingsManager = settingsManager)
                }
            }
        }
    }
}

fun updateStartupRegistry(enable: Boolean) {
    try {
        val regKey = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        val exePath = System.getProperty("compose.application.configure.path") ?: java.io.File(System.getProperty("user.dir"), "Chameleon.exe").absolutePath
        if (enable) {
            ProcessBuilder("reg", "add", regKey, "/v", "Chameleon", "/t", "REG_SZ", "/d", "\"$exePath\"", "/f").start().waitFor()
        } else {
            ProcessBuilder("reg", "delete", regKey, "/v", "Chameleon", "/f").start().waitFor()
        }
    } catch (e: Exception) {}
}
