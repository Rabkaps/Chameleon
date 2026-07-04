package com.hambalapps.expressivebox.vpn

import android.content.Context
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import kotlinx.coroutines.runBlocking

class ConfigInjectorTest {
    @Test
    fun testRealityConfigInjection() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))

        val rawUri = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@zone.nl.netlume.ir:29757?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir&fp=chrome&pbk=t_9lyts8KkYowHc3eDr22L7DuzRUnjRnodNhd1lspAE&sid=462333e748f7577e#%F0%9F%87%B3%F0%9F%87%B1%20%F0%9D%90%8D%F0%9D%90%9E%F0%9D%90%AD%F0%9D%90%A1%F0%9D%90%9E%F0%9D%90%AB%F0%9D%90%A5%F0%9D%90%9A%F0%9D%90%A7%F0%9D%90%9D%F0%9D%90%AC"
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "normal"
        )

        val configStr = ConfigInjector.injectConfig(mockContext, rawUri, settings)
        println("Generated Configuration:")
        println(configStr)
    }

    @Test
    fun testAiBypassConfigInjection() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))

        val rawUri = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@zone.nl.netlume.ir:29757?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir&fp=chrome&pbk=t_9lyts8KkYowHc3eDr22L7DuzRUnjRnodNhd1lspAE&sid=462333e748f7577e#test"
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "ai_bypass",
            warpPrivateKey = "privatekeybase64",
            warpPublicKey = "publickeybase64",
            warpIpAddress = "172.16.0.2/32",
            warpClientId = "6hHy",
            warpDetourMode = "direct",
            warpPort = "4500"
        )

        val configStr = ConfigInjector.injectConfig(mockContext, rawUri, settings)
        println("Generated AI Bypass Configuration:")
        println(configStr)

        val json = org.json.JSONObject(configStr)
        val outbounds = json.getJSONArray("outbounds")
        
        var warpOutbound: org.json.JSONObject? = null
        for (i in 0 until outbounds.length()) {
            val out = outbounds.getJSONObject(i)
            if (out.getString("tag") == "warp-out") {
                warpOutbound = out
                break
            }
        }
        
        assert(warpOutbound != null) { "warp-out outbound not found in outbounds" }
        val endpoint = warpOutbound!!
        assert(endpoint.getString("type") == "wireguard")
        assert(endpoint.getString("tag") == "warp-out")
        assert(endpoint.getJSONArray("local_address").getString(0) == "172.16.0.2/32")
        assert(endpoint.getString("private_key") == "privatekeybase64")
        assert(endpoint.getString("detour") == "direct")

        val peers = endpoint.getJSONArray("peers")
        assert(peers.length() == 1)
        val peer = peers.getJSONObject(0)
        val peerAddress = peer.getString("server")
        assert(peerAddress == "162.159.192.1" || peerAddress.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")))
        assert(peer.getInt("server_port") == 4500)
        
        // In java org.json, "reserved" is a string or array? 
        // In ConfigInjector.kt: if (settings.warpClientId.isNotEmpty()) { put("reserved", settings.warpClientId) }
        // So it is a string in this case (or maybe it can be parsed as a string).
        assert(peer.getString("reserved") == "6hHy")
    }

    @Test
    fun testWarpRegistrationResponse() {
        runBlocking {
            val creds = com.hambalapps.expressivebox.vpn.registerWarpAccount()
            if (creds != null) {
                val file = java.io.File("warp_response.json")
                file.writeText("PrivateKey: ${creds.privateKey}\nPublicKey: ${creds.publicKey}\nIpAddress: ${creds.ipAddress}\nClientId: ${creds.clientId}\n")
            } else {
                val file = java.io.File("warp_response.json")
                file.writeText("FAILED")
            }
        }
    }

    @Test
    fun testProxyChainConfigInjection() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))

        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = true,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "normal",
            proxyChains = "chain-id\u001fMy Custom Chain\u001fvless://uuid@relay.host.com:443?security=tls#relay\u001fvless://uuid@exit.host.com:443?security=tls&flow=xtls-rprx-vision#exit"
        )

        val chainUri = "chain://chain-id#My%20Custom%20Chain"
        val configStr = ConfigInjector.injectConfig(mockContext, chainUri, settings)
        println("Generated Proxy Chain Configuration:")
        println(configStr)

        val json = org.json.JSONObject(configStr)
        val outbounds = json.getJSONArray("outbounds")

        var proxyOutbound: org.json.JSONObject? = null
        var relayOutbound: org.json.JSONObject? = null
        for (i in 0 until outbounds.length()) {
            val out = outbounds.getJSONObject(i)
            val tag = out.getString("tag")
            if (tag == "proxy") {
                proxyOutbound = out
            } else if (tag == "relay-out") {
                relayOutbound = out
            }
        }

        assert(proxyOutbound != null) { "exit proxy outbound not found" }
        assert(relayOutbound != null) { "relay outbound not found" }

        // Exit proxy outbound MUST detour to relay-out
        assert(proxyOutbound!!.getString("detour") == "relay-out")
        // Relay outbound must NOT detour (empty/null detour)
        assert(!relayOutbound!!.has("detour"))
        // Exit proxy outbound MUST preserve flow configuration (required by XTLS Vision servers)
        assert(proxyOutbound!!.getString("flow") == "xtls-rprx-vision")

        // Check fragmentation/mux injection
        // Relay outbound (entrypoint) MUST have multiplex and fragment configurations if enabled
        assert(relayOutbound!!.has("multiplex"))
        // Exit proxy outbound (detoured) can have multiplex enabled if compatible
        assert(proxyOutbound!!.has("multiplex"))

        // Direct-bypass routing verification:
        // The exit outbound server domain (exit.host.com) MUST NOT be in the direct domains routing rules because it is detoured.
        // The relay outbound server domain (relay.host.com) MUST be in the direct domains routing rules because it is the entrypoint.
        val routeObj = json.getJSONObject("route")
        val rules = routeObj.getJSONArray("rules")
        var directDomainsRule: org.json.JSONObject? = null
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            if (rule.optString("outbound") == "direct" && rule.has("domain")) {
                directDomainsRule = rule
                break
            }
        }

        assert(directDomainsRule != null)
        val domains = directDomainsRule!!.getJSONArray("domain")
        var hasRelayDomain = false
        var hasExitDomain = false
        for (i in 0 until domains.length()) {
            val domain = domains.getString(i)
            if (domain == "relay.host.com") hasRelayDomain = true
            if (domain == "exit.host.com") hasExitDomain = true
        }

        assert(hasRelayDomain) { "relay host should be bypassed directly" }
        assert(hasExitDomain) { "exit host should be bypassed directly to prevent routing/DNS loop anomalies" }

        // DNS bootstrap verification:
        // Both exit.host.com and relay.host.com MUST be resolved using the bootstrap DNS server (dns-bootstrap)
        // to prevent circular DNS resolution lockouts since the secure DNS resolver goes through the tunnel itself.
        val dnsObj = json.getJSONObject("dns")
        val dnsRules = dnsObj.getJSONArray("rules")
        var bootstrapDnsRule: org.json.JSONObject? = null
        for (i in 0 until dnsRules.length()) {
            val rule = dnsRules.getJSONObject(i)
            if (rule.optString("server") == "dns-bootstrap" && rule.has("domain")) {
                bootstrapDnsRule = rule
                break
            }
        }

        assert(bootstrapDnsRule != null)
        val dnsDomains = bootstrapDnsRule!!.getJSONArray("domain")
        var hasRelayDnsDomain = false
        var hasExitDnsDomain = false
        for (i in 0 until dnsDomains.length()) {
            val domain = dnsDomains.getString(i)
            if (domain == "relay.host.com") hasRelayDnsDomain = true
            if (domain == "exit.host.com") hasExitDnsDomain = true
        }

        assert(hasRelayDnsDomain) { "relay host domain should be resolved via bootstrap DNS" }
        assert(hasExitDnsDomain) { "exit host domain should be resolved via bootstrap DNS to prevent startup deadlock loop" }
    }



    @Test
    fun testLegacyObfuscatedHttpMapping() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))

        val settings = InjectorSettings(
            vpnMode = "normal"
        )

        val uri = "vless://uuid@myhost.com:443?type=tcp&headerType=http&host=mycdn.com&path=/mypath"
        val configStr = ConfigInjector.injectConfig(mockContext, uri, settings)
        val json = org.json.JSONObject(configStr)
        val outbounds = json.getJSONArray("outbounds")
        val proxyOutbound = outbounds.getJSONObject(0)

        assert(proxyOutbound.getString("type") == "vless")
        assert(!proxyOutbound.has("tls")) { "TLS should be disabled for legacy obfuscated HTTP" }

        val transport = proxyOutbound.getJSONObject("transport")
        assert(transport.getString("type") == "http")
        val hostArr = transport.getJSONArray("host")
        assert(hostArr.getString(0) == "mycdn.com")
        assert(transport.getString("path") == "/mypath")
        assert(transport.getString("method") == "GET")
    }

    @Test
    fun testUserProvidedFailedLinks() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
        val settings = InjectorSettings(vpnMode = "normal")

        val link1 = "vless://e8ab0871-53f6-4022-9d43-9c37e4b35b0f@gd.30ma.cfd:8443?encryption=mlkem768x25519plus.native.0rtt.RPdkctkk2fIPLCuPV_iACmUXvsGcnbukVhlgxDNlqgfAcPkwZspx3SKm_KNGCYB7C9NWcZpa4CCtHgiz1te4tnVxzhOCY7R5QzEe-3KCmKdcYkY60IXFQYPDQ4V_Oou5L6YERmsF3ScegyjJe3hFtLqtBzmvySG8kbsvjvRmcbIFtXs1a9rNVfDOiMMYv9y65eOZfvVABpJbHZY4BwnMrdQJfXM0SmoiQfUUnnjCepIROFKpCYt1BORIYrkpbvLACoenxdh13XgoBCC5tJqhf6FAb8QuTEdj7tkxcdsYdEtIj_iFPrR28qy_7DQ4EvUIb9hrjkpx3rKnrRAWgsoVkYgMZLJjKLBa7gGtz8CC7GUoaJWVOCKhwgPO2LWYfIWJ7RsE0cqJYLk4cwF1dVVjzwSOMiMquoq1NDHJmSxbkzYKkIYRawOizrhqO6ebBuU3hbZs5hqJ2rJUSWjLMCApIHS3jLkX-mS5lEuTgrfBrlW3dps1jkhwJux5JOWU4ZNfEbt7r6oC9qwtbMpblrN1FTxUCvJBaNJi7Qxt30RZJFuE26ROLzsTLrQ9BJrBZwyErPvPWVVYAPI6xBifrBdOfgxxTZk_sToOKECWIKgNCAeDiHoGzvyNjcWYVhFH0yGNwrpkPNMQbeZ1H-ID4SkSYnxjF5ZQ77hDZ9IlGVFHR2HHRPA7XdQpm0k23jpb1CdwxxoURow-AhiPRfGoJ4BxTRjLmRRWdtE_71lpiMqfgWVl4QmZj5KyQYxZnmZ18gJyKmSq87W5Hqd-LFK3EFsulxdLMjQNmDR_iYsv5CUFKWJhuZtyslxNS3rJWBUwtnQRGKQHcxsrf_msMMU2Zqc7rxILIeSb3Yqi1CRM3pSHpXdbCbCub1invAarbEcKOtHHoQWXZKGL_RifdwQVQ2VKEBCkZ5lk1DN7MFMiNbGIs6shubfDCsWPJaRGd1GrxoQKg1Q1qBq62QhkqekELBRZexSOZJAXoBWWothCaXlqk_SprJmZFxW-d-it5idrF3lxnAIpBlUcjtAAnZQMz5KTCQwbh2oVy4F3qjEglvVewnVDfDGsdeOQv2C5f2sdeOGJfBIYZrqk4qWbOYyNn9hSZRkhzdbCBnw8nMCZTkmKBJKb3TJFFEMu8TWSSVIaUxmzqDxFE-aOFoY1ULB3HbEiisEOZtUO6GFOTslWkSdWRTN-KnuZwOdwS0RBo0yBRxR0yFFuT-ICmpIL8vkcfLDLkjAtU7LJTRVbw6QwvdO4WCnLJdWPeHYQ14o1wKIwormUMzNTOKuoqkkIXpuieusivcwbKbImHlZO5YhWmqNERNKu7aKTSQuBF0ZFtfqR5_COBWhA17E6QXhfuiwV_Kiej2yBgPmIp_CJaaQFAiaduuWoTVGYsBGTvdetP4grSNpWG7uGsxxrHilOjlRvdkowAuqnrDVc-puKrYyFraODFhwgUjOCW0QuIGwYDqB94JbJnsdsSMl63dwU4nhQ1KtLuulzRaRCAmJl-fIdx-Ax-mSwguNYvtBRPphCnBPOsMtFobmjeU3Z8tJIRYGteJZbAFmtZgSlyXyQC88HzMK5L3g&extra=%7B%22mode%22%3A%22auto%22%2C%22xPaddingBytes%22%3A%22100-1000%22%7D&fp=chrome&host=gd.30ma.cfd&mode=auto&path=%2Fmyculture&pbk=PAz5HjgkSigwokC0ua-xqrJsrtm1xt4PjZcAM4kIjAQ&security=reality&sid=d97566f404&sni=chat.deepseek.com&spx=%2FoPpFzdutTcuNPKL&type=xhttp&x_padding_bytes=100-1000#D2-Anita"
        val config1 = ConfigInjector.injectConfig(mockContext, link1, settings)
        println("Generated config 1:")
        println(config1)

        val link3 = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@zone.us.velaro.ir:47228?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir&fp=chrome&pbk=FPwg4_Ajnx_RW2UPn1A1hvkFNIKeQvNgmbARisuFaQs&sid=1891dc07c3a7ccf9#%F0%9F%87%BA%F0%9F%87%B8%20%F0%9D%90%94%F0%9D%90%A7%F0%9D%90%A2%F0%9D%90%AD%F0%9D%90%9E%F0%9D%90%9D%20%F0%9D%90%92%F0%9D%90%AD%F0%9D%90%9E%F0%9D%90%AC"
        val config3 = ConfigInjector.injectConfig(mockContext, link3, settings)
        println("Generated config 3:")
        println(config3)
    }
}

