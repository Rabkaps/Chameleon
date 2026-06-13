package com.hambalapps.expressivebox.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import android.content.Context

private fun getSystemColor(context: Context, name: String, fallback: Color): Color {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val resId = context.resources.getIdentifier(name, "color", "android")
        if (resId != 0) {
            try {
                return Color(context.getColor(resId))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    return fallback
}

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
    error = ErrorRed,
    onError = OnErrorRed,
    outline = DarkOutline
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
    error = ErrorRed,
    onError = OnErrorRed,
    outline = LightOutline
)

@Composable
fun ExpressiveBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enabled by default for native Monet accent coloring
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Read accent colors directly from system resources for robust Monet color adaptation
            val primary = getSystemColor(context, "system_accent1_200", DarkPrimary)
            val primaryLight = getSystemColor(context, "system_accent1_600", LightPrimary)
            
            val primaryContainer = getSystemColor(context, "system_accent1_700", DarkPrimaryContainer)
            val primaryContainerLight = getSystemColor(context, "system_accent1_100", LightPrimaryContainer)
            
            val onPrimary = getSystemColor(context, "system_accent1_800", DarkOnPrimary)
            val onPrimaryLight = getSystemColor(context, "system_accent1_0", LightOnPrimary)
            
            val onPrimaryContainer = getSystemColor(context, "system_accent1_100", DarkOnPrimaryContainer)
            val onPrimaryContainerLight = getSystemColor(context, "system_accent1_900", LightOnPrimaryContainer)
            
            val secondary = getSystemColor(context, "system_accent2_200", DarkSecondary)
            val secondaryLight = getSystemColor(context, "system_accent2_600", LightSecondary)
            
            val secondaryContainer = getSystemColor(context, "system_accent2_700", DarkSecondaryContainer)
            val secondaryContainerLight = getSystemColor(context, "system_accent2_100", LightSecondaryContainer)
            
            val onSecondary = getSystemColor(context, "system_accent2_800", DarkOnSecondary)
            val onSecondaryLight = getSystemColor(context, "system_accent2_0", LightOnSecondary)
            
            val onSecondaryContainer = getSystemColor(context, "system_accent2_100", DarkOnSecondaryContainer)
            val onSecondaryContainerLight = getSystemColor(context, "system_accent2_900", LightOnSecondaryContainer)

            val tertiary = getSystemColor(context, "system_accent3_200", DarkTertiary)
            val tertiaryLight = getSystemColor(context, "system_accent3_600", LightTertiary)

            val tertiaryContainer = getSystemColor(context, "system_accent3_700", DarkTertiaryContainer)
            val tertiaryContainerLight = getSystemColor(context, "system_accent3_100", LightTertiaryContainer)

            val onTertiary = getSystemColor(context, "system_accent3_800", DarkOnTertiary)
            val onTertiaryLight = getSystemColor(context, "system_accent3_0", LightOnTertiary)

            val onTertiaryContainer = getSystemColor(context, "system_accent3_100", DarkOnTertiaryContainer)
            val onTertiaryContainerLight = getSystemColor(context, "system_accent3_900", LightOnTertiaryContainer)

            val background = if (darkTheme) Color.Black else getSystemColor(context, "system_neutral1_10", LightBackground)
            val surface = if (darkTheme) Color.Black else getSystemColor(context, "system_neutral1_10", LightSurface)
            
            val onBackground = getSystemColor(context, "system_neutral1_100", DarkOnBackground)
            val onBackgroundLight = getSystemColor(context, "system_neutral1_900", LightOnBackground)
            
            val onSurface = getSystemColor(context, "system_neutral1_100", DarkOnSurface)
            val onSurfaceLight = getSystemColor(context, "system_neutral1_900", LightOnSurface)
            
            val surfaceVariant = if (darkTheme) Color.Black else getSystemColor(context, "system_neutral2_100", LightSurfaceVariant)
            val surfaceVariantLight = getSystemColor(context, "system_neutral2_100", LightSurfaceVariant)
            
            val onSurfaceVariant = getSystemColor(context, "system_neutral2_200", DarkOnSurfaceVariant)
            val onSurfaceVariantLight = getSystemColor(context, "system_neutral2_700", LightOnSurfaceVariant)
            
            val outline = getSystemColor(context, "system_neutral2_400", DarkOutline)
            val outlineLight = getSystemColor(context, "system_neutral2_500", LightOutline)

            if (darkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = onPrimaryContainer,
                    secondary = secondary,
                    onSecondary = onSecondary,
                    secondaryContainer = secondaryContainer,
                    onSecondaryContainer = onSecondaryContainer,
                    tertiary = tertiary,
                    onTertiary = onTertiary,
                    tertiaryContainer = tertiaryContainer,
                    onTertiaryContainer = onTertiaryContainer,
                    background = background,
                    onBackground = onBackground,
                    surface = surface,
                    onSurface = onSurface,
                    surfaceVariant = surfaceVariant,
                    onSurfaceVariant = onSurfaceVariant,
                    outline = outline
                )
            } else {
                lightColorScheme(
                    primary = primaryLight,
                    onPrimary = onPrimaryLight,
                    primaryContainer = primaryContainerLight,
                    onPrimaryContainer = onPrimaryContainerLight,
                    secondary = secondaryLight,
                    onSecondary = onSecondaryLight,
                    secondaryContainer = secondaryContainerLight,
                    onSecondaryContainer = onSecondaryContainerLight,
                    tertiary = tertiaryLight,
                    onTertiary = onTertiaryLight,
                    tertiaryContainer = tertiaryContainerLight,
                    onTertiaryContainer = onTertiaryContainerLight,
                    background = background,
                    onBackground = onBackgroundLight,
                    surface = surface,
                    onSurface = onSurfaceLight,
                    surfaceVariant = surfaceVariantLight,
                    onSurfaceVariant = onSurfaceVariantLight,
                    outline = outlineLight
                )
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (darkTheme) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceVariant = Color.Black
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
