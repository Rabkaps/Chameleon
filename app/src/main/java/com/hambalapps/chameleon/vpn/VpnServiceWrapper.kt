package com.hambalapps.chameleon.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.net.TrafficStats
import android.os.Process
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.hambalapps.chameleon.MainActivity
import com.hambalapps.chameleon.R
import com.hambalapps.chameleon.data.SettingsManager
import com.hambalapps.chameleon.ui.main.NodesPopupActivity
import com.hambalapps.chameleon.data.deserializeSubscriptions
import com.hambalapps.chameleon.data.Subscription
import com.hambalapps.chameleon.Config
import io.nekohasekai.libbox.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import java.nio.charset.StandardCharsets

class VpnServiceWrapper : VpnService(), PlatformInterface, CommandServerHandler {

    companion object {
        const val ACTION_START = "com.hambalapps.chameleon.START"
        const val ACTION_START_PROXY = "com.hambalapps.chameleon.START_PROXY"
        const val ACTION_STOP = "com.hambalapps.chameleon.STOP"
        const val ACTION_SET_MODE = "com.hambalapps.chameleon.SET_MODE"
        const val ACTION_SET_SERVER = "com.hambalapps.chameleon.SET_SERVER"
        private const val CHANNEL_ID = "vpn_service_channel_v2"
        private const val NOTIFICATION_ID = 101

        private val _vpnState = MutableStateFlow("DISCONNECTED")
        val vpnState: StateFlow<String> = _vpnState

        private val _sessionDownBytes = MutableStateFlow(0L)
        val sessionDownBytes: StateFlow<Long> = _sessionDownBytes

        private val _sessionUpBytes = MutableStateFlow(0L)
        val sessionUpBytes: StateFlow<Long> = _sessionUpBytes

        private val _vpnLogs = MutableStateFlow("")
        val vpnLogs: StateFlow<String> = _vpnLogs

        fun log(message: String) {
            android.util.Log.i("Chameleon", message)
            val combined = _vpnLogs.value + message + "\n"
            _vpnLogs.value = if (combined.length > 15000) {
                combined.takeLast(10000).substringAfter("\n", "")
            } else {
                combined
            }
        }

        fun clearLogs() {
            _vpnLogs.value = ""
        }

        fun checkAndLoadCrashLog(context: android.content.Context) {
            var logText = ""
            
            val crashFile = File(context.cacheDir, "crash.log")
            if (crashFile.exists()) {
                logText += "--- PREVIOUS JVM CRASH LOG ---\n" + crashFile.readText() + "\n------------------------------\n"
                try {
                    crashFile.delete()
                } catch (e: Exception) {}
            }

            val logDir = File(context.cacheDir, "logs")
            val errLogFile = File(logDir, "stderr.log")
            if (errLogFile.exists()) {
                val stderrText = errLogFile.readText()
                if (stderrText.trim().isNotEmpty()) {
                    logText += "--- PREVIOUS NATIVE LOG/CRASH ---\n" + stderrText + "\n---------------------------------\n"
                }
                try {
                    errLogFile.delete()
                } catch (e: Exception) {}
            }

            val crashReportFile = File(context.filesDir, "CrashReport-stderr.log")
            if (crashReportFile.exists()) {
                val stderrText = crashReportFile.readText()
                if (stderrText.trim().isNotEmpty()) {
                    logText += "--- PREVIOUS NATIVE LOG/CRASH ---\n" + stderrText + "\n---------------------------------\n"
                }
                try {
                    crashReportFile.delete()
                } catch (e: Exception) {}
            }

            if (logText.isNotEmpty()) {
                _vpnState.value = "DISCONNECTED" // ensure correct state
                _vpnLogs.value = logText
            }
        }
    }

    private var commandServer: CommandServer? = null
    private var localProxyOnlyMode = false
    private var tunFd: ParcelFileDescriptor? = null
    private var tunFdInt: Int = -1
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logReaderJob: Job? = null
    private var trafficMonitorJob: Job? = null
    private var commandClient: CommandClient? = null
    private var accumulatedProxyDown = 0L
    private var accumulatedProxyUp = 0L
    private var defaultInterfaceListener: InterfaceUpdateListener? = null
    private var defaultNetworkCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var lastSentPhysicalName: String? = null
    private var lastSentPhysicalIndex: Int = -1
    private var isForeground = false

    private var splitTunnelingEnabledVal = false
    private var splitTunnelingModeVal = "bypass"
    private var splitTunnelingAppsVal = emptySet<String>()
    private var showLiveNotificationVal = false
    private var activeProfileVal = ""
    private var rootModeVal = false
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    @Volatile
    private var cachedPhysicalNetworkInfo: PhysicalNetworkInfo? = null

    @Volatile
    private var cachedLibboxInterfaces: List<io.nekohasekai.libbox.NetworkInterface>? = null
    @Volatile
    private var lastInterfacesUpdateTime = 0L

    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("Chameleon", "VpnServiceWrapper onCreate called")
        createNotificationChannel()
        
        val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        cachedPhysicalNetworkInfo = getActivePhysicalNetworkInfo(cm)
        
        serviceScope.launch {
            _vpnState.collect { state ->
                if (isForeground) {
                    updateNotification(state)
                }
                updateWakeLock(state)
                
                if (state == "CONNECTED") {
                    trafficMonitorJob?.cancel()
                    accumulatedProxyDown = 0L
                    accumulatedProxyUp = 0L
                    _sessionDownBytes.value = 0L
                    _sessionUpBytes.value = 0L
                    
                    trafficMonitorJob = serviceScope.launch {
                        try {
                            val options = CommandClientOptions().apply {
                                setStatusInterval(1000)
                                addCommand(Libbox.CommandStatus)
                            }
                            
                            val clientHandler = object : CommandClientHandler {
                                override fun clearLogs() {}
                                override fun connected() {}
                                override fun disconnected(message: String?) {}
                                override fun initializeClashMode(modes: StringIterator?, currentMode: String?) {}
                                override fun setDefaultLogLevel(level: Int) {}
                                override fun updateClashMode(mode: String?) {}
                                override fun writeGroups(groups: OutboundGroupIterator?) {}
                                override fun writeLogs(logs: LogIterator?) {}
                                
                                override fun writeStatus(status: StatusMessage?) {
                                    if (status != null) {
                                        _sessionDownBytes.value = status.downlinkTotal
                                        _sessionUpBytes.value = status.uplinkTotal
                                    }
                                }
                                
                                override fun writeConnectionEvents(events: ConnectionEvents?) {}
                            }
                            
                            val client = Libbox.newCommandClient(clientHandler, options)
                            commandClient = client
                            client.connect()
                            
                            // Keep coroutine alive
                            while (isActive) {
                                delay(1000)
                            }
                        } catch (e: Exception) {
                            log("Traffic monitor client error: ${e.message}")
                            // Fallback to TrafficStats if command client fails
                            val uid = Process.myUid()
                            val rxBaseline = TrafficStats.getUidRxBytes(uid)
                            val txBaseline = TrafficStats.getUidTxBytes(uid)
                            while (isActive) {
                                val currentRx = TrafficStats.getUidRxBytes(uid)
                                val currentTx = TrafficStats.getUidTxBytes(uid)
                                val down = if (currentRx != TrafficStats.UNSUPPORTED.toLong() && rxBaseline != TrafficStats.UNSUPPORTED.toLong()) {
                                    (currentRx - rxBaseline).coerceAtLeast(0L)
                                } else {
                                    0L
                                }
                                val up = if (currentTx != TrafficStats.UNSUPPORTED.toLong() && txBaseline != TrafficStats.UNSUPPORTED.toLong()) {
                                    (currentTx - txBaseline).coerceAtLeast(0L)
                                } else {
                                    0L
                                }
                                _sessionDownBytes.value = down
                                _sessionUpBytes.value = up
                                delay(1000)
                            }
                        } finally {
                            try {
                                commandClient?.disconnect()
                            } catch (e: Exception) {}
                            commandClient = null
                        }
                    }
                } else if (state == "DISCONNECTED") {
                    trafficMonitorJob?.cancel()
                    trafficMonitorJob = null
                    _sessionDownBytes.value = 0L
                    _sessionUpBytes.value = 0L
                }

                // Notify home screen widget
                try {
                    val intent = Intent("com.hambalapps.chameleon.widget.ACTION_STATE_CHANGED").apply {
                        putExtra("state", state)
                        setPackage(packageName)
                    }
                    sendBroadcast(intent)
                } catch (e: Exception) {
                    android.util.Log.e("Chameleon", "Failed to broadcast widget update: ${e.message}")
                }
            }
        }

        serviceScope.launch {
            val settingsManager = SettingsManager(applicationContext)
            settingsManager.settings.collect { settings ->
                showLiveNotificationVal = settings.showLiveNotification
                activeProfileVal = settings.activeProfile
                if (isForeground) {
                    updateNotification(_vpnState.value)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        android.util.Log.i("Chameleon", "VpnServiceWrapper onStartCommand called with action: $action")
        if (intent != null) {
            if (intent.hasExtra("active_profile")) {
                activeProfileVal = intent.getStringExtra("active_profile") ?: ""
            }
            if (intent.hasExtra("show_live_notification")) {
                showLiveNotificationVal = intent.getBooleanExtra("show_live_notification", false)
            }
        }
        if (action == ACTION_START) {
            localProxyOnlyMode = false
            if (_vpnState.value == "CONNECTED" || commandServer != null) {
                reloadVpnEngine()
            } else {
                _vpnState.value = "CONNECTING"
                startForegroundServiceNotification()
                startVpnEngine()
            }
        } else if (action == ACTION_START_PROXY) {
            localProxyOnlyMode = true
            if (_vpnState.value != "CONNECTED" && _vpnState.value != "CONNECTING") {
                if (commandServer != null) {
                    reloadVpnEngine()
                } else {
                    _vpnState.value = "DISCONNECTED"
                    startForegroundServiceNotification()
                    startVpnEngine()
                }
            }
        } else if (action == ACTION_STOP) {
            val settingsManager = SettingsManager(applicationContext)
            val forceStop = intent.getBooleanExtra("force_stop", false)
            val enableMtProxyVal = if (forceStop) false else kotlinx.coroutines.runBlocking { settingsManager.enableMtProxy.first() }
            if (enableMtProxyVal) {
                log("VPN stopped, but MTProxy is enabled. Switching to Local Proxy Mode...")
                localProxyOnlyMode = true
                _vpnState.value = "DISCONNECTED"
                startForegroundServiceNotification()
                reloadVpnEngine()
            } else {
                stopVpnEngine()
            }
        } else if (action == ACTION_SET_MODE) {
            val mode = intent.getStringExtra("extra_mode") ?: "standard"
            serviceScope.launch {
                val settingsManager = SettingsManager(applicationContext)
                settingsManager.setVpnMode(mode)
                reloadVpnEngine()
                
                // Notify widget
                val updateIntent = Intent("com.hambalapps.chameleon.widget.ACTION_STATE_CHANGED").apply {
                    putExtra("state", _vpnState.value)
                    setPackage(packageName)
                }
                sendBroadcast(updateIntent)
            }
        } else if (action == ACTION_SET_SERVER) {
            val serverLink = intent.getStringExtra("extra_server_link")
            if (serverLink != null) {
                serviceScope.launch {
                    val settingsManager = SettingsManager(applicationContext)
                    val settings = settingsManager.settings.first()
                    val targetSub = settings.deserializedSubscriptions.find { sub ->
                        sub.servers.split("\n").map { it.trim() }.contains(serverLink.trim())
                    }
                    val subId = targetSub?.id ?: "manual"
                    settingsManager.setActiveSubId(subId)
                    settingsManager.setActiveProfile(serverLink)
                    activeProfileVal = serverLink
                    reloadVpnEngine()
                    
                    // Notify widget
                    val updateIntent = Intent("com.hambalapps.chameleon.widget.ACTION_STATE_CHANGED").apply {
                        putExtra("state", _vpnState.value)
                        setPackage(packageName)
                    }
                    sendBroadcast(updateIntent)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(state: String): android.app.Notification {
        val settingsManager = SettingsManager(applicationContext)
        val nodeName = if (activeProfileVal.isEmpty()) {
            getString(R.string.notif_no_node)
        } else if (activeProfileVal.startsWith("{")) {
            getString(R.string.notif_custom)
        } else {
            ProxyNameResolver.getProxyName(activeProfileVal, applicationContext)
        }
        
        val contentText = if (localProxyOnlyMode) {
            "Telegram Proxy server is running"
        } else {
            when (state) {
                "CONNECTED" -> getString(R.string.notif_connected, nodeName)
                "CONNECTING" -> getString(R.string.notif_connecting, nodeName)
                "DISCONNECTING" -> getString(R.string.notif_disconnecting)
                else -> getString(R.string.notif_disconnected)
            }
        }
        
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Prev Node Intent
        val prevIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_PREV_NODE
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            1,
            prevIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Next Node Intent
        val nextIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NEXT_NODE
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            2,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // List Intent targeting translucent Dialog NodesPopupActivity
        val listIntent = Intent(applicationContext, NodesPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val listPendingIntent = PendingIntent.getActivity(
            applicationContext,
            4,
            listIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Disconnect Intent
        val stopIntent = Intent(applicationContext, VpnServiceWrapper::class.java).apply {
            action = ACTION_STOP
            putExtra("force_stop", true)
        }
        val stopPendingIntent = PendingIntent.getService(
            applicationContext,
            3,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val prevAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_previous,
            getString(R.string.notif_action_prev),
            prevPendingIntent
        ).build()
        
        val nextAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_next,
            getString(R.string.notif_action_next),
            nextPendingIntent
        ).build()
        
        val disconnectAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.notif_action_disconnect),
            stopPendingIntent
        ).build()
        
        val listAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_agenda,
            getString(R.string.notif_action_list),
            listPendingIntent
        ).build()
        
        val showLiveNotif = showLiveNotificationVal
        
        val channelToUse = if (showLiveNotif) "vpn_service_channel_live" else CHANNEL_ID
        val priority = NotificationCompat.PRIORITY_DEFAULT
        
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelToUse)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            
        if (showLiveNotif) {
            notificationBuilder.addAction(nextAction)
        }
        notificationBuilder.addAction(listAction)
        notificationBuilder.addAction(disconnectAction)
            
        if (showLiveNotif) {
            notificationBuilder.extras.putBoolean("android.requestPromotedOngoing", true)
            val shortText = when (state) {
                "CONNECTED" -> {
                    val shortNode = nodeName.substringBefore(" ").take(7)
                    shortNode.ifEmpty { getString(R.string.active_node) }
                }
                "CONNECTING" -> getString(R.string.state_connecting)
                else -> getString(R.string.app_name)
            }
            notificationBuilder.extras.putCharSequence("android.shortCriticalText", shortText)
        }
        
        val notification = notificationBuilder.build()
        notification.flags = notification.flags or android.app.Notification.FLAG_ONGOING_EVENT or android.app.Notification.FLAG_NO_CLEAR
        return notification
    }

    private fun updateNotification(state: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (state == "DISCONNECTED") {
            manager?.cancel(NOTIFICATION_ID)
            return
        }
        val notification = buildNotification(state)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    @Synchronized
    private fun updateWakeLock(state: String) {
        if (state == "CONNECTED" || state == "CONNECTING") {
            if (wakeLock == null) {
                val pm = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
                wakeLock = pm?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Chameleon:VpnWakeLock")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
                log("WakeLock acquired")
            }
        } else {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
                log("WakeLock released")
            }
            wakeLock = null
        }
    }

    private fun startForegroundServiceNotification() {
        log("startForegroundServiceNotification called")
        val showLiveNotif = showLiveNotificationVal
        log("showLiveNotification toggle: $showLiveNotif")
        if (showLiveNotif && Build.VERSION.SDK_INT >= 36) {
            val manager = getSystemService(NotificationManager::class.java)
            val allowed = manager?.canPostPromotedNotifications() ?: false
            log("canPostPromotedNotifications check: $allowed")
        }
        val notification = buildNotification(_vpnState.value)
        
        log("Calling startForeground with ID $NOTIFICATION_ID")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForeground = true
        android.util.Log.i("Chameleon", "startForeground finished execution")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val channelLow = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_chan_status),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            manager?.createNotificationChannel(channelLow)
            
            val channelLive = NotificationChannel(
                "vpn_service_channel_live",
                getString(R.string.notif_chan_live),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            manager?.createNotificationChannel(channelLive)
        }
    }

    private fun startVpnEngine() {
        android.util.Log.i("Chameleon", "startVpnEngine called, launching coroutine")
        serviceScope.launch {
            try {
                log("Starting VPN Engine...")
                copyDatabasesFromAssets()
                val settingsManager = SettingsManager(applicationContext)
                
                val activeSubId = settingsManager.activeSubId.first()
                val autoConnectSubs = settingsManager.autoConnectSubs.first()
                var rawProfile = settingsManager.activeProfile.first()

                if (autoConnectSubs.contains(activeSubId)) {
                    log("Auto-Connect is enabled for active subscription. Finding best node...")
                    val subscriptionListStr = settingsManager.subscriptionList.first()
                    val subscriptions = deserializeSubscriptions(subscriptionListStr)
                    val activeSub = subscriptions.find { it.id == activeSubId }
                        ?: if (activeSubId == "manual") {
                            val manualStr = settingsManager.manualServers.first()
                            Subscription(id = "manual", name = "Manual", url = "local://manual", servers = manualStr)
                        } else null
                    
                    val servers = activeSub?.servers?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    if (servers.isNotEmpty()) {
                        log("Pinging ${servers.size} servers in parallel...")
                        val jobs = servers.map { server ->
                            async(Dispatchers.IO) {
                                val hostPort = getHostAndPortFromLink(server)
                                val delay = if (hostPort != null) {
                                    measurePingDelay(hostPort.first, hostPort.second)
                                } else {
                                    -1
                                }
                                server to delay
                            }
                        }
                        val results = jobs.awaitAll()
                        val bestServer = results.filter { it.second >= 0 }.minByOrNull { it.second }
                        if (bestServer != null) {
                            log("Selected best server: ${ProxyNameResolver.getProxyName(bestServer.first, applicationContext)} with delay ${bestServer.second} ms")
                            settingsManager.setActiveProfile(bestServer.first)
                            rawProfile = bestServer.first
                        } else {
                            log("All pings timed out. Using default active profile.")
                        }
                    } else {
                        log("No servers available for Auto-Connect.")
                    }
                }

                if (rawProfile.trim().isEmpty()) {
                    log("No profile selected. Aborting connection.")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            applicationContext,
                            getString(R.string.notif_no_node),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    _vpnState.value = "DISCONNECTED"
                    stopSelf()
                    return@launch
                }
                
                // Read configurations from Preferences DataStore
                val bypassIranVal = settingsManager.bypassIran.first()
                val secureDnsVal = settingsManager.secureDns.first()
                val tunStackVal = settingsManager.tunStack.first()
                val enableFragmentVal = settingsManager.enableFragment.first()
                val fragmentLengthVal = settingsManager.fragmentLength.first()
                val fragmentIntervalVal = settingsManager.fragmentInterval.first()
                val enableMuxVal = settingsManager.enableMux.first()
                val bypassLanVal = settingsManager.bypassLan.first()
                val vpnModeVal = settingsManager.vpnMode.first()
                var warpPrivateKeyVal = settingsManager.warpPrivateKey.first()
                var warpPublicKeyVal = settingsManager.warpPublicKey.first()
                var warpIpAddressVal = settingsManager.warpIpAddress.first()
                var warpClientIdVal = settingsManager.warpClientId.first()

                if (vpnModeVal == "ai_bypass" && warpPrivateKeyVal.isNotEmpty() && warpClientIdVal.isEmpty()) {
                    log("Upgrading WARP credentials to retrieve Client ID (Reserved Bytes)...")
                    val creds = registerWarpAccount()
                    if (creds != null) {
                        settingsManager.setWarpCredentials(
                            creds.privateKey,
                            creds.publicKey,
                            creds.ipAddress,
                            creds.clientId
                        )
                        warpPrivateKeyVal = creds.privateKey
                        warpPublicKeyVal = creds.publicKey
                        warpIpAddressVal = creds.ipAddress
                        warpClientIdVal = creds.clientId
                        log("WARP credentials upgraded successfully. Client ID: $warpClientIdVal")
                    } else {
                        log("Failed to auto-register WARP credentials for Client ID upgrade.")
                    }
                }

                val vpnModeTunnelGamesVal = settingsManager.vpnModeTunnelGames.first()
                val warpDetourModeVal = settingsManager.warpDetourMode.first()
                val warpPortVal = settingsManager.warpPort.first()
                val warpPeerIpVal = settingsManager.warpPeerIp.first()
                val shareVpnLanVal = settingsManager.settings.first().shareVpnLan
                val shareVpnPortVal = settingsManager.settings.first().shareVpnPort
                val enableMtProxyVal = settingsManager.enableMtProxy.first()
                val mtProxyPortVal = settingsManager.mtProxyPort.first()
                val mtProxySecretVal = settingsManager.mtProxySecret.first()
                val proxyChainsVal = settingsManager.proxyChains.first()
                val camouflageSettingsVal = settingsManager.camouflageSettings.first()
                val globalCamouflageEnabledVal = settingsManager.globalCamouflageEnabled.first()
                val globalCamouflagePresetVal = settingsManager.globalCamouflagePreset.first()
                val globalCamouflageSniVal = settingsManager.globalCamouflageSni.first()
                val globalCamouflageHostVal = settingsManager.globalCamouflageHost.first()
                val globalCamouflageCustomIpsVal = settingsManager.globalCamouflageCustomIps.first()
                val globalCamouflageTimeoutVal = settingsManager.globalCamouflageTimeout.first()
                val globalCamouflagePinnedIpVal = settingsManager.globalCamouflagePinnedIp.first()
                splitTunnelingEnabledVal = settingsManager.splitTunnelingEnabled.first()
                splitTunnelingModeVal = settingsManager.splitTunnelingMode.first()
                splitTunnelingAppsVal = settingsManager.splitTunnelingApps.first()
                val enableDebugLoggingVal = settingsManager.enableDebugLogging.first()
                val vpnMtuVal = settingsManager.vpnMtu.first()
                rootModeVal = settingsManager.rootMode.first()

                if (!rootModeVal) {
                    log("Standard Mode active. Ensuring any previous transparent proxy rules are cleared...")
                    val cleanupCommands = listOf(
                        "iptables -t nat -D OUTPUT -p tcp -j EXPRESSIVEBOX 2>/dev/null || true",
                        "iptables -t nat -F EXPRESSIVEBOX 2>/dev/null || true",
                        "iptables -t nat -X EXPRESSIVEBOX 2>/dev/null || true"
                    )
                    runRootCommands(cleanupCommands)
                }

                val injectorSettings = InjectorSettings(
                    bypassIran = bypassIranVal,
                    secureDns = secureDnsVal,
                    tunStack = tunStackVal,
                    enableFragment = enableFragmentVal,
                    fragmentLength = fragmentLengthVal,
                    fragmentInterval = fragmentIntervalVal,
                    enableMux = enableMuxVal,
                    bypassLan = bypassLanVal,
                    vpnMode = vpnModeVal,
                    warpPrivateKey = warpPrivateKeyVal,
                    warpPublicKey = warpPublicKeyVal,
                    warpIpAddress = warpIpAddressVal,
                    warpClientId = warpClientIdVal,
                    vpnModeTunnelGames = vpnModeTunnelGamesVal,
                    enableDebugLogging = enableDebugLoggingVal,
                    vpnMtu = vpnMtuVal,
                    warpDetourMode = warpDetourModeVal,
                    warpPort = warpPortVal,
                    warpPeerIp = warpPeerIpVal,
                    shareVpnLan = shareVpnLanVal,
                    shareVpnPort = shareVpnPortVal,
                    proxyChains = proxyChainsVal,
                    camouflageSettings = camouflageSettingsVal,
                    globalCamouflageEnabled = globalCamouflageEnabledVal,
                    globalCamouflagePreset = globalCamouflagePresetVal,
                    globalCamouflageSni = globalCamouflageSniVal,
                    globalCamouflageHost = globalCamouflageHostVal,
                    globalCamouflageCustomIps = globalCamouflageCustomIpsVal,
                    globalCamouflageTimeout = globalCamouflageTimeoutVal,
                    globalCamouflagePinnedIp = globalCamouflagePinnedIpVal,
                    rootMode = rootModeVal,
                    enableMtProxy = enableMtProxyVal,
                    mtProxyPort = mtProxyPortVal,
                    mtProxySecret = mtProxySecretVal,
                    localProxyOnly = localProxyOnlyMode
                )

                // Inject our custom bypass-Iran rules, split DNS, and advanced parameters
                val configJson = ConfigInjector.injectConfig(
                    applicationContext,
                    rawProfile,
                    injectorSettings
                )

                log("Generated Config JSON:\n$configJson")
                log("Configuration ready. Setting up environment...")
                
                // Redirect standard error of Go core to filesDir/CrashReport-stderr.log automatically via Setup
                val crashReportFile = File(filesDir, "CrashReport-stderr.log")
                if (crashReportFile.exists()) {
                    try { crashReportFile.delete() } catch (e: Exception) {}
                }
                
                val vpnLogFile = File(cacheDir, "vpn.log")
                startLogReader(vpnLogFile)

                log("Instantiating SetupOptions...")
                val setupOptions = SetupOptions()
                log("Setting BasePath...")
                setupOptions.setBasePath(filesDir.absolutePath)
                log("Setting WorkingPath...")
                setupOptions.setWorkingPath(filesDir.absolutePath)
                log("Setting TempPath...")
                setupOptions.setTempPath(cacheDir.absolutePath)
                log("Setting FixAndroidStack...")
                setupOptions.setFixAndroidStack(true)
                log("Setting CommandServerListenPort...")
                setupOptions.setCommandServerListenPort(3000)
                log("Calling Libbox.setup...")
                try {
                    Libbox.setup(setupOptions)
                    log("Libbox.setup finished successfully.")
                } catch (e: Throwable) {
                    log("Libbox setup warning: ${e.message}")
                }

                // Ensure any previous CommandServer and service instance are closed before starting a new one
                var hadPreviousServer = false
                if (commandServer != null) {
                    hadPreviousServer = true
                    try {
                        commandServer?.closeService()
                    } catch (e: Exception) {}
                    try {
                        commandServer?.close()
                    } catch (e: Exception) {}
                    commandServer = null
                }
                if (hadPreviousServer) {
                    delay(500)
                }

                log("Creating CommandServer...")
                commandServer = Libbox.newCommandServer(this@VpnServiceWrapper, this@VpnServiceWrapper)
                
                log("Starting CommandServer...")
                commandServer?.start()

                log("Starting sing-box service...")
                val overrideOptions = OverrideOptions().apply {
                    autoRedirect = false
                }
                tunFd = null
                commandServer?.startOrReloadService(configJson, overrideOptions)

                if (!rootModeVal && !localProxyOnlyMode) {
                    // Wait up to 10 seconds for sing-box core to initialize the TUN interface
                    var waitCount = 0
                    while (tunFd == null && waitCount < 100) {
                        delay(100)
                        waitCount++
                    }

                    if (tunFd == null) {
                        throw IllegalStateException("sing-box core failed to initialize TUN interface (timeout)")
                    }
                }

                if (localProxyOnlyMode) {
                    _vpnState.value = "DISCONNECTED"
                    log("Local Proxy running successfully (VPN Tunnel is disabled).")
                } else {
                    _vpnState.value = "CONNECTED"
                    log("VPN Connected successfully.")
                }
                if (rootModeVal) {
                    log("Root Mode is enabled. Configuring transparent proxy iptables rules...")
                    val portVal = shareVpnPortVal.toIntOrNull() ?: 10808
                    val myUid = android.os.Process.myUid()
                    val commands = listOf(
                        "iptables -t nat -F EXPRESSIVEBOX 2>/dev/null || true",
                        "iptables -t nat -X EXPRESSIVEBOX 2>/dev/null || true",
                        "iptables -t nat -N EXPRESSIVEBOX",
                        "iptables -t nat -A EXPRESSIVEBOX -d 0.0.0.0/8 -j RETURN",
                        "iptables -t nat -A EXPRESSIVEBOX -d 10.0.0.0/8 -j RETURN",
                        "iptables -t nat -A EXPRESSIVEBOX -d 127.0.0.0/8 -j RETURN",
                        "iptables -t nat -A EXPRESSIVEBOX -d 169.254.0.0/16 -j RETURN",
                        "iptables -t nat -A EXPRESSIVEBOX -d 172.16.0.0/12 -j RETURN",
                        "iptables -t nat -A EXPRESSIVEBOX -d 192.168.0.0/16 -j RETURN",
                        "iptables -t nat -A EXPRESSIVEBOX -m owner --uid-owner $myUid -j RETURN",
                        "iptables -t nat -A EXPRESSIVEBOX -p tcp -j REDIRECT --to-ports $portVal",
                        "iptables -t nat -D OUTPUT -p tcp -j EXPRESSIVEBOX 2>/dev/null || true",
                        "iptables -t nat -A OUTPUT -p tcp -j EXPRESSIVEBOX"
                    )
                    val success = runRootCommands(commands)
                    if (success) {
                        log("Transparent proxy iptables rules applied successfully.")
                    } else {
                        log("Failed to apply transparent proxy iptables rules.")
                    }
                }
                downloadDatabasesIfMissing()
            } catch (e: Throwable) {
                log("Failed to start VPN: ${e.message}")
                e.printStackTrace()
                stopVpnEngine()
            }
        }
    }

    @Volatile
    private var isReloading = false

    private fun reloadVpnEngine() {
        serviceScope.launch {
            if (isReloading) {
                log("Reconnection already in progress. Skipping duplicate event.")
                return@launch
            }
            isReloading = true
            try {
                log("Reconnecting VPN Engine (Teardown & Re-initialize)...")
                _vpnState.value = "CONNECTING"
                
                // Teardown core service
                val closeJob = kotlin.concurrent.thread(start = true) {
                    try {
                        commandServer?.closeService()
                    } catch (e: Exception) {
                        android.util.Log.e("Chameleon", "Error closing service during reconnect: ${e.message}")
                    }
                }
                val startTime = System.currentTimeMillis()
                while (closeJob.isAlive && System.currentTimeMillis() - startTime < 500) {
                    delay(50)
                }
                
                try {
                    commandServer?.close()
                } catch (e: Exception) {}
                commandServer = null
                
                try {
                    tunFd?.close()
                } catch (e: Exception) {}
                tunFd = null

                if (tunFdInt != -1) {
                    try {
                        log("Adopting and closing TUN file descriptor $tunFdInt during reconnect...")
                        ParcelFileDescriptor.adoptFd(tunFdInt).close()
                    } catch (e: Exception) {
                        log("Error closing adopted FD during reconnect: ${e.message}")
                    }
                    tunFdInt = -1
                }
                
                stopLogReader()
                lastSentPhysicalName = null
                lastSentPhysicalIndex = -1
                
                // Wait a moment for interface teardown to stabilize
                kotlinx.coroutines.delay(500)
                
                log("Re-initializing VPN core...")
                startForegroundServiceNotification()
                startVpnEngine()
            } catch (e: Throwable) {
                log("Failed to reconnect VPN: ${e.message}")
                e.printStackTrace()
                stopVpnEngine()
            } finally {
                isReloading = false
            }
        }
    }

    private fun stopVpnEngine() {
        localProxyOnlyMode = false
        serviceScope.launch {
            try {
                log("Stopping VPN Engine...")
                _vpnState.value = "DISCONNECTING"
                
                withContext(NonCancellable) {
                    log("Stopping core service...")
                    val closeJob = kotlin.concurrent.thread(start = true) {
                        try {
                            commandServer?.closeService()
                        } catch (e: Exception) {
                            android.util.Log.e("Chameleon", "Error closing service: ${e.message}")
                        }
                    }
                    val startTime = System.currentTimeMillis()
                    while (closeJob.isAlive && System.currentTimeMillis() - startTime < 500) {
                        delay(50)
                    }
                    
                    log("Stopping CommandServer...")
                    try {
                        commandServer?.close()
                    } catch (e: Exception) {}
                    commandServer = null
                    
                    try {
                        tunFd?.close()
                    } catch (e: Exception) {
                        // Ignore detached close warnings
                    }
                    tunFd = null

                    if (tunFdInt != -1) {
                        try {
                            log("Adopting and closing TUN file descriptor $tunFdInt...")
                            ParcelFileDescriptor.adoptFd(tunFdInt).close()
                            log("TUN file descriptor closed.")
                        } catch (e: Exception) {
                            log("Error closing adopted FD: ${e.message}")
                        }
                        tunFdInt = -1
                    }

                    if (rootModeVal) {
                        log("Root Mode is enabled. Cleaning up transparent proxy iptables rules...")
                        val commands = listOf(
                            "iptables -t nat -D OUTPUT -p tcp -j EXPRESSIVEBOX 2>/dev/null || true",
                            "iptables -t nat -F EXPRESSIVEBOX 2>/dev/null || true",
                            "iptables -t nat -X EXPRESSIVEBOX 2>/dev/null || true"
                        )
                        val success = runRootCommands(commands)
                        if (success) {
                            log("Transparent proxy iptables rules cleared successfully.")
                        } else {
                            log("Failed to clear transparent proxy iptables rules.")
                        }
                    }

                    log("VPN Engine stopped.")
                }
            } catch (e: Throwable) {
                log("Error stopping VPN: ${e.message}")
            } finally {
                stopLogReader()
                lastSentPhysicalName = null
                lastSentPhysicalIndex = -1
                _vpnState.value = "DISCONNECTED"
                val manager = getSystemService(NotificationManager::class.java)
                manager?.cancel(NOTIFICATION_ID)
                stopForeground(true)
                isForeground = false
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        localProxyOnlyMode = false
        updateWakeLock("DISCONNECTED")
        if (rootModeVal) {
            val commands = listOf(
                "iptables -t nat -D OUTPUT -p tcp -j EXPRESSIVEBOX 2>/dev/null || true",
                "iptables -t nat -F EXPRESSIVEBOX 2>/dev/null || true",
                "iptables -t nat -X EXPRESSIVEBOX 2>/dev/null || true"
            )
            runRootCommands(commands)
        }
        super.onDestroy()
        
        // Ensure core service and command server are closed
        val closeJob = kotlin.concurrent.thread(start = true) {
            try {
                commandServer?.closeService()
            } catch (e: Exception) {}
            try {
                commandServer?.close()
            } catch (e: Exception) {}
        }
        try {
            closeJob.join(500)
        } catch (e: Exception) {}
        commandServer = null

        // Ensure TUN descriptor is closed
        try {
            tunFd?.close()
        } catch (e: Exception) {}
        tunFd = null

        if (tunFdInt != -1) {
            try {
                ParcelFileDescriptor.adoptFd(tunFdInt).close()
            } catch (e: Exception) {}
            tunFdInt = -1
        }

        stopLogReader()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.cancel(NOTIFICATION_ID)
        _vpnState.value = "DISCONNECTED"
        serviceScope.cancel()
    }

    private fun startLogReader(logFile: File) {
        logReaderJob = serviceScope.launch {
            try {
                _vpnLogs.value = ""
                var readOffset = 0L
                while (isActive) {
                    if (logFile.exists()) {
                        val len = logFile.length()
                        val hasSubscribers = _vpnLogs.subscriptionCount.value > 0
                        
                        if (hasSubscribers && len > readOffset) {
                            logFile.inputStream().use { stream ->
                                if (len - readOffset > 30000) {
                                    val skipPos = len - 30000
                                    stream.skip(skipPos)
                                    readOffset = skipPos
                                } else {
                                    stream.skip(readOffset)
                                }
                                val bytes = stream.readBytes()
                                if (bytes.isNotEmpty()) {
                                    val newLogs = String(bytes, StandardCharsets.UTF_8)
                                    val combined = _vpnLogs.value + newLogs
                                    _vpnLogs.value = if (combined.length > 15000) {
                                        combined.takeLast(10000).substringAfter("\n", "")
                                    } else {
                                        combined
                                    }
                                }
                            }
                            readOffset = len
                        }
                    }
                    delay(500)
                }
            } catch (e: Exception) {
                log("Log reader error: ${e.message}")
            }
        }
    }

    private fun stopLogReader() {
        logReaderJob?.cancel()
        logReaderJob = null
    }

    // --- PlatformInterface Implementation ---

    override fun openTun(options: TunOptions): Int {
        try {
            log("openTun called by sing-box core (MTU: ${options.getMTU()}, AutoRoute: ${options.getAutoRoute()})")
            val builder = Builder()
                .setSession("Chameleon")
                .setMtu(options.getMTU().let { if (it > 0) it else 1500 })

            // 0. Configure Split Tunneling (Per-App Routing)
            if (splitTunnelingEnabledVal) {
                log("Applying Split Tunneling. Mode: $splitTunnelingModeVal, Apps: $splitTunnelingAppsVal")
                for (pkg in splitTunnelingAppsVal) {
                    if (pkg.isNotEmpty() && pkg != packageName) {
                        try {
                            if (splitTunnelingModeVal == "bypass") {
                                builder.addDisallowedApplication(pkg)
                            } else {
                                builder.addAllowedApplication(pkg)
                            }
                        } catch (e: Exception) {
                            log("Failed to apply split tunneling for package $pkg: ${e.message}")
                        }
                    }
                }
                if (splitTunnelingModeVal != "bypass") {
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (e: Exception) {
                        log("Failed to allow own package: ${e.message}")
                    }
                }
            }

            var addedIpv4Address = false
            var addedIpv6Address = false

            // 1. Configure Local Address
            val ipv4Addresses = options.getInet4Address()
            if (ipv4Addresses != null && ipv4Addresses.hasNext()) {
                while (ipv4Addresses.hasNext()) {
                    val prefix = ipv4Addresses.next()
                    try {
                        builder.addAddress(prefix.address(), prefix.prefix())
                        addedIpv4Address = true
                    } catch (e: Exception) {
                        log("Failed to add IPv4 address ${prefix.address()}: ${e.message}")
                    }
                }
            }
            
            if (!addedIpv4Address) {
                try {
                    builder.addAddress("172.19.0.1", 30)
                    addedIpv4Address = true
                } catch (e: Exception) {
                    log("Failed to add default IPv4 address: ${e.message}")
                }
            }

            val ipv6Addresses = options.getInet6Address()
            if (ipv6Addresses != null) {
                while (ipv6Addresses.hasNext()) {
                    val prefix = ipv6Addresses.next()
                    try {
                        builder.addAddress(prefix.address(), prefix.prefix())
                        addedIpv6Address = true
                    } catch (e: Exception) {
                        log("Failed to add IPv6 address ${prefix.address()}: ${e.message}")
                    }
                }
            }

            // 2. Configure Route redirects
            val ipv4Routes = options.getInet4RouteAddress()
            if (ipv4Routes != null && ipv4Routes.hasNext()) {
                while (ipv4Routes.hasNext()) {
                    val prefix = ipv4Routes.next()
                    try {
                        builder.addRoute(prefix.address(), prefix.prefix())
                    } catch (e: Exception) {
                        log("Failed to add IPv4 route ${prefix.address()}: ${e.message}")
                    }
                }
            } else if (options.getAutoRoute() && addedIpv4Address) {
                try {
                    builder.addRoute("0.0.0.0", 0)
                } catch (e: Exception) {
                    log("Failed to add default IPv4 route: ${e.message}")
                }
            }

            val ipv6Routes = options.getInet6RouteAddress()
            if (ipv6Routes != null && ipv6Routes.hasNext()) {
                while (ipv6Routes.hasNext()) {
                    val prefix = ipv6Routes.next()
                    try {
                        builder.addRoute(prefix.address(), prefix.prefix())
                    } catch (e: Exception) {
                        log("Failed to add IPv6 route ${prefix.address()}: ${e.message}")
                    }
                }
            } else if (options.getAutoRoute() && addedIpv6Address) {
                try {
                    builder.addRoute("::", 0)
                } catch (e: Exception) {
                    log("Failed to add default IPv6 route: ${e.message}")
                }
            }

            // 3. Configure DNS Servers
            val dnsServer = options.getDNSServerAddress()
            if (dnsServer != null && dnsServer.getValue().isNotEmpty()) {
                try {
                    builder.addDnsServer(dnsServer.getValue())
                } catch (e: Exception) {
                    log("Failed to add DNS server ${dnsServer.getValue()}: ${e.message}")
                }
                // Add one more IPv4 address for robust DNS hijacking
                try {
                    if (dnsServer.getValue() != "8.8.8.8") {
                        builder.addDnsServer("8.8.8.8")
                    } else {
                        builder.addDnsServer("1.1.1.1")
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            } else {
                try {
                    builder.addDnsServer("172.19.0.1")
                    builder.addDnsServer("8.8.8.8")
                } catch (e: Exception) {
                    // Ignore
                }
            }

            // 4. Establish TUN Interface and detach file descriptor
            val pfd = try {
                builder.establish()
            } catch (e: Exception) {
                log("VpnService establish threw: ${e.message}")
                null
            } ?: return -1 // Do NOT throw exception to Go thread (crashes JVM), return -1
            
            tunFd = pfd
            val fd = pfd.detachFd()
            tunFdInt = fd
            log("TUN interface established. FD: $fd")
            return fd
        } catch (t: Throwable) {
            log("Fatal error in openTun JNI callback: ${t.message}")
            t.printStackTrace()
            return -1
        }
    }

    override fun autoDetectInterfaceControl(fd: Int) {
        val success = protect(fd)
        if (!success) {
            log("Failed to protect socket FD: $fd")
        }
    }
    override fun clearDNSCache() {}
    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        log("closeDefaultInterfaceMonitor called")
        defaultInterfaceListener = null
        val callback = defaultNetworkCallback
        if (callback != null) {
            val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (e: Exception) {}
            defaultNetworkCallback = null
        }
    }
    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String?,
        sourcePort: Int,
        destinationAddress: String?,
        destinationPort: Int
    ): ConnectionOwner {
        return ConnectionOwner().apply {
            userId = -1
        }
    }
    private fun createStringIterator(list: List<String>): StringIterator {
        return object : StringIterator {
            private var index = 0
            override fun hasNext(): Boolean = index < list.size
            override fun len(): Int = list.size
            override fun next(): String {
                if (index >= list.size) return ""
                return list[index++]
            }
        }
    }

    class PhysicalNetworkInfo(
        val name: String,
        val index: Int,
        val metered: Boolean,
        val constrained: Boolean
    )

    private fun getNetworkInterfaceByName(name: String): java.net.NetworkInterface? {
        try {
            val enumeration = java.net.NetworkInterface.getNetworkInterfaces() ?: return null
            while (enumeration.hasMoreElements()) {
                val javaIf = enumeration.nextElement() ?: continue
                if (javaIf.name == name) {
                    return javaIf
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun getFallbackPhysicalInterfaceName(): String? {
        try {
            val enumeration = java.net.NetworkInterface.getNetworkInterfaces()
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    val javaIf = enumeration.nextElement() ?: continue
                    val name = javaIf.name ?: continue
                    val isLoopback = try { javaIf.isLoopback } catch (e: Exception) { false }
                    if (isLoopback) continue
                    
                    val lower = name.lowercase()
                    if (lower.startsWith("tun") || lower.startsWith("vpn") || lower.startsWith("ppp") || 
                        lower.startsWith("lo") || lower.startsWith("dummy") || lower.startsWith("tap")) {
                        continue
                    }
                    return name
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun getActivePhysicalNetworkInfo(cm: android.net.ConnectivityManager?): PhysicalNetworkInfo? {
        if (cm == null) return null
        try {
            val networks = cm.allNetworks
            for (network in networks) {
                val capabilities = cm.getNetworkCapabilities(network) ?: continue
                
                // Must NOT be a VPN
                val isVpn = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
                if (isVpn) continue
                
                val isWifi = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                val isCellular = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                val isEthernet = capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                
                if (isWifi || isCellular || isEthernet) {
                    val lp = cm.getLinkProperties(network) ?: continue
                    val name = lp.interfaceName ?: continue
                    val javaIf = getNetworkInterfaceByName(name)
                    val index = javaIf?.index ?: 0
                    val metered = !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    val constrained = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
                    } else {
                        false
                    }
                    return PhysicalNetworkInfo(name, index, metered, constrained)
                }
            }
            
            // Fallback 1: system-wide active network if not VPN
            val activeNetwork = cm.activeNetwork
            if (activeNetwork != null) {
                val capabilities = cm.getNetworkCapabilities(activeNetwork)
                if (capabilities != null && !capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)) {
                    val lp = cm.getLinkProperties(activeNetwork)
                    val name = lp?.interfaceName
                    if (name != null) {
                        val javaIf = getNetworkInterfaceByName(name)
                        val index = javaIf?.index ?: 0
                        val metered = !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        val constrained = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            !capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
                        } else {
                            false
                        }
                        return PhysicalNetworkInfo(name, index, metered, constrained)
                    }
                }
            }

            // Fallback 2: Scan local network interfaces to find physical interface name
            val fallbackName = getFallbackPhysicalInterfaceName()
            if (fallbackName != null) {
                val javaIf = getNetworkInterfaceByName(fallbackName)
                if (javaIf != null) {
                    return PhysicalNetworkInfo(javaIf.name, javaIf.index, false, false)
                }
            }
        } catch (e: Exception) {
            log("Error in getActivePhysicalNetworkInfo: ${e.message}")
        }
        return null
    }

    private fun buildLibboxInterface(
        javaIf: java.net.NetworkInterface,
        metered: Boolean
    ): io.nekohasekai.libbox.NetworkInterface {
        val libboxIf = io.nekohasekai.libbox.NetworkInterface()
        libboxIf.setName(javaIf.name)
        libboxIf.setIndex(javaIf.index)
        
        val mtu = try { javaIf.mtu } catch (e: Exception) { 1500 }
        libboxIf.setMTU(mtu)
        
        var ifFlags = 0
        val isUp = try { javaIf.isUp } catch (e: Exception) { true }
        if (isUp) ifFlags = ifFlags or 1
        val isPointToPoint = try { javaIf.isPointToPoint } catch (e: Exception) { false }
        if (!isPointToPoint) ifFlags = ifFlags or 2
        val isLoopback = try { javaIf.isLoopback } catch (e: Exception) { false }
        if (isLoopback) ifFlags = ifFlags or 4
        if (isPointToPoint) ifFlags = ifFlags or 8
        val supportsMulticast = try { javaIf.supportsMulticast() } catch (e: Exception) { false }
        if (supportsMulticast) ifFlags = ifFlags or 16
        libboxIf.setFlags(ifFlags)

        val addrs = mutableListOf<String>()
        try {
            val addressesEnum = javaIf.inetAddresses
            while (addressesEnum.hasMoreElements()) {
                val addr = addressesEnum.nextElement() ?: continue
                if (!addr.isLoopbackAddress) {
                    var hostAddr = addr.hostAddress ?: continue
                    if (hostAddr.contains("%")) {
                        hostAddr = hostAddr.substringBefore("%")
                    }
                    var prefixLength = 24
                    try {
                        val interfaceAddresses = javaIf.interfaceAddresses
                        for (ifAddr in interfaceAddresses) {
                            if (ifAddr.address == addr) {
                                prefixLength = ifAddr.networkPrefixLength.toInt()
                                break
                            }
                        }
                    } catch (e: Exception) {}
                    addrs.add("$hostAddr/$prefixLength")
                }
            }
        } catch (e: Exception) {}
        libboxIf.setAddresses(createStringIterator(addrs))
        
        return libboxIf
    }

    private fun rebuildInterfacesList(): List<io.nekohasekai.libbox.NetworkInterface> {
        val list = mutableListOf<io.nekohasekai.libbox.NetworkInterface>()
        try {
            val physicalInfo = cachedPhysicalNetworkInfo
            val physicalName = physicalInfo?.name
            val physicalMetered = physicalInfo?.metered ?: false

            val enumeration = java.net.NetworkInterface.getNetworkInterfaces()
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    val javaIf = enumeration.nextElement() ?: continue
                    try {
                        val isLoopback = try { javaIf.isLoopback } catch (e: Exception) { false }
                        if (isLoopback) continue
                        
                        val isMetered = if (javaIf.name == physicalName) physicalMetered else false
                        val libboxIf = buildLibboxInterface(javaIf, isMetered)
                        list.add(libboxIf)
                    } catch (e: Exception) {
                        log("Error processing interface ${javaIf.name}: ${e.message}")
                    }
                }
            }

            // Always ensure the active physical network interface is in the list
            if (physicalInfo != null) {
                val alreadyInList = list.any { it.getName() == physicalInfo.name }
                if (!alreadyInList) {
                    val javaIf = getNetworkInterfaceByName(physicalInfo.name)
                    if (javaIf != null) {
                        val libboxIf = buildLibboxInterface(javaIf, physicalInfo.metered)
                        list.add(libboxIf)
                    }
                }
            }
        } catch (e: Exception) {
            log("Error rebuilding network interfaces list: ${e.message}")
        }
        return list
    }

    override fun getInterfaces(): NetworkInterfaceIterator? {
        val now = System.currentTimeMillis()
        val cacheDuration = 4000L // 4 seconds TTL cache
        
        val finalList: List<io.nekohasekai.libbox.NetworkInterface>
        synchronized(this) {
            var cached = cachedLibboxInterfaces
            if (cached == null || now - lastInterfacesUpdateTime > cacheDuration) {
                cached = rebuildInterfacesList()
                cachedLibboxInterfaces = cached
                lastInterfacesUpdateTime = now
            }
            finalList = cached
        }

        return object : NetworkInterfaceIterator {
            private var idx = 0
            override fun hasNext(): Boolean = idx < finalList.size
            override fun next(): io.nekohasekai.libbox.NetworkInterface? {
                if (idx >= finalList.size) return null
                return finalList[idx++]
            }
        }
    }

    override fun includeAllNetworks(): Boolean = false
    override fun readWIFIState(): WIFIState? = null

    override fun localDNSTransport(): LocalDNSTransport? = null
    override fun bindInterfaceControl(fd: Int, interfaceName: String?) {
        log("bindInterfaceControl: fd=$fd, name=$interfaceName")
    }
    override fun systemCertificates(): StringIterator? = null
    override fun sendNotification(notification: Notification) {}

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        log("startDefaultInterfaceMonitor called")
        defaultInterfaceListener = listener
        val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return
        
        // Resolve initial physical default interface
        val info = getActivePhysicalNetworkInfo(cm)
        if (info != null) {
            log("Sending initial default interface: name=${info.name}, index=${info.index}, metered=${info.metered}, constrained=${info.constrained}")
            try {
                listener.updateDefaultInterface(info.name, info.index, info.metered, info.constrained)
                lastSentPhysicalName = info.name
                lastSentPhysicalIndex = info.index
            } catch (e: Exception) {
                log("Error updating default interface: ${e.message}")
            }
        } else {
            log("No initial physical default interface found")
        }

        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                updatePhysicalInterface()
            }

            override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                // Ignore to avoid high CPU binder calls on signal strength updates
            }

            override fun onLinkPropertiesChanged(network: android.net.Network, lp: android.net.LinkProperties) {
                // Ignore to avoid high CPU binder calls on link property changes
            }

            override fun onLost(network: android.net.Network) {
                updatePhysicalInterface()
            }

            private fun updatePhysicalInterface() {
                val currentInfo = getActivePhysicalNetworkInfo(cm)
                cachedPhysicalNetworkInfo = currentInfo
                synchronized(this@VpnServiceWrapper) {
                    cachedLibboxInterfaces = null
                }
                if (currentInfo != null) {
                    if (currentInfo.name != lastSentPhysicalName || currentInfo.index != lastSentPhysicalIndex) {
                        log("Default physical interface updated: name=${currentInfo.name}, index=${currentInfo.index}, metered=${currentInfo.metered}, constrained=${currentInfo.constrained}")
                        try {
                            defaultInterfaceListener?.updateDefaultInterface(currentInfo.name, currentInfo.index, currentInfo.metered, currentInfo.constrained)
                            lastSentPhysicalName = currentInfo.name
                            lastSentPhysicalIndex = currentInfo.index
                            
                            // Hot-reload configuration to pick up new system DNS servers
                            reloadVpnEngine()
                        } catch (e: Exception) {
                            log("Error sending default interface update: ${e.message}")
                        }
                    }
                } else {
                    log("No active physical network found during callback update")
                }
            }
        }
        defaultNetworkCallback = callback
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback)
                log("Successfully registered default network callback")
            } else {
                val builder = android.net.NetworkRequest.Builder()
                cm.registerNetworkCallback(builder.build(), callback)
                log("Successfully registered global network callback (legacy)")
            }
        } catch (e: Exception) {
            log("Failed to register network callback: ${e.message}")
        }
    }
    override fun underNetworkExtension(): Boolean = false
    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
    override fun useProcFS(): Boolean = false

    // --- CommandServerHandler Implementation ---

    override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus().apply {
        available = false
        enabled = false
    }

    override fun serviceReload() {
        log("Core service reloaded.")
    }

    override fun serviceStop() {
        log("CommandServer requested service stop.")
        stopVpnEngine()
    }



    override fun writeDebugMessage(message: String?) {
        android.util.Log.d("ChameleonCore", message ?: "")
    }

    override fun setSystemProxyEnabled(enabled: Boolean) {}

    private fun downloadDatabasesIfMissing() {
        serviceScope.launch {
            try {
                val geoipFile = File(filesDir, "geoip-ir.srs")
                val geositeFile = File(filesDir, "geosite-ir.srs")
                
                if (!geoipFile.exists()) {
                    log("Background downloading geoip-ir.srs from Chocolate4U...")
                    downloadFile("https://raw.githubusercontent.com/Chocolate4U/Iran-sing-box-rules/rule-set/geoip-ir.srs", geoipFile)
                    log("geoip-ir.srs downloaded successfully.")
                }
                if (!geositeFile.exists()) {
                    log("Background downloading geosite-ir.srs from Chocolate4U...")
                    downloadFile("https://raw.githubusercontent.com/Chocolate4U/Iran-sing-box-rules/rule-set/geosite-ir.srs", geositeFile)
                    log("geosite-ir.srs downloaded successfully.")
                }
            } catch (e: Exception) {
                log("Background assets download failed: ${e.message}")
            }
        }
    }

    private fun downloadFile(urlStr: String, destFile: File) {
        val url = java.net.URL(urlStr)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.connect()
        
        if (connection.responseCode == 200) {
            val tempFile = File(destFile.parentFile, destFile.name + ".tmp")
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.renameTo(destFile)
        } else {
            throw java.io.IOException("HTTP error ${connection.responseCode} downloading $urlStr")
        }
    }

    private fun copyDatabasesFromAssets() {
        try {
            // Cleanup legacy .db files if they exist to save space
            val legacyGeoip = File(filesDir, "geoip.db")
            val legacyGeosite = File(filesDir, "geosite.db")
            if (legacyGeoip.exists()) legacyGeoip.delete()
            if (legacyGeosite.exists()) legacyGeosite.delete()

            val geoipFile = File(filesDir, "geoip-ir.srs")
            val geositeFile = File(filesDir, "geosite-ir.srs")
            
            if (!geoipFile.exists()) {
                log("Copying geoip-ir.srs from assets...")
                assets.open("geoip-ir.srs").use { input ->
                    geoipFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                log("geoip-ir.srs copied from assets.")
            }
            if (!geositeFile.exists()) {
                log("Copying geosite-ir.srs from assets...")
                assets.open("geosite-ir.srs").use { input ->
                    geositeFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                log("geosite-ir.srs copied from assets.")
            }
        } catch (e: Exception) {
            log("Error copying databases from assets: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun runRootCommands(commands: List<String>): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream.bufferedWriter()
            for (cmd in commands) {
                os.write(cmd + "\n")
            }
            os.write("exit\n")
            os.flush()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    log("Root command execution timed out.")
                    return false
                }
                process.exitValue() == 0
            } else {
                val result = process.waitFor()
                result == 0
            }
        } catch (e: java.io.IOException) {
            log("Root command failed (IOException): ${e.message}")
            false
        } catch (e: Exception) {
            log("Root command failed: ${e.message}")
            false
        }
    }
}
