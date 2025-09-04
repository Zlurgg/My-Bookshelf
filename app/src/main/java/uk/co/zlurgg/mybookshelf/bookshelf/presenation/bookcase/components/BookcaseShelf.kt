package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.core.presentation.bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.toMaterial
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun BookcaseShelf(
    shelf: Bookshelf,
    onRemoveBookshelf: (Bookshelf) -> Unit,
    onBookshelfClick: (Bookshelf) -> Unit,
    modifier: Modifier = Modifier,
    bookCountOverride: Int? = null,
) {
    val haptic = LocalHapticFeedback.current

    var shouldRemoveOnRelease by remember { mutableStateOf(false) }

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (shouldRemoveOnRelease) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRemoveBookshelf(shelf)
                true
            } else {
                false
            }
        },
    )

    // Calculate swipe progress for visual feedback
    val swipeProgress by remember(swipeState) {
        derivedStateOf {
            when (swipeState.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> minOf(1f, -swipeState.progress)
                else -> 0f
            }
        }
    }

    // Calculate background color
    val backgroundColor = MaterialTheme.colorScheme.errorContainer
    val darkColor = MaterialTheme.colorScheme.error
    val blendedColor = lerp(backgroundColor, darkColor, -swipeProgress)

    // check threshold for delete passed
    LaunchedEffect(swipeProgress) {
        shouldRemoveOnRelease = -swipeProgress > 0.7f
    }

    // Reset swipe state when shelf changes
    LaunchedEffect(shelf) {
        swipeState.reset()
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(blendedColor)
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = uk.co.zlurgg.mybookshelf.R.string.cd_delete_shelf),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = shelf.shelfStyle.toMaterial().painter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(8.dp),
            ) {
                ListItem(
                    modifier = Modifier
                        .clickable { onBookshelfClick(shelf) }
                        .fillMaxWidth(),
                    headlineContent = { Text(shelf.name) },
                    supportingContent = { Text(
                        text = pluralStringResource(
                            id = uk.co.zlurgg.mybookshelf.R.plurals.bookcount_books,
                            count = bookCountOverride ?: shelf.books.size,
                            bookCountOverride ?: shelf.books.size
                        )
                    ) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookcaseShelfPreview() {
    MyBookshelfTheme {
        BookcaseShelf(
            shelf = bookshelf,
            onRemoveBookshelf = {},
            onBookshelfClick = {},
        )
    }
}
