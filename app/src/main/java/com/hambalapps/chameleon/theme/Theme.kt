package com.hambalapps.chameleon.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.hambalapps.chameleon.Config
import com.hambalapps.chameleon.data.SettingsManager
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

object M3ExpressiveShape {
    val None = RoundedCornerShape(0.dp)
    val ExtraSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val LargeIncreased = RoundedCornerShape(20.dp)
    val ExtraLarge = RoundedCornerShape(28.dp)
    val ExtraLargeIncreased = RoundedCornerShape(32.dp)
    val ExtraExtraLarge = RoundedCornerShape(48.dp)
    val Full = RoundedCornerShape(9999.dp)

    // Connected button group shapes with clean end caps (no opposite diagonal sharpness)
    val ConnectedLeft = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)
    val ConnectedRight = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp)
    val ConnectedMiddle = RoundedCornerShape(4.dp)
}

object M3ExpressiveMotion {
    fun <T> spatialFast() = spring<T>(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> spatialDefault() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessLow
    )

    fun <T> spatialSlow() = spring<T>(
        dampingRatio = 0.70f,
        stiffness = Spring.StiffnessVeryLow
    )

    fun <T> effectsDefault() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}


// 2026 Material 3 Expressive Baseline Palettes
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    surfaceDim = Color(0xFF141218),
    surfaceBright = Color(0xFF3B383E),
    outline = DarkOutline,
    outlineVariant = Color(0xFF49454F),
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceDim = Color(0xFFDED8E1),
    surfaceBright = Color(0xFFFEF7FF),
    outline = LightOutline,
    outlineVariant = Color(0xFFCAC4D0),
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

// Cherry Blossom Palette (2026 M3 Expressive)
private val CherryLightColorScheme = lightColorScheme(
    primary = Color(0xFFBC004B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3F0015),
    secondary = Color(0xFF75565B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DF),
    onSecondaryContainer = Color(0xFF2C1519),
    tertiary = Color(0xFF795831),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB9),
    onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524345),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F2),
    surfaceContainer = Color(0xFFFAEAEC),
    surfaceContainerHigh = Color(0xFFF4E4E7),
    surfaceContainerHighest = Color(0xFFEFDFE1),
    surfaceDim = Color(0xFFE6D6D8),
    surfaceBright = Color(0xFFFFF8F8),
    outline = Color(0xFF847375),
    outlineVariant = Color(0xFFD6C2C4)
)

private val CherryDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2BF),
    onPrimary = Color(0xFF660024),
    primaryContainer = Color(0xFF8F0037),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFE4BDC3),
    onSecondary = Color(0xFF43292E),
    secondaryContainer = Color(0xFF5B3F44),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = Color(0xFFEABF8F),
    onTertiary = Color(0xFF442B07),
    tertiaryContainer = Color(0xFF5E411B),
    onTertiaryContainer = Color(0xFFFFDDB9),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFDFE1),
    surface = Color(0xFF191113),
    onSurface = Color(0xFFEFDFE1),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD6C2C4),
    surfaceContainerLowest = Color(0xFF140C0E),
    surfaceContainerLow = Color(0xFF22191B),
    surfaceContainer = Color(0xFF261D1F),
    surfaceContainerHigh = Color(0xFF312829),
    surfaceContainerHighest = Color(0xFF3C3234),
    surfaceDim = Color(0xFF191113),
    surfaceBright = Color(0xFF413738),
    outline = Color(0xFF9F8C8F),
    outlineVariant = Color(0xFF524345)
)

// Lavender Dreams Palette (2026 M3 Expressive)
private val LavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF5E4DB2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1B0063),
    secondary = Color(0xFF605B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E0F9),
    onSecondaryContainer = Color(0xFF1C182B),
    tertiary = Color(0xFF7C5263),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E6),
    onTertiaryContainer = Color(0xFF301120),
    background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFE6E0EC),
    onSurfaceVariant = Color(0xFF48454E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F2FA),
    surfaceContainer = Color(0xFFEEECF4),
    surfaceContainerHigh = Color(0xFFE8E6EE),
    surfaceContainerHighest = Color(0xFFE2E0E9),
    surfaceDim = Color(0xFFDCD9E1),
    surfaceBright = Color(0xFFFAF8FF),
    outline = Color(0xFF79757F),
    outlineVariant = Color(0xFFC9C4D0)
)

private val LavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCABEFF),
    onPrimary = Color(0xFF2F1783),
    primaryContainer = Color(0xFF463499),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFC9C3DC),
    onSecondary = Color(0xFF322E41),
    secondaryContainer = Color(0xFF484459),
    onSecondaryContainer = Color(0xFFE6E0F9),
    tertiary = Color(0xFFEDB8CC),
    onTertiary = Color(0xFF482535),
    tertiaryContainer = Color(0xFF613B4B),
    onTertiaryContainer = Color(0xFFFFD9E6),
    background = Color(0xFF141318),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF141318),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFC9C4D0),
    surfaceContainerLowest = Color(0xFF0F0E13),
    surfaceContainerLow = Color(0xFF1C1B20),
    surfaceContainer = Color(0xFF211F24),
    surfaceContainerHigh = Color(0xFF2B2A2F),
    surfaceContainerHighest = Color(0xFF36343A),
    surfaceDim = Color(0xFF141318),
    surfaceBright = Color(0xFF3B393F),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF48454E)
)

// Minimalist Rose Gold Palette (2026 M3 Expressive)
private val RoseGoldLightColorScheme = lightColorScheme(
    primary = Color(0xFF8F4C56),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DD),
    onPrimaryContainer = Color(0xFF3B0715),
    secondary = Color(0xFF75565B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DD),
    onSecondaryContainer = Color(0xFF2C1519),
    tertiary = Color(0xFF775830),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB6),
    onTertiaryContainer = Color(0xFF2A1700),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191A),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF22191A),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524345),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F1),
    surfaceContainer = Color(0xFFFAEAEC),
    surfaceContainerHigh = Color(0xFFF4E5E6),
    surfaceContainerHighest = Color(0xFFEEDFE1),
    surfaceDim = Color(0xFFE6D7D8),
    surfaceBright = Color(0xFFFFF8F8),
    outline = Color(0xFF847375),
    outlineVariant = Color(0xFFD6C2C4)
)

private val RoseGoldDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2BC),
    onPrimary = Color(0xFF561D29),
    primaryContainer = Color(0xFF72343F),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFFE4BDC2),
    onSecondary = Color(0xFF43292E),
    secondaryContainer = Color(0xFF5B3F44),
    onSecondaryContainer = Color(0xFFFFD9DD),
    tertiary = Color(0xFFE8BF8F),
    onTertiary = Color(0xFF432B06),
    tertiaryContainer = Color(0xFF5C411B),
    onTertiaryContainer = Color(0xFFFFDDB6),
    background = Color(0xFF1A1112),
    onBackground = Color(0xFFEEDFE1),
    surface = Color(0xFF1A1112),
    onSurface = Color(0xFFEEDFE1),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD6C2C4),
    surfaceContainerLowest = Color(0xFF140C0D),
    surfaceContainerLow = Color(0xFF22191B),
    surfaceContainer = Color(0xFF271D1F),
    surfaceContainerHigh = Color(0xFF322829),
    surfaceContainerHighest = Color(0xFF3D3234),
    surfaceDim = Color(0xFF1A1112),
    surfaceBright = Color(0xFF423738),
    outline = Color(0xFF9E8C8F),
    outlineVariant = Color(0xFF524345)
)

// Midnight Blue Palette (2026 M3 Expressive)
private val MidnightLightColorScheme = lightColorScheme(
    primary = Color(0xFF215FA6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E3FF),
    onPrimaryContainer = Color(0xFF001B3C),
    secondary = Color(0xFF555F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF121C2B),
    tertiary = Color(0xFF6E5676),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7D8FF),
    onTertiaryContainer = Color(0xFF271330),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainer = Color(0xFFECEDF4),
    surfaceContainerHigh = Color(0xFFE6E8EF),
    surfaceContainerHighest = Color(0xFFE1E2E9),
    surfaceDim = Color(0xFFD9DAE0),
    surfaceBright = Color(0xFFF8F9FF),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0)
)

private val MidnightDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF003061),
    primaryContainer = Color(0xFF004787),
    onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD9E3F8),
    tertiary = Color(0xFFDBBCE2),
    onTertiary = Color(0xFF3E2846),
    tertiaryContainer = Color(0xFF563E5D),
    onTertiaryContainer = Color(0xFFF7D8FF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E9),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
    surfaceDim = Color(0xFF111418),
    surfaceBright = Color(0xFF37393E),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF43474E)
)

// Forest Green Palette (2026 M3 Expressive)
private val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF216C37),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F5B1),
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Color(0xFF516351),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4E8D2),
    onSecondaryContainer = Color(0xFF0F1F11),
    tertiary = Color(0xFF39656D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBDEAF4),
    onTertiaryContainer = Color(0xFF001F24),
    background = Color(0xFFF7FBF2),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF7FBF2),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5EC),
    surfaceContainer = Color(0xFFEBEFE6),
    surfaceContainerHigh = Color(0xFFE6EAE1),
    surfaceContainerHighest = Color(0xFFE0E4DB),
    surfaceDim = Color(0xFFD8DCD3),
    surfaceBright = Color(0xFFF7FBF2),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BD)
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8CD897),
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF005322),
    onPrimaryContainer = Color(0xFFA7F5B1),
    secondary = Color(0xFFB8CCB6),
    onSecondary = Color(0xFF233425),
    secondaryContainer = Color(0xFF394B3A),
    onSecondaryContainer = Color(0xFFD4E8D2),
    tertiary = Color(0xFFA1CED7),
    onTertiary = Color(0xFF00363E),
    tertiaryContainer = Color(0xFF1F4D55),
    onTertiaryContainer = Color(0xFFBDEAF4),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DB),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DB),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
    surfaceContainerLowest = Color(0xFF0B100B),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF272C26),
    surfaceContainerHighest = Color(0xFF313630),
    surfaceDim = Color(0xFF101510),
    surfaceBright = Color(0xFF363B35),
    outline = Color(0xFF8C9388),
    outlineVariant = Color(0xFF424940)
)

// Sunset Orange Palette (2026 M3 Expressive)
private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFF944A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC4),
    onPrimaryContainer = Color(0xFF301400),
    secondary = Color(0xFF755846),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC7),
    onSecondaryContainer = Color(0xFF2B1709),
    tertiary = Color(0xFF626033),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9E5AC),
    onTertiaryContainer = Color(0xFF1E1C00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DFD3),
    onSurfaceVariant = Color(0xFF52443C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFAECE4),
    surfaceContainerHigh = Color(0xFFF5E6DE),
    surfaceContainerHighest = Color(0xFFEFE0D9),
    surfaceDim = Color(0xFFE7D8D0),
    surfaceBright = Color(0xFFFFF8F5),
    outline = Color(0xFF84746A),
    outlineVariant = Color(0xFFD7C3B8)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB782),
    onPrimary = Color(0xFF4F2500),
    primaryContainer = Color(0xFF713700),
    onPrimaryContainer = Color(0xFFFFDCC4),
    secondary = Color(0xFFE5BFA9),
    onSecondary = Color(0xFF422B1C),
    secondaryContainer = Color(0xFF5B4130),
    onSecondaryContainer = Color(0xFFFFDCC7),
    tertiary = Color(0xFFCDC992),
    onTertiary = Color(0xFF343209),
    tertiaryContainer = Color(0xFF4B481D),
    onTertiaryContainer = Color(0xFFE9E5AC),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFEFE0D9),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFEFE0D9),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C3B8),
    surfaceContainerLowest = Color(0xFF140D09),
    surfaceContainerLow = Color(0xFF221A15),
    surfaceContainer = Color(0xFF271E19),
    surfaceContainerHigh = Color(0xFF322823),
    surfaceContainerHighest = Color(0xFF3D332D),
    surfaceDim = Color(0xFF1A120D),
    surfaceBright = Color(0xFF433932),
    outline = Color(0xFF9F8D83),
    outlineVariant = Color(0xFF52443C)
)

// Ocean Teal Palette (2026 M3 Expressive)
private val TealLightColorScheme = lightColorScheme(
    primary = Color(0xFF006A67),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF70F7F1),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Color(0xFF4A6362),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E6),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4A607C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E4FF),
    onTertiaryContainer = Color(0xFF041C35),
    background = Color(0xFFF4FAF9),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4FAF9),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F4),
    surfaceContainer = Color(0xFFE9EFEF),
    surfaceContainerHigh = Color(0xFFE3EAE9),
    surfaceContainerHighest = Color(0xFFDDE4E3),
    surfaceDim = Color(0xFFD5DCDB),
    surfaceBright = Color(0xFFF4FAF9),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C7)
)

private val TealDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4EDAD5),
    onPrimary = Color(0xFF003735),
    primaryContainer = Color(0xFF00504E),
    onPrimaryContainer = Color(0xFF70F7F1),
    secondary = Color(0xFFB1CCCB),
    onSecondary = Color(0xFF1C3533),
    secondaryContainer = Color(0xFF324B4A),
    onSecondaryContainer = Color(0xFFCCE8E6),
    tertiary = Color(0xFFB2C8E8),
    onTertiary = Color(0xFF1C324B),
    tertiaryContainer = Color(0xFF334963),
    onTertiaryContainer = Color(0xFFD2E4FF),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    surfaceContainerLowest = Color(0xFF090F0F),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF303635),
    surfaceDim = Color(0xFF0E1514),
    surfaceBright = Color(0xFF343B3B),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4948)
)

// Royal Amethyst Palette (2026 M3 Expressive)
private val AmethystLightColorScheme = lightColorScheme(
    primary = Color(0xFF704A9E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEDBFF),
    onPrimaryContainer = Color(0xFF2B0053),
    secondary = Color(0xFF655A70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECDEF7),
    onSecondaryContainer = Color(0xFF20182A),
    tertiary = Color(0xFF805158),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9DE),
    onTertiaryContainer = Color(0xFF321017),
    background = Color(0xFFFFF7FD),
    onBackground = Color(0xFF1E1A22),
    surface = Color(0xFFFFF7FD),
    onSurface = Color(0xFF1E1A22),
    surfaceVariant = Color(0xFFE8E0EC),
    onSurfaceVariant = Color(0xFF4A454F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F1FB),
    surfaceContainer = Color(0xFFF3EBF6),
    surfaceContainerHigh = Color(0xFFEDE5F0),
    surfaceContainerHighest = Color(0xFFE8DFEA),
    surfaceDim = Color(0xFFDFD7E2),
    surfaceBright = Color(0xFFFFF7FD),
    outline = Color(0xFF7B7580),
    outlineVariant = Color(0xFFCBC4CF)
)

private val AmethystDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD9B9FF),
    onPrimary = Color(0xFF40176D),
    primaryContainer = Color(0xFF573185),
    onPrimaryContainer = Color(0xFFEEDBFF),
    secondary = Color(0xFFCFBEDB),
    onSecondary = Color(0xFF362D40),
    secondaryContainer = Color(0xFF4D4357),
    onSecondaryContainer = Color(0xFFECDEF7),
    tertiary = Color(0xFFF2B7BF),
    onTertiary = Color(0xFF4B252C),
    tertiaryContainer = Color(0xFF653A41),
    onTertiaryContainer = Color(0xFFFFD9DE),
    background = Color(0xFF161219),
    onBackground = Color(0xFFE8DFEA),
    surface = Color(0xFF161219),
    onSurface = Color(0xFFE8DFEA),
    surfaceVariant = Color(0xFF4A454F),
    onSurfaceVariant = Color(0xFFCBC4CF),
    surfaceContainerLowest = Color(0xFF100C14),
    surfaceContainerLow = Color(0xFF1E1A22),
    surfaceContainer = Color(0xFF221E26),
    surfaceContainerHigh = Color(0xFF2D2831),
    surfaceContainerHighest = Color(0xFF38333C),
    surfaceDim = Color(0xFF161219),
    surfaceBright = Color(0xFF3C3741),
    outline = Color(0xFF958E99),
    outlineVariant = Color(0xFF4A454F)
)

// Nordic Slate Palette (2026 M3 Expressive)
private val SlateLightColorScheme = lightColorScheme(
    primary = Color(0xFF48617B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE5FF),
    onPrimaryContainer = Color(0xFF001D34),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF251432),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4FB),
    surfaceContainer = Color(0xFFECEEF5),
    surfaceContainerHigh = Color(0xFFE6E8EF),
    surfaceContainerHighest = Color(0xFFE0E2EA),
    surfaceDim = Color(0xFFD9DBE2),
    surfaceBright = Color(0xFFF8F9FF),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF)
)

private val SlateDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAFC9E7),
    onPrimary = Color(0xFF17324B),
    primaryContainer = Color(0xFF304962),
    onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253141),
    secondaryContainer = Color(0xFF3C4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFD7BEE5),
    onTertiary = Color(0xFF3B2A48),
    tertiaryContainer = Color(0xFF534060),
    onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE0E2EA),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE0E2EA),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    surfaceContainerLowest = Color(0xFF0C0F12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2E),
    surfaceContainerHighest = Color(0xFF323539),
    surfaceDim = Color(0xFF111418),
    surfaceBright = Color(0xFF37393E),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChameleonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enabled by default for native Android Monet coloring
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    // Check settings theme preference (defaulting to cherry_blossom for Special, dynamic for Standard)
    val defaultThemeKey = if (Config.IS_SPECIAL) "cherry_blossom" else "dynamic"
    val specialTheme = settingsManager.specialTheme.collectAsState(initial = defaultThemeKey).value
    val themeMode = settingsManager.themeMode.collectAsState(initial = "system").value
    val amoledMode = settingsManager.amoledMode.collectAsState(initial = true).value

    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = remember(isDark, dynamicColor, specialTheme, amoledMode) {
        val useDynamic = (specialTheme == "dynamic" || dynamicColor) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val baseScheme = if (useDynamic) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            when (specialTheme) {
                "lavender_dreams" -> if (isDark) LavenderDarkColorScheme else LavenderLightColorScheme
                "rose_gold" -> if (isDark) RoseGoldDarkColorScheme else RoseGoldLightColorScheme
                "cherry_blossom" -> if (isDark) CherryDarkColorScheme else CherryLightColorScheme
                "midnight_blue" -> if (isDark) MidnightDarkColorScheme else MidnightLightColorScheme
                "forest_green" -> if (isDark) ForestDarkColorScheme else ForestLightColorScheme
                "sunset_orange" -> if (isDark) SunsetDarkColorScheme else SunsetLightColorScheme
                "ocean_teal" -> if (isDark) TealDarkColorScheme else TealLightColorScheme
                "royal_amethyst" -> if (isDark) AmethystDarkColorScheme else AmethystLightColorScheme
                "nordic_slate" -> if (isDark) SlateDarkColorScheme else SlateLightColorScheme
                else -> if (isDark) DarkColorScheme else LightColorScheme
            }
        }

        if (isDark && amoledMode) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black
            )
        } else {
            baseScheme
        }
    }

    val sharedPrefs = remember(context) { context.getSharedPreferences("widget_theme_colors", Context.MODE_PRIVATE) }
    androidx.compose.runtime.LaunchedEffect(colorScheme) {
        sharedPrefs.edit().apply {
            putInt("colorPrimary", colorScheme.primary.toArgb())
            putInt("colorSecondary", colorScheme.secondary.toArgb())
            putInt("colorOutline", colorScheme.outline.toArgb())
            putInt("colorSurface", colorScheme.surface.toArgb())
            putInt("colorSurfaceVariant", colorScheme.surfaceVariant.toArgb())
            putInt("colorPrimaryContainer", colorScheme.primaryContainer.toArgb())
            putInt("colorSecondaryContainer", colorScheme.secondaryContainer.toArgb())
            putInt("onPrimaryContainer", colorScheme.onPrimaryContainer.toArgb())
            putInt("onSecondaryContainer", colorScheme.onSecondaryContainer.toArgb())
            putInt("onSurfaceVariant", colorScheme.onSurfaceVariant.toArgb())
            putInt("onSurface", colorScheme.onSurface.toArgb())
            apply()
        }
        val widgetIntent = android.content.Intent(context, com.hambalapps.chameleon.widget.VPNWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, com.hambalapps.chameleon.widget.VPNWidgetProvider::class.java)
            )
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(widgetIntent)
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = remember { MotionScheme.expressive() },
        content = content
    )
}
