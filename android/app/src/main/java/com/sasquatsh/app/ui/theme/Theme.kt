package com.sasquatsh.app.ui.theme

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

// Sasquatsh brand colors
private val Primary = Color(0xFF2D5A3D)
private val PrimaryDark = Color(0xFF1A3D28)
private val PrimaryLight = Color(0xFF4A7D5C)
private val Secondary = Color(0xFFE8C93A)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E6C8),
    onPrimaryContainer = Color(0xFF0D2818),
    secondary = Secondary,
    onSecondary = Color(0xFF3D3000),
    secondaryContainer = Color(0xFFFFF0B3),
    onSecondaryContainer = Color(0xFF3D3000),
    tertiary = Color(0xFF6366F1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E0FF),
    onTertiaryContainer = Color(0xFF1A1A5E),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF414942),
    surfaceContainerLow = Color(0xFFF3F5F0),
    surfaceContainer = Color(0xFFEDEFEA),
    surfaceContainerHigh = Color(0xFFE7E9E4),
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC0C9C0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CD4AE),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF1A5C35),
    onPrimaryContainer = Color(0xFFB8F0C9),
    secondary = Color(0xFFE8C93A),
    onSecondary = Color(0xFF3D3000),
    secondaryContainer = Color(0xFF574500),
    onSecondaryContainer = Color(0xFFFFF0B3),
    tertiary = Color(0xFFC4C6FF),
    onTertiary = Color(0xFF2A2D8E),
    tertiaryContainer = Color(0xFF4245B8),
    onTertiaryContainer = Color(0xFFE0E0FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C19),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF191C19),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC0C9C0),
    surfaceContainerLow = Color(0xFF1F221F),
    surfaceContainer = Color(0xFF242724),
    surfaceContainerHigh = Color(0xFF2E312E),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF414942)
)

@Composable
fun SasquatshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        content = content
    )
}
