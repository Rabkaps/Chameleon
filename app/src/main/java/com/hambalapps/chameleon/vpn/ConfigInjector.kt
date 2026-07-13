package com.hambalapps.chameleon.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.hambalapps.chameleon.data.deserializeProxyChains
import com.hambalapps.chameleon.data.deserializeCamouflageSettings
import com.hambalapps.chameleon.data.ProxyChain
import com.hambalapps.chameleon.data.CamouflageConfig

data class InjectorSettings(
    val bypassIran: Boolean,
    val secureDns: String,
    val tunStack: String,
    val enableFragment: Boolean,
    val fragmentLength: String,
    val fragmentInterval: String,
    val enableMux: Boolean,
    val bypassLan: Boolean,
    val vpnMode: String = "standard",
    val warpPrivateKey: String = "",
    val warpPublicKey: String = "",
    val warpIpAddress: String = "",
    val warpClientId: String = "",
    val vpnModeTunnelGames: Boolean = false,
    val warpDetourMode: String = "proxy",
    val warpPort: String = "2408",
    val shareVpnLan: Boolean = false,
    val shareVpnPort: String = "10808",
    val proxyChains: String = "",
    val camouflageSettings: String = "",
    val globalCamouflageEnabled: Boolean = false,
    val globalCamouflagePreset: String = "cloudflare",
    val globalCamouflageSni: String = "speedtest.net",
    val globalCamouflageHost: String = "",
    val globalCamouflageCustomIps: String = "",
    val globalCamouflageTimeout: String = "600",
    val globalCamouflagePinnedIp: String = "",
    val rootMode: Boolean = false,
    val enableMtProxy: Boolean = false,
    val mtProxyPort: String = "19999",
    val mtProxySecret: String = "ee000102030405060708090a0b0c0d0e0f7370656564746573742e6e6574",
    val localProxyOnly: Boolean = false
)

object ConfigInjector {

    @Volatile
    private var dohWorking = true

    private val gamingDomains = listOf(
        "pubgmobile.com", "pubg.com", "riotgames.com", "playvalorant.com", "leagueoflegends.com",
        "activision.com", "callofduty.com", "epicgames.com", "ea.com", "origin.com",
        "supercell.com", "clashofclans.com", "steampowered.com", "steamcommunity.com",
        "fortnite.com", "sony.com", "playstation.com", "playstation.net", "xbox.com", "xboxlive.com",
        "garena.com", "roblox.com", "blizzard.com", "battle.net", "ubisoft.com", "apexlegends.com",
        "levelinfinite.com", "steamstatic.com", "moonton.com", "mobilelegends.com"
    )

    private val aiBypassDomains = listOf(
        "google.com", "googleapis.com", "gstatic.com", "googleusercontent.com", "google.dev", "google.co", "google",
        "ggpht.com", "ytimg.com", "youtube.com", "doubleclick.net", "google-analytics.com", "googletagmanager.com",
        "g.co", "recaptcha.net",
        "openai.com", "chatgpt.com", "oaistatic.com", "oaiusercontent.com",
        "anthropic.com", "claude.ai",
        "netflix.com", "netflix.net", "nflximg.net", "nflxvideo.net", "nflxso.net", "nflxext.com",
        "aistudiocdn.com", "gemini.google.com", "generativelanguage.googleapis.com", "aistudio.google.com",
        "ai.google.dev", "notebooklm.google.com", "antigravity.google.com", "aiplatform.googleapis.com"
    )

    fun injectConfig(context: Context, rawProfile: String, settings: InjectorSettings): String {
        dohWorking = true
        try {
            val trimmedProfile = rawProfile.trim()
            val configJson = if (trimmedProfile.startsWith("{")) {
                JSONObject(rawProfile)
            } else if (trimmedProfile.startsWith("chain://")) {
                val chainId = trimmedProfile.substringAfter("chain://").substringBefore("#")
                val chains = deserializeProxyChains(settings.proxyChains)
                val chainItem = chains.find { it.id == chainId }
                if (chainItem != null) {
                    buildConfigFromChain(chainItem, settings)
                } else {
                    buildDefaultSkeleton(settings)
                }
            } else if (trimmedProfile.startsWith("vless://") ||
                trimmedProfile.startsWith("trojan://") ||
                trimmedProfile.startsWith("ss://") ||
                trimmedProfile.startsWith("socks5://") ||
                trimmedProfile.startsWith("socks://") ||
                trimmedProfile.startsWith("http://") ||
                trimmedProfile.startsWith("https://") ||
                trimmedProfile.startsWith("vmess://") ||
                trimmedProfile.startsWith("hysteria2://") ||
                trimmedProfile.startsWith("hy2://") ||
                trimmedProfile.startsWith("tuic://") ||
                trimmedProfile.startsWith("wireguard://") ||
                trimmedProfile.startsWith("awg://") ||
                trimmedProfile.startsWith("amneziawg://") ||
                trimmedProfile.startsWith("openvpn://") ||
                trimmedProfile.startsWith("ovpn://") ||
                trimmedProfile.startsWith("mieru://") ||
                trimmedProfile.startsWith("ssr://") ||
                trimmedProfile.startsWith("shadowtls://") ||
                trimmedProfile.startsWith("snell://") ||
                trimmedProfile.startsWith("client") ||
                trimmedProfile.contains("dev tun") ||
                (trimmedProfile.contains("[Interface]") && trimmedProfile.contains("[Peer]"))) {
                buildConfigFromUri(rawProfile, settings)
            } else {
                // Return default empty configuration skeleton
                buildDefaultSkeleton(settings)
            }

            // Override log configuration to output to vpn.log
            val logFile = java.io.File(context.cacheDir, "vpn.log")
            try {
                if (logFile.exists()) logFile.delete()
            } catch (e: Exception) {}
            val logObj = configJson.optJSONObject("log") ?: JSONObject().also { configJson.put("log", it) }
            logObj.put("level", "debug")
            logObj.put("output", logFile.absolutePath)
            logObj.put("timestamp", true)

            // Sanitize invalid port fields in outbounds and inbounds
            sanitizePortFields(configJson)
            sanitizeXhttpTransport(configJson)

            // 1. Pre-resolve proxy server domains to raw IPs to bypass DNS hijacking
            preResolveProxyServers(context, configJson, settings)

            // 2. Inject or update inbounds (TUN interface & LAN Proxy Sharing)
            if (!settings.localProxyOnly) {
                injectTunInbound(configJson, settings)
            }
            injectLocalProxyInbound(configJson, settings)
            injectMTProxyInbound(configJson, settings)

            // 3. Inject or update DNS (Split DNS rules)
            injectDns(context, configJson, settings)

            // 4. Inject or update Routing Rules (Iran bypass)
            injectRouting(context, configJson, settings)

            // 5. Inject direct/block outbounds
            injectOutbounds(context, configJson, settings)

            // 6. Inject endpoints (for sing-box 1.11+ WireGuard)
            injectEndpoints(context, configJson, settings)

            migrateWireGuardToEndpoints(configJson)
            return configJson.toString(2)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return buildDefaultSkeleton(settings).toString(2)
        }
    }

    private fun injectTunInbound(config: JSONObject, settings: InjectorSettings) {
        if (settings.rootMode) {
            return
        }
        val inbounds = config.optJSONArray("inbounds") ?: JSONArray().also { config.put("inbounds", it) }
        
        // Remove existing TUN inbounds if any
        val newInbounds = JSONArray()
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("type") != "tun") {
                newInbounds.put(inbound)
            }
        }

        val tunInbound = JSONObject().apply {
            put("type", "tun")
            put("tag", "tun-in")
            put("interface_name", "tun0")
            put("stack", if (settings.vpnMode == "gaming") "system" else (settings.run { if (tunStack.isEmpty()) "mixed" else tunStack }))
            put("mtu", 1280) // 1280 MTU ensures compatibility and avoids packet drops on mobile/encapsulated links
            put("auto_route", true)
            put("strict_route", true)
            put("address", JSONArray(listOf("172.19.0.1/30")))
        }
        newInbounds.put(tunInbound)
        config.put("inbounds", newInbounds)
    }

    private fun injectLocalProxyInbound(config: JSONObject, settings: InjectorSettings) {
        val inbounds = config.optJSONArray("inbounds") ?: JSONArray().also { config.put("inbounds", it) }
        
        // Remove existing mixed inbounds
        val newInbounds = JSONArray()
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("tag") != "mixed-in") {
                newInbounds.put(inbound)
            }
        }
        
        if (settings.shareVpnLan || settings.rootMode) {
            val portVal = settings.shareVpnPort.toIntOrNull() ?: 10808
            val mixedInbound = JSONObject().apply {
                put("type", "mixed")
                put("tag", "mixed-in")
                put("listen", if (settings.shareVpnLan) "0.0.0.0" else "127.0.0.1")
                put("listen_port", portVal)
            }
            newInbounds.put(mixedInbound)
        }
        config.put("inbounds", newInbounds)
    }

    private fun injectMTProxyInbound(config: JSONObject, settings: InjectorSettings) {
        val inbounds = config.optJSONArray("inbounds") ?: JSONArray().also { config.put("inbounds", it) }
        
        val newInbounds = JSONArray()
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            if (inbound.optString("tag") != "mtproxy-in") {
                newInbounds.put(inbound)
            }
        }
        
        if (settings.enableMtProxy) {
            val portVal = settings.mtProxyPort.toIntOrNull() ?: 19999
            
            val normalizedSecret = normalizeMtProxySecret(settings.mtProxySecret)

            val mtProxyInbound = JSONObject().apply {
                put("type", "mtproxy")
                put("tag", "mtproxy-in")
                put("listen", "0.0.0.0")
                put("listen_port", portVal)
                
                val user = JSONObject().apply {
                    put("secret", normalizedSecret)
                }
                put("users", JSONArray().apply { put(user) })
            }
            newInbounds.put(mtProxyInbound)
        }
        config.put("inbounds", newInbounds)
    }

    fun normalizeMtProxySecret(secret: String): String {
        val trimmed = secret.trim()
        val baseSecret = if (trimmed.startsWith("dd", ignoreCase = true)) {
            "ee" + trimmed.substring(2)
        } else if (!trimmed.startsWith("ee", ignoreCase = true)) {
            "ee" + trimmed
        } else {
            trimmed
        }
        return if (baseSecret.startsWith("ee", ignoreCase = true) && baseSecret.length == 34) {
            baseSecret + "7370656564746573742e6e6574"
        } else {
            baseSecret
        }
    }

    private fun getSystemDnsServers(context: Context): List<String> {
        val dnsList = mutableListOf<String>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            try {
                val activeNetwork = cm.activeNetwork
                if (activeNetwork != null) {
                    val lp = cm.getLinkProperties(activeNetwork)
                    lp?.dnsServers?.forEach { dnsAddr ->
                        val dnsHost = dnsAddr.hostAddress
                        if (dnsHost != null) {
                            val cleanHost = dnsHost.substringBefore("%")
                            if (cleanHost.isNotEmpty() && !cleanHost.contains(":")) {
                                dnsList.add(cleanHost)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Chameleon", "Failed to get system DNS: ${e.message}")
            }
        }
        return dnsList
    }
    private fun getSystemDnsAddress(context: Context): String {
        val systemDnsList = getSystemDnsServers(context)
        var directDnsAddr = "178.22.122.100" // Default Shecan/Local DNS
        for (dnsIp in systemDnsList) {
            // Filter out well-known hijacked public DNS servers in Iran
            if (dnsIp != "8.8.8.8" && dnsIp != "8.8.4.4" && dnsIp != "1.1.1.1" && dnsIp != "1.0.0.1" && dnsIp != "9.9.9.9") {
                directDnsAddr = dnsIp
                break
            }
        }
        return directDnsAddr
    }

    private fun createDnsServer(tag: String, address: String, detour: String?): JSONObject {
        val serverObj = JSONObject()
        serverObj.put("tag", tag)
        if (detour != null) {
            serverObj.put("detour", detour)
        }

        val trimmed = address.trim()
        if (trimmed.startsWith("https://")) {
            serverObj.put("type", "https")
            val hostPart = trimmed.substringAfter("https://").substringBefore("/")
            serverObj.put("server", hostPart)
            val path = "/" + trimmed.substringAfter("https://").substringAfter("/", "")
            if (path.length > 1) {
                serverObj.put("path", path)
            }
            val tls = JSONObject().apply {
                put("enabled", true)
                put("insecure", true)
                if (hostPart == "10.202.10.10") {
                    put("server_name", "radar.game")
                } else if (hostPart == "185.51.200.2" || hostPart == "178.22.122.100") {
                    put("server_name", "shecan.ir")
                }
            }
            serverObj.put("tls", tls)
        } else if (trimmed.startsWith("tls://")) {
            serverObj.put("type", "tls")
            serverObj.put("server", trimmed.substringAfter("tls://"))
            val tls = JSONObject().apply {
                put("enabled", true)
                put("insecure", true)
            }
            serverObj.put("tls", tls)
        } else if (trimmed.startsWith("tcp://")) {
            serverObj.put("type", "tcp")
            serverObj.put("server", trimmed.substringAfter("tcp://"))
        } else if (trimmed.startsWith("quic://")) {
            serverObj.put("type", "quic")
            serverObj.put("server", trimmed.substringAfter("quic://"))
            val tls = JSONObject().apply {
                put("enabled", true)
                put("insecure", true)
            }
            serverObj.put("tls", tls)
        } else {
            serverObj.put("type", "udp")
            serverObj.put("server", trimmed)
        }
        return serverObj
    }

    private fun injectDns(context: Context, config: JSONObject, settings: InjectorSettings) {
        val dns = JSONObject()
        dns.put("reverse_mapping", true)
        dns.put("strategy", "ipv4_only")
        val servers = JSONArray()

        // 1. Parsed DNS servers from outbounds (e.g. OpenVPN dhcp-options / AntiZapret) - prioritised first
        val outboundsList = config.optJSONArray("outbounds")
        if (outboundsList != null) {
            for (i in 0 until outboundsList.length()) {
                val outbound = outboundsList.optJSONObject(i) ?: continue
                val tag = outbound.optString("tag")
                val dnsArray = outbound.optJSONArray("_dns_servers")
                if (dnsArray != null) {
                    for (j in 0 until dnsArray.length()) {
                        val dnsIp = dnsArray.optString(j)
                        if (dnsIp.isNotEmpty()) {
                            val vpnDns = createDnsServer("dns-vpn-$tag-$j", dnsIp, tag)
                            servers.put(vpnDns)
                        }
                    }
                    outbound.remove("_dns_servers")
                }
            }
        }

        // 2. Clean secure DoH fallback for the proxy outbound (placed after parsed DNS)
        val fallbackSecureProxy = createDnsServer("dns-vpn-fallback-secure", "https://1.1.1.1/dns-query", "proxy")
        servers.put(fallbackSecureProxy)

        // 1. Secure DNS Server (routes via the proxy)
        val secureServer = createDnsServer("dns-secure", settings.secureDns, "proxy")

        // 2. Local Bypass DNS Server for Iran domains (runs directly, detouring proxy)
        val directDnsAddr = getSystemDnsAddress(context)

        android.util.Log.i("Chameleon", "Direct DNS set to: $directDnsAddr")

        val directServer = createDnsServer("dns-direct", directDnsAddr, null)

        // 3. Clean Bootstrap DNS Server for resolving proxy/DNS hostnames reliably (without carrier hijacking)
        val bootstrapServer = createDnsServer("dns-bootstrap", "https://1.1.1.1/dns-query", "direct")

        if (settings.vpnMode == "gaming" && !settings.vpnModeTunnelGames) {
            val radarServer = createDnsServer("dns-radar", "tcp://10.202.10.10", null)
            val shecanServer = createDnsServer("dns-shecan", "tcp://185.51.200.2", null)
            servers.put(secureServer)
            servers.put(radarServer)
            servers.put(shecanServer)
            servers.put(directServer)
            servers.put(bootstrapServer)
        } else {
            servers.put(secureServer)
            servers.put(directServer)
            servers.put(bootstrapServer)
        }

        dns.put("servers", servers)

        val rules = JSONArray()

        // Inject bootstrap rules for proxy server domain and secure DNS DoH domain to route directly
        val proxyHosts = getProxyServerHosts(config)
        val secureDnsHost = extractHostFromUrl(settings.secureDns)
        val directDomains = mutableListOf<String>()

        for (host in proxyHosts) {
            if (host.isNotEmpty() && !isIpAddress(host)) {
                directDomains.add(host)
            }
        }
        if (secureDnsHost != null && secureDnsHost.isNotEmpty() && !isIpAddress(secureDnsHost)) {
            directDomains.add(secureDnsHost)
        }

        if (directDomains.isNotEmpty()) {
            val bootstrapRule = JSONObject().apply {
                put("domain", JSONArray(directDomains))
                put("server", "dns-bootstrap")
            }
            rules.put(bootstrapRule)
        }
        
        if (settings.bypassIran) {
            val geositeFile = java.io.File(context.filesDir, "geosite-ir.srs")
            if (geositeFile.exists()) {
                // Rule: Route Iranian geosite to local DNS via rule_set
                val irGeositeRule = JSONObject().apply {
                    put("rule_set", JSONArray(listOf("geosite-ir")))
                    put("server", "dns-direct")
                }
                rules.put(irGeositeRule)
            }

            // Rule: Route .ir domains to local DNS
            val irSuffixRule = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("server", "dns-direct")
            }
            rules.put(irSuffixRule)
        }

        if (settings.vpnMode == "gaming") {
            val gameDnsRule = JSONObject().apply {
                put("domain_suffix", JSONArray(gamingDomains))
                put("server", if (settings.vpnModeTunnelGames) "dns-secure" else "dns-radar")
            }
            rules.put(gameDnsRule)
        }

        dns.put("rules", rules)
        config.put("dns", dns)
    }

    private fun injectRouting(context: Context, config: JSONObject, settings: InjectorSettings) {
        val route = config.optJSONObject("route") ?: JSONObject().also { config.put("route", it) }
        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

        // Filter and separate the original user-defined rules
        val originalRules = JSONArray()
        for (i in 0 until rules.length()) {
            val r = rules.optJSONObject(i) ?: continue
            val protocol = r.optString("protocol")
            val geosite = r.optJSONArray("geosite")
            val geoip = r.optJSONArray("geoip")
            val suffix = r.optJSONArray("domain_suffix")
            val ruleSetName = r.optString("rule_set")
            val ruleSetArrayVal = r.optJSONArray("rule_set")

            val isIranRule = (geosite != null && geosite.toString().contains("ir")) ||
                             (geoip != null && geoip.toString().contains("ir")) ||
                             (suffix != null && suffix.toString().contains(".ir")) ||
                             (ruleSetName != null && ruleSetName.contains("ir")) ||
                             (ruleSetArrayVal != null && ruleSetArrayVal.toString().contains("ir"))
            
            if (protocol != "dns" && !isIranRule && r.optString("action") != "sniff") {
                originalRules.put(r)
            }
        }

        val newRules = JSONArray()

        // Detect if this is an AntiZapret connection
        var isAntiZapret = false
        val outbounds = config.optJSONArray("outbounds")
        if (outbounds != null) {
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                if (outbound.optBoolean("_is_antizapret", false)) {
                    isAntiZapret = true
                    outbound.remove("_is_antizapret")
                }
            }
        }

        // 1. Sniff Rule (must be at the top to extract hostnames)
        val sniffRule = JSONObject().apply {
            put("action", "sniff")
            if (settings.vpnMode == "gaming") {
                put("sniffer", JSONArray(listOf("http", "tls")))
                put("network", "tcp")
            } else {
                put("sniffer", JSONArray(listOf("http", "tls", "quic", "dns", "stun")))
            }
        }
        newRules.put(sniffRule)

        // 2. DNS Hijack Rule (must be at the top)
        val dnsRule = JSONObject().apply {
            put("protocol", "dns")
            put("action", "hijack-dns")
        }
        newRules.put(dnsRule)

        // Route AntiZapret fake IP range directly to the proxy (preventing LAN bypass conflicts)
        if (isAntiZapret) {
            val antiZapretRouteRule = JSONObject().apply {
                put("ip_cidr", JSONArray(listOf("10.224.0.0/15")))
                put("outbound", "proxy")
            }
            newRules.put(antiZapretRouteRule)
        }

        // 3. Block Private DNS (DoT) on port 853 to force fallback to standard DNS
        val blockDotRule = JSONObject().apply {
            put("port", JSONArray(listOf(853)))
            put("action", "reject")
            put("method", "default")
        }
        newRules.put(blockDotRule)

        // 4. Local Bypass LAN/Private IP Networks
        val localIps = mutableListOf<String>().apply {
            add("127.0.0.0/8")
            add("::1/128")
            if (settings.bypassLan) {
                addAll(listOf(
                    "10.0.0.0/8",
                    "172.16.0.0/12",
                    "192.168.0.0/16",
                    "169.254.0.0/16",
                    "fc00::/7",
                    "fe80::/10"
                ))
            }
        }
        val directIps = mutableListOf<String>()
        val directDomains = mutableListOf<String>()
        val proxyHosts = getProxyServerHosts(config)

        val directDnsAddr = extractHostFromUrl(settings.secureDns) ?: ""
        if (directDnsAddr.isNotEmpty() && isIpAddress(directDnsAddr)) {
            directIps.add(directDnsAddr)
        }
        val systemDnsAddr = getSystemDnsAddress(context)
        if (systemDnsAddr.isNotEmpty() && isIpAddress(systemDnsAddr)) {
            directIps.add(systemDnsAddr)
        }
        val bootstrapDnsAddr = "8.8.8.8"
        if (bootstrapDnsAddr.isNotEmpty() && isIpAddress(bootstrapDnsAddr)) {
            directIps.add(bootstrapDnsAddr)
        }

        if (settings.vpnMode == "gaming") {
            listOf("10.202.10.10", "10.202.10.11", "185.51.200.2", "178.22.122.100").forEach { ip ->
                if (!directIps.contains(ip)) {
                    directIps.add(ip)
                }
            }
        }

        for (host in proxyHosts) {
            if (host.isNotEmpty()) {
                if (isIpAddress(host)) {
                    directIps.add(host)
                } else {
                    directDomains.add(host)
                }
            }
        }

        if (directDomains.isNotEmpty()) {
            val bypassBypassRule = JSONObject().apply {
                put("domain", JSONArray(directDomains))
                put("outbound", "direct")
            }
            newRules.put(bypassBypassRule)
        }

        if (directIps.isNotEmpty()) {
            val bypassIpsRule = JSONObject().apply {
                put("ip_cidr", JSONArray(directIps))
                put("outbound", "direct")
            }
            newRules.put(bypassIpsRule)
        }

        val privateIpsRule = JSONObject().apply {
            put("ip_cidr", JSONArray(localIps))
            put("outbound", "direct")
        }
        newRules.put(privateIpsRule)

        // 5. Bypass Iran Rules (must be high priority before custom/catch-all proxy rules)
        if (settings.bypassIran) {
            val geositeFile = java.io.File(context.filesDir, "geosite-ir.srs")
            val geoipFile = java.io.File(context.filesDir, "geoip-ir.srs")

            // Inject or update local rule sets declaration if files exist
            val ruleSetArray = JSONArray()
            if (geoipFile.exists()) {
                ruleSetArray.put(JSONObject().apply {
                    put("tag", "geoip-ir")
                    put("type", "local")
                    put("format", "binary")
                    put("path", geoipFile.absolutePath)
                })
            }
            if (geositeFile.exists()) {
                ruleSetArray.put(JSONObject().apply {
                    put("tag", "geosite-ir")
                    put("type", "local")
                    put("format", "binary")
                    put("path", geositeFile.absolutePath)
                })
            }
            if (ruleSetArray.length() > 0) {
                route.put("rule_set", ruleSetArray)
            }

            if (geositeFile.exists()) {
                val irGeosite = JSONObject().apply {
                    put("rule_set", JSONArray(listOf("geosite-ir")))
                    put("outbound", "direct")
                }
                newRules.put(irGeosite)
            }

            if (geoipFile.exists()) {
                val irGeoip = JSONObject().apply {
                    put("rule_set", JSONArray(listOf("geoip-ir")))
                    put("outbound", "direct")
                }
                newRules.put(irGeoip)
            }

            val irSuffix = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("outbound", "direct")
            }
            newRules.put(irSuffix)
        }

        // 6. Gaming / AI Bypass Outbounds Rules
        if (settings.vpnMode == "gaming") {
            val gameRouteRule = JSONObject().apply {
                put("domain_suffix", JSONArray(gamingDomains))
                put("outbound", if (settings.vpnModeTunnelGames) "proxy" else "direct")
            }
            newRules.put(gameRouteRule)
        } else if (settings.vpnMode == "ai_bypass" && settings.warpPrivateKey.isNotEmpty()) {
            val aiRouteRule = JSONObject().apply {
                put("domain_suffix", JSONArray(aiBypassDomains))
                put("outbound", "warp-out")
            }
            newRules.put(aiRouteRule)
        }

        // 7. Append original custom profile rules (lower priority)
        for (i in 0 until originalRules.length()) {
            newRules.put(originalRules.getJSONObject(i))
        }

        route.put("rules", newRules)
        route.put("auto_detect_interface", true)
        route.put("override_android_vpn", !settings.rootMode)
    }

    private fun injectOutbounds(context: Context, config: JSONObject, settings: InjectorSettings) {
        val outbounds = config.optJSONArray("outbounds") ?: JSONArray().also { config.put("outbounds", it) }

        val cleanOutbounds = JSONArray()
        var hasDirect = false
        var hasBlock = false

        val camConfigs = deserializeCamouflageSettings(settings.camouflageSettings)

        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            val type = out.optString("type")
            if (type == "openvpn") {
                continue
            }
            val tag = out.optString("tag")
            if (type == "dns" || tag == "dns-out") {
                continue // Remove deprecated DNS outbounds
            }
            if (tag == "direct") hasDirect = true
            if (tag == "block") hasBlock = true

            // Resolve and apply Stealth Camouflage if configured
            val detour = out.optString("detour")
            val isEntryProxy = (tag == "proxy" || tag == "relay-out") && detour.isEmpty()
            val isCompatibleType = type == "vless" || type == "trojan" || type == "vmess" || type == "shadowsocks" || type == "ss"
            if (isEntryProxy && isCompatibleType) {
                if (settings.globalCamouflageEnabled) {
                    val globalConfig = CamouflageConfig(
                        nodeLink = "",
                        enabled = true,
                        preset = settings.globalCamouflagePreset,
                        customSni = settings.globalCamouflageSni,
                        customHost = settings.globalCamouflageHost
                    )
                    applyCamouflage(out, globalConfig, settings)
                } else {
                    val originalLink = out.optString("_original_link")
                    if (originalLink.isNotEmpty()) {
                        val configLinkWithoutRemark = originalLink.substringBefore("#")
                        val camConfig = camConfigs.find { it.nodeLink.substringBefore("#") == configLinkWithoutRemark }
                        if (camConfig != null && camConfig.enabled) {
                            applyCamouflage(out, camConfig, settings)
                        }
                    }
                }
            }
            out.remove("_original_link") // Clean up temporary key

            // Inject fragmentation into proxy outbound if enabled
            val isProxyOrRelay = (tag == "proxy" || tag == "relay-out")
            val isOpenVpn = out.optString("type") == "openvpn"
            val isWireGuard = out.optString("type") == "wireguard" || out.optString("type") == "amneziawg"
            if (isProxyOrRelay && settings.enableFragment && !isOpenVpn && !isWireGuard) {
                injectFragmentToOutbound(out, settings)
            }
            // Inject multiplexing if enabled (disabled in gaming mode, and for Reality/xhttp/OpenVPN/WireGuard configs)
            val tls = out.optJSONObject("tls")
            val isReality = tls?.has("reality") ?: false
            val transport = out.optJSONObject("transport")
            val isXhttp = transport?.optString("type") == "xhttp"
            if (isProxyOrRelay && settings.enableMux && settings.vpnMode != "gaming" && !isReality && !isXhttp && !isOpenVpn && !isWireGuard) {
                val mux = JSONObject().apply {
                    put("enabled", true)
                    put("protocol", "smux")
                    put("max_connections", 4)
                    put("min_streams", 4)
                }
                out.put("multiplex", mux)
            } else if (isProxyOrRelay && (settings.vpnMode == "gaming" || isReality || isXhttp || isOpenVpn || isWireGuard)) {
                out.remove("multiplex")
            }
            cleanOutbounds.put(out)
        }

        if (!hasDirect) {
            cleanOutbounds.put(JSONObject().apply {
                put("type", "direct")
                put("tag", "direct")
            })
        }
        if (!hasBlock) {
            cleanOutbounds.put(JSONObject().apply {
                put("type", "block")
                put("tag", "block")
            })
        }


        if (settings.vpnMode == "ai_bypass" && settings.warpPrivateKey.isNotEmpty()) {
            val warpOutbound = JSONObject().apply {
                put("type", "direct")
                put("tag", "warp-out")
                put("detour", "warp-endpoint")
            }
            cleanOutbounds.put(warpOutbound)
        }

        config.put("outbounds", cleanOutbounds)
    }

    private fun buildConfigFromChain(chainItem: ProxyChain, settings: InjectorSettings): JSONObject {
        val config = buildDefaultSkeleton(settings)
        val outbounds = config.getJSONArray("outbounds")
        try {
            // Parse exit outbound with tag "proxy"
            val exitOutbound = parseOutboundFromUri(chainItem.exitLink, "proxy")
            // Detour exit outbound to relay outbound
            exitOutbound.put("detour", "relay-out")
            // Parse relay outbound with tag "relay-out"
            val relayOutbound = parseOutboundFromUri(chainItem.relayLink, "relay-out")

            // Prevent uTLS fingerprint collision in nested handshakes (e.g. Reality inside Reality)
            val exitTls = exitOutbound.optJSONObject("tls")
            val relayTls = relayOutbound.optJSONObject("tls")
            if (exitTls != null && relayTls != null) {
                val exitUtls = exitTls.optJSONObject("utls")
                val relayUtls = relayTls.optJSONObject("utls")
                if (exitUtls != null && relayUtls != null) {
                    val exitFp = exitUtls.optString("fingerprint", "chrome")
                    val relayFp = relayUtls.optString("fingerprint", "chrome")
                    if (exitFp == relayFp) {
                        val newFp = if (exitFp == "chrome") "firefox" else "chrome"
                        relayUtls.put("fingerprint", newFp)
                    }
                }
            }

            // Index 0 in buildDefaultSkeleton is "proxy". Replace it with exitOutbound
            outbounds.put(0, exitOutbound)
            // Add the relayOutbound
            outbounds.put(relayOutbound)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return config
    }

    private fun applyCamouflage(outbound: JSONObject, config: CamouflageConfig, settings: InjectorSettings) {
        if (outbound.optString("type") == "openvpn") return
        val originalServer = outbound.optString("server")
        if (originalServer.isEmpty()) return

        // 1. Get clean CDN IP from scanner based on preset and global settings
        val customIpsList = if (config.preset == "custom") {
            settings.globalCamouflageCustomIps.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        val timeoutVal = settings.globalCamouflageTimeout.toIntOrNull() ?: 600

        val cleanIp = CdnIpScanner.getCleanIp(
            preset = config.preset,
            customIps = customIpsList,
            port = 443,
            timeoutMs = timeoutVal,
            pinnedIp = settings.globalCamouflagePinnedIp
        ) ?: originalServer
        outbound.put("server", cleanIp)

        // 2. Setup TLS section
        val tls = outbound.optJSONObject("tls") ?: JSONObject().also { outbound.put("tls", it) }
        tls.put("enabled", true)

        val targetSni = when (config.preset) {
            "cloudflare" -> "speedtest.net" // Default Cloudflare SNI
            "cloudfront" -> "aws.amazon.com" // Default Cloudfront SNI
            else -> config.customSni.ifEmpty { "microsoft.com" }
        }
        tls.put("server_name", targetSni)

        // 3. Setup Transport Host Header (WS/HTTPUpgrade/xHTTP)
        val transport = outbound.optJSONObject("transport") ?: JSONObject().apply {
            put("type", "ws") // Default to WS if transport is not defined
        }.also { outbound.put("transport", it) }

        val transType = transport.optString("type")
        val targetHost = if (config.preset == "custom" && config.customHost.isNotEmpty()) {
            config.customHost
        } else {
            originalServer // Original hostname acts as the Host/routing header target
        }

        if (transType == "ws") {
            var headers = transport.optJSONObject("headers")
            if (headers == null) {
                headers = JSONObject()
                transport.put("headers", headers)
            }
            headers.put("Host", targetHost)
        } else if (transType == "httpupgrade" || transType == "http") {
            transport.put("host", targetHost)
            var headers = transport.optJSONObject("headers")
            if (headers == null) {
                headers = JSONObject()
                transport.put("headers", headers)
            }
            headers.put("Host", targetHost)
        } else if (transType == "xhttp") {
            transport.put("host", targetHost)
        }
    }

    private fun injectFragmentToOutbound(outbound: JSONObject, settings: InjectorSettings) {
        if (outbound.optString("type") == "openvpn") return
        val tls = outbound.optJSONObject("tls") ?: JSONObject().also { outbound.put("tls", it) }
        tls.put("enabled", true)
        tls.put("fragment", true)
        tls.put("record_fragment", true)
        tls.put("fragment_fallback_delay", "500ms")
    }

    private fun injectEndpoints(context: Context, config: JSONObject, settings: InjectorSettings) {
        val endpoints = config.optJSONArray("endpoints") ?: JSONArray()
        val cleanEndpoints = JSONArray()
        
        for (i in 0 until endpoints.length()) {
            val ep = endpoints.optJSONObject(i) ?: continue
            val tag = ep.optString("tag")
            if (tag != "warp-endpoint" && tag != "warp-out") {
                cleanEndpoints.put(ep)
            }
        }
        
        if (settings.vpnMode == "ai_bypass" && settings.warpPrivateKey.isNotEmpty()) {
            val warpEndpoint = JSONObject().apply {
                put("type", "wireguard")
                put("tag", "warp-endpoint")
                
                val rawIp = settings.warpIpAddress.trim()
                val formattedIp = if (rawIp.isEmpty()) {
                    "172.16.0.2/32"
                } else if (rawIp.contains("/")) {
                    rawIp
                } else {
                    "$rawIp/32"
                }
                put("address", JSONArray().apply { put(formattedIp) })
                put("private_key", settings.warpPrivateKey)
                
                val peerAddress = resolveDomainWithFallbacks(context, "engage.cloudflareclient.com", settings) ?: "162.159.192.1"
                android.util.Log.i("Chameleon", "WARP endpoint peer engage.cloudflareclient.com pre-resolved to: $peerAddress")

                val peerObj = JSONObject().apply {
                    put("address", peerAddress)
                    put("port", settings.warpPort.toIntOrNull() ?: 2408)
                    put("public_key", "bmXOC+F1fxEMDXGggWMuGcIy77Dd1KAD4kURmMyd378=")
                    put("allowed_ips", JSONArray().apply { put("0.0.0.0/0") })
                }
                put("peers", JSONArray().apply { put(peerObj) })
                put("detour", settings.warpDetourMode.ifEmpty { "direct" })
            }
            cleanEndpoints.put(warpEndpoint)
        }
        
        if (cleanEndpoints.length() > 0) {
            config.put("endpoints", cleanEndpoints)
        } else {
            config.remove("endpoints")
        }
    }

    private fun migrateWireGuardToEndpoints(config: JSONObject) {
        val outbounds = config.optJSONArray("outbounds") ?: return
        val endpoints = config.optJSONArray("endpoints") ?: JSONArray().also { config.put("endpoints", it) }
        
        val cleanOutbounds = JSONArray()
        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            val type = out.optString("type")
            if (type == "wireguard" || type == "amneziawg") {
                val tag = out.optString("tag")
                val epTag = "$tag-endpoint"
                
                val ep = JSONObject().apply {
                    put("type", "wireguard")
                    put("tag", epTag)
                    
                    if (out.has("address")) put("address", out.get("address"))
                    if (out.has("private_key")) put("private_key", out.get("private_key"))
                    if (out.has("mtu")) put("mtu", out.get("mtu"))
                    if (out.has("detour")) put("detour", out.get("detour"))
                    
                    val peers = out.optJSONArray("peers")
                    if (peers != null) {
                        val newPeers = JSONArray()
                        for (j in 0 until peers.length()) {
                            val peer = peers.optJSONObject(j) ?: continue
                            val newPeer = JSONObject().apply {
                                if (peer.has("server")) put("address", peer.get("server"))
                                if (peer.has("server_port")) put("port", peer.get("server_port"))
                                if (peer.has("public_key")) put("public_key", peer.get("public_key"))
                                if (peer.has("pre_shared_key")) put("pre_shared_key", peer.get("pre_shared_key"))
                                if (peer.has("allowed_ips")) put("allowed_ips", peer.get("allowed_ips"))
                                if (peer.has("persistent_keepalive_interval")) put("persistent_keepalive_interval", peer.get("persistent_keepalive_interval"))
                                // Omit reserved to prevent unmarshal crashes
                            }
                            newPeers.put(newPeer)
                        }
                        put("peers", newPeers)
                    }
                    
                    if (out.has("amnezia")) {
                        put("amnezia", out.get("amnezia"))
                    }
                }
                endpoints.put(ep)
                
                val bridgeOutbound = JSONObject().apply {
                    put("type", "direct")
                    put("tag", tag)
                    put("detour", epTag)
                }
                cleanOutbounds.put(bridgeOutbound)
            } else {
                cleanOutbounds.put(out)
            }
        }
        
        config.put("outbounds", cleanOutbounds)
        if (endpoints.length() > 0) {
            config.put("endpoints", endpoints)
        } else {
            config.remove("endpoints")
        }
    }

    private fun buildDefaultSkeleton(settings: InjectorSettings): JSONObject {
        return JSONObject().apply {
            put("log", JSONObject().apply {
                put("level", "debug")
                put("timestamp", true)
            })
            put("outbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "direct")
                    put("tag", "proxy") // Fallback direct outbound labeled as proxy
                })
            })
        }
    }

    private fun buildConfigFromUri(uriStr: String, settings: InjectorSettings): JSONObject {
        val config = buildDefaultSkeleton(settings)
        val outbounds = config.getJSONArray("outbounds")
        try {
            val outbound = parseOutboundFromUri(uriStr, "proxy")
            outbounds.put(0, outbound)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return config
    }

    private fun parseOutboundFromUri(uriStr: String, defaultTag: String): JSONObject {
        val trimmed = uriStr.trim()
        if (trimmed.startsWith("client") || trimmed.contains("dev tun") || trimmed.startsWith("openvpn://") || trimmed.startsWith("ovpn://")) {
            throw IllegalArgumentException("OpenVPN is not supported")
        }
        val outbound = JSONObject()
        try {
            outbound.put("tag", defaultTag)
            outbound.put("_original_link", uriStr)

            val fragmentIdx = trimmed.indexOf("#")
            val name = if (fragmentIdx >= 0) {
                URLDecoder.decode(trimmed.substring(fragmentIdx + 1), "UTF-8")
            } else {
                "proxy"
            }
            
            val rest = if (fragmentIdx >= 0) trimmed.substring(0, fragmentIdx) else trimmed
            val schemeIdx = rest.indexOf("://")
            if (schemeIdx < 0) {
                if (trimmed.contains("[Interface]") && trimmed.contains("[Peer]")) {
                    return parseWireGuardConf(trimmed, defaultTag)
                }
                return outbound
            }
            val scheme = rest.substring(0, schemeIdx).lowercase()
            
            val content = rest.substring(schemeIdx + 3)
            val queryIdx = content.indexOf("?")
            val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content
            val queryPart = if (queryIdx >= 0) content.substring(queryIdx + 1) else ""
            
            val atIdx = mainPart.indexOf("@")
            val userInfo = if (atIdx >= 0) mainPart.substring(0, atIdx) else ""
            val serverPart = if (atIdx >= 0) mainPart.substring(atIdx + 1) else mainPart
            
            val colonIdx = serverPart.lastIndexOf(":")
            val host = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
            val portStr = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
            val port = portStr.toIntOrNull() ?: 443
            
            val queryParams = parseQueryParams(queryPart)

            if (scheme == "vless") {
                outbound.put("type", "vless")
                outbound.put("uuid", userInfo)
                outbound.put("server", host)
                outbound.put("server_port", port)
                outbound.put("packet_encoding", "xudp")

                val encryption = queryParams["encryption"]
                if (encryption != null && encryption.isNotEmpty()) {
                    outbound.put("encryption", encryption)
                }

                val security = queryParams["security"]?.lowercase()
                val isReality = security == "reality"

                // Flow control (only allowed for standard TCP transport in sing-box)
                // headerType=http is a legacy obfuscation that Reality ignores,
                // so we skip flow injection when headerType=http to match original behavior.
                val type = queryParams["type"]
                val headerType = queryParams["headerType"] ?: queryParams["header_type"]
                val isStandardTcp = (type == null || type.equals("tcp", ignoreCase = true)) && headerType != "http"
                if (isStandardTcp) {
                    val flow = queryParams["flow"]
                    if (flow != null && flow.isNotEmpty() && flow != "none") {
                        outbound.put("flow", flow)
                    }
                }

                // TLS
                val isTlsOrReality = security != "none" && (security == "tls" || isReality || queryParams["tls"] == "true" || queryParams["tls"] == "1" || ((port == 443 || port == 8443) && headerType != "http"))
                val isObfuscatedHttp = (type == null || type.equals("tcp", ignoreCase = true)) && headerType == "http" && !isTlsOrReality
                val hasTls = isTlsOrReality && !isObfuscatedHttp
                if (hasTls) {
                    val tls = JSONObject()
                    tls.put("enabled", true)
                    
                    val sni = queryParams["sni"] ?: queryParams["host"]
                    if (sni != null && sni.isNotEmpty()) {
                        tls.put("server_name", sni)
                    }

                    // Enable uTLS if security is reality or fingerprint is specified
                    if (isReality || queryParams.containsKey("fp")) {
                        val utls = JSONObject()
                        utls.put("enabled", true)
                        val fingerprint = queryParams["fp"] ?: "chrome"
                        utls.put("fingerprint", fingerprint)
                        tls.put("utls", utls)
                    }

                    if (isReality) {
                        val reality = JSONObject()
                        reality.put("enabled", true)
                        queryParams["pbk"]?.let { reality.put("public_key", it) }
                        queryParams["sid"]?.let { reality.put("short_id", it) }
                        tls.put("reality", reality)
                    }
                    outbound.put("tls", tls)
                }

                // Transport
                injectTransport(outbound, queryParams, host)
            } else if (scheme == "trojan") {
                outbound.put("type", "trojan")
                outbound.put("password", userInfo)
                outbound.put("server", host)
                outbound.put("server_port", port)

                val tls = JSONObject()
                tls.put("enabled", true)
                queryParams["sni"]?.let { tls.put("server_name", it) }

                if (queryParams.containsKey("fp")) {
                    val utls = JSONObject()
                    utls.put("enabled", true)
                    val fingerprint = queryParams["fp"] ?: "chrome"
                    utls.put("fingerprint", fingerprint)
                    tls.put("utls", utls)
                }
                outbound.put("tls", tls)

                // Transport
                injectTransport(outbound, queryParams, host)
            } else if (scheme == "ss") {
                outbound.put("type", "shadowsocks")
                // Shadowsocks format: ss://base64(method:password)@host:port or ss://base64(method:password@host:port)
                if (userInfo.isEmpty()) {
                    // Modern format base64
                    val decoded = String(Base64.decode(mainPart, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), StandardCharsets.UTF_8)
                    if (decoded.contains("@")) {
                        val parts = decoded.split("@")
                        val creds = parts[0].split(":")
                        outbound.put("method", creds[0])
                        outbound.put("password", creds[1])
                        
                        val serverParts = parts[1].split(":")
                        outbound.put("server", serverParts[0])
                        outbound.put("server_port", serverParts[1].toInt())
                    }
                } else {
                    // Classic format
                    val decodedCreds = if (userInfo.contains(":")) {
                        userInfo
                    } else {
                        String(Base64.decode(userInfo, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), StandardCharsets.UTF_8)
                    }
                    val creds = decodedCreds.split(":")
                    outbound.put("method", creds[0])
                    outbound.put("password", creds[1])
                    outbound.put("server", host)
                    outbound.put("server_port", port)
                }
            } else if (scheme == "socks" || scheme == "socks5") {
                outbound.put("type", "socks")
                outbound.put("server", host)
                outbound.put("server_port", port)
                if (userInfo.isNotEmpty()) {
                    val creds = userInfo.split(":")
                    outbound.put("username", creds[0])
                    if (creds.size > 1) {
                        outbound.put("password", creds[1])
                    }
                }
            } else if (scheme == "http" || scheme == "https") {
                outbound.put("type", "http")
                outbound.put("server", host)
                outbound.put("server_port", port)
                if (userInfo.isNotEmpty()) {
                    val creds = userInfo.split(":")
                    outbound.put("username", creds[0])
                    if (creds.size > 1) {
                        outbound.put("password", creds[1])
                    }
                }
                if (scheme == "https") {
                    val tls = JSONObject().apply {
                        put("enabled", true)
                        queryParams["sni"]?.let { put("server_name", it) } ?: put("server_name", host)
                    }
                    outbound.put("tls", tls)
                }
            } else if (scheme == "vmess") {
                val decoded = tryBase64Decode(mainPart)
                if (decoded != null && decoded.startsWith("{")) {
                    val vmessJson = JSONObject(decoded)
                    val add = vmessJson.optString("add")
                    val portVal = vmessJson.opt("port")
                    val port = when (portVal) {
                        is Number -> portVal.toInt()
                        is String -> portVal.toIntOrNull() ?: 443
                        else -> 443
                    }
                    val id = vmessJson.optString("id")
                    val aidVal = vmessJson.opt("aid")
                    val aid = when (aidVal) {
                        is Number -> aidVal.toInt()
                        is String -> aidVal.toIntOrNull() ?: 0
                        else -> 0
                    }
                    val scy = vmessJson.optString("scy", "auto")
                    val net = vmessJson.optString("net").lowercase()
                    val host = vmessJson.optString("host")
                    val path = vmessJson.optString("path")
                    val tlsVal = vmessJson.optString("tls").lowercase()
                    val sni = vmessJson.optString("sni")

                    outbound.put("type", "vmess")
                    outbound.put("server", add)
                    outbound.put("server_port", port)
                    outbound.put("uuid", id)
                    outbound.put("security", if (scy.isEmpty()) "auto" else scy)
                    outbound.put("alter_id", aid)
                    outbound.put("packet_encoding", "xudp")

                    val hasTls = tlsVal == "tls" || tlsVal == "true" || tlsVal == "1" || sni.isNotEmpty()
                    if (hasTls) {
                        val tls = JSONObject()
                        tls.put("enabled", true)
                        if (sni.isNotEmpty()) {
                            tls.put("server_name", sni)
                        } else if (host.isNotEmpty() && net != "tcp") {
                            tls.put("server_name", host)
                        }

                        val utls = JSONObject()
                        utls.put("enabled", true)
                        utls.put("fingerprint", "chrome")
                        tls.put("utls", utls)

                        val alpnVal = vmessJson.optString("alpn")
                        if (alpnVal.isNotEmpty()) {
                            val alpnList = alpnVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (alpnList.isNotEmpty()) {
                                tls.put("alpn", JSONArray(alpnList))
                            }
                        }
                        outbound.put("tls", tls)
                    }

                    if (net == "ws" || net == "grpc" || net == "httpupgrade" || net == "kcp" || net == "mkcp" || net == "h2" || net == "http" || net == "xhttp") {
                        val transport = JSONObject()
                        val transType = if (net == "h2") "http" else net
                        transport.put("type", transType)

                        val fallbackHost = if (host.isNotEmpty()) host else add
                        if (net == "ws") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (fallbackHost.isNotEmpty()) {
                                val headers = JSONObject()
                                headers.put("Host", fallbackHost)
                                transport.put("headers", headers)
                            }
                        } else if (net == "grpc") {
                            transport.put("service_name", path)
                        } else if (net == "httpupgrade" || net == "http" || net == "h2") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (fallbackHost.isNotEmpty()) {
                                if (net == "http" || net == "h2") {
                                    val hostArray = JSONArray()
                                    hostArray.put(fallbackHost)
                                    transport.put("host", hostArray)
                                } else {
                                    transport.put("host", fallbackHost)
                                }
                                val headers = JSONObject()
                                headers.put("Host", fallbackHost)
                                transport.put("headers", headers)
                            }
                        } else if (net == "xhttp") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (fallbackHost.isNotEmpty()) {
                                transport.put("host", fallbackHost)
                            }
                            val modeVal = vmessJson.optString("mode")
                            if (modeVal.isNotEmpty()) {
                                transport.put("mode", modeVal)
                            }
                            val xPaddingBytes = if (vmessJson.has("x_padding_bytes")) {
                                vmessJson.optString("x_padding_bytes")
                            } else if (vmessJson.has("xPaddingBytes")) {
                                vmessJson.optString("xPaddingBytes")
                            } else {
                                ""
                            }
                            if (xPaddingBytes.isNotEmpty()) {
                                transport.put("x_padding_bytes", xPaddingBytes)
                            }
                        }
                        outbound.put("transport", transport)
                    }
                } else {
                    outbound.put("type", "vmess")
                    outbound.put("uuid", userInfo)
                    outbound.put("server", host)
                    outbound.put("server_port", port)
                    outbound.put("security", queryParams["scy"] ?: "auto")
                    outbound.put("alter_id", queryParams["aid"]?.toIntOrNull() ?: 0)
                    outbound.put("packet_encoding", "xudp")

                    val security = queryParams["security"]?.lowercase()
                    val hasTls = security == "tls" || queryParams["tls"] == "true" || queryParams["tls"] == "1"
                    if (hasTls) {
                        val tls = JSONObject()
                        tls.put("enabled", true)
                        queryParams["sni"]?.let { tls.put("server_name", it) }
                        val utls = JSONObject().apply {
                            put("enabled", true)
                            put("fingerprint", queryParams["fp"] ?: "chrome")
                        }
                        tls.put("utls", utls)
                        outbound.put("tls", tls)
                    }
                    injectTransport(outbound, queryParams, host)
                }
            } else if (scheme == "hysteria2" || scheme == "hy2") {
                outbound.put("type", "hysteria2")
                outbound.put("password", userInfo)
                outbound.put("server", host)
                outbound.put("server_port", port)

                val tls = JSONObject()
                tls.put("enabled", true)

                val sni = queryParams["sni"] ?: queryParams["peer"] ?: host
                if (sni.isNotEmpty()) {
                    tls.put("server_name", sni)
                }

                val insecure = queryParams["insecure"] == "1" || queryParams["insecure"] == "true" || queryParams["allowInsecure"] == "1" || queryParams["allowInsecure"] == "true"
                tls.put("insecure", insecure)

                val alpnVal = queryParams["alpn"]
                if (alpnVal != null && alpnVal.isNotEmpty()) {
                    val alpnList = alpnVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    tls.put("alpn", JSONArray(alpnList))
                }

                val pinSha256 = queryParams["pinSHA256"] ?: queryParams["pin_sha256"]
                if (pinSha256 != null && pinSha256.isNotEmpty()) {
                    tls.put("pin_sha256", JSONArray(listOf(pinSha256)))
                }

                outbound.put("tls", tls)

                val upStr = queryParams["up"] ?: queryParams["up_mbps"]
                if (upStr != null && upStr.isNotEmpty()) {
                    val upClean = upStr.filter { it.isDigit() }.toIntOrNull()
                    if (upClean != null) {
                        outbound.put("up_mbps", upClean)
                    }
                }
                val downStr = queryParams["down"] ?: queryParams["down_mbps"]
                if (downStr != null && downStr.isNotEmpty()) {
                    val downClean = downStr.filter { it.isDigit() }.toIntOrNull()
                    if (downClean != null) {
                        outbound.put("down_mbps", downClean)
                    }
                }

                val obfsType = queryParams["obfs"] ?: queryParams["obfs.type"] ?: queryParams["obfs_type"]
                val obfsPassword = queryParams["obfs-password"] ?: queryParams["obfs.password"] ?: queryParams["obfs_password"]
                if (obfsType != null && obfsType.isNotEmpty() && obfsType != "none") {
                    val obfsObj = JSONObject()
                    obfsObj.put("type", obfsType)
                    if (obfsPassword != null && obfsPassword.isNotEmpty()) {
                        obfsObj.put("password", obfsPassword)
                    }
                    outbound.put("obfs", obfsObj)
                }
            } else if (scheme == "tuic") {
                outbound.put("type", "tuic")

                if (userInfo.contains(":")) {
                    val parts = userInfo.split(":")
                    outbound.put("uuid", parts[0])
                    outbound.put("password", parts[1])
                } else {
                    outbound.put("uuid", userInfo)
                    queryParams["pass"]?.let { outbound.put("password", it) }
                    queryParams["password"]?.let { outbound.put("password", it) }
                }

                outbound.put("server", host)
                outbound.put("server_port", port)

                val tls = JSONObject()
                tls.put("enabled", true)

                val sni = queryParams["sni"] ?: queryParams["peer"] ?: host
                if (sni.isNotEmpty()) {
                    tls.put("server_name", sni)
                }

                val insecure = queryParams["insecure"] == "1" || queryParams["insecure"] == "true" || queryParams["allowInsecure"] == "1" || queryParams["allowInsecure"] == "true"
                tls.put("insecure", insecure)

                val alpnVal = queryParams["alpn"]
                if (alpnVal != null && alpnVal.isNotEmpty()) {
                    val alpnList = alpnVal.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    tls.put("alpn", JSONArray(alpnList))
                }

                val pinSha256 = queryParams["pinSHA256"] ?: queryParams["pin_sha256"]
                if (pinSha256 != null && pinSha256.isNotEmpty()) {
                    tls.put("pin_sha256", JSONArray(listOf(pinSha256)))
                }

                outbound.put("tls", tls)

                val cc = queryParams["congestion_control"] ?: queryParams["congestionControl"] ?: queryParams["cc"]
                if (cc != null && cc.isNotEmpty()) {
                    outbound.put("congestion_control", cc)
                }

                val urm = queryParams["udp_relay_mode"] ?: queryParams["udpRelayMode"]
                if (urm != null && urm.isNotEmpty()) {
                    outbound.put("udp_relay_mode", urm)
                }

                val zr = queryParams["zero_rtt_handshake"] ?: queryParams["zeroRttHandshake"] ?: queryParams["zero_rtt"] ?: queryParams["zeroRtt"]
                if (zr != null && zr.isNotEmpty()) {
                    val zrBool = zr == "1" || zr == "true"
                    outbound.put("zero_rtt_handshake", zrBool)
                }

                val hb = queryParams["heartbeat"] ?: queryParams["heartbeat_interval"]
                if (hb != null && hb.isNotEmpty()) {
                    val hbStr = if (hb.endsWith("s") || hb.endsWith("ms")) hb else "${hb}s"
                    outbound.put("heartbeat", hbStr)
                }
            } else if (scheme == "wireguard" || scheme == "awg" || scheme == "amneziawg") {
                val isAmnezia = scheme == "awg" || scheme == "amneziawg"
                val configText = queryParams["config"]?.let { tryBase64Decode(it) } ?: ""
                if (configText.isNotEmpty()) {
                    val wgOutbound = parseWireGuardConf(configText, defaultTag)
                    if (isAmnezia) {
                        wgOutbound.put("type", "amneziawg")
                    }
                    wgOutbound.keys().forEach { key ->
                        outbound.put(key, wgOutbound.get(key))
                    }
                } else {
                    outbound.put("type", if (isAmnezia) "amneziawg" else "wireguard")
                    outbound.put("private_key", userInfo)
                    outbound.put("mtu", queryParams["mtu"]?.toIntOrNull() ?: 1400)
                    
                    val addressStr = queryParams["address"] ?: queryParams["ip"] ?: "10.0.0.2/32"
                    val addressArray = JSONArray()
                    addressStr.split(",").forEach { addr ->
                        val trimmedAddr = addr.trim()
                        if (trimmedAddr.isNotEmpty()) {
                            if (trimmedAddr.contains("/")) {
                                addressArray.put(trimmedAddr)
                            } else {
                                addressArray.put("$trimmedAddr/32")
                            }
                        }
                    }
                    outbound.put("address", addressArray)

                    val peer = JSONObject().apply {
                        put("server", host)
                        put("server_port", port)
                        put("public_key", queryParams["public_key"] ?: queryParams["pk"] ?: "")
                        queryParams["preshared_key"]?.let { put("pre_shared_key", it) }
                        queryParams["psk"]?.let { put("pre_shared_key", it) }
                        
                        val allowedIps = queryParams["allowed_ips"] ?: "0.0.0.0/0"
                        val allowedIpsArray = JSONArray()
                        allowedIps.split(",").forEach { ip ->
                            val trimmedIp = ip.trim()
                            if (trimmedIp.isNotEmpty()) {
                                if (trimmedIp.contains("/")) {
                                    allowedIpsArray.put(trimmedIp)
                                } else {
                                    allowedIpsArray.put("$trimmedIp/0")
                                }
                            }
                        }
                        put("allowed_ips", allowedIpsArray)
                        
                        val keepalive = queryParams["keepalive"] ?: queryParams["persistent_keepalive"]
                        keepalive?.toIntOrNull()?.let { put("persistent_keepalive_interval", it) }
                        
                        val reserved = queryParams["reserved"]
                        if (reserved != null && reserved.isNotEmpty()) {
                            val reservedArray = JSONArray()
                            reserved.split(",").forEach { r ->
                                r.trim().toIntOrNull()?.let { reservedArray.put(it) }
                            }
                            if (reservedArray.length() > 0) {
                                put("reserved", reservedArray)
                            }
                        }
                    }
                    outbound.put("peers", JSONArray().apply { put(peer) })

                    val hasAmnezia = queryParams.containsKey("jc") || queryParams.containsKey("jmin") || 
                                     queryParams.containsKey("jmax") || queryParams.containsKey("s1") || 
                                     queryParams.containsKey("s2") || queryParams.containsKey("s3") || 
                                     queryParams.containsKey("s4") || queryParams.containsKey("h1") || 
                                     queryParams.containsKey("h2") || queryParams.containsKey("h3") || 
                                     queryParams.containsKey("h4") || queryParams.containsKey("i1") ||
                                     queryParams.containsKey("i2") || queryParams.containsKey("i3") ||
                                     queryParams.containsKey("i4") || queryParams.containsKey("i5") ||
                                     queryParams.containsKey("j1") || queryParams.containsKey("j2") ||
                                     queryParams.containsKey("j3") || queryParams.containsKey("itime")
                    if (hasAmnezia) {
                        val amnezia = JSONObject().apply {
                            queryParams["jc"]?.toIntOrNull()?.let { put("jc", it) }
                            queryParams["jmin"]?.toIntOrNull()?.let { put("jmin", it) }
                            queryParams["jmax"]?.toIntOrNull()?.let { put("jmax", it) }
                            queryParams["s1"]?.toIntOrNull()?.let { put("s1", it) }
                            queryParams["s2"]?.toIntOrNull()?.let { put("s2", it) }
                            queryParams["s3"]?.toIntOrNull()?.let { put("s3", it) }
                            queryParams["s4"]?.toIntOrNull()?.let { put("s4", it) }
                            queryParams["h1"]?.toLongOrNull()?.let { put("h1", it) }
                            queryParams["h2"]?.toLongOrNull()?.let { put("h2", it) }
                            queryParams["h3"]?.toLongOrNull()?.let { put("h3", it) }
                            queryParams["h4"]?.toLongOrNull()?.let { put("h4", it) }
                            queryParams["i1"]?.let { put("i1", it) }
                            queryParams["i2"]?.let { put("i2", it) }
                            queryParams["i3"]?.let { put("i3", it) }
                            queryParams["i4"]?.let { put("i4", it) }
                            queryParams["i5"]?.let { put("i5", it) }
                            queryParams["j1"]?.let { put("j1", it) }
                            queryParams["j2"]?.let { put("j2", it) }
                            queryParams["j3"]?.let { put("j3", it) }
                            queryParams["itime"]?.toLongOrNull()?.let { put("itime", it) }
                        }
                        outbound.put("amnezia", amnezia)
                    }
                }
            } else if (scheme == "openvpn" || scheme == "ovpn") {
                throw IllegalArgumentException("OpenVPN is not supported")
            } else if (scheme == "mieru") {
                outbound.put("type", "mieru")
                outbound.put("server", host)
                outbound.put("server_port", port)
                if (userInfo.contains(":")) {
                    val parts = userInfo.split(":")
                    outbound.put("username", parts[0])
                    outbound.put("password", parts[1])
                }
                val serverPorts = queryParams["ports"] ?: queryParams["server_ports"]
                if (serverPorts != null && serverPorts.isNotEmpty()) {
                    val portsArray = JSONArray()
                    serverPorts.split(",").forEach { portsArray.put(it.trim()) }
                    outbound.put("server_ports", portsArray)
                }
                outbound.put("transport", queryParams["transport"] ?: "tcp")
                outbound.put("multiplexing", queryParams["multiplexing"] ?: "multiplexing")
                outbound.put("traffic_pattern", queryParams["traffic_pattern"] ?: "heavy")
            } else if (scheme == "ssr") {
                outbound.put("type", "shadowsocksr")
                val decoded = tryBase64Decode(content)
                if (decoded != null && decoded.isNotEmpty()) {
                    val mainPartSsr = decoded.substringBefore("/?")
                    val queryPartSsr = if (decoded.contains("/?")) decoded.substringAfter("/?") else ""
                    val tokens = mainPartSsr.split(":")
                    if (tokens.size >= 6) {
                        outbound.put("server", tokens[0])
                        outbound.put("server_port", tokens[1].toIntOrNull() ?: 8388)
                        outbound.put("protocol", tokens[2])
                        outbound.put("method", tokens[3])
                        outbound.put("obfs", tokens[4])
                        outbound.put("password", tryBase64Decode(tokens[5]) ?: "")
                    }
                    if (queryPartSsr.isNotEmpty()) {
                        val ssrQueryParams = parseQueryParams(queryPartSsr)
                        ssrQueryParams["obfsparam"]?.let { outbound.put("obfs_param", tryBase64Decode(it) ?: "") }
                        ssrQueryParams["protoparam"]?.let { outbound.put("protocol_param", tryBase64Decode(it) ?: "") }
                    }
                }
            } else if (scheme == "shadowtls") {
                outbound.put("type", "shadowtls")
                outbound.put("server", host)
                outbound.put("server_port", port)
                outbound.put("password", userInfo)
                outbound.put("version", queryParams["version"]?.toIntOrNull() ?: 3)
                
                val tls = JSONObject().apply {
                    put("enabled", true)
                    val sni = queryParams["sni"] ?: queryParams["host"] ?: "speedtest.net"
                    put("server_name", sni)
                }
                outbound.put("tls", tls)
            } else if (scheme == "snell") {
                outbound.put("type", "snell")
                outbound.put("server", host)
                outbound.put("server_port", port)
                outbound.put("password", userInfo)
                outbound.put("version", queryParams["version"]?.toIntOrNull() ?: 2)
                val obfsType = queryParams["obfs"]
                if (obfsType != null && obfsType.isNotEmpty() && obfsType != "none") {
                    val obfsObj = JSONObject().apply {
                        put("type", obfsType)
                        put("host", queryParams["obfs-host"] ?: queryParams["obfs_host"] ?: "speedtest.net")
                    }
                    outbound.put("obfs", obfsObj)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return outbound
    }

    private fun parseWireGuardConf(confText: String, defaultTag: String): JSONObject {
        val outbound = JSONObject()
        outbound.put("type", "wireguard")
        outbound.put("tag", defaultTag)
        outbound.put("_original_link", confText)

        var privateKey = ""
        var addressStr = "10.0.0.2/32"
        var mtu = 1400
        
        var serverHost = ""
        var serverPort = 51820
        var publicKey = ""
        var presharedKey = ""
        var allowedIpsStr = "0.0.0.0/0"
        var keepalive = 0
        var reservedStr = ""

        // AmneziaWG parameters
        var jc: Int? = null
        var jmin: Int? = null
        var jmax: Int? = null
        var s1: Int? = null
        var s2: Int? = null
        var s3: Int? = null
        var s4: Int? = null
        var h1: Long? = null
        var h2: Long? = null
        var h3: Long? = null
        var h4: Long? = null
        var i1: String? = null
        var i2: String? = null
        var i3: String? = null
        var i4: String? = null
        var i5: String? = null
        var j1: String? = null
        var j2: String? = null
        var j3: String? = null
        var itime: Long? = null

        confText.split("\n").forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith(";")) return@forEach
            val parts = trimmedLine.split(Regex("="), 2)
            if (parts.size < 2) return@forEach
            val key = parts[0].trim().lowercase()
            val value = parts[1].trim()

            when (key) {
                "privatekey", "private_key", "private-key", "private key" -> privateKey = value
                "address" -> addressStr = value
                "mtu" -> mtu = value.toIntOrNull() ?: 1400
                "publickey", "public_key", "public-key", "public key" -> publicKey = value
                "presharedkey", "preshared_key", "preshared-key", "preshared key" -> presharedKey = value
                "allowedips", "allowed_ips", "allowed-ips", "allowed ips" -> allowedIpsStr = value
                "persistentkeepalive", "persistent_keepalive", "persistent-keepalive", "persistent keepalive", "persistent_keepalive_interval" -> keepalive = value.toIntOrNull() ?: 0
                "reserved" -> reservedStr = value
                "endpoint" -> {
                    val colonIdx = value.lastIndexOf(":")
                    if (colonIdx >= 0) {
                        serverHost = value.substring(0, colonIdx)
                        serverPort = value.substring(colonIdx + 1).toIntOrNull() ?: 51820
                    } else {
                        serverHost = value
                    }
                }
                // AmneziaWG properties
                "jc" -> jc = value.toIntOrNull()
                "jmin" -> jmin = value.toIntOrNull()
                "jmax" -> jmax = value.toIntOrNull()
                "s1" -> s1 = value.toIntOrNull()
                "s2" -> s2 = value.toIntOrNull()
                "s3" -> s3 = value.toIntOrNull()
                "s4" -> s4 = value.toIntOrNull()
                "h1" -> h1 = value.toLongOrNull()
                "h2" -> h2 = value.toLongOrNull()
                "h3" -> h3 = value.toLongOrNull()
                "h4" -> h4 = value.toLongOrNull()
                "i1" -> i1 = value
                "i2" -> i2 = value
                "i3" -> i3 = value
                "i4" -> i4 = value
                "i5" -> i5 = value
                "j1" -> j1 = value
                "j2" -> j2 = value
                "j3" -> j3 = value
                "itime" -> itime = value.toLongOrNull()
            }
        }

        outbound.put("private_key", privateKey)
        outbound.put("mtu", mtu)

        val addressArray = JSONArray()
        addressStr.split(",").forEach { addr ->
            val trimmedAddr = addr.trim()
            if (trimmedAddr.isNotEmpty()) {
                if (trimmedAddr.contains("/")) {
                    addressArray.put(trimmedAddr)
                } else {
                    addressArray.put("$trimmedAddr/32")
                }
            }
        }
        outbound.put("address", addressArray)

        val peer = JSONObject().apply {
            put("server", serverHost)
            put("server_port", serverPort)
            put("public_key", publicKey)
            if (presharedKey.isNotEmpty()) {
                put("pre_shared_key", presharedKey)
            }
            val allowedIpsArray = JSONArray()
            allowedIpsStr.split(",").forEach { ip ->
                val trimmedIp = ip.trim()
                if (trimmedIp.isNotEmpty()) {
                    if (trimmedIp.contains("/")) {
                        allowedIpsArray.put(trimmedIp)
                    } else {
                        allowedIpsArray.put("$trimmedIp/0")
                    }
                }
            }
            put("allowed_ips", allowedIpsArray)
            if (keepalive > 0) {
                put("persistent_keepalive_interval", keepalive)
            }
            if (reservedStr.isNotEmpty()) {
                val reservedArray = JSONArray()
                reservedStr.split(",").forEach { r ->
                    r.trim().toIntOrNull()?.let { reservedArray.put(it) }
                }
                if (reservedArray.length() > 0) {
                    put("reserved", reservedArray)
                }
            }
        }
        outbound.put("peers", JSONArray().apply { put(peer) })

        val hasAmnezia = jc != null || jmin != null || jmax != null || s1 != null || s2 != null || s3 != null || s4 != null || h1 != null || h2 != null || h3 != null || h4 != null || i1 != null || i2 != null || i3 != null || i4 != null || i5 != null || j1 != null || j2 != null || j3 != null || itime != null
        if (hasAmnezia) {
            outbound.put("type", "amneziawg")
            val amnezia = JSONObject().apply {
                jc?.let { put("jc", it) }
                jmin?.let { put("jmin", it) }
                jmax?.let { put("jmax", it) }
                s1?.let { put("s1", it) }
                s2?.let { put("s2", it) }
                s3?.let { put("s3", it) }
                s4?.let { put("s4", it) }
                h1?.let { put("h1", it) }
                h2?.let { put("h2", it) }
                h3?.let { put("h3", it) }
                h4?.let { put("h4", it) }
                i1?.let { put("i1", it) }
                i2?.let { put("i2", it) }
                i3?.let { put("i3", it) }
                i4?.let { put("i4", it) }
                i5?.let { put("i5", it) }
                j1?.let { put("j1", it) }
                j2?.let { put("j2", it) }
                j3?.let { put("j3", it) }
                itime?.let { put("itime", it) }
            }
            outbound.put("amnezia", amnezia)
        } else {
            outbound.put("type", "wireguard")
        }

        return outbound
    }

    private fun parseOpenVpnConfDetails(outbound: JSONObject, configText: String) {
        val serversArray = JSONArray()
        val dnsServersArray = JSONArray()
        var protoVal = "udp"
        var cipherVal = "AES-256-GCM"
        var authVal = "SHA512"
        var tlsCryptText = ""
        var tlsCryptV2Text = ""
        var tlsAuthText = ""
        var keyDir: Int? = null
        var usernameVal = ""
        var passwordVal = ""
        var verifyX509NameVal = ""
        var verifyX509NameModeVal = ""
        
        fun extractTag(text: String, tag: String): String {
            val start = "<$tag>"
            val end = "</$tag>"
            val startIdx = text.indexOf(start)
            val endIdx = text.indexOf(end)
            if (startIdx >= 0 && endIdx > startIdx) {
                return text.substring(startIdx + start.length, endIdx).trim()
            }
            return ""
        }
        
        fun cleanOpenVpnBlock(text: String): String {
            if (text.isEmpty()) return ""
            val lines = text.split("\n")
            val cleanedLines = lines.filter { line ->
                val trimmed = line.trim()
                !trimmed.startsWith("#") && !trimmed.startsWith(";")
            }
            return cleanedLines.joinToString("\n").trim()
        }
        
        tlsCryptText = cleanOpenVpnBlock(extractTag(configText, "tls-crypt"))
        tlsCryptV2Text = cleanOpenVpnBlock(extractTag(configText, "tls-crypt-v2"))
        tlsAuthText = cleanOpenVpnBlock(extractTag(configText, "tls-auth"))
        val caText = cleanOpenVpnBlock(extractTag(configText, "ca"))
        val certText = cleanOpenVpnBlock(extractTag(configText, "cert"))
        val keyText = cleanOpenVpnBlock(extractTag(configText, "key"))
        
        configText.split("\n").forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) return@forEach
            if (trimmedLine.startsWith("#") || trimmedLine.startsWith(";")) {
                if (trimmedLine.contains("EXT-USER:") || trimmedLine.contains("username:")) {
                    usernameVal = trimmedLine.substringAfter(":").trim()
                }
                if (trimmedLine.contains("EXT-PASS:") || trimmedLine.contains("password:")) {
                    passwordVal = trimmedLine.substringAfter(":").trim()
                }
                return@forEach
            }
            val tokens = trimmedLine.split(Regex("\\s+"))
            if (tokens.isEmpty()) return@forEach
            
            when (tokens[0].lowercase()) {
                "remote" -> {
                    if (tokens.size >= 2) {
                        val sHost = tokens[1]
                        val sPort = if (tokens.size >= 3) tokens[2].toIntOrNull() ?: 1194 else 1194
                        val sObj = JSONObject().apply {
                            put("server", sHost)
                            put("server_port", sPort)
                        }
                        serversArray.put(sObj)
                    }
                }
                "proto" -> {
                    if (tokens.size >= 2) {
                        val p = tokens[1].lowercase()
                        protoVal = if (p.contains("tcp")) "tcp" else "udp"
                    }
                }
                "tls-auth" -> {
                    if (tokens.size >= 3) {
                        keyDir = tokens[2].toIntOrNull()
                    } else if (tokens.size >= 2) {
                        val possibleDir = tokens[1].toIntOrNull()
                        if (possibleDir != null) {
                            keyDir = possibleDir
                        }
                    }
                }
                "cipher" -> {
                    if (tokens.size >= 2) {
                        cipherVal = tokens[1]
                    }
                }
                "auth" -> {
                    if (tokens.size >= 2) {
                        authVal = tokens[1]
                    }
                }
                "key-direction" -> {
                    if (tokens.size >= 2) {
                        keyDir = tokens[1].toIntOrNull()
                    }
                }
                "verify-x509-name" -> {
                    if (tokens.size >= 2) {
                        val name = tokens[1].replace("\"", "")
                        val mode = if (tokens.size >= 3) tokens[2] else "name"
                        verifyX509NameVal = name
                        verifyX509NameModeVal = mode
                    }
                }
                "dhcp-option" -> {
                    if (tokens.size >= 3 && tokens[1].lowercase() == "dns") {
                        val dnsIp = tokens[2].replace("\"", "").replace("'", "")
                        if (isIpAddress(dnsIp)) {
                            dnsServersArray.put(dnsIp)
                        }
                    }
                }
            }
        }
        
        outbound.put("servers", serversArray)
        outbound.put("proto", protoVal)
        outbound.put("cipher", cipherVal)
        outbound.put("auth", authVal)
        
        if (usernameVal.isNotEmpty()) outbound.put("username", usernameVal)
        if (passwordVal.isNotEmpty()) outbound.put("password", passwordVal)
        
        val tls = JSONObject().apply {
            if (caText.isNotEmpty()) put("ca", caText)
            if (certText.isNotEmpty()) put("certificate", certText)
            if (keyText.isNotEmpty()) put("key", keyText)
            if (verifyX509NameVal.isNotEmpty()) {
                put("verify_x509_name", verifyX509NameVal)
                put("verify_x509_name_mode", verifyX509NameModeVal)
            }
        }
        outbound.put("tls", tls)
        
        if (tlsCryptText.isNotEmpty()) {
            outbound.put("tls_crypt", tlsCryptText)
        }
        if (tlsCryptV2Text.isNotEmpty()) {
            outbound.put("tls_crypt_v2", tlsCryptV2Text)
        }
        if (tlsAuthText.isNotEmpty()) {
            outbound.put("tls_auth", tlsAuthText)
            if (keyDir != null) {
                outbound.put("key_direction", keyDir)
            }
        }
        val lowerProfile = configText.lowercase()
        val isAntiZapret = lowerProfile.contains("antizapret") || 
                           lowerProfile.contains("prostovpn") || 
                           lowerProfile.contains("31337.lol")
        if (isAntiZapret) {
            dnsServersArray.put("10.224.0.1")
            outbound.put("_is_antizapret", true)
        }

        if (dnsServersArray.length() > 0) {
            outbound.put("_dns_servers", dnsServersArray)
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isEmpty()) return result
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx).replace("+", "%2B"), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1).replace("+", "%2B"), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun sanitizePortFields(config: JSONObject) {
        // 1. Sanitize outbounds
        val outbounds = config.optJSONArray("outbounds")
        if (outbounds != null) {
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                if (outbound.has("port")) {
                    val portVal = outbound.get("port")
                    outbound.remove("port")
                    if (!outbound.has("server_port") || outbound.optInt("server_port") == 0) {
                        outbound.put("server_port", portVal)
                    }
                }
            }
        }

        // 2. Sanitize inbounds
        val inbounds = config.optJSONArray("inbounds")
        if (inbounds != null) {
            for (i in 0 until inbounds.length()) {
                val inbound = inbounds.optJSONObject(i) ?: continue
                if (inbound.has("port")) {
                    val portVal = inbound.get("port")
                    inbound.remove("port")
                    if (!inbound.has("listen_port") || inbound.optInt("listen_port") == 0) {
                        inbound.put("listen_port", portVal)
                    }
                }
            }
        }
    }

    private fun sanitizeXhttpTransport(config: JSONObject) {
        val outbounds = config.optJSONArray("outbounds") ?: return
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val transport = outbound.optJSONObject("transport") ?: continue
            if (transport.optString("type") == "xhttp") {
                // Ensure x_padding_bytes is present and valid for sing-box-extended
                if (!transport.has("x_padding_bytes") || transport.optString("x_padding_bytes").isEmpty()) {
                    transport.put("x_padding_bytes", "100-1000")
                }
                
                // Ensure download.x_padding_bytes is also present and valid if download block exists
                val download = transport.optJSONObject("download")
                if (download != null) {
                    if (!download.has("x_padding_bytes") || download.optString("x_padding_bytes").isEmpty()) {
                        download.put("x_padding_bytes", "100-1000")
                    }
                }
            }
        }
    }

    private fun injectTransport(outbound: JSONObject, queryParams: Map<String, String>, defaultHost: String) {
        var type = queryParams["type"]
        val headerType = queryParams["headerType"] ?: queryParams["header_type"]
        
        // Map type=tcp & headerType=http to http transport, and type=h2 to http transport
        val security = queryParams["security"]?.lowercase()
        val isReality = security == "reality"
        val hasTls = security == "tls" || isReality || queryParams["tls"] == "true" || queryParams["tls"] == "1"
        if ((type == null || type == "tcp") && headerType == "http" && !isReality) {
            type = "http"
        } else if (type == "h2") {
            type = "http"
        }
        
        if (type == null) return
        
        if (type == "ws" || type == "grpc" || type == "httpupgrade" || type == "xhttp" || type == "kcp" || type == "mkcp" || type == "http") {
            val transport = JSONObject()
            if (type == "kcp" || type == "mkcp") {
                transport.put("type", "kcp")
            } else {
                transport.put("type", type)
            }
            if (type == "ws") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
                val edVal = queryParams["ed"]?.toIntOrNull()
                if (edVal != null) {
                    transport.put("max_early_data", edVal)
                    transport.put("early_data_header_name", "Sec-Raw-Websocket-Protocol")
                }
            } else if (type == "grpc") {
                val serviceName = queryParams["serviceName"] ?: queryParams["service_name"] ?: ""
                transport.put("service_name", serviceName)
            } else if (type == "httpupgrade") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    transport.put("host", host)
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
            } else if (type == "http") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    val hostArray = JSONArray()
                    hostArray.put(host)
                    transport.put("host", hostArray)
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
                val method = queryParams["method"] ?: "GET"
                transport.put("method", method)
            } else if (type == "kcp" || type == "mkcp") {
                val seed = queryParams["seed"]
                if (seed != null && seed.isNotEmpty()) {
                    transport.put("seed", seed)
                }
                val hType = queryParams["headerType"] ?: queryParams["header_type"] ?: queryParams["header"]
                if (hType != null && hType.isNotEmpty()) {
                    transport.put("header_type", hType)
                    val headerObj = JSONObject()
                    headerObj.put("type", hType)
                    transport.put("header", headerObj)
                }
            } else if (type == "xhttp") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", path)
                val host = queryParams["host"] ?: queryParams["sni"] ?: defaultHost
                if (host.isNotEmpty()) {
                    transport.put("host", host)
                }
                val mode = queryParams["mode"] ?: "stream-one"
                transport.put("mode", mode)
                val paddingBytes = queryParams["x_padding_bytes"] ?: queryParams["padding"] ?: "100-1000"
                transport.put("x_padding_bytes", paddingBytes)
                
                val extraStr = queryParams["extra"]
                if (extraStr != null && extraStr.isNotEmpty()) {
                    try {
                        val extraObj = JSONObject(extraStr)
                        val keys = extraObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            transport.put(key, extraObj.get(key))
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
            outbound.put("transport", transport)
        }
    }

    private fun getProxyServerHosts(config: JSONObject): List<String> {
        val hosts = mutableListOf<String>()
        val outbounds = config.optJSONArray("outbounds") ?: return hosts
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val type = outbound.optString("type")
            if (type == "openvpn") {
                val serversArr = outbound.optJSONArray("servers")
                if (serversArr != null) {
                    for (j in 0 until serversArr.length()) {
                        val sObj = serversArr.optJSONObject(j)
                        val sHost = sObj?.optString("server") ?: ""
                        if (sHost.isNotEmpty()) {
                            hosts.add(sHost)
                        }
                    }
                }
            } else if (type == "wireguard" || type == "amneziawg") {
                val peersArr = outbound.optJSONArray("peers")
                if (peersArr != null) {
                    for (j in 0 until peersArr.length()) {
                        val pObj = peersArr.optJSONObject(j)
                        val pHost = pObj?.optString("server") ?: ""
                        if (pHost.isNotEmpty()) {
                            hosts.add(pHost)
                        }
                    }
                }
            } else {
                val server = outbound.optString("server")
                if (server.isNotEmpty()) {
                    hosts.add(server)
                }
            }
        }
        return hosts
    }

    private fun getEntrypointProxyServerHosts(config: JSONObject): List<String> {
        val hosts = mutableListOf<String>()
        val outbounds = config.optJSONArray("outbounds") ?: return hosts
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val type = outbound.optString("type")
            val detour = outbound.optString("detour")
            if (detour.isEmpty()) {
                if (type == "openvpn") {
                    val serversArr = outbound.optJSONArray("servers")
                    if (serversArr != null) {
                        for (j in 0 until serversArr.length()) {
                            val sObj = serversArr.optJSONObject(j)
                            val sHost = sObj?.optString("server") ?: ""
                            if (sHost.isNotEmpty()) {
                                hosts.add(sHost)
                            }
                        }
                    }
                } else if (type == "wireguard" || type == "amneziawg") {
                    val peersArr = outbound.optJSONArray("peers")
                    if (peersArr != null) {
                        for (j in 0 until peersArr.length()) {
                            val pObj = peersArr.optJSONObject(j)
                            val pHost = pObj?.optString("server") ?: ""
                            if (pHost.isNotEmpty()) {
                                hosts.add(pHost)
                            }
                        }
                    }
                } else {
                    val server = outbound.optString("server")
                    if (server.isNotEmpty()) {
                        hosts.add(server)
                    }
                }
            }
        }
        return hosts
    }

    private fun extractHostFromUrl(urlStr: String): String? {
        return try {
            val uri = URI(urlStr)
            uri.host ?: urlStr.substringAfter("://").substringBefore("/")
        } catch (e: Exception) {
            null
        }
    }

    private fun isIpAddress(host: String): Boolean {
        val ipv4Pattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$"
        val ipv6Pattern = "^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$"
        return host.matches(ipv4Pattern.toRegex()) || host.matches(ipv6Pattern.toRegex())
    }

    private fun preResolveProxyServers(context: Context, config: JSONObject, settings: InjectorSettings) {
        val outbounds = config.optJSONArray("outbounds") ?: return
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val detour = outbound.optString("detour")
            val tag = outbound.optString("tag")
            val type = outbound.optString("type")
            val isEntryProxy = (tag == "proxy" || tag == "relay-out") && detour.isEmpty()
            if (isEntryProxy) {
                if (type == "openvpn") {
                    val serversArr = outbound.optJSONArray("servers")
                    if (serversArr != null) {
                        for (j in 0 until serversArr.length()) {
                            val sObj = serversArr.optJSONObject(j) ?: continue
                            val server = sObj.optString("server")
                            if (server.isNotEmpty() && !isIpAddress(server)) {
                                android.util.Log.i("Chameleon", "Pre-resolving OpenVPN server domain: $server")
                                val resolvedIp = resolveDomainWithFallbacks(context, server, settings)
                                if (resolvedIp != null) {
                                    android.util.Log.i("Chameleon", "OpenVPN server $server successfully pre-resolved to IP: $resolvedIp")
                                    sObj.put("server", resolvedIp)
                                }
                            }
                        }
                    }
                } else if (type == "wireguard" || type == "amneziawg") {
                    val peersArr = outbound.optJSONArray("peers")
                    if (peersArr != null) {
                        for (j in 0 until peersArr.length()) {
                            val pObj = peersArr.optJSONObject(j) ?: continue
                            val server = pObj.optString("server")
                            if (server.isNotEmpty() && !isIpAddress(server)) {
                                android.util.Log.i("Chameleon", "Pre-resolving WireGuard server domain: $server")
                                val resolvedIp = resolveDomainWithFallbacks(context, server, settings)
                                if (resolvedIp != null) {
                                    android.util.Log.i("Chameleon", "WireGuard server $server successfully pre-resolved to IP: $resolvedIp")
                                    pObj.put("server", resolvedIp)
                                }
                            }
                        }
                    }
                } else {
                    val server = outbound.optString("server")
                    if (server.isNotEmpty() && !isIpAddress(server)) {
                        android.util.Log.i("Chameleon", "Pre-resolving proxy server domain: $server")
                        val resolvedIp = resolveDomainWithFallbacks(context, server, settings)
                        if (resolvedIp != null) {
                            android.util.Log.i("Chameleon", "Proxy server $server successfully pre-resolved to IP: $resolvedIp")
                            
                            // If outbound has TLS enabled, ensure SNI (server_name) is set to original hostname
                            val tls = outbound.optJSONObject("tls")
                            if (tls != null) {
                                if (tls.optBoolean("enabled", false) && !tls.has("server_name")) {
                                    tls.put("server_name", server)
                                }
                            }
                            
                            // Preserve original domain hostname in transport host / Host headers if pre-resolved
                            val transport = outbound.optJSONObject("transport")
                            if (transport != null) {
                                val transType = transport.optString("type")
                                if (transType == "ws") {
                                    var headers = transport.optJSONObject("headers")
                                    if (headers == null) {
                                        headers = JSONObject()
                                        transport.put("headers", headers)
                                    }
                                    if (!headers.has("Host")) {
                                        headers.put("Host", server)
                                    }
                                } else if (transType == "httpupgrade" || transType == "http") {
                                    if (!transport.has("host")) {
                                        if (transType == "http") {
                                            val hostArray = JSONArray()
                                            hostArray.put(server)
                                            transport.put("host", hostArray)
                                        } else {
                                            transport.put("host", server)
                                        }
                                    }
                                    var headers = transport.optJSONObject("headers")
                                    if (headers == null) {
                                        headers = JSONObject()
                                        transport.put("headers", headers)
                                    }
                                    if (!headers.has("Host")) {
                                        headers.put("Host", server)
                                    }
                                } else if (transType == "xhttp") {
                                    if (!transport.has("host")) {
                                        transport.put("host", server)
                                    }
                                }
                            }
                            
                            // Overwrite server domain with the resolved IP
                            outbound.put("server", resolvedIp)
                        } else {
                            android.util.Log.w("Chameleon", "Failed to pre-resolve proxy server: $server. Falling back to default routing.")
                        }
                    }
                }
            }
        }
    }

    private fun resolveDomainViaDoh(domain: String, timeoutMs: Int = 3000): String? {
        if (!dohWorking) return null
        try {
            val url = java.net.URL("https://1.1.1.1/dns-query?name=$domain&type=A")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/dns-json")
            
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val answers = json.optJSONArray("Answer")
                if (answers != null && answers.length() > 0) {
                    for (i in 0 until answers.length()) {
                        val ans = answers.getJSONObject(i)
                        val type = ans.optInt("type")
                        if (type == 1) { // Type A
                            val data = ans.getString("data")
                            if (isPublicIp(data)) {
                                return data
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            dohWorking = false
            android.util.Log.e("Chameleon", "DoH resolution for $domain failed: ${e.message}")
        }
        return null
    }

    private fun resolveDomainWithFallbacks(context: Context, domain: String, settings: InjectorSettings): String? {
        // Try fast system resolver first (works instantly for unblocked domestic/bridge nodes)
        try {
            val addresses = java.net.InetAddress.getAllByName(domain)
            for (addr in addresses) {
                val ip = addr.hostAddress
                if (ip != null && isPublicIp(ip)) {
                    android.util.Log.i("Chameleon", "System resolver successfully resolved $domain to $ip")
                    return ip
                }
            }
        } catch (e: Exception) {}

        // Try secure DoH resolution next to bypass local DNS poisoning and UDP DNS hijacking
        val dohIp = resolveDomainViaDoh(domain)
        if (dohIp != null) {
            return dohIp
        }

        val dnsServers = mutableListOf<String>()
        
        if (settings.bypassIran) {
            // For Iran: prioritize clean public resolvers (e.g. 4.2.2.4, 8.8.8.8, 1.1.1.1) to prevent local mobile carrier poisoning
            listOf("8.8.8.8", "1.1.1.1", "4.2.2.4", "185.51.200.2", "178.22.122.100").forEach { ip ->
                if (!dnsServers.contains(ip)) {
                    dnsServers.add(ip)
                }
            }
        } else {
            // Outside Iran: prioritize Cloudflare, Google, then Shecan
            listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "178.22.122.100").forEach { ip ->
                if (!dnsServers.contains(ip)) {
                    dnsServers.add(ip)
                }
            }
        }

        // Add system DNS at the end as a fallback
        val systemDns = getSystemDnsServers(context)
        systemDns.forEach { ip ->
            if (!dnsServers.contains(ip)) {
                dnsServers.add(ip)
            }
        }

        for (dnsServer in dnsServers) {
            val ip = resolveDomainDirectly(domain, dnsServer)
            if (ip != null) {
                return ip
            }
        }

        // Final fallback: try system DNS (may be hijacked, but better than nothing)
        try {
            val addresses = java.net.InetAddress.getAllByName(domain)
            for (addr in addresses) {
                val ip = addr.hostAddress
                if (ip != null && isPublicIp(ip)) {
                    return ip
                }
            }
        } catch (e: Exception) {}

        return null
    }

    private fun resolveDomainDirectly(domain: String, dnsServerIp: String, timeoutMs: Int = 2000): String? {
        // Clean dnsServerIp from protocol prefixes if present (e.g. tcp://)
        val cleanDnsIp = dnsServerIp.substringAfter("tcp://").substringAfter("udp://")
        
        // Try TCP DNS query first to bypass UDP DNS hijacking in Iran
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(cleanDnsIp, 53), timeoutMs)
            socket.soTimeout = timeoutMs

            val baos = java.io.ByteArrayOutputStream()
            val dos = java.io.DataOutputStream(baos)

            // DNS Header
            dos.writeShort(0x1234) // Transaction ID
            dos.writeShort(0x0100) // Flags: Standard Query
            dos.writeShort(1)      // Questions
            dos.writeShort(0)      // Answer RRs
            dos.writeShort(0)      // Authority RRs
            dos.writeShort(0)      // Additional RRs

            // Question: Domain Name
            val parts = domain.split(".")
            for (part in parts) {
                val bytes = part.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
            dos.writeByte(0) // End of domain

            dos.writeShort(1) // Type A
            dos.writeShort(1) // Class IN

            val queryData = baos.toByteArray()
            
            // For DNS over TCP, prefix the payload with its 2-byte length!
            val outStream = socket.getOutputStream()
            val dataOut = java.io.DataOutputStream(outStream)
            dataOut.writeShort(queryData.size)
            dataOut.write(queryData)
            dataOut.flush()

            val inStream = socket.getInputStream()
            val dataIn = java.io.DataInputStream(inStream)
            
            // Read 2-byte response length header
            val responseLength = dataIn.readUnsignedShort()
            val response = ByteArray(responseLength)
            dataIn.readFully(response)
            socket.close()

            // Parse Response
            val responseStream = java.io.DataInputStream(java.io.ByteArrayInputStream(response))
            val txId = responseStream.readUnsignedShort()
            val flags = responseStream.readUnsignedShort()
            val questions = responseStream.readUnsignedShort()
            val answers = responseStream.readUnsignedShort()
            val authority = responseStream.readUnsignedShort()
            val additional = responseStream.readUnsignedShort()

            // Skip Question Section
            for (q in 0 until questions) {
                var len = responseStream.readByte().toInt()
                while (len > 0) {
                    responseStream.skipBytes(len)
                    len = responseStream.readByte().toInt()
                }
                responseStream.skipBytes(4)
            }

            // Parse Answers
            for (a in 0 until answers) {
                var b = responseStream.readByte().toInt() and 0xFF
                while (b > 0) {
                    if ((b and 0xC0) == 0xC0) {
                        responseStream.readByte()
                        break
                    } else {
                        responseStream.skipBytes(b)
                        b = responseStream.readByte().toInt() and 0xFF
                    }
                }

                val type = responseStream.readUnsignedShort()
                val clazz = responseStream.readUnsignedShort()
                val ttl = responseStream.readInt()
                val dataLength = responseStream.readUnsignedShort()

                if (type == 1 && dataLength == 4) { // Type A (IPv4)
                    val ipBytes = ByteArray(4)
                    responseStream.readFully(ipBytes)
                    val ip = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}.${ipBytes[3].toInt() and 0xFF}"
                    if (isPublicIp(ip)) {
                        return ip
                    }
                } else {
                    responseStream.skipBytes(dataLength)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Chameleon", "TCP DNS query to $cleanDnsIp failed: ${e.message}. Falling back to UDP.")
        }

        // Fallback to UDP if TCP fails
        return resolveDomainDirectlyUDP(domain, cleanDnsIp, timeoutMs)
    }

    private fun resolveDomainDirectlyUDP(domain: String, dnsServerIp: String, timeoutMs: Int = 2000): String? {
        try {
            val socket = java.net.DatagramSocket()
            socket.soTimeout = timeoutMs
            val address = java.net.InetAddress.getByName(dnsServerIp)

            val baos = java.io.ByteArrayOutputStream()
            val dos = java.io.DataOutputStream(baos)

            // DNS Header
            dos.writeShort(0x1234) // Transaction ID
            dos.writeShort(0x0100) // Flags: Standard Query
            dos.writeShort(1)      // Questions
            dos.writeShort(0)      // Answer RRs
            dos.writeShort(0)      // Authority RRs
            dos.writeShort(0)      // Additional RRs

            // Question: Domain Name
            val parts = domain.split(".")
            for (part in parts) {
                val bytes = part.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
            dos.writeByte(0) // End of domain

            dos.writeShort(1) // Type A
            dos.writeShort(1) // Class IN

            val queryData = baos.toByteArray()
            val packet = java.net.DatagramPacket(queryData, queryData.size, address, 53)
            socket.send(packet)

            val buffer = ByteArray(512)
            val responsePacket = java.net.DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            socket.close()

            // Parse Response
            val response = responsePacket.data
            val length = responsePacket.length
            if (length < 12) return null

            val responseStream = java.io.DataInputStream(java.io.ByteArrayInputStream(response, 0, length))
            val txId = responseStream.readUnsignedShort()
            val flags = responseStream.readUnsignedShort()
            val questions = responseStream.readUnsignedShort()
            val answers = responseStream.readUnsignedShort()
            val authority = responseStream.readUnsignedShort()
            val additional = responseStream.readUnsignedShort()

            // Skip Question Section
            for (q in 0 until questions) {
                // Skip domain labels
                var len = responseStream.readByte().toInt()
                while (len > 0) {
                    responseStream.skipBytes(len)
                    len = responseStream.readByte().toInt()
                }
                responseStream.skipBytes(4) // Skip Type and Class
            }

            // Parse Answers
            for (a in 0 until answers) {
                // Name: skip compressed name pointer or labels
                var b = responseStream.readByte().toInt() and 0xFF
                while (b > 0) {
                    if ((b and 0xC0) == 0xC0) {
                        // Pointer: skip the second byte of the pointer and end
                        responseStream.readByte()
                        break
                    } else {
                        responseStream.skipBytes(b)
                        b = responseStream.readByte().toInt() and 0xFF
                    }
                }

                val type = responseStream.readUnsignedShort()
                val clazz = responseStream.readUnsignedShort()
                val ttl = responseStream.readInt()
                val dataLength = responseStream.readUnsignedShort()

                if (type == 1 && dataLength == 4) { // Type A (IPv4)
                    val ipBytes = ByteArray(4)
                    responseStream.readFully(ipBytes)
                    val ip = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}.${ipBytes[3].toInt() and 0xFF}"
                    if (isPublicIp(ip)) {
                        return ip
                    }
                } else {
                    responseStream.skipBytes(dataLength)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Chameleon", "UDP DNS query to $dnsServerIp failed: ${e.message}")
        }
        return null
    }

    private fun isPublicIp(ip: String): Boolean {
        if (!isIpAddress(ip)) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        try {
            val p0 = parts[0].toInt()
            val p1 = parts[1].toInt()
            if (p0 == 127) return false
            if (p0 == 10) return false
            if (p0 == 172 && p1 in 16..31) return false
            if (p0 == 192 && p1 == 168) return false
            if (p0 == 169 && p1 == 254) return false
            if (p0 == 0 || p0 >= 224) return false
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
