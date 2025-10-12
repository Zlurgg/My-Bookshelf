package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components

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
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

/**
 * Card with shelf management actions.
 * Shows: Add to Shelf / Remove from Shelf buttons.
 */
@Composable
fun ShelfActionsCard(
    book: Book,
    onShelf: Boolean,
    onAddToShelf: (Book) -> Unit,
    onRemoveFromShelf: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                    Spacer(modifier = Modifier.height(8.dp))
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.shelf_actions_add_to_shelf))
                }
            }
        }
    }
}
