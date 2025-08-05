package uk.co.zlurgg.mybookshelf.core.presentation.ui.theme

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

// Light mode colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C41),         // Rich Brown
    onPrimary = Color.White,
    background = Color(0xFFF5F0EB),      // Paper-like
    onBackground = Color(0xFF3E2723),
    surface = Color(0xFFEDE0D4),
    onSurface = Color(0xFF5D4037)
)

// Dark mode colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8D6E63),         // Lighter brown
    onPrimary = Color.Black,
    background = Color(0xFF212121),
    onBackground = Color(0xFFEDE7F6),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFD7CCC8)
)

@Composable
fun MyBookshelfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BookshelfTypography,
        shapes = Shapes,
        content = content
    )
}