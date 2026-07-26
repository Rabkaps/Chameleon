package com.hambalapps.chameleon.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.hambalapps.chameleon.desktop.data.SettingsManager

// Material 3 Color Palettes Ported from Android App

private val CherryLightColorScheme = lightColorScheme(
    primary = Color(0xFFD03A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3F0015),
    secondary = Color(0xFF7D5260),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFFFFF8F9),
    surface = Color(0xFFFFF8F9),
    onBackground = Color(0xFF25191C),
    onSurface = Color(0xFF25191C),
    outline = Color(0xFF857376)
)

private val CherryDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB0C8),
    onPrimary = Color(0xFF5F0024),
    primaryContainer = Color(0xFF80083B),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE8B9C7),
    onSecondary = Color(0xFF462631),
    secondaryContainer = Color(0xFF5E3C49),
    background = Color(0xFF1F1A1B),
    surface = Color(0xFF1F1A1B),
    onBackground = Color(0xFFEAE0E1),
    onSurface = Color(0xFFEAE0E1),
    outline = Color(0xFF9F8C90)
)

private val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF624FBE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1C0062),
    secondary = Color(0xFF605B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E0F9),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    outline = Color(0xFF7B758F)
)

private val LavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    onPrimary = Color(0xFF321190),
    primaryContainer = Color(0xFF4A35A5),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFC9C4DC),
    onSecondary = Color(0xFF322E41),
    secondaryContainer = Color(0xFF484459),
    background = Color(0xFF141318),
    surface = Color(0xFF141318),
    onBackground = Color(0xFFE6E1E6),
    onSurface = Color(0xFFE6E1E6),
    outline = Color(0xFF938F9F)
)

private val RoseGoldLightColorScheme = lightColorScheme(
    primary = Color(0xFF944B56),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DD),
    onPrimaryContainer = Color(0xFF3C0715),
    secondary = Color(0xFF775357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DC),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191A),
    onSurface = Color(0xFF22191A),
    outline = Color(0xFF857375)
)

private val RoseGoldDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2BB),
    onPrimary = Color(0xFF5A1D29),
    primaryContainer = Color(0xFF77343F),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFFE5BCC0),
    onSecondary = Color(0xFF44292C),
    secondaryContainer = Color(0xFF5D3F42),
    background = Color(0xFF201A1B),
    surface = Color(0xFF201A1B),
    onBackground = Color(0xFFECE0E1),
    onSurface = Color(0xFFECE0E1),
    outline = Color(0xFF9F8C8E)
)

private val MidnightLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B365D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2EC),
    onPrimaryContainer = Color(0xFF0A1D37),
    secondary = Color(0xFF486581),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0F4F8),
    background = Color(0xFFF0F4F8),
    surface = Color(0xFFF0F4F8),
    onBackground = Color(0xFF102A43),
    onSurface = Color(0xFF102A43),
    outline = Color(0xFF627D98)
)

private val MidnightDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9DB2C6),
    onPrimary = Color(0xFF0A1D37),
    primaryContainer = Color(0xFF1B365D),
    onPrimaryContainer = Color(0xFFD9E2EC),
    secondary = Color(0xFF627D98),
    onSecondary = Color(0xFF102A43),
    secondaryContainer = Color(0xFF243B53),
    background = Color(0xFF0F1E36),
    surface = Color(0xFF0F1E36),
    onBackground = Color(0xFFF0F4F8),
    onSurface = Color(0xFFF0F4F8),
    outline = Color(0xFF486581)
)

private val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF2E6F40),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4EDDA),
    onPrimaryContainer = Color(0xFF0A3013),
    secondary = Color(0xFF5A7361),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EBE4),
    background = Color(0xFFF4FAF6),
    surface = Color(0xFFF4FAF6),
    onBackground = Color(0xFF112215),
    onSurface = Color(0xFF112215),
    outline = Color(0xFF708577)
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90D5A1),
    onPrimary = Color(0xFF0B3A1C),
    primaryContainer = Color(0xFF2E6F40),
    onPrimaryContainer = Color(0xFFD4EDDA),
    secondary = Color(0xFFB1CBB7),
    onSecondary = Color(0xFF1C3423),
    secondaryContainer = Color(0xFF3A4E40),
    background = Color(0xFF111E15),
    surface = Color(0xFF111E15),
    onBackground = Color(0xFFE2EBE4),
    onSurface = Color(0xFFE2EBE4),
    outline = Color(0xFF5A7361)
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFFD35400),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFADBD8),
    onPrimaryContainer = Color(0xFF4A1504),
    secondary = Color(0xFF9E5A42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9EBEA),
    background = Color(0xFFFDF6F0),
    surface = Color(0xFFFDF6F0),
    onBackground = Color(0xFF2C130B),
    onSurface = Color(0xFF2C130B),
    outline = Color(0xFFB57C68)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFAB91),
    onPrimary = Color(0xFF5D2710),
    primaryContainer = Color(0xFFD35400),
    onPrimaryContainer = Color(0xFFFADBD8),
    secondary = Color(0xFFD7CCC8),
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFF5D4037),
    background = Color(0xFF211511),
    surface = Color(0xFF211511),
    onBackground = Color(0xFFF5EEEE),
    onSurface = Color(0xFFF5EEEE),
    outline = Color(0xFF8D6E63)
)

private val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF007A78),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC6ECEB),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6362),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7),
    background = Color(0xFFF2FAF9),
    surface = Color(0xFFF2FAF9),
    onBackground = Color(0xFF051F1F),
    onSurface = Color(0xFF051F1F),
    outline = Color(0xFF6F8483)
)

private val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF00504E),
    onPrimaryContainer = Color(0xFFC6ECEB),
    secondary = Color(0xFFB2DFDB),
    onSecondary = Color(0xFF1E3533),
    secondaryContainer = Color(0xFF334B49),
    background = Color(0xFF0E1A1A),
    surface = Color(0xFF0E1A1A),
    onBackground = Color(0xFFE0F2F1),
    onSurface = Color(0xFFE0F2F1),
    outline = Color(0xFF4A6362)
)

private val AmethystLightColorScheme = lightColorScheme(
    primary = Color(0xFF6F35A5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1E6FF),
    onPrimaryContainer = Color(0xFF2D005D),
    secondary = Color(0xFF665A73),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE6F2),
    background = Color(0xFFFAF8FD),
    surface = Color(0xFFFAF8FD),
    onBackground = Color(0xFF1C1A22),
    onSurface = Color(0xFF1C1A22),
    outline = Color(0xFF81758F)
)

private val AmethystDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD7BDE2),
    onPrimary = Color(0xFF4A157D),
    primaryContainer = Color(0xFF6F35A5),
    onPrimaryContainer = Color(0xFFF1E6FF),
    secondary = Color(0xFFD2C4D9),
    onSecondary = Color(0xFF382E43),
    secondaryContainer = Color(0xFF4F435A),
    background = Color(0xFF16121D),
    surface = Color(0xFF16121D),
    onBackground = Color(0xFFEDE8F2),
    onSurface = Color(0xFFEDE8F2),
    outline = Color(0xFF665A73)
)

private val SlateLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F6272),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E2EC),
    onPrimaryContainer = Color(0xFF0F1E2A),
    secondary = Color(0xFF5B6975),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5EDF4),
    background = Color(0xFFF2F6F9),
    surface = Color(0xFFF2F6F9),
    onBackground = Color(0xFF172026),
    onSurface = Color(0xFF172026),
    outline = Color(0xFF75828D)
)

private val SlateDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B8C8),
    onPrimary = Color(0xFF1C2D3C),
    primaryContainer = Color(0xFF384755),
    onPrimaryContainer = Color(0xFFD7E2EC),
    secondary = Color(0xFFBAC4CE),
    onSecondary = Color(0xFF24303B),
    secondaryContainer = Color(0xFF3B4854),
    background = Color(0xFF14191E),
    surface = Color(0xFF14191E),
    onBackground = Color(0xFFEBF0F5),
    onSurface = Color(0xFFEBF0F5),
    outline = Color(0xFF5B6975)
)

@Composable
fun ExpressiveDesktopTheme(
    settingsManager: SettingsManager,
    content: @Composable () -> Unit
) {
    val settings by settingsManager.settings.collectAsState()

    val isDark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (settings.specialTheme) {
        "lavender_dreams" -> if (isDark) LavenderDarkColorScheme else LavenderLightColorScheme
        "rose_gold" -> if (isDark) RoseGoldDarkColorScheme else RoseGoldLightColorScheme
        "cherry_blossom" -> if (isDark) CherryDarkColorScheme else CherryLightColorScheme
        "midnight_blue" -> if (isDark) MidnightDarkColorScheme else MidnightLightColorScheme
        "forest_green" -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
        "sunset_orange" -> if (isDark) SunsetDarkColorScheme else SunsetLightColorScheme
        "ocean_teal" -> if (isDark) TealDarkColorScheme else TealLightColorScheme
        "royal_amethyst" -> if (isDark) AmethystDarkColorScheme else AmethystLightColorScheme
        "nordic_slate" -> if (isDark) SlateDarkColorScheme else SlateLightColorScheme
        else -> if (isDark) LavenderDarkColorScheme else LavenderLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
