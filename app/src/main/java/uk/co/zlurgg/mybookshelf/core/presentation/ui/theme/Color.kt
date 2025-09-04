package uk.co.zlurgg.mybookshelf.core.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// MyBookshelf professional palette
// - Neutral, bookstore-inspired browns with soft neutrals
// - Accessible contrast for text and controls
// - Complete scheme to ensure consistent look across components

// Light mode colors
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C41),            // Rich Brown (brand)
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7CCC8),   // Muted brown container
    onPrimaryContainer = Color(0xFF3E2723),

    secondary = Color(0xFF5E6A71),          // Subtle slate
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3EA),
    onSecondaryContainer = Color(0xFF1B2A30),

    tertiary = Color(0xFF8E6C88),           // Muted plum accent
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEDCF0),
    onTertiaryContainer = Color(0xFF38253B),

    background = Color(0xFFFAF7F3),         // Paper-like background
    onBackground = Color(0xFF2B2623),

    surface = Color(0xFFF5EFEA),            // Surface for cards/sheets
    onSurface = Color(0xFF3D332E),
    surfaceVariant = Color(0xFFE7E0DC),
    onSurfaceVariant = Color(0xFF51443D),

    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFD4C6BE),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF312A26),
    inverseOnSurface = Color(0xFFECE0D8),
    inversePrimary = Color(0xFFB68F7F),
)

// Dark mode colors
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB68F7F),            // Softer brown for dark
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFF4B372F),
    onPrimaryContainer = Color(0xFFEADFD8),

    secondary = Color(0xFFB3C7D0),
    onSecondary = Color(0xFF21333A),
    secondaryContainer = Color(0xFF3A4A51),
    onSecondaryContainer = Color(0xFFDCE8EE),

    tertiary = Color(0xFFD2BFD0),
    onTertiary = Color(0xFF36283A),
    tertiaryContainer = Color(0xFF4E3C50),
    onTertiaryContainer = Color(0xFFF4E7F3),

    background = Color(0xFF1E1B19),
    onBackground = Color(0xFFE7E0DA),

    surface = Color(0xFF23201D),
    onSurface = Color(0xFFD9CFC8),
    surfaceVariant = Color(0xFF4A403A),
    onSurfaceVariant = Color(0xFFCFC1B8),

    outline = Color(0xFF9E8E84),
    outlineVariant = Color(0xFF3C342F),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFECE0D8),
    inverseOnSurface = Color(0xFF2E2622),
    inversePrimary = Color(0xFF6D4C41),
)
