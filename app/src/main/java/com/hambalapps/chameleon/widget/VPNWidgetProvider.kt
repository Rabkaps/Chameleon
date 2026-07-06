package com.hambalapps.chameleon.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.hambalapps.chameleon.R
import com.hambalapps.chameleon.data.SettingsManager
import com.hambalapps.chameleon.vpn.VpnServiceWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VPNWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.hambalapps.chameleon.widget.ACTION_TOGGLE"
        const val ACTION_STATE_CHANGED = "com.hambalapps.chameleon.widget.ACTION_STATE_CHANGED"
        const val ACTION_SET_MODE = "com.hambalapps.chameleon.widget.ACTION_SET_MODE"
        const val ACTION_SET_SERVER = "com.hambalapps.chameleon.widget.ACTION_SET_SERVER"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_SERVER_LINK = "extra_server_link"
        
        @Volatile
        private var lastVpnState = "DISCONNECTED"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAllWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        
        if (action == ACTION_TOGGLE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsManager = SettingsManager(context.applicationContext)
                    val activeProfile = settingsManager.activeProfile.first()
                    val showLiveNotification = settingsManager.showLiveNotification.first()
                    
                    val toggleIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                        this.action = if (lastVpnState == "CONNECTED") {
                            VpnServiceWrapper.ACTION_STOP
                        } else {
                            VpnServiceWrapper.ACTION_START
                        }
                        putExtra("active_profile", activeProfile)
                        putExtra("show_live_notification", showLiveNotification)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(toggleIntent)
                    } else {
                        context.startService(toggleIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Chameleon", "Widget failed to toggle VPN: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_STATE_CHANGED) {
            val state = intent.getStringExtra("state") ?: "DISCONNECTED"
            lastVpnState = state
            
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VPNWidgetProvider::class.java))
            updateAllWidgets(context, manager, ids)
        } else if (action == ACTION_SET_MODE) {
            val pendingResult = goAsync()
            val mode = intent.getStringExtra(EXTRA_MODE) ?: "standard"
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsManager = SettingsManager(context.applicationContext)
                    settingsManager.setVpnMode(mode)
                    if (lastVpnState == "CONNECTED") {
                        val showLiveNotification = settingsManager.showLiveNotification.first()
                        val activeProfile = settingsManager.activeProfile.first()
                        val startIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                            this.action = VpnServiceWrapper.ACTION_START
                            putExtra("active_profile", activeProfile)
                            putExtra("show_live_notification", showLiveNotification)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(startIntent)
                        } else {
                            context.startService(startIntent)
                        }
                    }
                    
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, VPNWidgetProvider::class.java))
                    updateAllWidgets(context, manager, ids)
                } catch (e: Exception) {
                    android.util.Log.e("Chameleon", "Widget failed to set mode: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_SET_SERVER) {
            val pendingResult = goAsync()
            val serverLink = intent.getStringExtra(EXTRA_SERVER_LINK)
            if (serverLink != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val settingsManager = SettingsManager(context.applicationContext)
                        settingsManager.setActiveSubId("manual")
                        settingsManager.setActiveProfile(serverLink)
                        
                        val showLiveNotification = settingsManager.showLiveNotification.first()
                        val startIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                            this.action = VpnServiceWrapper.ACTION_START
                            putExtra("active_profile", serverLink)
                            putExtra("show_live_notification", showLiveNotification)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(startIntent)
                        } else {
                            context.startService(startIntent)
                        }
                        
                        val manager = AppWidgetManager.getInstance(context)
                        val ids = manager.getAppWidgetIds(ComponentName(context, VPNWidgetProvider::class.java))
                        updateAllWidgets(context, manager, ids)
                    } catch (e: Exception) {
                        android.util.Log.e("Chameleon", "Widget failed to switch server: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun updateAllWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsManager = SettingsManager(context.applicationContext)
                val activeProfile = settingsManager.activeProfile.first()
                val currentVpnMode = settingsManager.vpnMode.first()
                val manualServersStr = settingsManager.manualServers.first()
                val manualList = manualServersStr.split("\n").filter { it.trim().isNotEmpty() }
                
                val stateText = when (lastVpnState) {
                    "CONNECTED" -> "SECURED"
                    "CONNECTING" -> "SHIELD ACTIVE..."
                    "DISCONNECTING" -> "DISCONNECTING..."
                    else -> "UNPROTECTED"
                }

                val colorPrimary = resolveCustomThemeColor(context, "colorPrimary", androidx.core.content.ContextCompat.getColor(context, R.color.widget_toggle_connected))
                val colorSecondary = resolveCustomThemeColor(context, "colorSecondary", androidx.core.content.ContextCompat.getColor(context, R.color.widget_toggle_connecting))
                val colorOutline = resolveCustomThemeColor(context, "colorOutline", androidx.core.content.ContextCompat.getColor(context, R.color.widget_toggle_disconnected))
                val textPrimary = resolveCustomThemeColor(context, "onSurface", androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_primary))
                val textSecondary = resolveCustomThemeColor(context, "onSurfaceVariant", androidx.core.content.ContextCompat.getColor(context, R.color.widget_text_secondary))
                val colorSurface = resolveCustomThemeColor(context, "colorSurface", androidx.core.content.ContextCompat.getColor(context, R.color.widget_background))
                val colorSurfaceVariant = resolveCustomThemeColor(context, "colorSurfaceVariant", colorSurface)

                val statusColor = when (lastVpnState) {
                    "CONNECTED" -> colorPrimary
                    "CONNECTING" -> colorSecondary
                    else -> colorOutline
                }

                // Drawables
                val bgDrawable = when (lastVpnState) {
                    "CONNECTED" -> R.drawable.widget_background_connected
                    "CONNECTING" -> R.drawable.widget_background_connecting
                    else -> R.drawable.widget_background
                }
                val bgPillDrawable = when (lastVpnState) {
                    "CONNECTED" -> R.drawable.widget_background_pill_connected
                    "CONNECTING" -> R.drawable.widget_background_pill_connecting
                    else -> R.drawable.widget_background_pill
                }

                val toggleBgDrawable = when (lastVpnState) {
                    "CONNECTED" -> R.drawable.widget_button_background_connected
                    "CONNECTING" -> R.drawable.widget_button_background_connecting
                    else -> R.drawable.widget_button_background
                }

                val iconTintColor = when (lastVpnState) {
                    "CONNECTED" -> android.graphics.Color.WHITE
                    "CONNECTING" -> android.graphics.Color.WHITE
                    else -> textSecondary
                }

                val nodeName = if (activeProfile.isNotEmpty()) {
                    extractServerName(activeProfile)
                } else {
                    "No active node selected"
                }

                val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                // Toggle Intent (Broadcast receiver)
                val toggleIntent = Intent(context, VPNWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE
                }
                val piToggle = PendingIntent.getBroadcast(context, 0, toggleIntent, flag)

                // App launch Intent (Open MainActivity)
                val mainIntent = Intent(context, com.hambalapps.chameleon.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val piMain = PendingIntent.getActivity(context, 1, mainIntent, flag)

                for (id in ids) {
                    // Create RemoteViews for three layout sizes
                    val viewsSmall = RemoteViews(context.packageName, R.layout.vpn_widget_small)
                    val viewsMedium = RemoteViews(context.packageName, R.layout.vpn_widget)
                    val viewsLarge = RemoteViews(context.packageName, R.layout.vpn_widget_large)

                    // 1. Bind viewsSmall
                    viewsSmall.setTextViewText(R.id.widget_status, stateText)
                    viewsSmall.setTextColor(R.id.widget_status, statusColor)
                    viewsSmall.setTextViewText(R.id.widget_node_name, if (lastVpnState == "CONNECTED") nodeName else "Tap to secure")
                    viewsSmall.setTextColor(R.id.widget_node_name, textSecondary)
                    viewsSmall.setInt(R.id.widget_container, "setBackgroundResource", bgPillDrawable)
                    setViewBackgroundTint(viewsSmall, R.id.widget_container, colorSurface)
                    
                    viewsSmall.setInt(R.id.widget_button_toggle, "setBackgroundResource", toggleBgDrawable)
                    val toggleBgColor = when (lastVpnState) {
                        "CONNECTED" -> colorPrimary
                        "CONNECTING" -> colorSecondary
                        else -> colorSurfaceVariant
                    }
                    setViewBackgroundTint(viewsSmall, R.id.widget_button_toggle, toggleBgColor)
                    viewsSmall.setInt(R.id.widget_button_icon, "setColorFilter", iconTintColor)
                    viewsSmall.setOnClickPendingIntent(R.id.widget_button_toggle, piToggle)
                    viewsSmall.setOnClickPendingIntent(R.id.widget_container, piMain)

                    // 2. Bind viewsMedium
                    viewsMedium.setTextViewText(R.id.widget_status, stateText)
                    viewsMedium.setTextColor(R.id.widget_status, statusColor)
                    viewsMedium.setTextViewText(R.id.widget_node_name, nodeName)
                    viewsMedium.setTextColor(R.id.widget_node_name, textSecondary)
                    viewsMedium.setInt(R.id.widget_container, "setBackgroundResource", bgDrawable)
                    setViewBackgroundTint(viewsMedium, R.id.widget_container, colorSurface)
                    setViewBackgroundTint(viewsMedium, R.id.widget_node_badge, colorSurfaceVariant)
                    
                    viewsMedium.setInt(R.id.widget_button_toggle, "setBackgroundResource", toggleBgDrawable)
                    setViewBackgroundTint(viewsMedium, R.id.widget_button_toggle, toggleBgColor)
                    viewsMedium.setInt(R.id.widget_button_toggle, "setColorFilter", iconTintColor)
                    viewsMedium.setOnClickPendingIntent(R.id.widget_button_toggle, piToggle)
                    viewsMedium.setOnClickPendingIntent(R.id.widget_container, piMain)

                    // 3. Bind viewsLarge
                    viewsLarge.setTextViewText(R.id.widget_status, stateText)
                    viewsLarge.setTextColor(R.id.widget_status, statusColor)
                    viewsLarge.setTextViewText(R.id.widget_node_name, nodeName)
                    viewsLarge.setTextColor(R.id.widget_node_name, textSecondary)
                    viewsLarge.setInt(R.id.widget_container, "setBackgroundResource", bgDrawable)
                    setViewBackgroundTint(viewsLarge, R.id.widget_container, colorSurface)
                    setViewBackgroundTint(viewsLarge, R.id.widget_node_badge, colorSurfaceVariant)
                    
                    viewsLarge.setInt(R.id.widget_button_toggle, "setBackgroundResource", toggleBgDrawable)
                    setViewBackgroundTint(viewsLarge, R.id.widget_button_toggle, toggleBgColor)
                    viewsLarge.setInt(R.id.widget_button_toggle, "setColorFilter", iconTintColor)
                    viewsLarge.setOnClickPendingIntent(R.id.widget_button_toggle, piToggle)
                    viewsLarge.setOnClickPendingIntent(R.id.widget_container, piMain)

                    // Bind mode buttons in viewsLarge
                    val modes = listOf(
                        Triple(R.id.btn_mode_standard, "standard", 10),
                        Triple(R.id.btn_mode_gaming, "gaming", 11),
                        Triple(R.id.btn_mode_ai, "ai_bypass", 12)
                    )
                    for ((btnId, mName, pCode) in modes) {
                        val mIntent = Intent(context, VPNWidgetProvider::class.java).apply {
                            action = ACTION_SET_MODE
                            putExtra(EXTRA_MODE, mName)
                        }
                        val piMode = PendingIntent.getBroadcast(context, pCode, mIntent, flag)
                        viewsLarge.setOnClickPendingIntent(btnId, piMode)

                        if (currentVpnMode == mName) {
                            viewsLarge.setInt(btnId, "setBackgroundResource", R.drawable.widget_button_background_connected)
                            setViewBackgroundTint(viewsLarge, btnId, colorPrimary)
                            viewsLarge.setTextColor(btnId, resolveCustomThemeColor(context, "onPrimary", android.graphics.Color.WHITE))
                        } else {
                            viewsLarge.setInt(btnId, "setBackgroundResource", R.drawable.widget_node_badge_background)
                            setViewBackgroundTint(viewsLarge, btnId, colorSurfaceVariant)
                            viewsLarge.setTextColor(btnId, textPrimary)
                        }
                    }

                    // Bind server slots in viewsLarge
                    val serverBtns = listOf(R.id.btn_server_1, R.id.btn_server_2, R.id.btn_server_3)
                    for (i in serverBtns.indices) {
                        val btnId = serverBtns[i]
                        if (i < manualList.size) {
                            val serverLink = manualList[i]
                            val sName = extractServerName(serverLink)
                            viewsLarge.setTextViewText(btnId, sName)
                            
                            val sIntent = Intent(context, VPNWidgetProvider::class.java).apply {
                                action = ACTION_SET_SERVER
                                putExtra(EXTRA_SERVER_LINK, serverLink)
                            }
                            val piServer = PendingIntent.getBroadcast(context, 100 + i, sIntent, flag)
                            viewsLarge.setOnClickPendingIntent(btnId, piServer)

                            if (activeProfile == serverLink) {
                                viewsLarge.setInt(btnId, "setBackgroundResource", R.drawable.widget_button_background_connected)
                                setViewBackgroundTint(viewsLarge, btnId, colorPrimary)
                                viewsLarge.setTextColor(btnId, resolveCustomThemeColor(context, "onPrimary", android.graphics.Color.WHITE))
                            } else {
                                viewsLarge.setInt(btnId, "setBackgroundResource", R.drawable.widget_node_badge_background)
                                setViewBackgroundTint(viewsLarge, btnId, colorSurfaceVariant)
                                viewsLarge.setTextColor(btnId, textPrimary)
                            }
                        } else {
                            viewsLarge.setTextViewText(btnId, "Empty")
                            viewsLarge.setInt(btnId, "setBackgroundResource", R.drawable.widget_node_badge_background)
                            setViewBackgroundTint(viewsLarge, btnId, colorSurfaceVariant)
                            viewsLarge.setTextColor(btnId, textSecondary)
                            viewsLarge.setOnClickPendingIntent(btnId, null)
                        }
                    }

                    // Options responsive layout selection
                    val options = manager.getAppWidgetOptions(id)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                    val selectedViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val viewMapping = mapOf(
                            android.util.SizeF(120f, 40f) to viewsSmall,
                            android.util.SizeF(120f, 90f) to viewsMedium,
                            android.util.SizeF(220f, 170f) to viewsLarge
                        )
                        RemoteViews(viewMapping)
                    } else {
                        if (minWidth >= 220 && minHeight >= 170) {
                            viewsLarge
                        } else if (minHeight < 90) {
                            viewsSmall
                        } else {
                            viewsMedium
                        }
                    }
                    manager.updateAppWidget(id, selectedViews)
                }
            } catch (e: Exception) {
                android.util.Log.e("Chameleon", "Widget update failed: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun extractServerName(link: String): String {
        val hashIdx = link.indexOf("#")
        return if (hashIdx >= 0) {
            try {
                java.net.URLDecoder.decode(link.substring(hashIdx + 1), "UTF-8")
            } catch (e: Exception) {
                link.substring(hashIdx + 1)
            }
        } else {
            val clean = link.substringAfter("://").substringBefore("@").substringBefore(":")
            if (clean.length > 10) clean.take(10) + "..." else clean
        }
    }

    private fun resolveThemeColor(context: Context, attrId: Int, fallbackColor: Int): Int {
        val sharedPrefs = context.getSharedPreferences("widget_theme_colors", Context.MODE_PRIVATE)
        val name = when (attrId) {
            android.R.attr.colorAccent -> "colorPrimary"
            else -> null
        }
        if (name != null && sharedPrefs.contains(name)) {
            return sharedPrefs.getInt(name, fallbackColor)
        }

        return try {
            val typedValue = android.util.TypedValue()
            if (context.theme.resolveAttribute(attrId, typedValue, true)) {
                typedValue.data
            } else {
                fallbackColor
            }
        } catch (e: Exception) {
            fallbackColor
        }
    }

    private fun resolveCustomThemeColor(context: Context, attrName: String, fallbackColor: Int): Int {
        val sharedPrefs = context.getSharedPreferences("widget_theme_colors", Context.MODE_PRIVATE)
        if (sharedPrefs.contains(attrName)) {
            return sharedPrefs.getInt(attrName, fallbackColor)
        }
        val attrId = context.resources.getIdentifier(attrName, "attr", context.packageName)
        if (attrId == 0) return fallbackColor
        return resolveThemeColor(context, attrId, fallbackColor)
    }

    private fun setViewBackgroundTint(views: RemoteViews, viewId: Int, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(viewId, "setBackgroundTintList", android.content.res.ColorStateList.valueOf(color))
        }
    }
}
