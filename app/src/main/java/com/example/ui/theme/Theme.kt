package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GlassColorScheme = lightColorScheme(
    primary = CalmingPineGreen,
    secondary = SageHerb,
    tertiary = ClayTerracotta,
    background = OatmealCream,
    surface = CeramicPearl,
    onPrimary = CeramicPearl,
    onSecondary = WarmCharcoal,
    onBackground = WarmCharcoal,
    onSurface = WarmCharcoal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We enforce our luxurious warm-glass ceramic color scheme to avoid AI-looking default purple or harsh dark modes
    MaterialTheme(
        colorScheme = GlassColorScheme,
        typography = Typography,
        content = content
    )
}
