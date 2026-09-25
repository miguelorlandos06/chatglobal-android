package com.chatglobal.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ChatDark = darkColorScheme(
    primary = ChatPrimary,
    onPrimary = Color.White,
    primaryContainer = ChatPrimary,
    onPrimaryContainer = Color.White,
    secondary = ChatText2,
    onSecondary = ChatBg,
    background = ChatBg,
    onBackground = ChatText,
    surface = ChatBg2,
    onSurface = ChatText,
    surfaceVariant = ChatBg3,
    onSurfaceVariant = ChatText2,
    outline = ChatBorder,
    outlineVariant = ChatBorder,
    error = ChatError,
    onError = Color.White
)

@Composable
fun ChatTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ChatBg.toArgb()
            window.navigationBarColor = ChatBg.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ChatDark,
        typography = ChatTypography,
        content = content
    )
}
