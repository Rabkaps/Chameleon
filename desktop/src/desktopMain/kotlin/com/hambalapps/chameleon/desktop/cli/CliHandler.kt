package com.hambalapps.chameleon.desktop.cli

import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.vpn.CdnIpScanner
import com.hambalapps.chameleon.desktop.vpn.SingboxManager
import com.hambalapps.chameleon.desktop.vpn.getHostAndPortFromLink
import com.hambalapps.chameleon.desktop.vpn.measurePingDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object CliHandler {

    fun handleCli(args: Array<String>): Boolean {
        if (args.isEmpty()) return false // Launch GUI by default

        val command = args[0].lowercase()

        when (command) {
            "help", "--help", "-h" -> {
                printHelp()
                return true
            }
            "status" -> {
                runBlocking {
                    val status = SingboxManager.vpnState.value
                    val settings = SettingsManager.settings.first()
                    println("==========================================")
                    println("        Chameleon Desktop CLI Status      ")
                    println("==========================================")
                    println(" Status      : $status")
                    println(" Mode        : ${if (settings.enableTun) "WinTUN Mode" else "System Proxy Mode"}")
                    println(" Active Node : ${settings.activeProfile.ifEmpty { "None" }}")
                    println("==========================================")
                }
                return true
            }
            "connect", "start" -> {
                val profileArg = if (args.size > 1) args[1] else ""
                runBlocking {
                    val settingsManager = SettingsManager()
                    val settings = settingsManager.currentSettings
                    val profileToUse = profileArg.ifEmpty { settings.activeProfile }

                    if (profileToUse.isEmpty()) {
                        println("[ERROR] No profile specified and no active profile set in settings.")
                        println("Usage: chameleon connect <profile_link_or_url>")
                        return@runBlocking
                    }

                    println("[Chameleon CLI] Connecting to profile...")
                    val success = SingboxManager.start(profileToUse, settingsManager)
                    if (success) {
                        println("[SUCCESS] VPN Connected successfully.")
                    } else {
                        println("[ERROR] Failed to connect VPN.")
                    }
                }
                return true
            }
            "disconnect", "stop" -> {
                println("[Chameleon CLI] Stopping VPN...")
                SingboxManager.stop()
                println("[SUCCESS] VPN Disconnected.")
                return true
            }
            "nodes", "list" -> {
                runBlocking {
                    val settings = SettingsManager.settings.first()
                    val combined = settings.allSubscriptionServers + "\n" + settings.manualServers
                    val lines = combined.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                    
                    println("==========================================")
                    println("        Available Proxy Nodes (${lines.size})     ")
                    println("==========================================")
                    lines.forEachIndexed { i, line ->
                        val name = if (line.contains("#")) line.substringAfter("#") else "Node #${i + 1}"
                        val isCurrent = line.trim() == settings.activeProfile.trim()
                        println(" [${i + 1}] ${if (isSelected(line, settings.activeProfile)) "*" else " "} $name")
                    }
                    println("==========================================")
                }
                return true
            }
            "ping" -> {
                runBlocking(Dispatchers.IO) {
                    val settings = SettingsManager.settings.first()
                    val combined = settings.allSubscriptionServers + "\n" + settings.manualServers
                    val lines = combined.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()

                    println("==========================================")
                    println("       Testing Latency for Nodes (${lines.size})  ")
                    println("==========================================")
                    lines.forEachIndexed { i, line ->
                        val name = if (line.contains("#")) line.substringAfter("#") else "Node #${i + 1}"
                        val hp = getHostAndPortFromLink(line)
                        if (hp != null) {
                            val delay = measurePingDelay(hp.first, hp.second)
                            val status = if (delay > 0) "${delay}ms" else "TIMEOUT"
                            println(" [${i + 1}] $name -> $status")
                        } else {
                            println(" [${i + 1}] $name -> INVALID LINK")
                        }
                    }
                    println("==========================================")
                }
                return true
            }
            "scan" -> {
                runBlocking(Dispatchers.IO) {
                    println("[Chameleon CLI] Starting Cloudflare Clean IP Scanner...")
                    val res = CdnIpScanner.performScan("cloudflare")
                    println("==========================================")
                    println("        Cloudflare Clean IP Results       ")
                    println("==========================================")
                    println(" Working IPs : ${res.workingIpsCount}")
                    println(" Fastest IP  : ${res.fastestIp ?: "None"} (${res.fastestLatencyMs}ms)")
                    println("==========================================")
                    res.workingIps.take(10).forEach { ipItem ->
                        println("  - ${ipItem.ip} (${ipItem.latencyMs}ms)")
                    }
                    println("==========================================")
                }
                return true
            }
            "mode" -> {
                val tunArg = if (args.size > 1) args[1].lowercase() else ""
                val settingsManager = SettingsManager()
                val isTun = when (tunArg) {
                    "tun", "true", "1" -> true
                    "proxy", "false", "0" -> false
                    else -> !settingsManager.currentSettings.enableTun
                }
                settingsManager.setEnableTun(isTun)
                println("[Chameleon CLI] Mode updated to: ${if (isTun) "WinTUN Mode" else "System Proxy Mode"}")
                return true
            }
            else -> {
                println("[Chameleon CLI] Unknown command: '$command'")
                printHelp()
                return true
            }
        }
    }

    private fun isSelected(node: String, active: String): Boolean {
        return node.trim() == active.trim() && active.isNotEmpty()
    }

    private fun printHelp() {
        println("""
            Chameleon Desktop CLI Interface
            ==============================================
            Usage: chameleon [command] [options]

            Commands:
              status                 Print current VPN connection status and mode
              connect [link]         Start VPN connection (uses active profile or provided link)
              disconnect / stop      Stop current VPN connection
              nodes / list           List all available proxy nodes
              ping                   Measure latency pings for all nodes
              scan                   Run Cloudflare & CDN clean IP scanner
              mode [tun|proxy]       Toggle or set WinTUN / System Proxy mode
              help                   Show this help menu

            Examples:
              chameleon status
              chameleon connect
              chameleon mode tun
              chameleon scan
            ==============================================
        """.trimIndent())
    }
}
