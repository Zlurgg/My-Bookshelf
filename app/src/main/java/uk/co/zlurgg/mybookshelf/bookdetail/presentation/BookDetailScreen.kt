package uk.co.zlurgg.mybookshelf.bookdetail.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.BookHeroSection
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.ClubCommentsCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.ClubRatingCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.CommunityRatingsCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.DescriptionCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.LanguagesCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.PersonalNotesCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.PublicationDetailsCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.PurchasedToggleCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.ReadingStatusCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.ShelfActionsCard
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.components.SignInHintCard
import uk.co.zlurgg.mybookshelf.book.presentation.preview.sampleBook
import uk.co.zlurgg.mybookshelf.book.presentation.util.withMediumImage

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
        onAction = viewModel::onAction
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
        val isTutorialBook = state.isTutorialBook

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
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
            bottomBar = {
                // Hide actions for tutorial book and guests on club shelves
                if (!isTutorialBook && state.hasShelfContext && !(state.isBookClub && !state.isSignedIn)) {
                    ShelfActionsCard(
                        book = state.book,
                        onShelf = state.onShelf,
                        canRemove = state.canRemoveFromShelf,
                        onAddToShelf = { book ->
                            onAction(BookDetailAction.OnAddBookClick(book))
                        },
                        onRemoveFromShelf = { book ->
                            onAction(BookDetailAction.OnRemoveBookClick(book))
                        }
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(BookDetailUiConstants.CardContentPadding),
                verticalArrangement = Arrangement.spacedBy(BookDetailUiConstants.SectionSpacing)
            ) {
                // 1. Common hero (all paths)
                item {
                    BookHeroSection(
                        title = state.book.title,
                        authors = state.book.authors,
                        firstPublishYear = state.book.firstPublishYear,
                        numPages = state.book.numPages,
                        numEditions = state.book.numEditions,
                        imageUrl = state.book.withMediumImage(),
                        onImageLoadResult = { }
                    )
                }

                // 2. Variant-specific interactive section
                when {
                    isTutorialBook -> {
                        // Nothing — tutorial has no interactive cards
                    }
                    state.isBookClub && state.isSignedIn -> {
                        item {
                            ClubRatingCard(
                                averageRating = state.clubAverageRating,
                                ratedReviewCount = state.clubReviews.count { it.rating > 0 },
                                userClubRating = state.userClubRating,
                                onClubRatingChange = { rating ->
                                    onAction(BookDetailAction.OnClubRatingChange(rating))
                                }
                            )
                        }
                        item {
                            ClubCommentsCard(
                                comments = state.clubComments,
                                currentUserId = state.currentUserId,
                                commentText = state.commentText,
                                onCommentTextChange = { text ->
                                    onAction(BookDetailAction.OnCommentTextChange(text))
                                },
                                onCommentSubmit = {
                                    onAction(BookDetailAction.OnCommentSubmit)
                                },
                                editingCommentId = state.editingCommentId,
                                editingCommentText = state.editingCommentText,
                                onCommentEditStart = { commentId, currentText ->
                                    onAction(BookDetailAction.OnCommentEditStart(commentId, currentText))
                                },
                                onCommentEditTextChange = { text ->
                                    onAction(BookDetailAction.OnCommentEditTextChange(text))
                                },
                                onCommentEditSave = {
                                    onAction(BookDetailAction.OnCommentEditSave)
                                },
                                onCommentEditCancel = {
                                    onAction(BookDetailAction.OnCommentEditCancel)
                                },
                                onCommentDelete = { commentId ->
                                    onAction(BookDetailAction.OnCommentDelete(commentId))
                                },
                                isLoading = state.isLoadingComments
                            )
                        }
                    }
                    state.isBookClub && !state.isSignedIn -> {
                        item {
                            SignInHintCard()
                        }
                    }
                    else -> {
                        // Regular book — personal cards for all owned books
                        item {
                            ReadingStatusCard(
                                readingStatus = state.book.readingStatus,
                                onReadingStatusChange = { status ->
                                    onAction(BookDetailAction.OnReadingStatusChange(status))
                                }
                            )
                        }
                        item {
                            PersonalNotesCard(
                                personalRating = state.book.personalRating,
                                onPersonalRatingChange = { rating ->
                                    onAction(BookDetailAction.OnPersonalRatingChange(rating))
                                },
                                notes = state.book.personalNotes,
                                onNotesChange = { notes ->
                                    onAction(BookDetailAction.OnPersonalNotesChange(notes))
                                }
                            )
                        }
                        item {
                            PurchasedToggleCard(
                                purchased = state.book.purchased,
                                onPurchaseToggle = {
                                    onAction(BookDetailAction.OnPurchaseClick)
                                }
                            )
                        }
                    }
                }

                // 3. Common info section
                if (!isTutorialBook) {
                    item {
                        CommunityRatingsCard(
                            averageRating = state.book.averageRating,
                            ratingCount = state.book.ratingCount,
                            modifier = Modifier.padding(top = BookDetailUiConstants.InfoSectionExtraTopPadding)
                        )
                    }
                }
                item {
                    DescriptionCard(
                        description = state.book.description,
                        initiallyExpanded = isTutorialBook,
                        outlined = !isTutorialBook
                    )
                }
                if (!isTutorialBook) {
                    item {
                        PublicationDetailsCard(
                            isbn = state.book.isbn,
                            publisher = state.book.publisher,
                            publishDate = state.book.publishDate,
                            internetArchiveId = state.book.internetArchiveId
                        )
                    }
                    item {
                        LanguagesCard(
                            languages = state.book.languages
                        )
                    }
                }
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
