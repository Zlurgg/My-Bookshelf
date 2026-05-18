package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import uk.co.zlurgg.mybookshelf.book.presentation.components.resolveImageModel

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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (imageUrl?.isNotBlank() == true) {
            SubcomposeAsyncImage(
                model = resolveImageModel(imageUrl),
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
