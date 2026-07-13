package com.hambalapps.chameleon.vpn

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
        assert(warpOutbound == null) { "warp-out outbound should not be present in outbounds array" }

        val endpoints = json.getJSONArray("endpoints")
        var warpEndpoint: org.json.JSONObject? = null
        for (i in 0 until endpoints.length()) {
            val ep = endpoints.getJSONObject(i)
            if (ep.getString("tag") == "warp-endpoint") {
                warpEndpoint = ep
                break
            }
        }
        assert(warpEndpoint != null) { "warp-endpoint endpoint not found in endpoints" }
        val endpoint = warpEndpoint!!
        assert(endpoint.getString("type") == "wireguard")
        assert(endpoint.getBoolean("system") == false)
        assert(endpoint.getString("private_key") == "privatekeybase64")
        assert(endpoint.getString("detour") == "direct")
        
        val addressArr = endpoint.getJSONArray("address")
        assert(addressArr.length() == 2)
        
        val peers = endpoint.getJSONArray("peers")
        assert(peers.length() == 1)
        val peer = peers.getJSONObject(0)
        assert(peer.getString("address") == "engage.cloudflareclient.com")
        assert(peer.getString("public_key") == "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=")
        
        val experimental = json.getJSONObject("experimental")
        val cacheFile = experimental.getJSONObject("cache_file")
        assert(cacheFile.getBoolean("enabled") == true)
        assert(cacheFile.getBoolean("store_warp_config") == true)
    }

    @Test
    fun testWarpRegistrationResponse() {
        runBlocking {
            val file = java.io.File("warp_response.json")
            file.writeText("PrivateKey: fake_private_key\nPublicKey: fake_public_key\nIpAddress: 127.0.0.1/32\nClientId: fake_client_id\n")
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
        val directDomains = mutableSetOf<String>()
        val directIps = mutableSetOf<String>()
        for (i in 0 until rules.length()) {
            val rule = rules.getJSONObject(i)
            if (rule.optString("outbound") == "direct") {
                if (rule.has("domain")) {
                    val arr = rule.getJSONArray("domain")
                    for (j in 0 until arr.length()) {
                        directDomains.add(arr.getString(j))
                    }
                }
                if (rule.has("ip_cidr")) {
                    val arr = rule.getJSONArray("ip_cidr")
                    for (j in 0 until arr.length()) {
                        directIps.add(arr.getString(j))
                    }
                }
            }
        }

        val exitServer = proxyOutbound!!.getString("server")
        val hasExitBypass = directDomains.contains("exit.host.com") || directIps.contains(exitServer)
        assert(hasExitBypass) { "exit host or IP should be bypassed directly to prevent routing/DNS loop anomalies" }

        val relayServer = relayOutbound!!.getString("server")
        val hasRelayBypass = directDomains.contains("relay.host.com") || directIps.contains(relayServer)
        assert(hasRelayBypass) { "relay host or IP should be bypassed directly" }

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

        val needsRelayDnsBootstrap = relayOutbound!!.getString("server") == "relay.host.com"
        if (needsRelayDnsBootstrap) {
            assert(hasRelayDnsDomain) { "relay host domain should be resolved via bootstrap DNS" }
        }
        assert(hasExitDnsDomain) { "exit host domain should be resolved via bootstrap DNS to prevent startup deadlock loop" }
    }



    @Test
    fun testLegacyObfuscatedHttpMapping() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))

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

        val link1 = "vless://e8ab0871-53f6-4022-9d43-9c37e4b35b0f@gd.30ma.cfd:8443?encryption=mlkem768x25519plus.native.0rtt.RPdkctkk2fIPLCuPV_iACmUXvsGcnbukVhlgxDNlqgfAcPkwZspx3SKm_KNGCYB7C9NWcZpa4CCtHgiz1te4tnVxzhOCY7R5QzEe-3KCmKdcYkY60IXFQYPDQ4V_Oou5L6YERmsF3ScegyjJe3hFtLqtBzmvySG8kbsvjvRmcbIFtXs1a9rNVfDOiMMYv9y65eOZfvVABpJbHZY4BwnMrdQJfXM0SmoiQfUUnnjCepIROFKpCYt1BORIYrkpbvLACoenxdh13XgoBCC5tJqhf6FAb8QuTEdj7tkxcdsYdEtIj_iFPrR28qy_7DQ4EvUIb9hrjkpx3rKnrRAWgsoVkYgMZLJjKLBa7gGtz8CC7GUoaJWVOCKhwgPO2LWYfIWJ7RsE0cqJYLk4cwF1dVVjzwSOMiMquoq1NDHJmSxbkzYKkIYRawOizrhqO6ebBuU3hbZs5hqJ2rJUSWjLMCApIHS3jLkX-mS5lEuTgrfBrlW3dps1jkhwJux5JOWU4ZNfEbt7r6oC9qwtbMpblrN1FTxUCvJBaNJi7Qxt30RZJFuE26ROLzsTLrQ9BJrBZwyErPvPWVVYAPI6xBifrBdOfgxxTZk_sToOKECWIKgNCAeDiHoGzvyNjcWYVhFH0yGNwrpkPNMQbeZ1H-ID4SkSYnxjF5ZQ77hDZ9IlGVFHR2HHRPA7XdQpm0k23jpb1CdwxxoURow-AhiPRfGoJ4BxTRjLmRRWdtE_71lpiMqfgWVl4QmZj5KyQYxZnmZ18gJyKmSq87W5Hqd-LFK3EFsulxdLMjQNmDR_iYsv5CUFKWJhuZtyslxNS3rJWBUwtnQRGKQHcxsrf_msMMU2Zqc7rxILIeSb3Yqi1CRM3pSHpXdbCbCub1invAarbEcKOtHHoQWXZKGL_RifdwQVQ2VKEBCkZ5lk1DN7MFMiNbGIs6shubfDCsWPJaRGd1GrxoQKg1Q1qBq62QhkqekELBRZexSOZJAXoBWWothCaXlqk_SprJmZFxW-d-it5idrF3lxnAIpBlUcjtAAnZQMz5KTCQwbh2oVy4F3qjEglvVewnVDfDGsdeOQv2C5f2sdeOGJfBIYZrqk4qWbOYyNn9hSZRkhzdbCBnw8nMCZTkmKBJKb3TJFFEMu8TWSSVIaUxmzqDxFE-aOFoY1ULB3HbEiisEOZtUO6GFOTslWkSdWRTN-KnuZwOdwS0RBo0yBRxR0yFFuT-ICmpIL8vkcfLDLkjAtU7LJTRVbw6QwvdO4WCnLJdWPeHYQ14o1wKIwormUMzNTOKuoqkkIXpuieusivcwbKbImHlZO5YhWmqNERNKu7aKTSQuBF0ZFtfqR5_COBWhA17E6QXhfuiwV_Kiej2yBgPmIp_CJaaQFAiaduuWoTVGYsBGTvdetP4grSNpWG7uGsxxrHilOjlRvdkowAuqnrDVc-puKrYyFraODFhwgUjOCW0QuIGwYDqB94JbJnsdsSMl63dwU4nhQ1KtLuulzRaRCAmJl-fIdx-Ax-mSwguNYvtBRPphCnBPOsMtFobmjeU3Z8tJIRYGteJZbAFmtZgSlyXyQC88HzMK5L3g&extra=%7B%22mode%22%3A%22auto%22%2C%22xPaddingBytes%22%3A%22100-1000%22%7D&fp=chrome&host=gd.30ma.cfd&mode=auto&path=%2Fmyculture&pbk=PAz5HjgkSigwokC0ua-xqrJsrtm1xt4PjZcAM4kIjAQ&security=reality&sid=d97566f404&sni=chat.deepseek.com&spx=%2FoPpFzdutTcuNPKL&type=xhttp&x_padding_bytes=100-1000#D2-Anita"
        val config1 = ConfigInjector.injectConfig(mockContext, link1, settings)
        println("Generated config 1:")
        println(config1)

        val link3 = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@zone.us.velaro.ir:47228?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir&fp=chrome&pbk=FPwg4_Ajnx_RW2UPn1A1hvkFNIKeQvNgmbARisuFaQs&sid=1891dc07c3a7ccf9#%F0%9F%87%BA%F0%9F%87%B8%20%F0%9D%90%94%F0%9D%90%A7%F0%9D%90%A2%F0%9D%90%AD%F0%9D%90%9E%F0%9D%90%9D%20%F0%9D%90%92%F0%9D%90%AD%F0%9D%90%9E%F0%9D%90%AC"
        val config3 = ConfigInjector.injectConfig(mockContext, link3, settings)
        println("Generated config 3:")
        println(config3)
    }

    @Test
    fun testAmneziaWGParsing() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        val awgUri = "awg://my_private_key_base64@my.server.com:51820?public_key=peer_public_key_base64&address=10.0.0.2/32,fd00::2/128&mtu=1360&jc=4&jmin=40&jmax=80&s1=123&s2=456&h1=789"
        val configStr = ConfigInjector.injectConfig(mockContext, awgUri, settings)
        println("Generated AmneziaWG Configuration:")
        println(configStr)

        val json = org.json.JSONObject(configStr)
        val outbounds = json.getJSONArray("outbounds")
        // The first outbound in the cleanOutbounds list should now be the default "direct" outbound, since "proxy" was migrated to endpoints.
        val firstOutbound = outbounds.getJSONObject(0)
        assert(firstOutbound.getString("tag") == "direct")
        assert(firstOutbound.getString("type") == "direct")

        val endpoints = json.getJSONArray("endpoints")
        assert(endpoints.length() == 1)
        val endpoint = endpoints.getJSONObject(0)
        assert(endpoint.getString("type") == "wireguard")
        assert(endpoint.getBoolean("system") == false)
        assert(endpoint.getString("tag") == "proxy")
        assert(endpoint.getString("private_key") == "my_private_key_base64")
        assert(endpoint.getInt("mtu") == 1360)

        val localAddresses = endpoint.getJSONArray("address")
        assert(localAddresses.length() == 2)
        assert(localAddresses.getString(0) == "10.0.0.2/32")
        assert(localAddresses.getString(1) == "fd00::2/128")

        val peers = endpoint.getJSONArray("peers")
        assert(peers.length() == 1)
        val peer = peers.getJSONObject(0)
        val peerAddress = peer.getString("address")
        assert(peerAddress == "my.server.com" || peerAddress.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")))
        assert(peer.getInt("port") == 51820)
        assert(peer.getString("public_key") == "peer_public_key_base64")

        val amnezia = endpoint.getJSONObject("amnezia")
        assert(amnezia.getInt("jc") == 4)
        assert(amnezia.getInt("jmin") == 40)
        assert(amnezia.getInt("jmax") == 80)
        assert(amnezia.getInt("s1") == 123)
        assert(amnezia.getInt("s2") == 456)
        assert(amnezia.getInt("h1") == 789)
    }

    @Test
    fun testNewProtocolsParsing() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        // 1. Mieru parsing test
        val mieruUri = "mieru://myuser:mypass@my.mieru.com:20000?ports=10000-20000,25000&transport=udp&multiplexing=multiplexing&traffic_pattern=heavy"
        val configMieru = ConfigInjector.injectConfig(mockContext, mieruUri, settings)
        println("Generated Mieru Config: " + configMieru)
        val jsonMieru = org.json.JSONObject(configMieru)
        val outboundMieru = jsonMieru.getJSONArray("outbounds").getJSONObject(0)
        assert(outboundMieru.getString("type") == "mieru")
        val serverMieru = outboundMieru.getString("server")
        assert(serverMieru == "my.mieru.com" || serverMieru.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")))
        assert(outboundMieru.getInt("server_port") == 20000)
        assert(outboundMieru.getString("username") == "myuser")
        assert(outboundMieru.getString("password") == "mypass")
        assert(outboundMieru.getString("transport") == "udp")
        assert(outboundMieru.getString("multiplexing") == "multiplexing")
        assert(outboundMieru.getString("traffic_pattern") == "heavy")
        val ports = outboundMieru.getJSONArray("server_ports")
        assert(ports.length() == 2)
        assert(ports.getString(0) == "10000-20000")
        assert(ports.getString(1) == "25000")

        // 2. ShadowsocksR parsing test
        // We'll base64 encode: server:port:protocol:method:obfs:password_base64/?obfsparam=...
        // Let's use a standard base64 test case
        val ssrUri = "ssr://MTI3LjAuMC4xOjgzODg6YXV0aF9hZXMxMjhfbWQ1OmFlcy0yNTYtY2ZiOnRsczEuMl90aWNrZXRfYXV0aDpZV056TVEvP29iZnNwYXJhbT1kR2hwY3k1amIyMD0mcHJvdG9wYXJhbT1kR2hwY3k1amIyMD0"
        val configSsr = ConfigInjector.injectConfig(mockContext, ssrUri, settings)
        val jsonSsr = org.json.JSONObject(configSsr)
        val outboundSsr = jsonSsr.getJSONArray("outbounds").getJSONObject(0)
        assert(outboundSsr.getString("type") == "shadowsocksr")
        assert(outboundSsr.getString("server") == "127.0.0.1")
        assert(outboundSsr.getInt("server_port") == 8388)
        assert(outboundSsr.getString("protocol") == "auth_aes128_md5")
        assert(outboundSsr.getString("method") == "aes-256-cfb")
        assert(outboundSsr.getString("obfs") == "tls1.2_ticket_auth")
        assert(outboundSsr.getString("password") == "acs1")

        // 3. ShadowTLS parsing test
        val shadowTlsUri = "shadowtls://mypassword@my.shadowtls.com:443?version=3&sni=microsoft.com"
        val configStls = ConfigInjector.injectConfig(mockContext, shadowTlsUri, settings)
        val jsonStls = org.json.JSONObject(configStls)
        val outboundStls = jsonStls.getJSONArray("outbounds").getJSONObject(0)
        assert(outboundStls.getString("type") == "shadowtls")
        val serverStls = outboundStls.getString("server")
        assert(serverStls == "my.shadowtls.com" || serverStls.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")))
        assert(outboundStls.getInt("server_port") == 443)
        assert(outboundStls.getString("password") == "mypassword")
        assert(outboundStls.getInt("version") == 3)
        val tlsStls = outboundStls.getJSONObject("tls")
        assert(tlsStls.getBoolean("enabled"))
        assert(tlsStls.getString("server_name") == "microsoft.com")

        // 4. Snell parsing test
        val snellUri = "snell://mypassword@my.snell.com:443?version=2&obfs=http&obfs-host=speedtest.net"
        val configSnell = ConfigInjector.injectConfig(mockContext, snellUri, settings)
        val jsonSnell = org.json.JSONObject(configSnell)
        val outboundSnell = jsonSnell.getJSONArray("outbounds").getJSONObject(0)
        assert(outboundSnell.getString("type") == "snell")
        val serverSnell = outboundSnell.getString("server")
        assert(serverSnell == "my.snell.com" || serverSnell.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")))
        assert(outboundSnell.getInt("server_port") == 443)
        assert(outboundSnell.getString("password") == "mypassword")
        assert(outboundSnell.getInt("version") == 2)
        val obfsSnell = outboundSnell.getJSONObject("obfs")
        assert(obfsSnell.getString("type") == "http")
        assert(obfsSnell.getString("host") == "speedtest.net")
    }

    @Test
    fun testMTProxyInboundServer() {
        // Test direct helper method
        assert(ConfigInjector.normalizeMtProxySecret("dd000102030405060708090a0b0c0d0e0f") == "ee000102030405060708090a0b0c0d0e0f7370656564746573742e6e6574")
        assert(ConfigInjector.normalizeMtProxySecret("000102030405060708090a0b0c0d0e0f") == "ee000102030405060708090a0b0c0d0e0f7370656564746573742e6e6574")
        assert(ConfigInjector.normalizeMtProxySecret("ee000102030405060708090a0b0c0d0e0f676f6f676c652e636f6d") == "ee000102030405060708090a0b0c0d0e0f676f6f676c652e636f6d")

        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "normal",
            enableMtProxy = true,
            mtProxyPort = "20202",
            mtProxySecret = "dd000102030405060708090a0b0c0d0e0f"
        )

        val link = "vless://uuid@exit.host.com:443?security=tls"
        val configStr = ConfigInjector.injectConfig(mockContext, link, settings)
        val json = org.json.JSONObject(configStr)
        val inbounds = json.getJSONArray("inbounds")
        
        var hasMtProxyInbound = false
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.getJSONObject(i)
            if (inbound.optString("tag") == "mtproxy-in") {
                hasMtProxyInbound = true
                assert(inbound.getString("type") == "mtproxy")
                assert(inbound.getString("listen") == "0.0.0.0")
                assert(inbound.getInt("listen_port") == 20202)
                val user = inbound.getJSONArray("users").getJSONObject(0)
                assert(user.getString("secret") == "ee000102030405060708090a0b0c0d0e0f7370656564746573742e6e6574")
            }
        }
        assert(hasMtProxyInbound) { "MTProxy inbound was not injected successfully" }
    }

    @Test
    fun testXHttpPaddingSanitization() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        // 1. Check URI parsing
        val xhttpUri = "vless://uuid@my.server.com:443?type=xhttp&path=/xhttp&security=tls"
        val configStr = ConfigInjector.injectConfig(mockContext, xhttpUri, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        val transport = outbound.getJSONObject("transport")
        assert(transport.getString("type") == "xhttp")
        assert(transport.getString("x_padding_bytes") == "100-1000")

        // 2. Check JSON profile sanitization
        val rawJson = """
            {
                "outbounds": [
                    {
                        "type": "vless",
                        "tag": "proxy",
                        "server": "my.server.com",
                        "server_port": 443,
                        "uuid": "uuid",
                        "transport": {
                            "type": "xhttp",
                            "path": "/xhttp"
                        }
                    }
                ]
            }
        """.trimIndent()
        val sanitizedStr = ConfigInjector.injectConfig(mockContext, rawJson, settings)
        val jsonSanitized = org.json.JSONObject(sanitizedStr)
        val outboundSanitized = jsonSanitized.getJSONArray("outbounds").getJSONObject(0)
        val transportSanitized = outboundSanitized.getJSONObject("transport")
        assert(transportSanitized.getString("type") == "xhttp")
        assert(transportSanitized.getString("x_padding_bytes") == "100-1000")
    }

    @Test
    fun testTcpHttpObfsWithTls() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        val tcpHttpObfsUri = "vless://uuid@my.server.com:443?type=tcp&headerType=http&security=tls"
        val configStr = ConfigInjector.injectConfig(mockContext, tcpHttpObfsUri, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        
        // Should map to transport type "http" (HTTP obfuscation)
        val transport = outbound.getJSONObject("transport")
        assert(transport.getString("type") == "http")
        
        // Should also enable TLS
        val tls = outbound.getJSONObject("tls")
        assert(tls.getBoolean("enabled"))
    }

    @Test
    fun testVlessSecurityNone() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        // Explicit security=none on port 443 must NOT enable TLS auto-detection
        val link = "vless://uuid@my.server.com:443?security=none"
        val configStr = ConfigInjector.injectConfig(mockContext, link, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assert(!outbound.has("tls"))
    }

    @Test
    fun testVlessPqEncryption() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        val link = "vless://uuid@my.server.com:443?security=tls&encryption=mlkem768x25519plus"
        val configStr = ConfigInjector.injectConfig(mockContext, link, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assert(outbound.getString("encryption") == "mlkem768x25519plus")
    }

    @Test
    fun testRealityIgnoresHttpObfs() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        // Reality connection with type=tcp and headerType=http should IGNORE http obfuscation (no transport block)
        val link = "vless://463e7702-e5e0-4ab4-a84c-392f4927ce77@5.160.77.201:47228?encryption=none&security=reality&type=tcp&headerType=http&path=%2Fassets&host=telewebion.ir&sni=telewebion.ir"
        val configStr = ConfigInjector.injectConfig(mockContext, link, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assert(!outbound.has("transport"))
    }

    @Test
    fun testLocalProxyOnlyMode() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "normal",
            localProxyOnly = true
        )

        val link = "vless://uuid@my.server.com:443?security=tls"
        val configStr = ConfigInjector.injectConfig(mockContext, link, settings)
        val json = org.json.JSONObject(configStr)
        val inbounds = json.getJSONArray("inbounds")
        
        // Should NOT have the tun-in inbound
        var hasTunInbound = false
        for (i in 0 until inbounds.length()) {
            val inbound = inbounds.getJSONObject(i)
            if (inbound.optString("tag") == "tun-in") {
                hasTunInbound = true
            }
        }
        assert(!hasTunInbound)
    }

    @Test
    fun testOpenVpnCertAndCredentialsParsing() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        val rawOvpn = """
            client
            dev tun
            proto tcp-client
            remote my-ovpn-server.com 1194
        """.trimIndent()

        var threw = false
        try {
            ConfigInjector.injectConfig(mockContext, rawOvpn, settings)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assert(threw)
    }

    @Test
    fun testAntiZapretConfigInjection() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
        
        val rawOvpn = """
            client
            dev tun
            proto tcp
            remote v.31337.lol
        """.trimIndent()
        
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = false,
            bypassLan = true,
            vpnMode = "normal"
        )
        
        var threw = false
        try {
            ConfigInjector.injectConfig(mockContext, rawOvpn, settings)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assert(threw)
    }

    @Test
    fun testRawWireGuardConfParsing() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        val rawConf = """
            [Interface]
            PrivateKey = my_private_key
            Address = 10.0.0.2/32, fd00::2/128
            MTU = 1420
            Jc = 4
            Jmin = 40
            Jmax = 70
            S1 = 15
            S2 = 25
            H1 = 12345
            H2 = 67890
            H3 = 13579
            H4 = 24680
            itime = 9999

            [Peer]
            PublicKey = peer_public_key
            PresharedKey = preshared_key
            AllowedIPs = 0.0.0.0/0
            Endpoint = 192.168.1.100:51820
            PersistentKeepalive = 25
            Reserved = 1, 2, 3
        """.trimIndent()

        val configStr = ConfigInjector.injectConfig(mockContext, rawConf, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assert(outbound.getString("tag") == "direct")
        assert(outbound.getString("type") == "direct")
        
        val endpoints = json.getJSONArray("endpoints")
        assert(endpoints.length() == 1)
        val endpoint = endpoints.getJSONObject(0)
        assert(endpoint.getString("type") == "wireguard")
        assert(endpoint.getBoolean("system") == false)
        assert(endpoint.getString("tag") == "proxy")
        assert(endpoint.getString("private_key") == "my_private_key")
        assert(endpoint.getInt("mtu") == 1420)
        
        val localAddress = endpoint.getJSONArray("address")
        assert(localAddress.getString(0) == "10.0.0.2/32")
        assert(localAddress.getString(1) == "fd00::2/128")
        
        val peer = endpoint.getJSONArray("peers").getJSONObject(0)
        val peerAddress = peer.getString("address")
        assert(peerAddress == "192.168.1.100" || peerAddress.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")))
        assert(peer.getInt("port") == 51820)
        assert(peer.getString("public_key") == "peer_public_key")
        assert(peer.getString("pre_shared_key") == "preshared_key")
        assert(peer.getInt("persistent_keepalive_interval") == 25)
        
        assert(!peer.has("reserved"))

        val amnezia = endpoint.getJSONObject("amnezia")
        assert(amnezia.getInt("jc") == 4)
        assert(amnezia.getInt("jmin") == 40)
        assert(amnezia.getInt("jmax") == 70)
        assert(amnezia.getInt("s1") == 15)
        assert(amnezia.getInt("s2") == 25)
        assert(amnezia.getLong("h1") == 12345L)
        assert(amnezia.getLong("h2") == 67890L)
        assert(amnezia.getLong("h3") == 13579L)
        assert(amnezia.getLong("h4") == 24680L)
        assert(amnezia.getLong("itime") == 9999L)
    }

    @Test
    fun testBase64ConfigUriParsing() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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

        // 1. Test OpenVPN base64 config URI
        val rawOvpn = "client\ndev tun\nproto udp\nremote 192.168.1.1 1194"
        val ovpnB64 = java.util.Base64.getEncoder().encodeToString(rawOvpn.toByteArray())
        val ovpnUri = "openvpn://vpn?config=$ovpnB64#TestOpenVPN"

        var threwOpenVpn = false
        try {
            ConfigInjector.injectConfig(mockContext, ovpnUri, settings)
        } catch (e: IllegalArgumentException) {
            threwOpenVpn = true
        }
        assert(threwOpenVpn)

        // 2. Test AmneziaWG base64 config URI
        val rawWg = "[Interface]\nPrivateKey = my_private_key\nAddress = 10.0.0.2/32\n\n[Peer]\nPublicKey = peer_public_key\nEndpoint = 192.168.1.100:51820"
        val wgB64 = java.util.Base64.getEncoder().encodeToString(rawWg.toByteArray())
        val wgUri = "awg://vpn?config=$wgB64#TestAmneziaWG"

        val configStr2 = ConfigInjector.injectConfig(mockContext, wgUri, settings)
        val json2 = org.json.JSONObject(configStr2)
        val outbound2 = json2.getJSONArray("outbounds").getJSONObject(0)
        assert(outbound2.getString("tag") == "direct")
        assert(outbound2.getString("type") == "direct")
        
        val endpoints2 = json2.getJSONArray("endpoints")
        assert(endpoints2.length() == 1)
        val endpoint2 = endpoints2.getJSONObject(0)
        assert(endpoint2.getString("type") == "wireguard")
        assert(endpoint2.getBoolean("system") == false)
        assert(endpoint2.getString("tag") == "proxy")
        assert(endpoint2.getString("private_key") == "my_private_key")
    }

    @Test
    fun testVmessXhttpConfigParsing() {
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
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
        
        val vmessJsonStr = """
        {
          "add": "gd.30ma.cfd",
          "host": "gd.30ma.cfd",
          "id": "e8ab0871-53f6-4022-9d43-9c37e4b35b0f",
          "mode": "auto",
          "net": "xhttp",
          "path": "/web3",
          "port": 2095,
          "ps": "D-Anita",
          "scy": "auto",
          "tls": "none",
          "type": "none",
          "v": "2",
          "xPaddingBytes": "100-1000"
        }
        """.trimIndent()
        val vmessB64 = java.util.Base64.getEncoder().encodeToString(vmessJsonStr.toByteArray())
        val vmessUri = "vmess://$vmessB64#TestVmessXhttp"
        
        val configStr = ConfigInjector.injectConfig(mockContext, vmessUri, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        
        assert(outbound.getString("type") == "vmess")
        val serverStr = outbound.getString("server")
        assert(serverStr == "gd.30ma.cfd" || serverStr.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")))
        assert(outbound.getInt("server_port") == 2095)
        assert(outbound.getString("uuid") == "e8ab0871-53f6-4022-9d43-9c37e4b35b0f")
        
        val transport = outbound.getJSONObject("transport")
        assert(transport.getString("type") == "xhttp")
        assert(transport.getString("path") == "/web3")
        assert(transport.getString("host") == "gd.30ma.cfd")
        assert(transport.getString("mode") == "auto")
        assert(transport.getString("x_padding_bytes") == "100-1000")
    }

    @Test
    fun testCamouflageAndCdnScanner() {
        // 1. Verify getCleanIp returns pinnedIp if not empty
        val pinned = "8.8.8.8"
        val ip = CdnIpScanner.getCleanIp(preset = "cloudflare", pinnedIp = pinned)
        assert(ip == pinned)

        // 2. Verify performScan execution
        val scanRes = kotlinx.coroutines.runBlocking {
            CdnIpScanner.performScan(preset = "custom", customIps = listOf("127.0.0.1"), timeoutMs = 100)
        }
        println("Scan count for localhost custom check: " + scanRes.workingIpsCount)

        // 3. Verify ConfigInjector applies camouflage pinned IP
        val mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "/tmp"))
        val settings = InjectorSettings(
            bypassIran = true,
            secureDns = "1.1.1.1",
            tunStack = "system",
            enableFragment = false,
            fragmentLength = "10-20",
            fragmentInterval = "10-20",
            enableMux = true,
            bypassLan = true,
            vpnMode = "normal",
            globalCamouflageEnabled = true,
            globalCamouflagePreset = "cloudflare",
            globalCamouflageSni = "speedtest.net",
            globalCamouflageHost = "speedtest.net",
            globalCamouflagePinnedIp = "1.2.3.4"
        )
        val vlessUri = "vless://myuuid@my.server.com:443?encryption=none#TestVLESS"
        val configStr = ConfigInjector.injectConfig(mockContext, vlessUri, settings)
        val json = org.json.JSONObject(configStr)
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assert(outbound.getString("server") == "1.2.3.4")
        val tls = outbound.getJSONObject("tls")
        assert(tls.getBoolean("enabled"))
        assert(tls.getString("server_name") == "speedtest.net")
    }
}

