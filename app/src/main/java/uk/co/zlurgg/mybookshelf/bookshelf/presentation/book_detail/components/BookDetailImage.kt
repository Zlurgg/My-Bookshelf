package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

@Composable
fun BookDetailImage(
    imageUrl: String?,
    title: String,
    onImageLoadResult: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var shouldShowImage by remember(imageUrl) { mutableStateOf(imageUrl?.isNotBlank() == true) }
    
    if (shouldShowImage && imageUrl?.isNotBlank() == true) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            onState = { state ->
                when (state) {
                    is AsyncImagePainter.State.Success -> {
                        onImageLoadResult(true)
                    }
                    is AsyncImagePainter.State.Error -> {
                        shouldShowImage = false
                        onImageLoadResult(false)
                    }
                    else -> { /* Loading or Empty state */ }
                }
            }
        )
    }
}