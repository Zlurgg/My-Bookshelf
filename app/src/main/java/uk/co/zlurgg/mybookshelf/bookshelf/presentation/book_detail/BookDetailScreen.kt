package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components.BookDetailActionButtons
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components.BookDetailImage
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.components.RatingBar
import uk.co.zlurgg.mybookshelf.core.presentation.sampleBook

@Composable
fun BookDetailsScreenRoot(
    viewModel: BookDetailViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Set navigation callback
    LaunchedEffect(Unit) {
        viewModel.setNavigationCallback(onBackClick)
    }

    BookDetailsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                BookDetailAction.OnBackClick -> onBackClick()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    state: BookDetailState,
    onAction: (BookDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.book != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = state.book.title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { onAction(BookDetailAction.OnBackClick) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.action_close)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                BookDetailActionButtons(
                    book = state.book,
                    onShelf = state.onShelf,
                    onAction = onAction
                )
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

                Text(
                    text = "by ${state.book.authors.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                BookDetailImage(
                    imageUrl = state.book.imageUrl,
                    title = state.book.title
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