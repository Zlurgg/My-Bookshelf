package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants

/**
 * Card displaying publication details.
 * Shows: ISBN, Publisher, Publish Date, Google Books links.
 */
@Composable
fun PublicationDetailsCard(
    isbn: String?,
    publisher: String?,
    publishDate: String?,
    infoLink: String? = null,
    previewLink: String? = null,
    modifier: Modifier = Modifier
) {
    // Only show card if there's at least one detail
    val hasAnyDetail = isbn != null || publisher != null || publishDate != null ||
        infoLink != null || previewLink != null
    if (!hasAnyDetail) {
        return
    }

    val uriHandler = LocalUriHandler.current

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(BookDetailUiConstants.CardContentPadding)
        ) {
            Text(
                text = stringResource(R.string.publication_details_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(BookDetailUiConstants.SectionSpacing))

            isbn?.let {
                Text(
                    text = stringResource(R.string.publication_isbn_label, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            publisher?.let {
                Text(
                    text = stringResource(R.string.publication_publisher_label, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            publishDate?.let {
                Text(
                    text = stringResource(R.string.publication_publish_date_label, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            infoLink?.let { url ->
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { uriHandler.openUri(url) }) {
                    Text(stringResource(R.string.publication_view_on_google_books))
                }
            }

            previewLink?.let { url ->
                TextButton(onClick = { uriHandler.openUri(url) }) {
                    Text(stringResource(R.string.publication_google_preview))
                }
            }
        }
    }
}
