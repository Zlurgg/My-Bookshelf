package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle
import java.util.UUID

class BookcaseViewModel(
    private val repository: BookcaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state

    init {
        loadBookshelves()
    }

    fun onAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnAddBookshelfClick -> {
                addBookshelf(action.name)
            }

            is BookcaseAction.ShowAddDialog -> {
                _state.update { it.copy(showAddDialog = true) }
            }

            is BookcaseAction.ResetOperationState -> {
                _state.update {
                    it.copy(
                        operationSuccess = false,
                        errorMessage = null
                    )
                }
            }

            is BookcaseAction.OnRemoveBookShelf -> {
                // Optimistic UI update
                _state.update {
                    it.copy(
                        bookshelves = it.bookshelves - action.bookshelf,
                        recentlyDeleted = action.bookshelf,
                    )
                }
                // Persist deletion
                viewModelScope.launch {
                    try {
                        repository.removeShelf(action.bookshelf.id)
                    } catch (e: Exception) {
                        // Revert UI on failure
                        _state.update { current ->
                            current.copy(
                                bookshelves = current.bookshelves + action.bookshelf,
                                recentlyDeleted = null,
                                errorMessage = "Failed to remove shelf: ${e.message}"
                            )
                        }
                    }
                }
            }

            is BookcaseAction.OnUndoRemove -> {
                val toRestore = state.value.recentlyDeleted
                if (toRestore != null) {
                    viewModelScope.launch {
                        try {
                            repository.addShelf(toRestore)
                            _state.update { current ->
                                current.copy(
                                    bookshelves = current.bookshelves + toRestore,
                                    recentlyDeleted = null,
                                    operationSuccess = true
                                )
                            }
                        } catch (e: Exception) {
                            _state.update { current ->
                                current.copy(
                                    errorMessage = "Failed to restore shelf: ${e.message}"
                                )
                            }
                        }
                    }
                }
            }

            is BookcaseAction.OnBookshelfClick -> {
                // no-op: handled by the screen root for navigation
            }

            is BookcaseAction.OnChangeShelfMaterial -> {

            }
        }
    }

    private fun addBookshelf(name: String) {
        viewModelScope.launch {
            try {
                val newShelf = Bookshelf(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    books = emptyList(),
                    shelfStyle = ShelfStyle.entries.toTypedArray().random()
                )
                repository.addShelf(newShelf)
                _state.update {
                    it.copy(
                        bookshelves = it.bookshelves + newShelf,
                        isLoading = false,
                        operationSuccess = true,
                        showAddDialog = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to add shelf: ${e.message}",
                    )
                }
            }
        }
    }

    private fun loadBookshelves() {
        viewModelScope.launch {
            _state.update { it.copy() }
            try {
                val shelves = repository.getAllShelves().first()
                _state.update {
                    it.copy(
                        bookshelves = shelves,
                        isLoading = false,
                    )
                }
                // Start observing counts per shelf and update state.bookCounts reactively
                shelves.forEach { shelf ->
                    launch {
                        repository.getBookCountForShelf(shelf.id).collect { count ->
                            _state.update { current ->
                                current.copy(bookCounts = current.bookCounts + (shelf.id to count))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to add shelf: ${e.message}",
                    )
                }
            }
        }
    }
}

