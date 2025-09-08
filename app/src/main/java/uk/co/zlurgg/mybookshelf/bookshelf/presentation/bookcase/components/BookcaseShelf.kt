package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.core.presentation.bookshelf
import uk.co.zlurgg.mybookshelf.core.presentation.ui.theme.MyBookshelfTheme

@Composable
fun BookcaseShelf(
    shelf: Bookshelf,
    onRemoveBookshelf: (Bookshelf) -> Unit,
    onBookshelfClick: (Bookshelf) -> Unit,
    modifier: Modifier = Modifier,
    bookCountOverride: Int? = null,
    isReorderMode: Boolean = false,
    onReorderShelf: ((Bookshelf, Int) -> Unit)? = null,
) {
    // Fixed height for consistent drag calculations (card height + vertical padding)
    val totalItemHeight = 88.dp // 80dp card + 8dp vertical padding
    val bookCount = bookCountOverride ?: shelf.books.size
    
    if (isReorderMode) {
        // Drag and drop mode - use fresh position from database for each drag
        var offsetY by remember { mutableFloatStateOf(0f) }
        
        Box(
            modifier = modifier
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY.toInt()) }
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .pointerInput(shelf.id, shelf.position) { // Reset on position change
                    detectDragGestures(
                        onDragEnd = {
                            // Calculate movement from current database position
                            val itemHeightPx = totalItemHeight.toPx()
                            val positionsMoved = (offsetY / itemHeightPx).toInt()
                            
                            if (positionsMoved != 0) {
                                val newPosition = (shelf.position + positionsMoved).coerceAtLeast(0)
                                onReorderShelf?.invoke(shelf, newPosition)
                            }
                            
                            offsetY = 0f
                        }
                    ) { _, dragAmount ->
                        offsetY += dragAmount.y
                    }
                }
        ) {
            BookshelfCard(
                shelf = shelf,
                bookCount = bookCount,
                isReorderMode = true,
                onBookshelfClick = onBookshelfClick
            )
        }
    } else {
        // Normal swipe-to-delete mode
        val haptic = LocalHapticFeedback.current
        var shouldRemoveOnRelease by remember { mutableStateOf(false) }

        val swipeState = rememberSwipeToDismissBoxState(
            confirmValueChange = { _ ->
                if (shouldRemoveOnRelease) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemoveBookshelf(shelf)
                    true
                } else {
                    false
                }
            },
        )

        val swipeProgress by remember(swipeState) {
            derivedStateOf {
                when (swipeState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> minOf(1f, -swipeState.progress)
                    else -> 0f
                }
            }
        }

        val backgroundColor = MaterialTheme.colorScheme.errorContainer
        val darkColor = MaterialTheme.colorScheme.error
        val blendedColor = lerp(backgroundColor, darkColor, -swipeProgress)

        LaunchedEffect(swipeProgress) {
            shouldRemoveOnRelease = -swipeProgress > 0.7f
        }

        LaunchedEffect(shelf) {
            swipeState.reset()
        }

        SwipeToDismissBox(
            state = swipeState,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                if (swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(blendedColor, RoundedCornerShape(12.dp))
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.cd_delete_shelf),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        ) {
            BookshelfCard(
                shelf = shelf,
                bookCount = bookCount,
                isReorderMode = false,
                onBookshelfClick = onBookshelfClick
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun BookcaseShelfPreview() {
    MyBookshelfTheme {
        Column {
            BookcaseShelf(
                shelf = bookshelf,
                onRemoveBookshelf = {},
                onBookshelfClick = {},
                isReorderMode = false
            )
            BookcaseShelf(
                shelf = bookshelf.copy(name = "My Reading List"),
                onRemoveBookshelf = {},
                onBookshelfClick = {},
                isReorderMode = true
            )
        }
    }
}
