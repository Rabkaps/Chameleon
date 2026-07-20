package com.hambalapps.chameleon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hambalapps.chameleon.theme.ChameleonTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.hambalapps.chameleon.vpn.VpnServiceWrapper
import com.hambalapps.chameleon.vpn.getHostAndPortFromLink
import com.hambalapps.chameleon.vpn.measurePingDelay
import com.hambalapps.chameleon.data.SettingsManager
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.io.File

class MainActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: android.content.Context) {
    val language = kotlinx.coroutines.runBlocking {
      com.hambalapps.chameleon.data.SettingsManager.getInstance(newBase).appLanguage.first()
    }
    val locale = when (language) {
      "en" -> java.util.Locale.ENGLISH
      "fa" -> java.util.Locale("fa")
      else -> java.util.Locale.getDefault()
    }
    java.util.Locale.setDefault(locale)
    val resources = newBase.resources
    val config = resources.configuration
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    val context = newBase.createConfigurationContext(config)
    super.attachBaseContext(context)
  }

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      VpnServiceWrapper.log("Notification permission granted")
    } else {
      VpnServiceWrapper.log("Notification permission denied")
    }
  }

  private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override fun onCreate(savedInstanceState: Bundle?) {
    DynamicColors.applyToActivityIfAvailable(this)
    super.onCreate(savedInstanceState)

    val action = intent?.action
    if (action == "com.hambalapps.chameleon.ACTION_SHORTCUT_CONNECT" || 
        action == "com.hambalapps.chameleon.ACTION_SHORTCUT_DISCONNECT") {
      if (android.net.VpnService.prepare(applicationContext) != null) {
        Toast.makeText(applicationContext, "Please open the app to grant VPN permission", Toast.LENGTH_LONG).show()
      } else {
        handleShortcutAction(action)
        finish()
        return
      }
    }

    registerShortcuts()

    // Uncaught exception handler to log JVM crashes
    val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val crashFile = File(cacheDir, "crash.log")
        crashFile.writeText("Crash in thread ${thread.name}:\n" + throwable.stackTraceToString())
      } catch (e: Exception) {
        // Ignore
      }
      originalHandler?.uncaughtException(thread, throwable)
    }

    // Check for previous crash log and load it on background thread
    lifecycleScope.launch(Dispatchers.IO) {
      try {
        VpnServiceWrapper.checkAndLoadCrashLog(applicationContext)
      } catch (e: Exception) {
        // Ignore
      }
    }

    // Request notification permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    enableEdgeToEdge()
    setContent {
      @OptIn(ExperimentalMaterial3ExpressiveApi::class)
      ChameleonTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val action = intent.action
    if (action == "com.hambalapps.chameleon.ACTION_SHORTCUT_CONNECT" || 
        action == "com.hambalapps.chameleon.ACTION_SHORTCUT_DISCONNECT") {
      if (android.net.VpnService.prepare(applicationContext) != null) {
        Toast.makeText(applicationContext, "Please open the app to grant VPN permission", Toast.LENGTH_LONG).show()
      } else {
        handleShortcutAction(action)
        finish()
      }
    }
  }

  private fun handleShortcutAction(action: String) {
    val context = applicationContext
    val settingsManager = SettingsManager(context)

    if (action == "com.hambalapps.chameleon.ACTION_SHORTCUT_CONNECT") {
      Toast.makeText(context, "Finding fastest node and connecting...", Toast.LENGTH_SHORT).show()
      activityScope.launch {
        val serverListStr = settingsManager.subscriptionServers.first()
        val manualStr = settingsManager.manualServers.first()
        val list = mutableListOf<String>()
        if (serverListStr.isNotEmpty()) {
          list.addAll(serverListStr.split("\u001e"))
        }
        if (manualStr.isNotEmpty()) {
          list.addAll(manualStr.split("\n").filter { it.trim().isNotEmpty() })
        }

        if (list.isEmpty()) {
          Toast.makeText(context, "No server nodes available to connect", Toast.LENGTH_LONG).show()
          return@launch
        }

        var bestLink: String? = null
        var bestLatency = Int.MAX_VALUE
        val delayUrl = settingsManager.settings.first().delayTestUrl

        kotlinx.coroutines.withContext(Dispatchers.IO) {
          list.forEach { link ->
            val hostPort = getHostAndPortFromLink(link)
            if (hostPort != null) {
              val latency = measurePingDelay(hostPort.first, hostPort.second)
              if (latency in 0 until bestLatency) {
                bestLatency = latency
                bestLink = link
              }
            }
          }
        }

        val target = bestLink ?: list.first()
        settingsManager.setActiveProfile(target)

        val subscriptions = settingsManager.settings.first().deserializedSubscriptions
        val parentSub = subscriptions.find { it.servers.split("\n").map { s -> s.trim() }.contains(target.trim()) }
        val newSubId = parentSub?.id ?: "manual"
        settingsManager.setActiveSubId(newSubId)

        val startIntent = Intent(context, VpnServiceWrapper::class.java).apply {
          this.action = VpnServiceWrapper.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(startIntent)
        } else {
          context.startService(startIntent)
        }
        Toast.makeText(context, "VPN connecting...", Toast.LENGTH_SHORT).show()
      }
    } else if (action == "com.hambalapps.chameleon.ACTION_SHORTCUT_DISCONNECT") {
      activityScope.launch {
        val stopIntent = Intent(context, VpnServiceWrapper::class.java).apply {
          this.action = VpnServiceWrapper.ACTION_STOP
          putExtra("force_stop", true)
        }
        context.startService(stopIntent)
        Toast.makeText(context, "VPN Disconnected", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun registerShortcuts() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return

      val connectIntent = Intent(applicationContext, MainActivity::class.java).apply {
        action = "com.hambalapps.chameleon.ACTION_SHORTCUT_CONNECT"
      }
      val connectShortcut = ShortcutInfo.Builder(applicationContext, "shortcut_connect")
        .setShortLabel("Connect Fastest")
        .setLongLabel("Connect to the fastest proxy node")
        .setIcon(Icon.createWithResource(applicationContext, R.drawable.ic_vpn_tile))
        .setIntent(connectIntent)
        .build()

      val disconnectIntent = Intent(applicationContext, MainActivity::class.java).apply {
        action = "com.hambalapps.chameleon.ACTION_SHORTCUT_DISCONNECT"
      }
      val disconnectShortcut = ShortcutInfo.Builder(applicationContext, "shortcut_disconnect")
        .setShortLabel("Disconnect")
        .setLongLabel("Disconnect VPN")
        .setIcon(Icon.createWithResource(applicationContext, R.drawable.ic_power))
        .setIntent(disconnectIntent)
        .build()

      try {
        shortcutManager.dynamicShortcuts = listOf(connectShortcut, disconnectShortcut)
      } catch (e: Exception) {
        // Ignore shortcut registry exceptions
      }
    }
  }
}
