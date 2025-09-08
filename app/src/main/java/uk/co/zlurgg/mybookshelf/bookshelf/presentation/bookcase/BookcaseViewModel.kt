package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfIdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.ErrorFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class BookcaseViewModel(
    private val repository: BookcaseRepository,
    private val idGenerator: BookshelfIdGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state

    init {
        loadBookshelves()
    }

    fun onAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnAddBookshelfClick -> {
                addBookshelf(action.name, action.style)
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

            is BookcaseAction.ToggleReorderMode -> {
                _state.update { it.copy(isReorderMode = !it.isReorderMode) }
            }

            is BookcaseAction.OnReorderShelf -> {
                reorderShelf(action.bookshelf, action.newPosition)
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
                                errorMessage = ErrorFormatter.formatOperationError("remove shelf", e)
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
                                    errorMessage = ErrorFormatter.formatOperationError("restore shelf", e)
                                )
                            }
                        }
                    }
                }
            }

            is BookcaseAction.OnBookshelfClick -> {
                // no-op: handled by the screen root for navigation
            }
        }
    }

    private fun addBookshelf(name: String, style: ShelfStyle) {
        viewModelScope.launch {
            try {
                val nextPosition = state.value.bookshelves.maxOfOrNull { it.position }?.plus(1) ?: 0
                val newShelf = Bookshelf(
                    id = idGenerator.generateId(),
                    name = name,
                    books = emptyList(),
                    shelfStyle = style,
                    position = nextPosition
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
                        errorMessage = ErrorFormatter.formatOperationError("add shelf", e),
                    )
                }
            }
        }
    }

    private fun loadBookshelves() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            repository.getAllShelves()
                .flatMapLatest { shelves ->
                    // Create a flow of book counts for all shelves
                    if (shelves.isEmpty()) {
                        // If no shelves, just emit the shelves with empty counts
                        flowOf(shelves to emptyMap())
                    } else {
                        // Create individual flows for each shelf's book count
                        val countFlows = shelves.map { shelf ->
                            repository.getBookCountForShelf(shelf.id)
                                .map { count -> shelf.id to count }
                        }
                        
                        // Combine all count flows together
                        combine(countFlows) { counts ->
                            shelves to counts.toMap()
                        }
                    }
                }
                .catch { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = ErrorFormatter.formatOperationError("load shelves", e as Exception),
                        )
                    }
                }
                .collect { (shelves, bookCounts) ->
                    _state.update {
                        it.copy(
                            bookshelves = shelves,
                            bookCounts = bookCounts,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private fun reorderShelf(shelf: Bookshelf, newPosition: Int) {
        viewModelScope.launch {
            try {
                val currentShelves = state.value.bookshelves
                val currentShelfIndex = currentShelves.indexOfFirst { it.id == shelf.id }
                
                if (currentShelfIndex == -1) return@launch
                
                // Create reordered list with updated positions
                val reorderedList = currentShelves.toMutableList().apply {
                    removeAt(currentShelfIndex)
                    add(newPosition.coerceIn(0, size), shelf.copy(position = newPosition))
                }
                
                // Update positions for all affected shelves
                val updatedShelves = reorderedList.mapIndexed { index, bookshelf ->
                    bookshelf.copy(position = index)
                }
                
                // Optimistic UI update
                _state.update { it.copy(bookshelves = updatedShelves) }
                
                // Persist changes
                updatedShelves.forEach { updatedShelf ->
                    repository.updateShelf(updatedShelf)
                }
                
            } catch (e: Exception) {
                // Revert on error by reloading from database
                _state.update {
                    it.copy(
                        errorMessage = ErrorFormatter.formatOperationError("reorder shelves", e)
                    )
                }
                loadBookshelves()
            }
        }
    }
}

