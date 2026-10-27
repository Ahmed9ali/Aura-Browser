package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = AuraPrimaryLight,
    onPrimary = Color(0xFF00354E),
    primaryContainer = Color(0xFF004D6F),
    onPrimaryContainer = Color(0xFFC7E7FF),
    secondary = AuraSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2E3378),
    onSecondaryContainer = Color(0xFFE0E0FF),
    tertiary = AuraTertiary,
    onTertiary = Color(0xFF003731),
    background = AuraBgDark,
    onBackground = Color(0xFFF1F5F9),
    surface = AuraSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = AuraSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B)
)

private val LightColorScheme = lightColorScheme(
    primary = AuraPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2F),
    secondary = AuraSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0FF),
    onSecondaryContainer = Color(0xFF131754),
    tertiary = AuraTertiary,
    onTertiary = Color.White,
    background = AuraBgLight,
    onBackground = Color(0xFF0F172A),
    surface = AuraSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = AuraSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8)
)

@Composable
fun AuraBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our sleek Aura theme by default
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
