package uk.co.zlurgg.mybookshelf.core.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// MyBookshelf palette — warm parchment, gold, and umber
// Derived from the app icon: cream backgrounds, gilded gold accents, aged paper tones

// Light mode colors
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B6B3E), // Antique gold (icon outlines)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DCCA), // Cream (icon page fill)
    onPrimaryContainer = Color(0xFF2E2010),

    secondary = Color(0xFF6B5D4F), // Warm umber
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDD1C0),
    onSecondaryContainer = Color(0xFF261E14),

    tertiary = Color(0xFFA07842), // Amber accent (icon darker gold)
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8D8BE),
    onTertiaryContainer = Color(0xFF2C1F08),

    background = Color(0xFFF2EBE0), // Warm parchment
    onBackground = Color(0xFF2B2520),

    surface = Color(0xFFE8E0D4), // Aged paper
    onSurface = Color(0xFF342C24),
    surfaceVariant = Color(0xFFDDD3C4), // Tan paper
    onSurfaceVariant = Color(0xFF4E4235),

    outline = Color(0xFF877660),
    outlineVariant = Color(0xFFD0C4B2),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF342C24),
    inverseOnSurface = Color(0xFFF0E6DA),
    inversePrimary = Color(0xFFD4AE7C),
)

// Dark mode colors
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4AE7C), // Warm gold
    onPrimary = Color(0xFF3A2A14),
    primaryContainer = Color(0xFF52401E), // Deep gold
    onPrimaryContainer = Color(0xFFF0E0C8),

    secondary = Color(0xFFC4AD92), // Muted tan
    onSecondary = Color(0xFF2E2418),
    secondaryContainer = Color(0xFF44392C),
    onSecondaryContainer = Color(0xFFE4D6C4),

    tertiary = Color(0xFFCBA66E), // Light amber
    onTertiary = Color(0xFF332410),
    tertiaryContainer = Color(0xFF4E3D20),
    onTertiaryContainer = Color(0xFFF2E0C4),

    background = Color(0xFF1C1814), // Dark leather
    onBackground = Color(0xFFE6DDD4),

    surface = Color(0xFF262018), // Aged wood
    onSurface = Color(0xFFDAD0C4),
    surfaceVariant = Color(0xFF3D3429), // Warm charcoal
    onSurfaceVariant = Color(0xFFCFC1B2),

    outline = Color(0xFF9E8E7C),
    outlineVariant = Color(0xFF3C342C),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFF0E6DA),
    inverseOnSurface = Color(0xFF2E2620),
    inversePrimary = Color(0xFF8B6B3E),
)
