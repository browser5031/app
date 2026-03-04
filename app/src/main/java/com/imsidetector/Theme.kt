package com.imsidetector.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Color palette for threat levels
val GreenThreat = Color(0xFF4CAF50)
val YellowThreat = Color(0xFFFFC107)
val OrangeThreat = Color(0xFFFF9800)
val RedThreat = Color(0xFFF44336)

// Primary brand colors - deep blue for security theme
val PrimaryBlue = Color(0xFF1976D2)
val PrimaryDarkBlue = Color(0xFF1565C0)
val PrimaryLightBlue = Color(0xFF42A5F5)

// Secondary accent - teal for technical feel
val SecondaryTeal = Color(0xFF00897B)
val SecondaryLightTeal = Color(0xFF26A69A)

val lightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryLightBlue,
    onPrimaryContainer = Color(0xFF001A41),
    
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryLightTeal,
    onSecondaryContainer = Color.White,
    
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE7F6),
    onTertiaryContainer = Color(0xFF21005E),
    
    error = RedThreat,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    background = Color(0xFFFAFBFC),
    onBackground = Color(0xFF1A1C1E),
    
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454E),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC7D0),
    
    scrim = Color.Black,
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFF42A5F5)
)

val darkColorScheme = darkColorScheme(
    primary = PrimaryLightBlue,
    onPrimary = Color(0xFF003366),
    primaryContainer = PrimaryDarkBlue,
    onPrimaryContainer = Color(0xFF42A5F5),
    
    secondary = SecondaryLightTeal,
    onSecondary = Color(0xFF003D36),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFF26A69A),
    
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEDE7F6),
    
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC7D0),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454E),
    
    scrim = Color.Black,
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF1A1C1E),
    inversePrimary = PrimaryBlue
)

@Composable
fun IMSIDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)?.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
