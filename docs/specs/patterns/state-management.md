# State Management Pattern

Patterns for managing UI state in ViewModels using StateFlow and immutable state.

## Core Pattern

ViewModels expose a single immutable state via StateFlow and receive user interactions via an Action sealed interface:

```kotlin
data class BookcaseState(
    val bookshelves: List<Bookshelf> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val isReorderMode: Boolean = false,
    // ...
)

sealed interface BookcaseAction {
    data class OnBookshelfClick(val bookshelf: Bookshelf) : BookcaseAction
    data class OnAddBookshelfClick(val name: String, val style: ShelfStyle) : BookcaseAction
    data class OnRemoveBookShelf(val bookshelf: Bookshelf) : BookcaseAction
    data object ToggleReorderMode : BookcaseAction
    // ...
}
```

## State Field Types

| Field Type | Purpose | UI Handling |
|------------|---------|-------------|
| `errorMessage: String?` | Persistent error | Inline error display |
| `operationSuccess: Boolean` | Transient success | Snackbar, cleared via action |
| `isLoading: Boolean` | Loading indicator | Show spinner/shimmer |
| `showXxxDialog: Boolean` | Dialog visibility | Conditional composition |

## ViewModel Implementation

```kotlin
class BookcaseViewModel(
    private val bookcaseUseCases: BookcaseUseCases,
    private val authUseCases: AuthUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(BookcaseState())
    val state: StateFlow<BookcaseState> = _state.asStateFlow()

    init {
        loadBookshelves()
    }

    fun onAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnAddBookshelfClick -> addBookshelf(action.name, action.style)
            is BookcaseAction.ToggleReorderMode -> {
                _state.update { it.copy(isReorderMode = !it.isReorderMode) }
            }
            // ...
        }
    }

    companion object {
        private const val TAG = "BookcaseVM"
    }
}
```

## State Update Patterns

### Simple Update
```kotlin
_state.update { it.copy(isLoading = true) }
```

### Named State Helpers (for complex updates)
```kotlin
private fun BookcaseState.withError(error: DataError, operation: String): BookcaseState {
    return copy(
        isLoading = false,
        errorMessage = ErrorFormatter.formatDataErrorMessage(error, operation)
    )
}

private fun BookcaseState.withShelfAdded(newShelf: Bookshelf): BookcaseState {
    return copy(
        bookshelves = bookshelves + newShelf,
        isLoading = false,
        operationSuccess = true,
        showAddDialog = false
    )
}
```

### Optimistic UI Updates
```kotlin
// Immediately update UI, then persist
_state.update { it.withShelfDeleted(shelf) }

viewModelScope.launch {
    when (val result = shelfOperations.deleteShelf(shelf.id)) {
        is Result.Success -> { /* Already applied */ }
        is Result.Error -> {
            // Revert on failure
            _state.update { it.withShelfDeleteError(shelf, result.error) }
        }
    }
}
```

## Handler Pattern (Large ViewModels)

When a ViewModel grows too large, extract operation groups into handler classes:

```kotlin
class ShelfOperationsHandler(
    private val useCases: BookcaseUseCases
) {
    suspend fun createShelf(...): Result<Bookshelf, DataError.Local> { ... }
    suspend fun deleteShelf(id: String): Result<Unit, DataError.Local> { ... }
    suspend fun restoreShelf(shelf: Bookshelf): Result<Unit, DataError.Local> { ... }
}

// ViewModel receives handlers via DI
class BookcaseViewModel(
    private val shelfOperations: ShelfOperationsHandler,
    private val shelfManagement: ShelfManagementHandler,
    private val bookClubOperations: BookClubOperationsHandler,
    ...
)
```

## Testing State Changes

```kotlin
@Test
fun `ShowAddDialog action toggles dialog visibility`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val stateHelper = viewModel.state.testHelper(this)

    val stateAfterAction = stateHelper.executeAndGetState {
        viewModel.onAction(BookcaseAction.ShowAddDialog(true))
    }

    assertTrue("Dialog should be visible", stateAfterAction?.showAddDialog == true)
    stateHelper.cleanup()
}
```
