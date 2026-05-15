package uk.co.zlurgg.mybookshelf.bookdetail.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailUiConstants
import uk.co.zlurgg.mybookshelf.book.domain.model.Book

/**
 * Card with shelf management actions.
 * Shows: Add to Shelf / Remove from Shelf buttons.
 */
@Composable
fun ShelfActionsCard(
    book: Book,
    onShelf: Boolean,
    canRemove: Boolean = true,
    onAddToShelf: (Book) -> Unit,
    onRemoveFromShelf: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't render the card at all if on shelf but can't remove (nothing to show)
    if (onShelf && !canRemove) return

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = BookDetailUiConstants.CardElevation)
    ) {
        Column(
            modifier = Modifier.padding(BookDetailUiConstants.CardContentPadding)
        ) {
            if (onShelf) {
                OutlinedButton(
                    onClick = { onRemoveFromShelf(book) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.cd_remove_from_shelf)
                    )
                    Spacer(modifier = Modifier.height(BookDetailUiConstants.SmallSpacing))
                    Text(stringResource(R.string.shelf_actions_remove_from_shelf))
                }
            } else {
                Button(
                    onClick = { onAddToShelf(book) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_to_shelf)
                    )
                    Spacer(modifier = Modifier.height(BookDetailUiConstants.SmallSpacing))
                    Text(stringResource(R.string.shelf_actions_add_to_shelf))
                }
            }
        }
    }
}
