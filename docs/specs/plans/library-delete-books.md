# Library: Delete Books

Gallery-style multi-select deletion of books from the library with confirmation dialog. Full removal of book data (entity, cross-refs, notes, ratings, reading status).

## Problem

There is no way to permanently delete a book from the app. `BookDao` has no delete operations. When a shelf is deleted, only the cross-refs are removed — the `BookEntity` remains. Users need a way to permanently remove books they no longer want, including all personal metadata (ratings, notes, reading status, purchase info).

## Architecture Decisions

### Selection mode with non-removable filtering

When the user enters selection mode, the library filters to show only deletable books. Books on club shelves are excluded from the view entirely during selection mode. Search, sort, and filter controls are hidden — selection mode is a focused "pick and delete" flow, not a browsing flow. Exiting selection mode restores the previous filtered view automatically (search/sort/filter state is untouched in `LibraryState`).

**Why filter rather than disable/grey out:** A greyed-out book with a tooltip explaining "remove from club first" adds UX complexity and raises questions. Filtering is simpler — the user sees only what they can act on. If the filtered set is smaller than expected, that's a natural prompt to investigate.

**Why hide search/sort/filter during selection mode:** Selection mode is a destructive operation flow. Mixing browsing controls with deletion creates confusing UX — "am I filtering what I see or what I'll delete?" Keeping it simple: you see all deletable books, you pick, you delete. No ambiguity.

### getNonRemovableBookIds in BookRepository

Library needs to know which books can't be deleted without knowing *why*. `BookRepository` exposes:

```kotlin
fun getNonRemovableBookIds(): Flow<Set<String>>
```

The implementation queries `CrossRefDao` + `BookshelfEntity.isBookClub` — both in `core/`. Library never imports from `bookclub/` or `bookshelf/`. The domain contract is "these books can't be removed" — the club shelf reason is an implementation detail.

```sql
-- CrossRefDao: books on at least one club shelf
SELECT DISTINCT cr.bookId FROM BookshelfBookCrossRef cr
INNER JOIN BookshelfEntity s ON cr.shelfId = s.id
WHERE s.isBookClub = 1
```

### Batch deletion in a Room @Transaction

Deletion removes cross-refs first (no FK constraints, but keeps data consistent), then deletes `BookEntity` rows. All in one transaction to avoid partial state.

```kotlin
// BookshelfDao (composite DAO) — cross-cutting transaction boundary.
// Lives here because it coordinates operations from both BookDao and CrossRefDao.
// Individual DAOs remain focused; BookshelfDao is where multi-table transactions go.
@Transaction
suspend fun deleteBooks(bookIds: List<String>) {
    deleteAllCrossRefsForBooks(bookIds)
    deleteBooksById(bookIds)
}
```

### SQLite IN clause chunking in repository

SQLite has a hard limit of 999 bind arguments in an `IN` clause. `BookRepositoryImpl.deleteBooks()` chunks the ID list into batches of 500 and runs each through the DAO transaction. This is the data layer's responsibility — callers should not need to know about SQLite limits.

```kotlin
override suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        bookIds.chunked(SQLITE_BATCH_SIZE).forEach { batch ->
            dao.deleteBooks(batch)
        }
    }
}

companion object {
    private const val SQLITE_BATCH_SIZE = 500
}
```

### No undo / snackbar pattern

The user specified a confirmation dialog rather than undo. This is appropriate — deletion destroys personal metadata (notes, ratings) that can't be recovered. The confirmation dialog lists the count and warns about permanent loss.

### deleteBooks on BookRepository

New method on the existing `BookRepository` interface:

```kotlin
suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local>
```

`BookRepositoryImpl` wraps the DAO transaction in `ErrorMapper.safeSuspendCall()` with chunking.

Note: The spec documents a `SyncingBookRepository` decorator that overrides `deleteBook`. If/when sync decorators exist, `deleteBooks` should be added to the overridden methods list. For now (pre-release, no sync decorator in code), this is just the direct implementation.

### UseCase enforces the club book invariant (defense-in-depth)

The UI filters non-removable books out of selection mode, but the `DeleteBooksFromLibraryUseCaseImpl` also cross-checks against `getNonRemovableBookIds()` and rejects any overlap. This makes the domain layer the enforcement point, not just the UI.

If a caller passes a club book ID (via bug, future feature, or test), the use case returns `Result.Error` rather than silently deleting shared data.

```kotlin
class DeleteBooksFromLibraryUseCaseImpl(
    private val bookRepository: BookRepository
) : DeleteBooksFromLibraryUseCase {

    override suspend operator fun invoke(
        bookIds: List<String>
    ): Result<Unit, DataError.Local> {
        val nonRemovable = bookRepository.getNonRemovableBookIds().first()
        val blocked = bookIds.filter { it in nonRemovable }
        if (blocked.isNotEmpty()) {
            Timber.tag(TAG).w("Blocked deletion of %d non-removable books", blocked.size)
            return Result.Error(DataError.Local.PROTECTED_RESOURCE)
        }
        return bookRepository.deleteBooks(bookIds)
    }

    companion object {
        private const val TAG = "DeleteBooksUC"
    }
}
```

Note: This requires adding `PROTECTED_RESOURCE` to `DataError.Local` if it doesn't exist (or use the most appropriate existing variant). Check at implementation time.

### deletableBooks is a computed property, not stored state

`deletableBooks` is always `allBooks.filter { it.id !in nonRemovableBookIds }`. Storing it separately would require manual `updateDeletableBooks()` calls whenever either input changes — miss one and the list goes stale.

Instead, compute on read:

```kotlin
data class LibraryState(
    // ... existing fields ...
    val isSelectionMode: Boolean = false,
    val selectedBookIds: Set<String> = emptySet(),
    val nonRemovableBookIds: Set<String> = emptySet(),
    val showDeleteConfirmation: Boolean = false,
) {
    val deletableBooks: List<Book> get() = allBooks.filter { it.id !in nonRemovableBookIds }
}
```

This eliminates the staleness class entirely. Both `allBooks` and `nonRemovableBookIds` are updated via separate flows; the computed property always reflects the current combination.

## Implementation Steps

### Phase 1: Data layer — DAO + Repository

1. Add to `BookDao`:
   ```kotlin
   @Query("DELETE FROM BookEntity WHERE id IN (:bookIds)")
   suspend fun deleteBooksById(bookIds: List<String>)
   ```

2. Add to `CrossRefDao`:
   ```kotlin
   @Query("DELETE FROM BookshelfBookCrossRef WHERE bookId IN (:bookIds)")
   suspend fun deleteAllCrossRefsForBooks(bookIds: List<String>)

   @Query("""
       SELECT DISTINCT cr.bookId FROM BookshelfBookCrossRef cr
       INNER JOIN BookshelfEntity s ON cr.shelfId = s.id
       WHERE s.isBookClub = 1
   """)
   fun getBookIdsOnClubShelves(): Flow<Set<String>>
   ```

3. Add `@Transaction` method to `BookshelfDao` (composite DAO):
   ```kotlin
   // Cross-cutting transaction: coordinates BookDao.deleteBooksById + CrossRefDao.deleteAllCrossRefsForBooks.
   // Lives on the composite DAO because it spans both focused DAOs.
   @Transaction
   suspend fun deleteBooks(bookIds: List<String>) {
       deleteAllCrossRefsForBooks(bookIds)
       deleteBooksById(bookIds)
   }
   ```

4. Add to `BookRepository` interface:
   ```kotlin
   suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local>
   fun getNonRemovableBookIds(): Flow<Set<String>>
   ```

5. Implement in `BookRepositoryImpl` with SQLite chunking:
   ```kotlin
   override suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local> {
       return ErrorMapper.safeSuspendCall(TAG) {
           bookIds.chunked(SQLITE_BATCH_SIZE).forEach { batch ->
               dao.deleteBooks(batch)
           }
       }
   }

   override fun getNonRemovableBookIds(): Flow<Set<String>> {
       return dao.getBookIdsOnClubShelves()
   }
   ```

6. Update `MockBookRepository` in test utilities with stub implementations.

### Phase 2: Domain layer — UseCase

7. Add `PROTECTED_RESOURCE` to `DataError.Local` if not already present (check at implementation time).

8. Create `DeleteBooksFromLibraryUseCase` interface in `library/domain/usecase/`:
   ```kotlin
   interface DeleteBooksFromLibraryUseCase {
       suspend operator fun invoke(bookIds: List<String>): Result<Unit, DataError.Local>
   }
   ```

9. Create `DeleteBooksFromLibraryUseCaseImpl` with club book guard:
   ```kotlin
   class DeleteBooksFromLibraryUseCaseImpl(
       private val bookRepository: BookRepository
   ) : DeleteBooksFromLibraryUseCase {

       override suspend operator fun invoke(
           bookIds: List<String>
       ): Result<Unit, DataError.Local> {
           val nonRemovable = bookRepository.getNonRemovableBookIds().first()
           val blocked = bookIds.filter { it in nonRemovable }
           if (blocked.isNotEmpty()) {
               Timber.tag(TAG).w("Blocked deletion of %d non-removable books", blocked.size)
               return Result.Error(DataError.Local.PROTECTED_RESOURCE)
           }
           return bookRepository.deleteBooks(bookIds)
       }

       companion object {
           private const val TAG = "DeleteBooksUC"
       }
   }
   ```

10. Create `GetNonRemovableBookIdsUseCase` interface + impl in `library/domain/usecase/`:
    ```kotlin
    interface GetNonRemovableBookIdsUseCase {
        operator fun invoke(): Flow<Set<String>>
    }

    class GetNonRemovableBookIdsUseCaseImpl(
        private val bookRepository: BookRepository
    ) : GetNonRemovableBookIdsUseCase {
        override operator fun invoke(): Flow<Set<String>> {
            return bookRepository.getNonRemovableBookIds()
        }
    }
    ```

11. Update `LibraryUseCases`:
    ```kotlin
    data class LibraryUseCases(
        val getAllLibraryBooks: GetAllLibraryBooksUseCase,
        val deleteBooks: DeleteBooksFromLibraryUseCase,
        val getNonRemovableBookIds: GetNonRemovableBookIdsUseCase
    )
    ```

12. Update `LibraryModule` DI with new use case bindings.

### Phase 3: Presentation layer — State + Actions

13. Add selection state to `LibraryState`:
    ```kotlin
    data class LibraryState(
        // ... existing fields ...
        val isSelectionMode: Boolean = false,
        val selectedBookIds: Set<String> = emptySet(),
        val nonRemovableBookIds: Set<String> = emptySet(),
        val showDeleteConfirmation: Boolean = false,
    ) {
        val deletableBooks: List<Book> get() = allBooks.filter { it.id !in nonRemovableBookIds }
    }
    ```

14. Add selection actions to `LibraryAction`:
    ```kotlin
    sealed interface LibraryAction {
        // ... existing actions ...
        data object OnToggleSelectionMode : LibraryAction
        data class OnToggleBookSelection(val bookId: String) : LibraryAction
        data object OnSelectAll : LibraryAction
        data object OnDeselectAll : LibraryAction
        data object OnDeleteSelectedClick : LibraryAction  // shows confirmation
        data object OnConfirmDelete : LibraryAction
        data object OnDismissDeleteDialog : LibraryAction
    }
    ```

### Phase 4: Presentation layer — ViewModel

15. Add `nonRemovableBookIds` observation to `LibraryViewModel`:
    ```kotlin
    init {
        loadTidyMode()
        observeBooks()
        observeNonRemovableBookIds()
        observeDebouncedQuery()
    }

    private fun observeNonRemovableBookIds() {
        viewModelScope.launch {
            libraryUseCases.getNonRemovableBookIds().collectLatest { ids ->
                _state.update { state ->
                    val deletableIds = state.allBooks.map { it.id }.toSet() - ids
                    state.copy(
                        nonRemovableBookIds = ids,
                        // Prune any selected books that became non-removable (e.g. added to club
                        // by sync while selection mode is active). Prevents stale selection count.
                        selectedBookIds = state.selectedBookIds.intersect(deletableIds)
                    )
                }
            }
        }
    }
    ```
    Note: No stored `deletableBooks` list needed — it's a computed property that automatically reflects current `allBooks` and `nonRemovableBookIds`. The `selectedBookIds` intersect prevents a UX glitch where the count shows "1 selected" but nothing is visually checked.

16. Add selection mode handling in `onAction()`:
    - `OnToggleSelectionMode`: Toggle `isSelectionMode`, clear `selectedBookIds`
    - `OnToggleBookSelection`: Add/remove from `selectedBookIds`
    - `OnSelectAll`: Set `selectedBookIds` to all `deletableBooks` IDs
    - `OnDeselectAll`: Clear `selectedBookIds`
    - `OnDeleteSelectedClick`: Set `showDeleteConfirmation = true`
    - `OnConfirmDelete`: Call `deleteBooks` use case, exit selection mode on success
    - `OnDismissDeleteDialog`: Set `showDeleteConfirmation = false`

17. Add delete operation:
    ```kotlin
    private fun deleteSelectedBooks() {
        val selectedIds = _state.value.selectedBookIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = libraryUseCases.deleteBooks(selectedIds)) {
                is Result.Success -> {
                    _state.update { it.copy(
                        isSelectionMode = false,
                        selectedBookIds = emptySet(),
                        showDeleteConfirmation = false,
                        isLoading = false
                    )}
                    // allBooks flow will automatically update via Room
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        showDeleteConfirmation = false,
                        isLoading = false,
                        errorMessage = ErrorFormatter.formatDataErrorMessage(
                            result.error, "delete books"
                        )
                    )}
                }
            }
        }
    }
    ```

### Phase 5: Presentation layer — UI

18. Add "Select" icon button to `LibraryScreen` top bar (next to tidy mode toggle):
    - Only visible when `allBooks` is not empty and not in search dialog
    - Icon: `Icons.Default.CheckCircleOutline` or similar
    - Dispatches `OnToggleSelectionMode`

19. Selection mode top bar changes:
    - Title changes to selection count: "X selected"
    - Show "Select All" / "Deselect All" action
    - Show "Delete" action (enabled only when `selectedBookIds` is not empty)
    - Show "Cancel" / close button to exit selection mode
    - Hide tidy mode toggle, search bar, sort chips, and filter chips during selection mode

20. Book display changes in selection mode:
    - Show books from `state.deletableBooks` instead of `state.filteredBooks`
    - Overlay a checkbox on each book cover
    - Checkbox checked state driven by `selectedBookIds.contains(book.id)`
    - Book click dispatches `OnToggleBookSelection(book.id)` instead of `OnBookClick`
    - Disable navigation to book detail during selection mode

21. Add `BackHandler` in `LibraryScreen` for selection mode:
    ```kotlin
    BackHandler(enabled = state.isSelectionMode) {
        onAction(LibraryAction.OnToggleSelectionMode)
    }
    ```
    This intercepts the system back gesture/button to exit selection mode instead of navigating back.

22. Confirmation dialog:
    ```kotlin
    @Composable
    fun DeleteBooksConfirmationDialog(
        bookCount: Int,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    )
    ```
    - Title: "Delete X book(s)?"
    - Body: "This will permanently remove these books and all their data (notes, ratings, reading status). This cannot be undone."
    - Buttons: "Cancel" / "Delete" (destructive style)

23. Hide the FAB (from add-book plan) during selection mode.

24. Empty deletable state: When `deletableBooks` is empty in selection mode, show a message: "No books can be deleted. Remove books from book clubs first." with a button to exit selection mode.

### Phase 6: Tests

25. Integration test: `BookDao.deleteBooksById` removes entities
26. Integration test: `BookshelfDao.deleteBooks` transaction removes cross-refs and entities atomically
27. Integration test: `CrossRefDao.getBookIdsOnClubShelves` returns correct IDs (books on club shelves only, not personal shelves)
28. Unit test: `DeleteBooksFromLibraryUseCaseImpl`:
    - Delegates to repository for valid IDs
    - Rejects and returns `PROTECTED_RESOURCE` when any ID is non-removable
    - Logs warning with blocked count
29. Unit test: `BookRepositoryImpl.deleteBooks` chunks large lists (verify >500 IDs are split into batches)
30. Unit test: `LibraryViewModel` selection mode:
    - Toggle selection mode sets `isSelectionMode`, clears `selectedBookIds`
    - `deletableBooks` computed property excludes non-removable books
    - `deletableBooks` updates reactively when `allBooks` or `nonRemovableBookIds` change (no staleness)
    - `selectedBookIds` pruned when `nonRemovableBookIds` updates (no stale selection count)
    - Select/deselect updates `selectedBookIds`
    - SelectAll uses `deletableBooks` IDs, not `allBooks` IDs
    - Confirm delete calls use case and exits selection mode
    - Error handling uses `ErrorFormatter`, not hardcoded strings
    - BackHandler exits selection mode
31. Unit test: `LibraryViewModel` - exiting selection mode clears selected IDs, preserves search/sort/filter state

## Edge Cases

| Case | Handling |
|------|----------|
| All books are on club shelves | Selection mode shows empty state: "No books can be deleted. Remove books from book clubs first." with exit button |
| Book removed from club shelf while in selection mode | `nonRemovableBookIds` is a Flow — `deletableBooks` computed property updates reactively; book appears in the deletable list |
| Book added to club shelf while selected | `nonRemovableBookIds` updates, `selectedBookIds` pruned via intersect — book vanishes from list and selection count stays accurate |
| Large batch delete (100+ books) | `BookRepositoryImpl` chunks into batches of 500 to stay within SQLite's 999 bind arg limit |
| Selection mode entered while search/sort active | Search/sort/filter controls hidden; selection mode shows flat `deletableBooks` list. Filter state preserved in `LibraryState`, restored on exit |
| Rapid double-tap on Delete button | `isLoading = true` disables the delete button; dialog dismissed immediately |
| Book on personal shelf + no club | Fully deletable — cross-refs to personal shelves are cleaned up in the transaction |
| Orphaned book (not on any shelf) | Fully deletable — no cross-refs to clean up |
| Back button during selection mode | `BackHandler` intercepts — exits selection mode (clears selection), doesn't navigate back |
| Bug/future caller passes club book ID to UseCase | UseCase guard rejects with `PROTECTED_RESOURCE` error + Timber warning. Defense-in-depth beyond UI filtering |

## Security Considerations

- Confirmation dialog prevents accidental deletion
- No network calls involved (local-only operation)
- Club books protected at two levels: UI filtering (selection mode) + domain guard (UseCase)
- Room `@Transaction` ensures atomic operation per batch. Note: chunking means multiple transactions — if a mid-loop failure occurs (e.g. disk full), earlier batches are already committed. Accepted risk: near-zero probability on local SQLite for a personal app, and the UseCase guard + UI filtering prevent the more dangerous scenario (deleting club books)
- SQLite batch chunking prevents query failures on large selections

## Dependency Flow

```
LibraryViewModel
    -> GetNonRemovableBookIdsUseCase -> BookRepository -> CrossRefDao + BookshelfEntity (core/)
    -> DeleteBooksFromLibraryUseCase -> BookRepository -> BookshelfDao.deleteBooks() (core/)
```

Library depends only on `book/` (domain) and `core/` (data). No imports from `bookshelf/`, `bookclub/`, or `bookcase/`.

## Files Changed/Created

| Action | File |
|--------|------|
| **Edit** | `core/data/database/dao/BookDao.kt` (add `deleteBooksById`) |
| **Edit** | `core/data/database/dao/CrossRefDao.kt` (add `deleteAllCrossRefsForBooks`, `getBookIdsOnClubShelves`) |
| **Edit** | `core/data/database/dao/BookshelfDao.kt` (add `deleteBooks` @Transaction with comment) |
| **Edit** | `core/domain/error/DataError.kt` (add `PROTECTED_RESOURCE` to `DataError.Local` if needed) |
| **Edit** | `book/domain/repository/BookRepository.kt` (add `deleteBooks`, `getNonRemovableBookIds`) |
| **Edit** | `book/data/repository/BookRepositoryImpl.kt` (implement with chunking) |
| **Create** | `library/domain/usecase/DeleteBooksFromLibraryUseCase.kt` |
| **Create** | `library/domain/usecase/DeleteBooksFromLibraryUseCaseImpl.kt` (with club book guard) |
| **Create** | `library/domain/usecase/GetNonRemovableBookIdsUseCase.kt` |
| **Create** | `library/domain/usecase/GetNonRemovableBookIdsUseCaseImpl.kt` |
| **Edit** | `library/domain/usecase/LibraryUseCases.kt` |
| **Edit** | `library/di/LibraryModule.kt` |
| **Edit** | `library/presentation/LibraryState.kt` (selection state + computed `deletableBooks`) |
| **Edit** | `library/presentation/LibraryAction.kt` |
| **Edit** | `library/presentation/LibraryViewModel.kt` |
| **Edit** | `library/presentation/LibraryScreen.kt` (selection mode UI, top bar, checkboxes, BackHandler) |
| **Create** | `library/presentation/components/DeleteBooksConfirmationDialog.kt` |
| **Edit** | `app/src/test/.../testutil/mocks/MockBookRepository.kt` (add stubs) |
