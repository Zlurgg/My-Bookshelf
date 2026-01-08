package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.bookshelf_components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * Composable for loading book cover images with instant placeholder pattern.
 *
 * UX Strategy:
 * - Shows book icon placeholder IMMEDIATELY (no spinner, no waiting)
 * - When image loads successfully, smoothly replaces placeholder with actual cover
 * - If image fails or takes too long, placeholder remains (appears intentional)
 *
 * Benefits:
 * - Instant visual feedback (app feels responsive)
 * - No perceived wait time for users
 * - Progressive enhancement (placeholder → image when available)
 * - No hanging spinners or loading states
 *
 * Note: Images may be slow/unavailable due to Archive.org CDN unreliability.
 * See IMAGE_LOADING_INVESTIGATION.md for details.
 */
@Composable
fun LoadImage(
    imageUrl: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    // Helper composable for book icon placeholder
    @Composable
    fun BookIconPlaceholder() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = "Book cover placeholder",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (imageUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            modifier = modifier,
            model = imageUrl,
            loading = {
                // Show placeholder immediately (no spinner - instant feedback)
                BookIconPlaceholder()
            },
            error = {
                // Show same placeholder on error (seamless, appears intentional)
                BookIconPlaceholder()
            },
            contentDescription = title,
        )
    } else {
        // Show placeholder when no URL is available
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = "No book cover",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
