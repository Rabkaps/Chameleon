package com.hambalapps.expressivebox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hambalapps.expressivebox.theme.ExpressiveBoxTheme
import com.hambalapps.expressivebox.vpn.VpnServiceWrapper
import com.hambalapps.expressivebox.data.SettingsManager
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      VpnServiceWrapper.log("Notification permission granted")
    } else {
      VpnServiceWrapper.log("Notification permission denied")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    DynamicColors.applyToActivityIfAvailable(this)
    super.onCreate(savedInstanceState)

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

    // Check for previous crash log and load it
    try {
      VpnServiceWrapper.checkAndLoadCrashLog(applicationContext)
    } catch (e: Exception) {
      // Ignore
    }

    // Request notification permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    // Set default VLESS profile if none is active
    val settingsManager = SettingsManager(applicationContext)
    lifecycleScope.launch {
      if (settingsManager.activeProfile.first().isEmpty()) {
        settingsManager.setActiveProfile("vless://20e30989-998e-4922-8e6e-30dc64728f1c@panel.hambal.space:443?security=reality&sni=samsung.com&fp=chrome&pbk=ulFOuEZuZpjmNh-AxLLHU1A0_tWpyVqegNDzU0Z7jU0&sid=68&type=xhttp&path=%2Fswdownload&host=www.samsung.com&mode=stream-one#Game-rgaerrgergr")
      }
    }

    enableEdgeToEdge()
    setContent {
      ExpressiveBoxTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}

