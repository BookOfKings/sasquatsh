package com.sasquatsh.app.extensions

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// MARK: - Primary (Forest Green)
val Primary = Color(0xFF2C5A3C)
val OnPrimary = Color.White
val PrimaryContainer = Color(0xFFDCECDE)
val OnPrimaryContainer = Color(0xFF172C1F)

val PrimaryDark = Color(0xFF7ACD8E)
val OnPrimaryDark = Color(0xFF172C1F)
val PrimaryContainerDark = Color(0xFF28432E)
val OnPrimaryContainerDark = Color(0xFFDCECDE)

// MARK: - Secondary (Leather Brown)
val Secondary = Color(0xFF6B4422)
val OnSecondary = Color.White
val SecondaryContainer = Color(0xFFF1E8DF)
val OnSecondaryContainer = Color(0xFF362113)

val SecondaryDark = Color(0xFFD29E72)
val OnSecondaryDark = Color(0xFF362113)
val SecondaryContainerDark = Color(0xFF4A3321)
val OnSecondaryContainerDark = Color(0xFFF1E8DF)

// MARK: - Tertiary (Gold)
val Tertiary = Color(0xFFD39209)
val OnTertiary = Color.White
val TertiaryContainer = Color(0xFFFCEFC7)
val OnTertiaryContainer = Color(0xFF663B12)

val TertiaryDark = Color(0xFFF9C74F)
val OnTertiaryDark = Color(0xFF663B12)
val TertiaryContainerDark = Color(0xFF664A0D)
val OnTertiaryContainerDark = Color(0xFFFCEFC7)

// MARK: - Surface / Background
val Surface = Color.White
val SurfaceVariant = Color(0xFFE7E0EC)
val OnSurface = Color(0xFF1C1B1F)
val OnSurfaceVariant = Color(0xFF49454F)
val SurfaceContainerLowest = Color.White
val SurfaceContainerLow = Color(0xFFF7F2FA)
val SurfaceContainer = Color(0xFFF5F5EF)
val SurfaceContainerHigh = Color(0xFFECE6F0)

val SurfaceDark = Color(0xFF121214)
val SurfaceVariantDark = Color(0xFF2E2D32)
val OnSurfaceDark = Color(0xFFE7E4EA)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val SurfaceContainerLowestDark = Color(0xFF0E0E10)
val SurfaceContainerLowDark = Color(0xFF1C1B1F)
val SurfaceContainerDark = Color(0xFF212024)
val SurfaceContainerHighDark = Color(0xFF2D2C31)

// MARK: - Error
val Error = Color(0xFFB3261E)
val OnError = Color.White
val ErrorContainer = Color(0xFFF9DEDC)

val ErrorDark = Color(0xFFF9B5B1)
val OnErrorDark = Color(0xFF60140F)
val ErrorContainerDark = Color(0xFF8E1D17)

// MARK: - Outline
val Outline = Color(0xFF79747E)
val OutlineVariant = Color(0xFFCAC4D0)

val OutlineDark = Color(0xFF928E97)
val OutlineVariantDark = Color(0xFF49454F)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)

@Composable
fun SasquatshTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SasquatshTypography,
        content = content
    )
}
