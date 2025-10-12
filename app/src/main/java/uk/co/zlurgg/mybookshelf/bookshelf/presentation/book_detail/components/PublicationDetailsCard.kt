package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/**
 * Card displaying publication details.
 * Shows: ISBN, Publisher, Publish Date, Internet Archive link.
 */
@Composable
fun PublicationDetailsCard(
    isbn: String?,
    publisher: String?,
    publishDate: String?,
    internetArchiveId: String?,
    modifier: Modifier = Modifier
) {
    // Only show card if there's at least one detail
    if (isbn == null && publisher == null && publishDate == null && internetArchiveId == null) {
        return
    }

    val uriHandler = LocalUriHandler.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Publication Details",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            isbn?.let {
                Text(
                    text = "ISBN: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            publisher?.let {
                Text(
                    text = "Publisher: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            publishDate?.let {
                Text(
                    text = "Published: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            internetArchiveId?.let { iaId ->
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        uriHandler.openUri("https://archive.org/details/$iaId")
                    }
                ) {
                    Text("View on Internet Archive")
                }
            }
        }
    }
}
