package com.hambalapps.chameleon.ui.main

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

object IpCountryResolver {
    private const val CACHE_FILE = "ip_countries_cache.json"
    private val cache = ConcurrentHashMap<String, String>()
    private var cacheFile: File? = null

    fun init(context: Context) {
        cacheFile = File(context.cacheDir, CACHE_FILE)
        loadCache()
    }

    private fun loadCache() {
        val file = cacheFile ?: return
        if (file.exists()) {
            try {
                val jsonStr = file.readText()
                val json = JSONObject(jsonStr)
                json.keys().forEach { key ->
                    cache[key] = json.getString(key)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveCache() {
        val file = cacheFile ?: return
        try {
            val json = JSONObject()
            cache.forEach { (k, v) ->
                json.put(k, v)
            }
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedCountryCode(host: String): String? {
        return cache[host]
    }

    private fun isPrivateIp(ip: String): Boolean {
        if (ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")) return true
        if (ip.startsWith("172.")) {
            val parts = ip.split(".")
            if (parts.size >= 2) {
                val secondOctet = parts[1].toIntOrNull()
                if (secondOctet != null && secondOctet in 16..31) {
                    return true
                }
            }
        }
        return false
    }

    fun resolveCountryCode(host: String): String {
        val trimmed = host.trim()
        if (trimmed.isEmpty() || trimmed.equals("localhost", ignoreCase = true) || isPrivateIp(trimmed)) {
            return "🌐"
        }

        val cached = cache[trimmed]
        if (cached != null) {
            return cached
        }

        // Try to resolve host domain to IP address
        val ipToResolve = try {
            val address = InetAddress.getByName(trimmed)
            address.hostAddress ?: trimmed
        } catch (e: Exception) {
            trimmed
        }

        // Check if the resolved IP is local/private
        if (isPrivateIp(ipToResolve)) {
            return "🌐"
        }

        var cc = fetchCountryCodeFromFreeIpApi(ipToResolve)
        if (cc.isEmpty() || cc == "🌐") {
            cc = fetchCountryCodeFromCountryIs(ipToResolve)
        }
        if (cc.isEmpty() || cc == "🌐") {
            cc = fetchCountryCodeFromIpWhoIs(ipToResolve)
        }

        if (cc.isNotEmpty() && cc != "🌐") {
            cache[trimmed] = cc
            saveCache()
            return cc
        }
        return "🌐"
    }

    private fun fetchCountryCodeFromFreeIpApi(ip: String): String {
        return try {
            val url = URL("https://freeipapi.com/api/json/$ip")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            json.optString("countryCode", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun fetchCountryCodeFromCountryIs(ip: String): String {
        return try {
            val url = URL("https://api.country.is/$ip")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            json.optString("country", "")
        } catch (e: Exception) {
            ""
        }
    }

    private fun fetchCountryCodeFromIpWhoIs(ip: String): String {
        return try {
            val url = URL("https://ipwho.is/$ip")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            if (json.optBoolean("success", false)) {
                json.optString("country_code", "")
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}

fun getFlagEmojiFromCountryCode(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val code = countryCode.uppercase()
    val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

fun getFlagEmoji(serverName: String, countryCode: String? = null): String {
    if (!countryCode.isNullOrEmpty() && countryCode != "🌐" && countryCode != "??") {
        return getFlagEmojiFromCountryCode(countryCode)
    }
    val name = serverName.lowercase()
    
    // Helper to check if a country code token is present in name (e.g. "de", "us", "ir")
    fun hasCountryToken(code: String): Boolean {
        val regex = Regex("(^|[^a-z])$code([^a-z]|$)", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(name)
    }

    return when {
        name.contains("germany") || name.contains("frankfurt") || hasCountryToken("de") -> "🇩🇪"
        name.contains("spain") || name.contains("madrid") || name.contains("barcelona") || hasCountryToken("es") -> "🇪🇸"
        name.contains("japan") || name.contains("tokyo") || name.contains("osaka") || hasCountryToken("jp") -> "🇯🇵"
        name.contains("united states") || name.contains("new%20york") || name.contains("new york") || name.contains("chicago") || name.contains("los angeles") || hasCountryToken("us") -> "🇺🇸"
        name.contains("united kingdom") || name.contains("london") || hasCountryToken("uk") || hasCountryToken("gb") -> "🇬🇧"
        name.contains("france") || name.contains("paris") || hasCountryToken("fr") -> "🇫🇷"
        name.contains("netherlands") || name.contains("amsterdam") || hasCountryToken("nl") -> "🇳🇱"
        name.contains("singapore") || hasCountryToken("sg") -> "🇸🇬"
        name.contains("turkey") || name.contains("istanbul") || hasCountryToken("tr") -> "🇹🇷"
        name.contains("canada") || name.contains("toronto") || hasCountryToken("ca") -> "🇨🇦"
        name.contains("iran") || name.contains("tehran") || hasCountryToken("ir") -> "🇮🇷"
        name.contains("finland") || name.contains("helsinki") || hasCountryToken("fi") -> "🇫🇮"
        name.contains("sweden") || name.contains("stockholm") || hasCountryToken("se") -> "🇸🇪"
        name.contains("italy") || name.contains("milan") || name.contains("rome") || hasCountryToken("it") -> "🇮🇹"
        name.contains("switzerland") || name.contains("zurich") || hasCountryToken("ch") -> "🇨🇭"
        name.contains("uae") || name.contains("dubai") || hasCountryToken("ae") -> "🇦🇪"
        name.contains("hong kong") || name.contains("hk") -> "🇭🇰"
        name.contains("korea") || name.contains("seoul") || hasCountryToken("kr") -> "🇰🇷"
        else -> "🌐"
    }
}
