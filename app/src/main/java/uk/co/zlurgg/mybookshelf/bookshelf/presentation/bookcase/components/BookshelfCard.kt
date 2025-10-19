package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.util.ShelfMaterial
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.preview.bookshelf
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun BookshelfCard(
    shelf: Bookshelf,
    bookCount: Int,
    isReorderMode: Boolean,
    onBookshelfClick: (Bookshelf) -> Unit,
    onLongClick: (Bookshelf) -> Unit,
    onChangeStyle: (Bookshelf) -> Unit,
    onDelete: (Bookshelf) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // Use Box overlay approach to show shelf texture as border
    Box(modifier = modifier.fillMaxWidth()) {
        // Background layer with shelf texture
        Image(
            painter = ShelfMaterial.fromShelfStyle(shelf.shelfStyle).painterMedium(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        // Inner card with content (creates border effect)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(8.dp)
                .combinedClickable(
                    onClick = { onBookshelfClick(shelf) },
                    onLongClick = { onLongClick(shelf) }
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = shelf.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = pluralStringResource(
                            id = R.plurals.bookcount_books,
                            count = bookCount,
                            bookCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Show drag handle in reorder mode, overflow menu otherwise
                if (isReorderMode) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.cd_unlock_reorder_mode),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(id = R.string.cd_shelf_options)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_rename_shelf)) },
                                onClick = {
                                    menuExpanded = false
                                    onLongClick(shelf)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_change_style)) },
                                onClick = {
                                    menuExpanded = false
                                    onChangeStyle(shelf)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Palette, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_delete_shelf)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(shelf)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_share_shelf)) },
                                onClick = { },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                enabled = false
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.menu_duplicate_shelf)) },
                                onClick = { },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                },
                                enabled = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookshelfCardPreview() {
    MyBookshelfTheme {
        Column {
            BookshelfCard(
                shelf = bookshelf,
                bookCount = 5,
                isReorderMode = false,
                onBookshelfClick = {},
                onLongClick = {},
                onChangeStyle = {},
                onDelete = {}
            )
            BookshelfCard(
                shelf = bookshelf.copy(name = "My Reading List"),
                bookCount = 12,
                isReorderMode = true,
                onBookshelfClick = {},
                onLongClick = {},
                onChangeStyle = {},
                onDelete = {}
            )
        }
    }
}