package uk.co.zlurgg.mybookshelf.core.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light mode colors
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C41),         // Rich Brown
    onPrimary = Color.White,
    background = Color(0xFFF5F0EB),      // Paper-like
    onBackground = Color(0xFF3E2723),
    surface = Color(0xFFEDE0D4),
    onSurface = Color(0xFF5D4037),
    error = Color(0xFFB00020),
    errorContainer = Color(0xFFFCD8DF),
)

// Dark mode colors
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8D6E63),         // Lighter brown
    onPrimary = Color.Black,
    background = Color(0xFF212121),
    onBackground = Color(0xFFEDE7F6),
    surface = Color(0xFF2C2C2C),
    onSurface = Color(0xFFD7CCC8),
    error = Color(0xFFB00020),
    errorContainer = Color(0xFFFCD8DF),
)
