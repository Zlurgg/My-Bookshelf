package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.bookshelf
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme


@Composable
fun BookcaseShelf(
    shelf: Bookshelf,
    onRemove: (Bookshelf) -> Unit,
    onClick: (Bookshelf) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onRemove(shelf)
            }
            it != SwipeToDismissBoxValue.EndToStart
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        backgroundContent = {
            if (swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete shelf",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        ListItem(
            modifier = Modifier
                .clickable { onClick(shelf) }
                .fillMaxWidth(),
            headlineContent = { Text(shelf.name) },
            supportingContent = { Text("${shelf.bookCount} books") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BookcaseShelfPreview() {
    MyBookshelfTheme {
        BookcaseShelf(
            shelf = bookshelf,
            onRemove = {},
            onClick = {},
        )
    }
}