package com.timome.eggyhub.ui.theme

import android.app.Activity
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
import com.timome.eggyhub.data.ExtractedColors

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun EggyhubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    extractedColors: ExtractedColors? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        extractedColors != null -> {
            createCustomColorScheme(extractedColors, darkTheme)
        }
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

private fun createCustomColorScheme(colors: ExtractedColors, darkTheme: Boolean) = if (darkTheme) {
    darkColorScheme(
        primary = colors.primaryDark?.let { Color(it) } ?: Purple80,
        onPrimary = Color.White,
        primaryContainer = colors.primary?.let { Color(it) } ?: PurpleGrey80,
        onPrimaryContainer = Color.White,
        secondary = colors.secondary?.let { Color(it) } ?: PurpleGrey80,
        onSecondary = Color.White,
        tertiary = colors.tertiary?.let { Color(it) } ?: Pink80
    )
} else {
    lightColorScheme(
        primary = colors.primary?.let { Color(it) } ?: Purple40,
        onPrimary = Color.White,
        primaryContainer = colors.primaryLight?.let { Color(it) } ?: PurpleGrey40,
        onPrimaryContainer = Color(0xFF1C1B1F),
        secondary = colors.secondary?.let { Color(it) } ?: PurpleGrey40,
        onSecondary = Color.White,
        tertiary = colors.tertiary?.let { Color(it) } ?: Pink40
    )
}
