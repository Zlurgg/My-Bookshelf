package uk.co.zlurgg.mybookshelf.book.presentation.bookdetail

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.book.domain.util.BookDetailConstants
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.BookDetailImage
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.BookOverviewCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.ClubCommentsCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.ClubRatingCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.CommunityRatingsCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.DescriptionCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.LanguagesCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.PersonalNotesCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.PublicationDetailsCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.PurchasedToggleCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.RecommendationStatusCard
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.components.ShelfActionsCard
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
    authService: AuthService = koinInject()
) {
    if (state.book != null) {
        val isTutorialBook = state.book.id == BookDetailConstants.TUTORIAL_BOOK_ID
        val currentUserId = authService.getSignedInUser()?.userId

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
            bottomBar = {
                // Hide actions for tutorial book - it shouldn't be removed from shelf
                if (!isTutorialBook) {
                    ShelfActionsCard(
                        book = state.book,
                        onShelf = state.onShelf,
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
            // Image visibility state
            var showImageWithSpacing by remember(state.book.imageUrl) {
                mutableStateOf(state.book.imageUrl.isNotBlank())
            }

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isTutorialBook) {
                    // Simplified read-only view for tutorial book
                    // 1. Book Overview Card
                    item {
                        BookOverviewCard(
                            title = state.book.title,
                            authors = state.book.authors,
                            firstPublishYear = state.book.firstPublishYear,
                            numPages = state.book.numPages,
                            numEditions = state.book.numEditions
                        )
                    }

                    // Book Image (if available)
                    if (showImageWithSpacing) {
                        item {
                            BookDetailImage(
                                imageUrl = state.book.withMediumImage(),
                                title = state.book.title,
                                onImageLoadResult = { success ->
                                    if (!success) {
                                        showImageWithSpacing = false
                                    }
                                }
                            )
                        }
                    }

                    // 2. Description Card (contains tutorial content)
                    item {
                        DescriptionCard(
                            description = state.book.description,
                            initiallyExpanded = true // Show full tutorial content by default
                        )
                    }
                } else if (state.isBookClub) {
                    // Book Club view - minimal, club-focused only
                    // 1. Book Overview Card
                    item {
                        BookOverviewCard(
                            title = state.book.title,
                            authors = state.book.authors,
                            firstPublishYear = state.book.firstPublishYear,
                            numPages = state.book.numPages,
                            numEditions = state.book.numEditions
                        )
                    }

                    // Book Image (if available)
                    if (showImageWithSpacing) {
                        item {
                            BookDetailImage(
                                imageUrl = state.book.withMediumImage(),
                                title = state.book.title,
                                onImageLoadResult = { success ->
                                    if (!success) {
                                        showImageWithSpacing = false
                                    }
                                }
                            )
                        }
                    }

                    // 2. Club Rating Card
                    item {
                        ClubRatingCard(
                            reviews = state.clubReviews,
                            userClubRating = state.userClubRating,
                            onClubRatingChange = { rating ->
                                onAction(BookDetailAction.OnClubRatingChange(rating))
                            }
                        )
                    }

                    // 3. Club Comments Card (Discussion)
                    item {
                        ClubCommentsCard(
                            comments = state.clubComments,
                            currentUserId = currentUserId,
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
                } else {
                    // Full view for regular books
                    // 1. Book Overview Card
                    item {
                        BookOverviewCard(
                            title = state.book.title,
                            authors = state.book.authors,
                            firstPublishYear = state.book.firstPublishYear,
                            numPages = state.book.numPages,
                            numEditions = state.book.numEditions
                        )
                    }

                    // Book Image (if available)
                    if (showImageWithSpacing) {
                        item {
                            BookDetailImage(
                                imageUrl = state.book.withMediumImage(),
                                title = state.book.title,
                                onImageLoadResult = { success ->
                                    if (!success) {
                                        showImageWithSpacing = false
                                    }
                                }
                            )
                        }
                    }

                    // 2. Recommendation Status Card (only if on shelf)
                    if (state.onShelf) {
                        item {
                            RecommendationStatusCard(
                                readingStatus = state.book.readingStatus,
                                personalRating = state.book.personalRating,
                                onReadingStatusChange = { status ->
                                    onAction(BookDetailAction.OnReadingStatusChange(status))
                                },
                                onPersonalRatingChange = { rating ->
                                    onAction(BookDetailAction.OnPersonalRatingChange(rating))
                                }
                            )
                        }
                    }

                    // 3. Personal Notes Card (only if on shelf)
                    if (state.onShelf) {
                        item {
                            PersonalNotesCard(
                                notes = state.book.personalNotes,
                                onNotesChange = { notes ->
                                    onAction(BookDetailAction.OnPersonalNotesChange(notes))
                                }
                            )
                        }
                    }

                    // 4. Community Ratings Card
                    item {
                        CommunityRatingsCard(
                            averageRating = state.book.averageRating,
                            ratingCount = state.book.ratingCount
                        )
                    }

                    // 5. Description Card
                    item {
                        DescriptionCard(
                            description = state.book.description
                        )
                    }

                    // 6. Publication Details Card
                    item {
                        PublicationDetailsCard(
                            isbn = state.book.isbn,
                            publisher = state.book.publisher,
                            publishDate = state.book.publishDate,
                            internetArchiveId = state.book.internetArchiveId
                        )
                    }

                    // 7. Languages Card
                    item {
                        LanguagesCard(
                            languages = state.book.languages
                        )
                    }

                    // 8. Purchased Toggle Card
                    item {
                        PurchasedToggleCard(
                            purchased = state.book.purchased,
                            onPurchaseToggle = {
                                onAction(BookDetailAction.OnPurchaseClick)
                            }
                        )
                    }

                    // Note: ShelfActionsCard moved to Scaffold bottomBar (sticky at bottom, always visible)
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
