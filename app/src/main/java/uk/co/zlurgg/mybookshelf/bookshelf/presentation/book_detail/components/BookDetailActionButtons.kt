package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailAction

@Composable
fun BookDetailActionButtons(
    book: Book,
    onShelf: Boolean,
    onAction: (BookDetailAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(16.dp)
    ) {
        FloatingActionButton(
            onClick = { onAction(BookDetailAction.OnAddBookClick(book)) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (!onShelf) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.action_add_short))
            } else {
                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.action_remove_short))
            }
        }
        FloatingActionButton(
            onClick = { onAction(BookDetailAction.OnPurchaseClick) },
            containerColor = if (book.purchased) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(id = R.string.action_purchase))
        }
    }
}