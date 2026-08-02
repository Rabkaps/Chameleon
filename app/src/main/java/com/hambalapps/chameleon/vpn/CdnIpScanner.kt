package com.hambalapps.chameleon.vpn

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class ScannedIp(
    val ip: String,
    val latencyMs: Long
)

data class ScanResult(
    val workingIpsCount: Int,
    val fastestIp: String?,
    val fastestLatencyMs: Long,
    val workingIps: List<ScannedIp> = emptyList()
)

object CdnIpScanner {
    private val cleanIpCache = ConcurrentHashMap<String, Pair<String, Long>>() // preset -> Pair(IP, timestamp)
    private val CACHE_DURATION_MS = 15 * 60 * 1000L // 15 minutes cache
    
    // Thread-safe store for the latest scan results per preset
    val lastScanResults = ConcurrentHashMap<String, List<ScannedIp>>()
    // Thread-safe store for active clean IP pools (top 3-5 IPs)
    val activeCleanIpPools = ConcurrentHashMap<String, List<String>>()

    val PRESET_SUBNETS = mapOf(
        "Cloudflare 104.16" to "104.16.0.0/13",
        "Cloudflare 104.18" to "104.18.0.0/13",
        "Cloudflare Anycast" to "172.64.0.0/13",
        "Cloudflare WARP/DoH" to "162.159.0.0/16",
        "GCore CDN" to "92.223.0.0/16",
        "Amazon CloudFront" to "13.32.0.0/15"
    )

    val CLOUDFLARE_IPS = listOf(
        "104.16.85.20", "104.16.86.20", "104.17.2.20", "104.18.26.240", "104.19.241.100",
        "172.67.2.20", "172.67.73.1", "172.67.180.12", "162.159.192.1", "162.159.193.1",
        "104.21.3.1", "104.21.3.2", "104.22.3.1", "104.22.3.2", "172.67.74.152", "104.20.10.10"
    )

    val CLOUDFRONT_IPS = listOf(
        "13.32.0.1", "13.33.0.1", "13.35.0.1", "13.224.0.1", "13.225.0.1",
        "18.64.0.1", "18.65.0.1", "99.84.0.1", "99.86.0.1", "54.230.0.1"
    )

    fun getCleanIp(
        preset: String,
        customIps: List<String> = emptyList(),
        port: Int = 443,
        timeoutMs: Int = 600,
        pinnedIp: String = ""
    ): String? {
        if (pinnedIp.isNotEmpty()) {
            android.util.Log.i("Chameleon", "Using pinned clean IP for $preset: $pinnedIp")
            return pinnedIp
        }

        val cached = cleanIpCache[preset]
        if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_DURATION_MS) {
            android.util.Log.i("Chameleon", "Using cached clean IP for $preset: ${cached.first}")
            return cached.first
        }

        // Use Dispatchers.IO to ensure the blocking scan runs on IO threads,
        // not on whatever thread called this (which could be Main → ANR)
        val cleanIp = runBlocking(Dispatchers.IO) {
            val res = performScan(preset, customIps, port, timeoutMs)
            res.fastestIp
        }
        return cleanIp
    }

    /**
     * Generates randomized IP candidates across given CIDR notations or IP strings.
     */
    fun generateSampleIpsFromCidrs(cidrs: List<String>, count: Int = 50): List<String> {
        val result = mutableSetOf<String>()
        val parsedRanges = mutableListOf<Pair<Long, Long>>()

        for (cidrRaw in cidrs) {
            val cidr = cidrRaw.trim()
            if (cidr.isEmpty()) continue
            if (!cidr.contains("/")) {
                // Direct IP string
                result.add(cidr)
                continue
            }
            try {
                val parts = cidr.split("/")
                val ipLong = ipToLong(parts[0])
                val prefix = parts[1].toInt()
                val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
                val startIp = (ipLong and mask) + 1
                val endIp = (startIp or mask.inv()) - 1
                if (startIp < endIp) {
                    parsedRanges.add(Pair(startIp, endIp))
                }
            } catch (e: Exception) {
                // Invalid CIDR string ignored
            }
        }

        if (parsedRanges.isEmpty()) {
            return result.toList()
        }

        var attempts = 0
        val maxAttempts = count * 5
        while (result.size < count && attempts < maxAttempts) {
            attempts++
            val range = parsedRanges[Random.nextInt(parsedRanges.size)]
            val randomIpLong = Random.nextLong(range.first, range.second + 1)
            result.add(longToIp(randomIpLong))
        }

        return result.toList()
    }

    private fun ipToLong(ipStr: String): Long {
        val parts = ipStr.split(".")
        var result = 0L
        for (i in 0..3) {
            result = result or (parts[i].toLong() shl (24 - i * 8))
        }
        return result
    }

    private fun longToIp(longIp: Long): String {
        return "${(longIp shr 24) and 0xFF}.${(longIp shr 16) and 0xFF}.${(longIp shr 8) and 0xFF}.${longIp and 0xFF}"
    }

    suspend fun performScan(
        preset: String,
        customIps: List<String> = emptyList(),
        port: Int = 443,
        timeoutMs: Int = 600,
        maxConcurrency: Int = 32
    ): ScanResult {
        val targetIps = if (preset == "custom" || customIps.isNotEmpty()) {
            customIps.filter { it.trim().isNotEmpty() }
        } else {
            when (preset) {
                "cloudflare" -> CLOUDFLARE_IPS
                "cloudfront" -> CLOUDFRONT_IPS
                else -> emptyList()
            }
        }

        if (targetIps.isEmpty()) {
            return ScanResult(0, null, -1L, emptyList())
        }

        return executeProbes(preset, targetIps, port, timeoutMs, maxConcurrency)
    }

    /**
     * Power-User CIDR Subnet Scanner with custom IP sampling density.
     */
    suspend fun performCidrScan(
        cidrs: List<String>,
        sampleCount: Int = 50,
        port: Int = 443,
        timeoutMs: Int = 600,
        maxConcurrency: Int = 32
    ): ScanResult {
        val sampledIps = generateSampleIpsFromCidrs(cidrs, sampleCount)
        if (sampledIps.isEmpty()) {
            return ScanResult(0, null, -1L, emptyList())
        }

        return executeProbes("cidr_scan", sampledIps, port, timeoutMs, maxConcurrency)
    }

    private suspend fun executeProbes(
        preset: String,
        targetIps: List<String>,
        port: Int,
        timeoutMs: Int,
        maxConcurrency: Int
    ): ScanResult = withContext(Dispatchers.IO) {
        val jobs = targetIps.map { ip ->
            async {
                var result: Pair<String, Long>? = null
                val startTime = System.currentTimeMillis()
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip.trim(), port), timeoutMs)
                    }
                    val latency = System.currentTimeMillis() - startTime
                    result = Pair(ip.trim(), latency)
                } catch (e: Exception) {
                    // ignore
                }
                result
            }
        }
        val results = jobs.awaitAll().filterNotNull().sortedBy { it.second }
        val workingCount = results.size
        val best = results.firstOrNull()
        val scannedList = results.map { ScannedIp(it.first, it.second) }
        val topCleanPool = scannedList.take(5).map { it.ip }

        if (best != null) {
            cleanIpCache[preset] = Pair(best.first, System.currentTimeMillis())
            lastScanResults[preset] = scannedList
            activeCleanIpPools[preset] = topCleanPool
            ScanResult(workingCount, best.first, best.second, scannedList)
        } else {
            lastScanResults[preset] = emptyList()
            activeCleanIpPools[preset] = emptyList()
            ScanResult(workingCount, null, -1L, emptyList())
        }
    }
}
