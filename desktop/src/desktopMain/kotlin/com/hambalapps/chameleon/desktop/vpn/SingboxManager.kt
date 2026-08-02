package com.hambalapps.chameleon.desktop.vpn

import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.data.serializeSubscriptions
import com.hambalapps.chameleon.desktop.ui.subs.fetchSubscription
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object SingboxManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null
    
    private val _vpnState = MutableStateFlow("DISCONNECTED")
    val vpnState: StateFlow<String> = _vpnState.asStateFlow()

    private val _vpnLogs = MutableStateFlow("")
    val vpnLogs: StateFlow<String> = _vpnLogs.asStateFlow()

    private val _trafficStats = MutableStateFlow(Pair(0L, 0L)) // upload, download in bytes/sec
    val trafficStats: StateFlow<Pair<Long, Long>> = _trafficStats.asStateFlow()

    val workingDir = File(System.getProperty("user.home"), ".chameleon").apply { mkdirs() }
    val exeFile = File(workingDir, "sing-box.exe")
    val geoip = File(workingDir, "geoip-ir.srs")
    val geosite = File(workingDir, "geosite-ir.srs")
    val configFile = File(workingDir, "config.json")
    val logFile = File(workingDir, "vpn.log")

    private var statsJob: Job? = null
    private var readerJob: Job? = null
    private var watcherJob: Job? = null

    init {
        // Extract assets from JAR resources to working directory
        extractResource("sing-box.exe", exeFile)
        extractResource("geoip-ir.srs", geoip)
        extractResource("geosite-ir.srs", geosite)
        
        // Add shutdown hook to disable proxy and kill sing-box on exit
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })
    }

    private fun extractResource(name: String, dest: File) {
        try {
            val stream: InputStream? = SingboxManager::class.java.classLoader.getResourceAsStream(name)
            if (stream != null) {
                Files.copy(stream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                stream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun autoUpdateSubscriptionsIfNeeded(settingsManager: SettingsManager) {
        scope.launch {
            try {
                val settings = settingsManager.currentSettings
                if (!settings.autoUpdateSubs) return@launch

                val intervalMs = settings.autoUpdateIntervalHours * 3600 * 1000L
                val now = System.currentTimeMillis()

                if ((now - settings.lastSubsUpdateTime) >= intervalMs) {
                    log("[AutoUpdate] Running background subscription refresh...")
                    val subs = settings.deserializedSubscriptions
                    val updatedSubs = subs.map { sub ->
                        if (sub.url.startsWith("http")) {
                            val res = fetchSubscription(sub.url)
                            sub.copy(
                                servers = res.servers.joinToString("\n"),
                                upload = res.upload ?: sub.upload,
                                download = res.download ?: sub.download,
                                total = res.total ?: sub.total,
                                expire = res.expire ?: sub.expire
                            )
                        } else sub
                    }
                    settingsManager.setSubscriptionList(serializeSubscriptions(updatedSubs))
                    settingsManager.setLastSubsUpdateTime(now)
                    log("[AutoUpdate] Subscriptions refreshed successfully.")
                }
            } catch (e: Exception) {
                log("[AutoUpdate Error] ${e.message}")
            }
        }
    }

    private fun downloadWintunIfNeeded() {
        val wintunFile = File(workingDir, "wintun.dll")
        if (wintunFile.exists()) return

        log("wintun.dll is missing. Downloading TUN driver...")
        try {
            val url = URL("https://www.wintun.net/builds/wintun-0.14.1.zip")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode == 200) {
                val arch = System.getProperty("os.arch").lowercase()
                val archDir = when {
                    arch.contains("amd64") || arch.contains("x86_64") -> "amd64"
                    arch.contains("arm") || arch.contains("aarch64") -> "arm64"
                    arch.contains("x86") || arch.contains("i386") -> "x86"
                    else -> "amd64"
                }
                val targetEntry = "wintun/bin/$archDir/wintun.dll"
                log("Extracting $targetEntry for architecture: $arch")

                java.util.zip.ZipInputStream(conn.inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == targetEntry) {
                            Files.copy(zis, wintunFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                            log("TUN driver (wintun.dll) installed successfully.")
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            } else {
                log("Failed to download TUN driver. HTTP code: ${conn.responseCode}")
            }
            conn.disconnect()
        } catch (e: Exception) {
            log("Failed to download TUN driver: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun start(rawProfile: String, settingsManager: SettingsManager): Boolean {
        if (_vpnState.value == "CONNECTED" || _vpnState.value == "CONNECTING") {
            stop()
        }

        _vpnState.value = "CONNECTING"
        _vpnLogs.value = "Starting sing-box...\n"
        
        try {
            // Trigger subscription auto-update check on connection start
            autoUpdateSubscriptionsIfNeeded(settingsManager)

            // Check for admin privileges if TUN is enabled
            val useTun = settingsManager.currentSettings.enableTun
            if (useTun) {
                if (!isRunningAsAdmin()) {
                    log("[ERROR] TUN Mode requires Administrator privileges!")
                    log("Please right click Chameleon.exe and select 'Run as Administrator'.")
                    stop()
                    return false
                }
                downloadWintunIfNeeded()
            }

            val effectiveSettings = settingsManager.currentSettings

            extractResource("geoip-ir.srs", geoip)
            extractResource("geosite-ir.srs", geosite)
            extractResource("wintun.dll", File(workingDir, "wintun.dll"))

            val profileToUse = if (rawProfile.isNotEmpty()) rawProfile else {
                val allServers = settingsManager.currentSettings.allSubscriptionServers + "\n" + settingsManager.currentSettings.manualServers
                allServers.lines().firstOrNull { it.trim().isNotEmpty() } ?: ""
            }

            if (profileToUse.isEmpty()) {
                log("[ERROR] No proxy node selected and no nodes found in subscriptions/manual links!")
                log("Please add a subscription URL or paste nodes in the Subscriptions screen.")
                stop()
                return false
            }

            val configJson = ConfigInjector.injectConfig(
                rawProfile = profileToUse,
                settings = effectiveSettings,
                geoipPath = geoip.absolutePath.replace("\\", "/"),
                geositePath = geosite.absolutePath.replace("\\", "/"),
                logPath = logFile.absolutePath.replace("\\", "/")
            )
            configFile.writeText(configJson)

            if (logFile.exists()) logFile.delete()

            val pb = ProcessBuilder(exeFile.absolutePath, "run", "-c", configFile.absolutePath)
            pb.directory(workingDir)
            pb.redirectErrorStream(true)
            
            val proc = pb.start()
            process = proc

            readerJob = scope.launch {
                proc.inputStream.bufferedReader().use { reader ->
                    while (proc.isAlive) {
                        val line = reader.readLine() ?: break
                        log(line)
                    }
                }
            }

            watcherJob = scope.launch {
                try {
                    proc.waitFor()
                    if (process == proc) {
                        log("Sing-box process terminated unexpectedly.")
                        stop()
                    }
                } catch (e: Exception) {}
            }

            delay(1000)
            if (!proc.isAlive) {
                val exitCode = proc.exitValue()
                log("Failed to start VPN: sing-box process exited immediately with code $exitCode")
                try {
                    if (logFile.exists()) {
                        val errorLog = logFile.readText()
                        if (errorLog.contains("configure tun interface: Access is denied")) {
                            log("CRITICAL ERROR: TUN interface configuration failed (Access is denied). Please run Chameleon as Administrator!")
                        } else {
                            log("Sing-box Error Log:\n$errorLog")
                        }
                    }
                } catch (le: Exception) {}
                stop()
                return false
            }

            if (!useTun) {
                val proxyPort = 2080
                SystemProxy.enable("127.0.0.1", proxyPort)
            } else {
                SystemProxy.disable()
            }

            _vpnState.value = "CONNECTED"
            log("VPN Connected successfully.")

            startStatsPolling()

            return true
        } catch (e: Exception) {
            log("Failed to start VPN: ${e.message}")
            e.printStackTrace()
            stop()
            return false
        }
    }

    fun stop() {
        _vpnState.value = "DISCONNECTING"
        
        SystemProxy.disable()
        
        // Cancel all background coroutines to ensure clean JVM shutdown
        readerJob?.cancel()
        readerJob = null
        watcherJob?.cancel()
        watcherJob = null
        statsJob?.cancel()
        statsJob = null
        _trafficStats.value = Pair(0L, 0L)

        process?.let { proc ->
            proc.destroy()
            runBlocking {
                withTimeoutOrNull(2000) {
                    while (proc.isAlive) {
                        delay(100)
                    }
                }
            }
            if (proc.isAlive) {
                try {
                    proc.destroyForcibly()
                } catch (e: Exception) {}
            }
        }
        process = null
        
        _vpnState.value = "DISCONNECTED"
        log("VPN Disconnected.")
    }

    private fun log(message: String) {
        val line = message + "\n"
        val current = _vpnLogs.value
        _vpnLogs.value = if (current.length > 15000) {
            current.takeLast(10000).substringAfter("\n") + line
        } else {
            current + line
        }
    }

    fun clearLogs() {
        _vpnLogs.value = ""
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                try {
                    val url = URL("http://127.0.0.1:9090/traffic")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 0
                    
                    if (conn.responseCode == 200) {
                        conn.inputStream.bufferedReader().use { reader ->
                            while (isActive) {
                                val line = reader.readLine() ?: break
                                val up = line.substringAfter("\"up\":").substringBefore(",").trim().toLongOrNull() ?: 0L
                                val down = line.substringAfter("\"down\":").substringBefore("}").trim().toLongOrNull() ?: 0L
                                _trafficStats.value = Pair(up, down)
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {
                    _trafficStats.value = Pair(0L, 0L)
                }
                delay(2000)
            }
        }
    }

    private fun isRunningAsAdmin(): Boolean {
        return try {
            val process = ProcessBuilder("net", "session").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
