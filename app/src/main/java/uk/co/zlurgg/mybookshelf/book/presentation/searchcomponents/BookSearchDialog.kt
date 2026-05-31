package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.co.zlurgg.mybookshelf.R
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.displayDescription
import uk.co.zlurgg.mybookshelf.book.presentation.components.LoadImage

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BookSearchDialog(
    state: BookSearchState,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onToggleSearchByTitle: () -> Unit,
    onToggleSearchByAuthor: () -> Unit,
    onToggleSearchBySubject: () -> Unit,
    onToggleSafeSearch: () -> Unit,
    onBookClick: (Book) -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    trailingContent: @Composable (book: Book, isExisting: Boolean) -> Unit,
    // Hoisted so callers can preserve scroll across the search → detail → back
    // round trip. The AlertDialog's own saveable scope is destroyed when the
    // platform window is torn down on navigation, so an internal state would
    // reset to top on return.
    lazyListState: LazyListState = rememberLazyListState(),
    // Gates BOTH the toggle UI visibility AND whether state.libraryScopeEnabled
    // is honoured for display branching. When false (Library tab), display
    // treats library-scope as off: Google attribution shows, remote empty-state
    // renders. This prevents a persisted libraryScope=true from the Bookshelf
    // tab silently re-skinning the Library tab dialog (reviewer N2). Anyone
    // flipping this to false to suppress just the toggle UI must understand
    // they are also opting into remote-only display semantics.
    showLibraryScopeToggle: Boolean = false,
    onToggleLibraryScope: () -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard = remember(keyboardController, focusManager) {
        {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    // Display-side gate: when the caller hasn't opted into the scope toggle,
    // treat library scope as off so a leaked persisted flag from the other
    // tab's preference doesn't break attribution/empty-state on this tab.
    val effectiveLibraryScope = state.libraryScopeEnabled && showLibraryScopeToggle
    // Race-guard: Load More only renders while the typed query still matches
    // the submitted one. Diverging typed input hides the affordance.
    val canShowLoadMore = (state.canLoadMore || state.isLoadingMore) &&
        state.query.trim() == state.lastSubmittedQuery.trim()
    // Recomputed only when the result set changes; avoids a linear scan on
    // every recomposition (typing, scroll, focus changes) of the dialog.
    // Library-scope results are local, never Google-sourced, so the
    // attribution is suppressed in that mode regardless of the results.
    val showGoogleAttribution = remember(state.results, effectiveLibraryScope) {
        !effectiveLibraryScope && state.results.any { it.provider == BookProvider.GOOGLE_BOOKS }
    }
    AlertDialog(
        onDismissRequest = {
            dismissKeyboard()
            if (!state.isLoading) onDismiss()
        },
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                BookSearchBar(
                    searchQuery = state.query,
                    onSearchQueryChange = onQueryChange,
                    onImeSearch = {
                        dismissKeyboard()
                        onSubmitSearch()
                    },
                    onSubmitSearch = {
                        dismissKeyboard()
                        onSubmitSearch()
                    },
                    onClear = onClearSearch,
                )

                Spacer(modifier = Modifier.height(4.dp))

                SearchFilters(
                    searchByTitle = state.searchByTitle,
                    searchByAuthor = state.searchByAuthor,
                    searchBySubject = state.searchBySubject,
                    titleEnabled = state.canToggleTitle,
                    authorEnabled = state.canToggleAuthor,
                    subjectEnabled = state.canToggleSubject,
                    safeSearchEnabled = state.safeSearchEnabled,
                    onToggleTitle = onToggleSearchByTitle,
                    onToggleAuthor = onToggleSearchByAuthor,
                    onToggleSubject = onToggleSearchBySubject,
                    onToggleSafeSearch = onToggleSafeSearch,
                    showLibraryScopeToggle = showLibraryScopeToggle,
                    libraryScopeEnabled = state.libraryScopeEnabled,
                    onToggleLibraryScope = onToggleLibraryScope,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Inline error message (distinct from empty results)
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (!state.isLoading && state.hasSearched) {
                    val resultText = if (state.filteredCount > 0) {
                        stringResource(
                            R.string.safe_search_filtered,
                            state.results.size,
                            state.filteredCount
                        )
                    } else if (state.results.isNotEmpty()) {
                        stringResource(R.string.search_results_found, state.results.size)
                    } else {
                        null
                    }
                    resultText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 2.dp
                            )
                        )
                    }
                    // Google Books TOS attribution — shown alongside the result
                    // count so it's visible without scrolling to the end of the list.
                    if (showGoogleAttribution && state.results.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.powered_by_google),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 6.dp
                            )
                        )
                    }
                }

                val isEmptyResult = state.results.isEmpty() &&
                    !state.isLoading &&
                    state.hasSearched && state.errorMessage == null
                val showLibraryEmptyState = isEmptyResult && effectiveLibraryScope
                val showRemoteEmptyState = isEmptyResult &&
                    !effectiveLibraryScope && state.lastSubmittedQuery.isNotBlank()
                when {
                    showLibraryEmptyState || showRemoteEmptyState -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val (headline, subline) = when {
                                showLibraryEmptyState && state.lastSubmittedQuery.isBlank() ->
                                    stringResource(R.string.search_empty_library_hint) to null
                                showLibraryEmptyState ->
                                    stringResource(
                                        R.string.search_no_library_results,
                                        state.lastSubmittedQuery
                                    ) to null
                                else ->
                                    stringResource(
                                        R.string.search_no_results_found,
                                        state.lastSubmittedQuery
                                    ) to stringResource(R.string.search_no_results_suggestion)
                            }
                            Text(
                                text = headline,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (subline != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = subline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(state.results) { index, book ->
                                val isExisting = state.existingBookIds.contains(book.id)
                                val stripeColor = if (index % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(stripeColor)
                                        .clickable { onBookClick(book) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (book.imageUrl.isNotBlank()) {
                                        LoadImage(
                                            imageUrl = book.imageUrl,
                                            title = book.title,
                                            modifier = Modifier
                                                .width(56.dp)
                                                .aspectRatio(2f / 3f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = book.authors.joinToString(", "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        book.firstPublishYear?.let { year ->
                                            Text(
                                                text = year,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        book.displayDescription()?.let { preview ->
                                            Text(
                                                text = preview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    trailingContent(book, isExisting)
                                }
                            }
                            if (canShowLoadMore) {
                                item(key = LOAD_MORE_ITEM_KEY) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !state.isLoadingMore) {
                                                dismissKeyboard()
                                                onLoadMore()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (state.isLoadingMore) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.search_load_more),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) { Text(stringResource(id = R.string.action_close)) }
        }
    )
}

// LazyColumn key for the load-more footer. Stable so the item doesn't recompose
// out of the saver across spinner ↔ button state changes.
private const val LOAD_MORE_ITEM_KEY = "__load_more_footer__"
