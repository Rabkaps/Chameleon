package com.hambalapps.chameleon.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hambalapps.chameleon.MainActivity
import com.hambalapps.chameleon.data.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class VpnQuickSettingsTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        observeJob?.cancel()
        observeJob = serviceScope.launch {
            VpnServiceWrapper.vpnState.collectLatest { state ->
                updateTileState(state)
            }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateTileState(state: String) {
        val tile = qsTile ?: return
        when (state) {
            "CONNECTED" -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Chameleon: Secured"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Connected"
                }
            }
            "CONNECTING" -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Chameleon: Connecting"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Connecting..."
                }
            }
            "DISCONNECTING" -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Chameleon: Disconnecting"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnecting..."
                }
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Chameleon VPN"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnected"
                }
            }
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val currentState = VpnServiceWrapper.vpnState.value
        if (currentState == "CONNECTED" || currentState == "CONNECTING") {
            stopVpnService()
        } else {
            // Check VPN permission first
            if (VpnService.prepare(this) != null) {
                // Open App to grant permission
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                if (isLocked) {
                    unlockAndRun {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val pendingIntent = PendingIntent.getActivity(
                                this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                            startActivityAndCollapse(pendingIntent)
                        } else {
                            @Suppress("DEPRECATION")
                            startActivityAndCollapse(intent)
                        }
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val pendingIntent = PendingIntent.getActivity(
                            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        startActivityAndCollapse(pendingIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        startActivityAndCollapse(intent)
                    }
                }
            } else {
                // Permission is granted, start the VPN
                if (isLocked) {
                    unlockAndRun {
                        startVpnService()
                    }
                } else {
                    startVpnService()
                }
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, VpnServiceWrapper::class.java).apply {
            action = VpnServiceWrapper.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVpnService() {
        val intent = Intent(this, VpnServiceWrapper::class.java).apply {
            action = VpnServiceWrapper.ACTION_STOP
            putExtra("force_stop", true)
        }
        startService(intent)
    }
}
