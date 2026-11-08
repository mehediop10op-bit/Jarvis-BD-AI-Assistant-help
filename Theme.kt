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

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color.Black,
    primaryContainer = JarvisSurfaceNavy,
    onPrimaryContainer = JarvisCyan,
    secondary = JarvisAqua,
    onSecondary = Color.Black,
    secondaryContainer = JarvisCardNavy,
    onSecondaryContainer = JarvisAqua,
    tertiary = JarvisAmber,
    onTertiary = Color.Black,
    background = JarvisSpaceBlack,
    onBackground = TextWhite,
    surface = JarvisDarkNavy,
    onSurface = TextWhite,
    surfaceVariant = JarvisSurfaceNavy,
    onSurfaceVariant = TextMuted,
    outline = JarvisBorderNavy,
    error = JarvisCrimson,
    onError = Color.White
)

private val JarvisLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = LightSecondary,
    tertiary = JarvisOrange,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCBD5E1),
    error = JarvisCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to futuristic dark theme for Jarvis
    dynamicColor: Boolean = false, // Keep distinctive Jarvis cyberpunk cyan branding by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> JarvisDarkColorScheme
        else -> JarvisLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
