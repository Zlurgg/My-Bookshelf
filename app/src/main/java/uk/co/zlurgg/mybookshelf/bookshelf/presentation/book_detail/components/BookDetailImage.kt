package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

/**
 * Book detail image with instant placeholder pattern.
 *
 * Shows large book icon placeholder immediately, replaces with cover when loaded.
 * Uses larger icon (64dp) for detail view prominence.
 */
@Composable
fun BookDetailImage(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    onImageLoadResult: (Boolean) -> Unit = {},
) {
    @Composable
    fun BookPlaceholder() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = "Book cover placeholder",
                modifier = Modifier.size(64.dp), // Larger for detail view
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (imageUrl?.isNotBlank() == true) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    // Show placeholder immediately (no spinner, instant feedback)
                    BookPlaceholder()
                    onImageLoadResult(false) // Loading, not yet successful
                },
                error = {
                    // Show same placeholder on error (seamless, appears intentional)
                    BookPlaceholder()
                    onImageLoadResult(false)
                },
                onSuccess = {
                    onImageLoadResult(true)
                }
            )
        } else {
            // No URL available - show placeholder
            BookPlaceholder()
            onImageLoadResult(false)
        }
    }
}