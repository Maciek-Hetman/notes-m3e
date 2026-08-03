package com.maciejhetman.notes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import com.maciejhetman.notes.data.AppThemeColor
import com.maciejhetman.notes.data.ThemeMode

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
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
)

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF2E2E2E),
    surfaceContainerHighest = Color(0xFF383838)
)

private fun getCustomColorScheme(isDark: Boolean, themeColor: AppThemeColor): ColorScheme {
    return if (isDark) {
        when (themeColor) {
            AppThemeColor.DEFAULT -> DarkColorScheme
            AppThemeColor.BLUE -> darkColorScheme(primary = Color(0xFF9ECAFF), secondary = Color(0xFFBBC7DB), tertiary = Color(0xFFD6BEE4))
            AppThemeColor.GREEN -> darkColorScheme(primary = Color(0xFF9CD67D), secondary = Color(0xFFBDCBAE), tertiary = Color(0xFFA0CFD0))
            AppThemeColor.PURPLE -> darkColorScheme(primary = Color(0xFFD0BCFF), secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8))
            AppThemeColor.RED -> darkColorScheme(primary = Color(0xFFF2B8B5), secondary = Color(0xFFE7BDB8), tertiary = Color(0xFFE5C158))
        }
    } else {
        when (themeColor) {
            AppThemeColor.DEFAULT -> LightColorScheme
            AppThemeColor.BLUE -> lightColorScheme(primary = Color(0xFF0061A4), secondary = Color(0xFF535F70), tertiary = Color(0xFF6B5778))
            AppThemeColor.GREEN -> lightColorScheme(primary = Color(0xFF386A20), secondary = Color(0xFF55624C), tertiary = Color(0xFF19686A))
            AppThemeColor.PURPLE -> lightColorScheme(primary = Color(0xFF6750A4), secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260))
            AppThemeColor.RED -> lightColorScheme(primary = Color(0xFFB3261E), secondary = Color(0xFF775652), tertiary = Color(0xFF755B00))
        }
    }
}

@Composable
fun isAppDarkTheme(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themeColor: AppThemeColor = AppThemeColor.DEFAULT,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> getCustomColorScheme(darkTheme, themeColor)
    }.let { scheme -> if (darkTheme && amoledBlack) scheme.toAmoled() else scheme }

    val view = LocalView.current
    val backgroundArgb = colorScheme.background.toArgb()
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // Keep the raw window background in sync with the current theme. Without this it
            // stays whatever the static Activity theme declares (white), which briefly flashes
            // through during nav transitions/config changes since Compose only paints inside the
            // Surface below, not the window itself.
            window.setBackgroundDrawable(backgroundArgb.toDrawable())
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive()
    ) {
        // A full-bleed opaque Surface behind all navigation content so that any gap revealed
        // during scene transitions shows the correct themed background instead of the window's
        // default (white) background peeking through.
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.background) {
            content()
        }
    }
}