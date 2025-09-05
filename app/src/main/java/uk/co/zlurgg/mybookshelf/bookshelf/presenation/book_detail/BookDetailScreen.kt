package uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.book_detail.components.RatingBar
import uk.co.zlurgg.mybookshelf.core.presentation.sampleBook

@Composable
fun BookDetailsScreenRoot(
    viewModel: BookDetailViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BookDetailEvent.NavigateBack -> onBackClick()
            }
        }
    }

    BookDetailsScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
        }
    )
}

@Composable
fun BookDetailsScreen(
    state: BookDetailState,
    onAction: (BookDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.book != null) {
        Scaffold(
            floatingActionButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { onAction(BookDetailAction.OnAddBookClick(state.book)) },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (!state.onShelf) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.action_add_short))
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.action_remove_short))
                        }
                    }
                    FloatingActionButton(
                        onClick = { onAction(BookDetailAction.OnPurchaseClick) },
                        containerColor = if (state.book.purchased) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(id = R.string.action_purchase))
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Text(text = state.book.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "by ${state.book.authors.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                AsyncImage(
                    model = state.book.imageUrl,
                    contentDescription = state.book.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                RatingBar(
                    rating = state.book.averageRating?.toInt() ?: 0,
                    onRatingChanged = { rating -> onAction(BookDetailAction.OnRateBookDetailClick(rating)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = state.book.description ?: "No description available.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    } else {
        // Minimal fallback to avoid blank page
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(stringResource(id = R.string.bookdetail_loading))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookDetailScreenPreview() {
    BookDetailsScreen(
        state = BookDetailState(
            book = sampleBook,
            onShelf = false
        ),
        onAction = {}
    )
}