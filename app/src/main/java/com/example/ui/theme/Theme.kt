package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SaudiGreenPrimaryDark,
    onPrimary = SaudiGreenOnPrimaryDark,
    primaryContainer = SaudiGreenPrimaryContainerDark,
    onPrimaryContainer = SaudiGreenOnPrimaryContainerDark,
    secondary = SaudiGoldSecondaryDark,
    onSecondary = SaudiGoldOnSecondaryDark,
    secondaryContainer = SaudiGoldSecondaryContainerDark,
    onSecondaryContainer = SaudiGoldOnSecondaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = SaudiGreenPrimary,
    onPrimary = SaudiGreenOnPrimary,
    primaryContainer = SaudiGreenPrimaryContainer,
    onPrimaryContainer = SaudiGreenOnPrimaryContainer,
    secondary = SaudiGoldSecondary,
    onSecondary = SaudiGoldOnSecondary,
    secondaryContainer = SaudiGoldSecondaryContainer,
    onSecondaryContainer = SaudiGoldOnSecondaryContainer,
    tertiary = SaudiTealTertiary,
    onTertiary = SaudiTealOnTertiary,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

@Composable
fun SaudiInvoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded emerald colors active
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
