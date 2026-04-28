package uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R

/**
 * Book cover placeholder component.
 *
 * Shows a book icon on a surface-variant background when:
 * - Image is loading
 * - Image failed to load
 * - No image URL is available
 *
 * Uses 64dp icon size for detail view prominence.
 */
@Composable
fun BookPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = stringResource(R.string.book_cover_placeholder),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
