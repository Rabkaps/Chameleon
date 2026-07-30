package com.hambalapps.chameleon.desktop.vpn

import com.hambalapps.chameleon.desktop.data.UserSettings
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ConfigInjector {

    private fun tryParseJsonConfig(raw: String, settings: UserSettings): JSONObject? {
        try {
            var trimmed = raw.trim()
            if (trimmed.startsWith("{")) {
                val json = JSONObject(trimmed)
                return if (json.has("outbounds")) {
                    json
                } else if (json.has("type") || json.has("server")) {
                    val proxyOutbound = JSONObject(json.toString())
                    proxyOutbound.put("tag", "proxy")
                    val skeleton = buildDefaultSkeleton(settings)
                    val outbounds = JSONArray()
                    outbounds.put(proxyOutbound)
                    skeleton.put("outbounds", outbounds)
                    skeleton
                } else if (json.has("outbound")) {
                    val out = json.getJSONObject("outbound")
                    val proxyOutbound = JSONObject(out.toString())
                    proxyOutbound.put("tag", "proxy")
                    val skeleton = buildDefaultSkeleton(settings)
                    val outbounds = JSONArray()
                    outbounds.put(proxyOutbound)
                    skeleton.put("outbounds", outbounds)
                    skeleton
                } else {
                    json
                }
            } else if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                val skeleton = buildDefaultSkeleton(settings)
                skeleton.put("outbounds", array)
                return skeleton
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun injectConfig(
        rawProfile: String,
        settings: UserSettings,
        geoipPath: String,
        geositePath: String,
        logPath: String
    ): String {
        try {
            var trimmedProfile = rawProfile.trim().replace("\"", "").replace("'", "")
            val jsonParsed = tryParseJsonConfig(rawProfile.trim(), settings)
            val schemes = listOf("vless://", "vmess://", "trojan://", "ss://", "socks5://", "socks://", "http://", "https://", "hysteria2://", "hy2://", "tuic://")
            val matchingScheme = schemes.find { trimmedProfile.contains(it, ignoreCase = true) }
            if (matchingScheme != null && !trimmedProfile.startsWith(matchingScheme, ignoreCase = true)) {
                val idx = trimmedProfile.indexOf(matchingScheme, ignoreCase = true)
                if (idx >= 0) {
                    trimmedProfile = trimmedProfile.substring(idx)
                }
            }

            val configJson = if (jsonParsed != null) {
                jsonParsed
            } else if (trimmedProfile.startsWith("chain://")) {
                val chainId = trimmedProfile.substringAfter("chain://").substringBefore("#")
                val chains = com.hambalapps.chameleon.desktop.data.deserializeProxyChains(settings.proxyChains)
                val chainItem = chains.find { it.id == chainId }
                if (chainItem != null) {
                    buildConfigFromChain(chainItem, settings)
                } else {
                    buildDefaultSkeleton(settings)
                }
            } else if (matchingScheme != null) {
                buildConfigFromUri(trimmedProfile, settings)
            } else {
                buildDefaultSkeleton(settings)
            }

            // Override log configuration to output to logPath
            val logObj = configJson.optJSONObject("log") ?: JSONObject().also { configJson.put("log", it) }
            logObj.put("level", "info")
            logObj.put("output", "")
            logObj.put("timestamp", true)

            // Sanitize invalid port fields in outbounds and inbounds
            sanitizePortFields(configJson)

            // 1. Pre-resolve proxy server domains to raw IP addresses to bypass DNS hijacking
            preResolveProxyServers(configJson)

            // 2. Inject or update inbounds (mixed local proxy at 127.0.0.1:2080)
            injectMixedInbound(configJson, settings)

            // 2. Inject or update DNS (Split DNS rules)
            injectDns(configJson, settings)

            // 3. Inject or update Routing Rules (Iran bypass)
            injectRouting(configJson, settings, geoipPath, geositePath)

            // 4. Inject direct/block outbounds
            injectOutbounds(configJson, settings)

            // 5. Add Clash API for traffic stats
            injectClashApi(configJson)

            return configJson.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            return buildDefaultSkeleton(settings).toString(2)
        }
    }

    private fun injectMixedInbound(config: JSONObject, settings: UserSettings) {
        val inbounds = config.optJSONArray("inbounds") ?: JSONArray().also { config.put("inbounds", it) }
        val newInbounds = JSONArray()
        
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.optJSONObject(i) ?: continue
            val type = inbound.optString("type")
            if (type != "mixed" && type != "tun") {
                newInbounds.put(inbound)
            }
        }

        val mixedInbound = JSONObject().apply {
            put("type", "mixed")
            put("tag", "mixed-in")
            put("listen", "127.0.0.1")
            put("listen_port", 2080)
        }
        newInbounds.put(mixedInbound)

        if (settings.enableTun) {
            val tunInbound = JSONObject().apply {
                put("type", "tun")
                put("tag", "tun-in")
                put("interface_name", "sing-box-tun")
                put("address", JSONArray(listOf("172.19.0.1/30")))
                put("mtu", 1280)
                put("auto_route", true)
                put("strict_route", true)
                put("stack", settings.tunStack.ifEmpty { "mixed" })
            }
            newInbounds.put(tunInbound)
        }

        config.put("inbounds", newInbounds)
    }

    private fun injectClashApi(config: JSONObject) {
        val experimental = config.optJSONObject("experimental") ?: JSONObject().also { config.put("experimental", it) }
        val clashApi = JSONObject().apply {
            put("external_controller", "127.0.0.1:9090")
            put("secret", "")
        }
        experimental.put("clash_api", clashApi)
    }

    private fun getSystemDnsServers(): List<String> {
        val dnsList = mutableListOf<String>()
        try {
            val clazz = Class.forName("sun.net.dns.ResolverConfiguration")
            val openMethod = clazz.getMethod("open")
            val instance = openMethod.invoke(null)
            val nameserversMethod = clazz.getMethod("nameservers")
            val ns = nameserversMethod.invoke(instance) as List<*>
            ns.forEach {
                val dnsHost = it.toString().trim()
                if (dnsHost.isNotEmpty() && !dnsHost.contains(":")) {
                    dnsList.add(dnsHost)
                }
            }
        } catch (e: Exception) {
            // Silently ignore
        }
        return dnsList
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
                } else if (hostPart == "8.8.8.8" || hostPart == "8.8.4.4") {
                    put("server_name", "dns.google")
                } else if (hostPart == "1.1.1.1" || hostPart == "1.0.0.1") {
                    put("server_name", "cloudflare-dns.com")
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
            val serverIp = if (trimmed.contains(":")) trimmed.substringBefore(":") else trimmed
            serverObj.put("server", serverIp)
            if (trimmed.contains(":")) {
                val port = trimmed.substringAfter(":").toIntOrNull()
                if (port != null) {
                    serverObj.put("server_port", port)
                }
            }
        }
        return serverObj
    }

    private fun injectDns(config: JSONObject, settings: UserSettings) {
        val dns = JSONObject()
        dns.put("reverse_mapping", true)
        dns.put("strategy", "ipv4_only")
        val servers = JSONArray()

        // 1. Secure DNS Server (routes via the proxy)
        val secureServer = createDnsServer("dns-secure", settings.secureDns, "proxy")

        // 2. Local Bypass & Bootstrap DNS Servers
        val directServer = createDnsServer("dns-direct", "1.1.1.1", "direct")
        val shecanServer = createDnsServer("dns-shecan", "178.22.122.100", "direct")
        val radarServer = createDnsServer("dns-radar", "10.202.10.10", "direct")
        val online403Server = createDnsServer("dns-403", "10.202.10.202", "direct")
        val bootstrapServer = createDnsServer("dns-bootstrap", "178.22.122.100", "direct")

        if (settings.bypassIran) {
            servers.put(secureServer)
            servers.put(directServer)
            servers.put(shecanServer)
            servers.put(radarServer)
            servers.put(online403Server)
            servers.put(bootstrapServer)
        } else {
            servers.put(secureServer)
            servers.put(directServer)
            servers.put(bootstrapServer)
        }

        dns.put("servers", servers)
        dns.put("final", "dns-secure")

        val rules = JSONArray()

        // Inject bootstrap rules for proxy server domain and secure DNS DoH domain
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
            val irGeositeRule = JSONObject().apply {
                put("rule_set", JSONArray(listOf("geosite-ir")))
                put("server", "dns-direct")
            }
            rules.put(irGeositeRule)

            val irSuffixRule = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("server", "dns-direct")
            }
            rules.put(irSuffixRule)
        }

        dns.put("rules", rules)
        config.put("dns", dns)
    }

    private fun injectRouting(config: JSONObject, settings: UserSettings, geoipPath: String, geositePath: String) {
        val route = config.optJSONObject("route") ?: JSONObject().also { config.put("route", it) }
        val rules = route.optJSONArray("rules") ?: JSONArray().also { route.put("rules", it) }

        val originalRules = JSONArray()
        for (i in 0 until rules.length()) {
            val r = rules.optJSONObject(i) ?: continue
            val protocol = r.optString("protocol")
            val geosite = r.optJSONArray("geosite")
            val geoip = r.optJSONArray("geoip")
            val suffix = r.optJSONArray("domain_suffix")
            val ruleSetName = r.optString("rule_set")
            val ruleSetArrayVal = r.optJSONArray("rule_set")

            val isIranRule = run {
                val hasIrGeosite = geosite?.let { arr ->
                    (0 until arr.length()).any { j ->
                        val item = arr.optString(j)
                        item == "ir" || item == "geosite-ir" || item == "geoip-ir"
                    }
                } ?: false
                
                val hasIrGeoip = geoip?.let { arr ->
                    (0 until arr.length()).any { j ->
                        val item = arr.optString(j)
                        item == "ir" || item == "geoip-ir" || item == "geosite-ir"
                    }
                } ?: false
                
                val hasIrSuffix = suffix?.let { arr ->
                    (0 until arr.length()).any { j ->
                        val item = arr.optString(j)
                        item == "ir" || item == ".ir" || item.endsWith(".ir")
                    }
                } ?: false
                
                val hasIrRuleSet = ruleSetArrayVal?.let { arr ->
                    (0 until arr.length()).any { j ->
                        val item = arr.optString(j)
                        item == "geoip-ir" || item == "geosite-ir" || item == "ir"
                    }
                } ?: (ruleSetName != null && (ruleSetName == "geoip-ir" || ruleSetName == "geosite-ir" || ruleSetName == "ir"))

                hasIrGeosite || hasIrGeoip || hasIrSuffix || hasIrRuleSet
            }
            
            if (protocol != "dns" && !isIranRule && r.optString("action") != "sniff") {
                originalRules.put(r)
            }
        }

        val newRules = JSONArray()

        // Add sniffing rule at the beginning
        val sniffRule = JSONObject().apply {
            put("action", "sniff")
            put("sniffer", JSONArray(listOf("http", "tls", "quic", "dns", "stun")))
        }
        newRules.put(sniffRule)

        // Add standard DNS routing rule (scope to inbounds to prevent hijacking internal outbound DNS client traffic)
        val dnsRule = JSONObject().apply {
            put("inbound", JSONArray(listOf("tun-in", "mixed-in", "socks-in", "http-in")))
            put("protocol", "dns")
            put("action", "hijack-dns")
        }
        newRules.put(dnsRule)

        // Block Private DNS (DoT)
        val blockDotRule = JSONObject().apply {
            put("port", JSONArray(listOf(853)))
            put("outbound", "block")
        }
        newRules.put(blockDotRule)

        // Route private/local IP networks directly
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
        val privateIpsRule = JSONObject().apply {
            put("ip_cidr", JSONArray(localIps))
            put("outbound", "direct")
        }
        newRules.put(privateIpsRule)

        // Route proxy and secure DNS domains/IPs directly
        val proxyHosts = getProxyServerHosts(config)
        val secureDnsHost = extractHostFromUrl(settings.secureDns)
        val directDomains = mutableListOf<String>()
        val directIps = mutableListOf<String>()

        val systemDnsList = getSystemDnsServers()
        
        // Add all active system DNS server IPs to bypass proxy (direct)
        for (dnsIp in systemDnsList) {
            if (dnsIp.isNotEmpty() && isIpAddress(dnsIp)) {
                directIps.add(dnsIp)
            }
        }
        
        // Ensure default fallback direct DNS address is added
        val defaultDirectDns = "178.22.122.100"
        if (!directIps.contains(defaultDirectDns)) {
            directIps.add(defaultDirectDns)
        }
        
        // Dynamic bootstrap DNS address matching the one in injectDns (Shecan 178.22.122.100)
        val bootstrapDnsAddr = "178.22.122.100"
        if (!directIps.contains(bootstrapDnsAddr)) {
            directIps.add(bootstrapDnsAddr)
        }

        if (settings.bypassIran) {
            listOf("10.202.10.10", "10.202.10.11", "185.51.200.2", "178.22.122.100").forEach { ip ->
                if (!directIps.contains(ip)) {
                    directIps.add(ip)
                }
            }
        }

        val proxyEndpoints = getProxyServerEndpoints(config)
        for (host in proxyEndpoints) {
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
            val directIpsCidr = directIps.map { ip ->
                if (ip.contains("/")) ip
                else if (ip.contains(":")) "$ip/128"
                else "$ip/32"
            }
            val bypassIpsRule = JSONObject().apply {
                put("ip_cidr", JSONArray(directIpsCidr))
                put("outbound", "direct")
            }
            newRules.put(bypassIpsRule)
        }

        if (settings.splitTunnelingEnabled && settings.splitTunnelingApps.isNotEmpty()) {
            if (settings.splitTunnelingMode == "bypass") {
                val bypassRule = JSONObject().apply {
                    put("process_name", JSONArray(settings.splitTunnelingApps.toList()))
                    put("outbound", "direct")
                }
                newRules.put(bypassRule)
            } else if (settings.splitTunnelingMode == "only_route" || settings.splitTunnelingMode == "proxy") {
                val proxyRule = JSONObject().apply {
                    put("process_name", JSONArray(settings.splitTunnelingApps.toList()))
                    put("outbound", "proxy")
                }
                newRules.put(proxyRule)
            }
        }

        if (settings.bypassIran) {
            // Inject or update local rule sets declaration
            val existingRuleSets = route.optJSONArray("rule_set") ?: JSONArray()
            val mergedRuleSets = JSONArray()
            for (i in 0 until existingRuleSets.length()) {
                val rs = existingRuleSets.optJSONObject(i) ?: continue
                val tag = rs.optString("tag")
                if (tag != "geoip-ir" && tag != "geosite-ir") {
                    mergedRuleSets.put(rs)
                }
            }
            mergedRuleSets.put(JSONObject().apply {
                put("tag", "geoip-ir")
                put("type", "local")
                put("format", "binary")
                put("path", geoipPath)
            })
            mergedRuleSets.put(JSONObject().apply {
                put("tag", "geosite-ir")
                put("type", "local")
                put("format", "binary")
                put("path", geositePath)
            })
            route.put("rule_set", mergedRuleSets)

            // Add Iran Bypass Geosite Rule via rule_set
            val irGeosite = JSONObject().apply {
                put("rule_set", JSONArray(listOf("geosite-ir")))
                put("outbound", "direct")
            }
            newRules.put(irGeosite)

            // Add Iran Bypass GeoIP Rule via rule_set
            val irGeoip = JSONObject().apply {
                put("rule_set", JSONArray(listOf("geoip-ir")))
                put("outbound", "direct")
            }
            newRules.put(irGeoip)

            // Add Iran .ir Suffix Rule
            val irSuffix = JSONObject().apply {
                put("domain_suffix", JSONArray(listOf(".ir")))
                put("outbound", "direct")
            }
            newRules.put(irSuffix)
        }

        // Append original profile rules after injected system rules
        for (i in 0 until originalRules.length()) {
            newRules.put(originalRules.getJSONObject(i))
        }

        // Catch-all rule to route all remaining traffic to the proxy outbound
        if (!(settings.splitTunnelingEnabled && settings.splitTunnelingApps.isNotEmpty() && 
            (settings.splitTunnelingMode == "only_route" || settings.splitTunnelingMode == "proxy"))) {
            val catchAllRule = JSONObject().apply {
                put("outbound", "proxy")
            }
            newRules.put(catchAllRule)
        }

        route.put("rules", newRules)
        route.put("default_domain_resolver", "dns-bootstrap")
        route.put("auto_detect_interface", true)
    }

    private fun isCloudflareDomain(host: String = "", sni: String = "", hostHeader: String = ""): Boolean {
        val targets = listOf(host, sni, hostHeader).filter { it.isNotEmpty() }.map { it.lowercase() }
        if (targets.isEmpty()) return false

        return targets.any { target ->
            target.contains(".workers.dev") || target.contains(".pages.dev") ||
            target.contains(".trycloudflare.com") || target.contains(".argotunnel.com") ||
            target.contains(".cloudflare.com") || target.contains(".cloudflareaccess.com") ||
            target.contains(".cloudflarestorage.com") || target.contains(".cloudflare-dns.com") ||
            target.contains(".cloudflareclient.com") || target.contains(".cf-ipfs.com") ||
            target.contains(".cf-dns.com") || target.contains(".cf-ns.com") ||
            target.contains(".cf-ns.net") || target.contains(".cf-ns.org") ||
            target.contains("novaproxy") || target.contains("bpb-worker") ||
            target.contains("cloudflared") || target.contains("cf-panel") ||
            target.contains("cf-edge")
        }
    }

    private fun injectOutbounds(config: JSONObject, settings: UserSettings) {
        val outbounds = config.optJSONArray("outbounds") ?: JSONArray().also { config.put("outbounds", it) }
        val cleanOutbounds = JSONArray()
        var hasDirect = false
        var hasBlock = false

        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            val type = out.optString("type")
            val tag = out.optString("tag")
            if (type == "dns" || tag == "dns-out") {
                continue
            }
            if (tag == "direct") hasDirect = true
            if (tag == "block") hasBlock = true

            val tls = out.optJSONObject("tls")
            val serverHost = out.optString("server")
            val serverName = tls?.optString("server_name") ?: ""
            val transport = out.optJSONObject("transport")
            val transType = transport?.optString("type") ?: ""
            val isWs = transType == "ws"
            val isCloudflareWorker = isCloudflareDomain(serverHost, serverName, hostHeader)
            val isCloudflare = isCloudflareWorker || isWs

            val isProxyOrRelay = (tag == "proxy" || tag == "relay-out")
            val isOpenVpn = type == "openvpn"
            val isWireGuard = type == "wireguard" || type == "amneziawg"
            val hasTls = tls?.optBoolean("enabled", false) ?: (tls != null)
            val flow = out.optString("flow")
            val isVision = flow.contains("vision")
            val isReality = tls?.has("reality") ?: false

            if (settings.enableFragment && isProxyOrRelay && !isOpenVpn && !isWireGuard && hasTls && !isReality && !isVision && !isCloudflare) {
                injectFragmentToOutbound(out, settings)
            } else if (tls != null) {
                tls.remove("fragment")
                tls.remove("record_fragment")
                tls.remove("fragment_fallback_delay")
            }

            if (tag == "proxy" && settings.enableMux && !isWs && !isReality && !isVision) {
                val mux = JSONObject().apply {
                    put("enabled", true)
                    put("protocol", "h2mux")
                    put("max_connections", 8)
                }
                out.put("multiplex", mux)
            } else {
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
        config.put("outbounds", cleanOutbounds)
    }

    private fun injectFragmentToOutbound(outbound: JSONObject, settings: UserSettings) {
        if (outbound.optString("type") == "openvpn") return
        val tls = outbound.optJSONObject("tls") ?: JSONObject().also { outbound.put("tls", it) }
        tls.put("enabled", true)
        tls.put("fragment", true)
        tls.put("record_fragment", true)
        
        val interval = settings.fragmentInterval.trim()
        val delayStr = if (interval.contains("-")) {
            val lastNum = interval.substringAfter("-").filter { it.isDigit() }
            if (lastNum.isNotEmpty()) "${lastNum}ms" else "20ms"
        } else if (interval.endsWith("ms")) {
            interval
        } else {
            val cleanNum = interval.filter { it.isDigit() }
            if (cleanNum.isNotEmpty()) "${cleanNum}ms" else "20ms"
        }
        tls.put("fragment_fallback_delay", delayStr)
    }

    private fun buildDefaultSkeleton(settings: UserSettings): JSONObject {
        return JSONObject().apply {
            put("log", JSONObject().apply {
                put("level", "info")
                put("timestamp", true)
            })
            put("outbounds", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "direct")
                    put("tag", "proxy")
                })
            })
        }
    }

    private fun buildConfigFromUri(uriStr: String, settings: UserSettings): JSONObject {
        val config = buildDefaultSkeleton(settings)
        val outbounds = config.getJSONArray("outbounds")

        try {
            val trimmed = uriStr.trim()
            val fragmentIdx = trimmed.indexOf("#")
            val name = if (fragmentIdx >= 0) {
                URLDecoder.decode(trimmed.substring(fragmentIdx + 1), "UTF-8")
            } else {
                "proxy"
            }
            
            val rest = if (fragmentIdx >= 0) trimmed.substring(0, fragmentIdx) else trimmed
            val schemeIdx = rest.indexOf("://")
            if (schemeIdx < 0) return config
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
            val tag = "proxy"
            val outbound = JSONObject()
            outbound.put("tag", tag)

            if (scheme == "vless") {
                outbound.put("type", "vless")
                outbound.put("uuid", userInfo)
                outbound.put("server", host)
                outbound.put("server_port", port)
                outbound.put("packet_encoding", "xudp")

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

                val hasTls = security == "tls" || isReality || queryParams["tls"] == "true" || queryParams["tls"] == "1"
                if (hasTls) {
                    val tls = JSONObject()
                    tls.put("enabled", true)
                    
                    val sni = queryParams["sni"] ?: queryParams["host"]
                    if (sni != null && sni.isNotEmpty()) {
                        tls.put("server_name", sni)
                    }

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

                injectTransport(outbound, queryParams)
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

                injectTransport(outbound, queryParams)
            } else if (scheme == "ss") {
                outbound.put("type", "shadowsocks")
                if (userInfo.isEmpty()) {
                    val decoded = String(java.util.Base64.getUrlDecoder().decode(mainPart), StandardCharsets.UTF_8)
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
                    val decodedCreds = if (userInfo.contains(":")) {
                        userInfo
                    } else {
                        tryBase64Decode(userInfo) ?: userInfo
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
                    val portNum = when (portVal) {
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
                    val h = vmessJson.optString("host")
                    val path = vmessJson.optString("path")
                    val tlsVal = vmessJson.optString("tls").lowercase()
                    val sni = vmessJson.optString("sni")

                    outbound.put("type", "vmess")
                    outbound.put("server", add)
                    outbound.put("server_port", portNum)
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
                        } else if (h.isNotEmpty() && net != "tcp") {
                            tls.put("server_name", h)
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

                    if (net == "ws" || net == "grpc" || net == "httpupgrade" || net == "kcp" || net == "mkcp" || net == "h2" || net == "http") {
                        val transport = JSONObject()
                        val transType = if (net == "h2") "http" else net
                        transport.put("type", transType)

                        if (net == "ws") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (h.isNotEmpty()) {
                                val headers = JSONObject()
                                headers.put("Host", h)
                                transport.put("headers", headers)
                            }
                        } else if (net == "grpc") {
                            transport.put("service_name", path)
                        } else if (net == "httpupgrade" || net == "http" || net == "h2") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (h.isNotEmpty()) {
                                transport.put("host", h)
                                val headers = JSONObject()
                                headers.put("Host", h)
                                transport.put("headers", headers)
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
                    injectTransport(outbound, queryParams)
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
            } else if (scheme == "tuic") {
                outbound.put("type", "tuic")
                if (userInfo.contains(":")) {
                    val parts = userInfo.split(":")
                    outbound.put("uuid", parts[0])
                    outbound.put("password", parts[1])
                } else {
                    outbound.put("uuid", userInfo)
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
                outbound.put("tls", tls)

                val congestionControl = queryParams["congestion_control"] ?: queryParams["congestionControl"] ?: "bbr"
                outbound.put("congestion_control", congestionControl)
                
                val udpRelayMode = queryParams["udp_relay_mode"] ?: queryParams["udpRelayMode"] ?: "native"
                outbound.put("udp_relay_mode", udpRelayMode)
            }

            outbound.put("tag", "proxy")
            outbounds.put(0, outbound)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return config
    }

    private fun injectTransport(outbound: JSONObject, queryParams: Map<String, String>) {
        var type = queryParams["type"]?.lowercase()
        val headerType = queryParams["headerType"]?.lowercase() ?: queryParams["header_type"]?.lowercase()
        val security = queryParams["security"]?.lowercase()
        if ((type == null || type == "tcp") && headerType == "http") {
            type = "http"
        }
        if (type == null) return
        if (type == "ws" || type == "grpc" || type == "httpupgrade" || type == "kcp" || type == "mkcp" || type == "http" || type == "xhttp") {
            val transport = JSONObject()
            transport.put("type", if (type == "mkcp") "kcp" else type)

            if (type == "ws") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", if (path.startsWith("/")) path else "/$path")
                queryParams["host"]?.let { host ->
                    val headers = JSONObject()
                    headers.put("Host", host)
                    transport.put("headers", headers)
                }
            } else if (type == "grpc") {
                queryParams["serviceName"]?.let { transport.put("service_name", it) }
            } else if (type == "httpupgrade") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", if (path.startsWith("/")) path else "/$path")
                queryParams["host"]?.let { transport.put("host", it) }
            } else if (type == "http") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", if (path.startsWith("/")) path else "/$path")
                queryParams["host"]?.let { transport.put("host", it) }
            } else if (type == "xhttp") {
                val path = queryParams["path"] ?: "/"
                transport.put("path", if (path.startsWith("/")) path else "/$path")
                queryParams["host"]?.let { transport.put("host", it) }
                queryParams["mode"]?.let { transport.put("mode", it) }

                val extraStr = queryParams["extra"]
                if (extraStr != null && extraStr.isNotEmpty()) {
                    try {
                        val extraObj = JSONObject(extraStr)
                        val keys = extraObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val snakeKey = when (key) {
                                "xPaddingBytes" -> "x_padding_bytes"
                                else -> key
                            }
                            transport.put(snakeKey, extraObj.get(key))
                        }
                    } catch (e: Exception) {}
                }

                if (!transport.has("x_padding_bytes") || transport.optString("x_padding_bytes").isEmpty()) {
                    val xPadding = queryParams["x_padding_bytes"] ?: queryParams["xPaddingBytes"] ?: "100-1000"
                    transport.put("x_padding_bytes", xPadding)
                }
            }
            outbound.put("transport", transport)
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (query.isEmpty()) return params
        try {
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                val key = if (idx > 0) URLDecoder.decode(pair.substring(0, idx).replace("+", "%2B"), "UTF-8") else pair
                val value = if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1).replace("+", "%2B"), "UTF-8") else ""
                params[key] = value
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    fun tryBase64Decode(src: String): String? {
        val clean = src.trim().replace("\r", "").replace("\n", "").replace(" ", "")
        if (clean.isEmpty() || clean.startsWith("{") || clean.startsWith("[")) return null
        return try {
            val bytes = java.util.Base64.getDecoder().decode(clean)
            String(bytes, java.nio.charset.StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                val bytes = java.util.Base64.getUrlDecoder().decode(clean)
                String(bytes, java.nio.charset.StandardCharsets.UTF_8)
            } catch (e2: Exception) {
                try {
                    val padded = when (clean.length % 4) {
                        2 -> "$clean=="
                        3 -> "$clean="
                        else -> clean
                    }
                    val bytes = java.util.Base64.getDecoder().decode(padded)
                    String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                } catch (e3: Exception) {
                    try {
                        val padded = when (clean.length % 4) {
                            2 -> "$clean=="
                            3 -> "$clean="
                            else -> clean
                        }
                        val bytes = java.util.Base64.getUrlDecoder().decode(padded)
                        String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                    } catch (e4: Exception) {
                        null
                    }
                }
            }
        }
    }

    private fun sanitizePortFields(config: JSONObject) {
        val inbounds = config.optJSONArray("inbounds")
        if (inbounds != null) {
            for (i in 0 until inbounds.length()) {
                val inbound = inbounds.optJSONObject(i) ?: continue
                sanitizePortInObject(inbound, "listen_port")
            }
        }

        val outbounds = config.optJSONArray("outbounds")
        if (outbounds != null) {
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.optJSONObject(i) ?: continue
                sanitizePortInObject(outbound, "server_port")
                val transport = outbound.optJSONObject("transport")
                if (transport != null && transport.optString("type") == "xhttp") {
                    if (!transport.has("x_padding_bytes") || transport.optString("x_padding_bytes").isEmpty()) {
                        transport.put("x_padding_bytes", "100-1000")
                    }
                    val download = transport.optJSONObject("download")
                    if (download != null) {
                        if (!download.has("x_padding_bytes") || download.optString("x_padding_bytes").isEmpty()) {
                            download.put("x_padding_bytes", "100-1000")
                        }
                    }
                }
            }
        }
    }

    private fun sanitizePortInObject(obj: JSONObject, portField: String) {
        if (obj.has(portField)) {
            val value = obj.opt(portField)
            if (value is String) {
                val parsed = value.toIntOrNull()
                if (parsed != null) {
                    obj.put(portField, parsed)
                } else {
                    obj.remove(portField)
                }
            }
        }
    }

    private fun getProxyServerEndpoints(config: JSONObject): List<String> {
        val endpoints = mutableListOf<String>()
        val outbounds = config.optJSONArray("outbounds") ?: return endpoints
        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            val tag = out.optString("tag")
            if (tag != "direct" && tag != "block") {
                val server = out.optString("server")
                if (server.isNotEmpty()) {
                    endpoints.add(server)
                }
            }
        }
        return endpoints.distinct()
    }

    private fun getProxyServerHosts(config: JSONObject): List<String> {
        val hosts = mutableListOf<String>()
        val outbounds = config.optJSONArray("outbounds") ?: return hosts
        for (i in 0 until outbounds.length()) {
            val out = outbounds.optJSONObject(i) ?: continue
            val tag = out.optString("tag")
            if (tag != "direct" && tag != "block") {
                val server = out.optString("server")
                if (server.isNotEmpty()) hosts.add(server)

                val tlsObj = out.optJSONObject("tls")
                if (tlsObj != null) {
                    val sni = tlsObj.optString("server_name")
                    if (sni.isNotEmpty()) hosts.add(sni)
                }

                val transObj = out.optJSONObject("transport")
                if (transObj != null) {
                    val hostOpt = transObj.opt("host")
                    if (hostOpt is String && hostOpt.isNotEmpty()) {
                        hosts.add(hostOpt)
                    } else if (hostOpt is JSONArray) {
                        for (j in 0 until hostOpt.length()) {
                            val h = hostOpt.optString(j)
                            if (h.isNotEmpty()) hosts.add(h)
                        }
                    }
                    val headers = transObj.optJSONObject("headers")
                    if (headers != null) {
                        val headerHost = headers.opt("Host")
                        if (headerHost is String && headerHost.isNotEmpty()) {
                            hosts.add(headerHost)
                        } else if (headerHost is JSONArray) {
                            for (j in 0 until headerHost.length()) {
                                val h = headerHost.optString(j)
                                if (h.isNotEmpty()) hosts.add(h)
                            }
                        }
                    }
                }
            }
        }
        hosts.add("dns.google")
        hosts.add("cloudflare-dns.com")
        hosts.add("dns.quad9.net")
        return hosts.distinct()
    }

    private fun extractHostFromUrl(urlString: String): String? {
        return try {
            val cleaned = urlString.trim()
            val withoutProtocol = cleaned.substringAfter("://")
            val hostPortPath = withoutProtocol.substringBefore("/")
            hostPortPath.substringBefore(":")
        } catch (e: Exception) {
            null
        }
    }

    private fun isIpAddress(host: String): Boolean {
        if (host.isEmpty()) return false
        val parts = host.split(".")
        if (parts.size == 4) {
            return parts.all { it.toIntOrNull() in 0..255 }
        }
        return host.contains(":")
    }

    private fun buildConfigFromChain(chainItem: com.hambalapps.chameleon.desktop.data.ProxyChain, settings: UserSettings): JSONObject {
        val config = buildDefaultSkeleton(settings)
        val outbounds = config.getJSONArray("outbounds")
        try {
            val exitOutbound = parseOutboundFromUri(chainItem.exitLink, "proxy", settings)
            exitOutbound.put("detour", "relay-out")
            val relayOutbound = parseOutboundFromUri(chainItem.relayLink, "relay-out", settings)

            // Prevent uTLS fingerprint collision
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

            outbounds.put(0, exitOutbound)
            outbounds.put(relayOutbound)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return config
    }

    fun parseOutboundFromUri(uriStr: String, targetTag: String, settings: UserSettings): JSONObject {
        val outbound = JSONObject()
        outbound.put("tag", targetTag)
        try {
            val trimmed = uriStr.trim()
            val fragmentIdx = trimmed.indexOf("#")
            
            val rest = if (fragmentIdx >= 0) trimmed.substring(0, fragmentIdx) else trimmed
            val schemeIdx = rest.indexOf("://")
            if (schemeIdx < 0) return outbound
            val scheme = rest.substring(0, schemeIdx).lowercase()
            
            val content = rest.substring(schemeIdx + 3)
            val queryIdx = content.indexOf("?")
            val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content
            val queryPart = if (queryIdx >= 0) content.substring(queryIdx + 1) else ""
            
            val atIdx = mainPart.indexOf("@")
            val userInfoRaw = if (atIdx >= 0) mainPart.substring(0, atIdx) else ""
            val userInfo = try { URLDecoder.decode(userInfoRaw.replace("+", "%2B"), "UTF-8") } catch (e: Exception) { userInfoRaw }
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

                val security = queryParams["security"]?.lowercase()
                val isReality = security == "reality"

                val type = queryParams["type"]
                val headerType = queryParams["headerType"] ?: queryParams["header_type"]
                val isStandardTcp = (type == null || type.equals("tcp", ignoreCase = true)) && headerType != "http"
                if (isStandardTcp) {
                    val flow = queryParams["flow"]
                    if (flow != null && flow.isNotEmpty() && flow != "none") {
                        outbound.put("flow", flow)
                    }
                }

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

                injectTransport(outbound, queryParams)
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

                injectTransport(outbound, queryParams)
            } else if (scheme == "ss") {
                outbound.put("type", "shadowsocks")
                if (userInfo.isEmpty()) {
                    val decoded = String(java.util.Base64.getUrlDecoder().decode(mainPart), StandardCharsets.UTF_8)
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
                    val decodedCreds = if (userInfo.contains(":")) {
                        userInfo
                    } else {
                        tryBase64Decode(userInfo) ?: userInfo
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
                    val portNum = when (portVal) {
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
                    val h = vmessJson.optString("host")
                    val path = vmessJson.optString("path")
                    val tlsVal = vmessJson.optString("tls").lowercase()
                    val sni = vmessJson.optString("sni")

                    outbound.put("type", "vmess")
                    outbound.put("server", add)
                    outbound.put("server_port", portNum)
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
                        } else if (h.isNotEmpty() && net != "tcp") {
                            tls.put("server_name", h)
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

                    if (net == "ws" || net == "grpc" || net == "httpupgrade" || net == "kcp" || net == "mkcp" || net == "h2" || net == "http") {
                        val transport = JSONObject()
                        val transType = if (net == "h2") "http" else net
                        transport.put("type", transType)

                        if (net == "ws") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (h.isNotEmpty()) {
                                val headers = JSONObject()
                                headers.put("Host", h)
                                transport.put("headers", headers)
                            }
                        } else if (net == "grpc") {
                            transport.put("service_name", path)
                        } else if (net == "httpupgrade" || net == "http" || net == "h2") {
                            transport.put("path", if (path.startsWith("/")) path else "/$path")
                            if (h.isNotEmpty()) {
                                transport.put("host", h)
                                val headers = JSONObject()
                                headers.put("Host", h)
                                transport.put("headers", headers)
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
                    injectTransport(outbound, queryParams)
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
            } else if (scheme == "tuic") {
                outbound.put("type", "tuic")
                if (userInfo.contains(":")) {
                    val parts = userInfo.split(":")
                    outbound.put("uuid", parts[0])
                    outbound.put("password", parts[1])
                } else {
                    outbound.put("uuid", userInfo)
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
                outbound.put("tls", tls)

                val congestionControl = queryParams["congestion_control"] ?: queryParams["congestionControl"] ?: "bbr"
                outbound.put("congestion_control", congestionControl)
                
                val udpRelayMode = queryParams["udp_relay_mode"] ?: queryParams["udpRelayMode"] ?: "native"
                outbound.put("udp_relay_mode", udpRelayMode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return outbound
    }

    private fun preResolveProxyServers(config: JSONObject) {
        val outbounds = config.optJSONArray("outbounds") ?: return
        for (i in 0 until outbounds.length()) {
            val outbound = outbounds.optJSONObject(i) ?: continue
            val tag = outbound.optString("tag")
            if (tag == "proxy") {
                val server = outbound.optString("server")
                if (server.isNotEmpty() && !isIpAddress(server)) {
                    val resolvedIp = preResolveDomain(server)
                    if (resolvedIp != null) {
                        val tls = outbound.optJSONObject("tls")
                        if (tls != null) {
                            if (!tls.has("server_name") || tls.optString("server_name").isEmpty()) {
                                tls.put("server_name", server)
                            }
                        } else {
                            val newTls = JSONObject().apply {
                                put("enabled", true)
                                put("server_name", server)
                            }
                            outbound.put("tls", newTls)
                        }
                        outbound.put("server", resolvedIp)
                    }
                }
            }
        }
    }

    private fun preResolveDomain(domain: String): String? {
        if (domain.isEmpty() || isIpAddress(domain)) return domain
        try {
            val addr = java.net.InetAddress.getByName(domain)
            val ip = addr.hostAddress
            if (ip != null && isPublicIp(ip)) {
                return ip
            }
        } catch (e: Exception) {}

        listOf("178.22.122.100", "10.202.10.10", "8.8.8.8", "1.1.1.1").forEach { dnsIp ->
            val resolved = resolveDomainDirectlyUDP(domain, dnsIp)
            if (resolved != null && isPublicIp(resolved)) {
                return resolved
            }
        }
        return null
    }

    private fun resolveDomainDirectlyUDP(domain: String, dnsServerIp: String, timeoutMs: Int = 2000): String? {
        try {
            val socket = java.net.DatagramSocket()
            socket.soTimeout = timeoutMs
            val address = java.net.InetAddress.getByName(dnsServerIp)

            val baos = java.io.ByteArrayOutputStream()
            val dos = java.io.DataOutputStream(baos)

            dos.writeShort(0x1234)
            dos.writeShort(0x0100)
            dos.writeShort(1)
            dos.writeShort(0)
            dos.writeShort(0)
            dos.writeShort(0)

            val parts = domain.split(".")
            for (part in parts) {
                val bytes = part.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                dos.writeByte(bytes.size)
                dos.write(bytes)
            }
            dos.writeByte(0)

            dos.writeShort(1)
            dos.writeShort(1)

            val queryData = baos.toByteArray()
            val packet = java.net.DatagramPacket(queryData, queryData.size, address, 53)
            socket.send(packet)

            val buffer = ByteArray(512)
            val responsePacket = java.net.DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            socket.close()

            val response = responsePacket.data
            val length = responsePacket.length
            if (length < 12) return null

            val responseStream = java.io.DataInputStream(java.io.ByteArrayInputStream(response, 0, length))
            responseStream.skipBytes(6)
            val questions = responseStream.readUnsignedShort()
            val answers = responseStream.readUnsignedShort()
            responseStream.skipBytes(4)

            for (q in 0 until questions) {
                var len = responseStream.readByte().toInt()
                while (len > 0) {
                    responseStream.skipBytes(len)
                    len = responseStream.readByte().toInt()
                }
                responseStream.skipBytes(4)
            }

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
                responseStream.skipBytes(6)
                val dataLength = responseStream.readUnsignedShort()

                if (type == 1 && dataLength == 4) {
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
        } catch (e: Exception) {}
        return null
    }

    private fun isPublicIp(ip: String): Boolean {
        if (!isIpAddress(ip)) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        try {
            val p0 = parts[0].toInt()
            val p1 = parts[1].toInt()
            if (p0 == 127 || p0 == 10) return false
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
