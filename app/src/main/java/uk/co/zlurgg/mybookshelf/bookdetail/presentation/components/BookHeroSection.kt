package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.SubcomposeAsyncImage
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.presentation.components.resolveImageModel
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants

@Composable
fun BookHeroSection(
    title: String,
    subtitle: String?,
    authors: List<String>,
    firstPublishYear: String?,
    numPages: Int?,
    imageUrl: String?,
    onImageLoadResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column {
            // Cover image
            if (imageUrl?.isNotBlank() == true) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BookDetailUiConstants.BookImageHeight)
                        .clip(
                            RoundedCornerShape(
                                topStart = BookDetailUiConstants.ImageCornerRadius,
                                topEnd = BookDetailUiConstants.ImageCornerRadius
                            )
                        )
                ) {
                    SubcomposeAsyncImage(
                        model = resolveImageModel(imageUrl),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        loading = {
                            BookPlaceholder()
                            onImageLoadResult(false)
                        },
                        error = {
                            BookPlaceholder()
                            onImageLoadResult(false)
                        },
                        onSuccess = {
                            onImageLoadResult(true)
                        }
                    )
                }
            }

            // Overview text
            Column(
                modifier = Modifier.padding(BookDetailUiConstants.CardContentPadding)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(BookDetailUiConstants.SmallSpacing))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (authors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(BookDetailUiConstants.SmallSpacing))
                    Text(
                        text = stringResource(R.string.book_overview_by_author, authors.joinToString(", ")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(BookDetailUiConstants.SmallSpacing))

                if (firstPublishYear != null) {
                    Text(
                        text = stringResource(R.string.publication_first_published_label, firstPublishYear),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (numPages != null && numPages > 0) {
                    Text(
                        text = stringResource(R.string.publication_pages_label, numPages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
