package com.hambalapps.chameleon.vpn

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

import com.hambalapps.chameleon.data.SettingsManager
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
            startVpnService()
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
