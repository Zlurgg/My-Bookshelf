# Fix: Scope book removal permissions on book club shelves

## Bug

Any signed-in club member can remove any book from a book club shelf. Only the club owner should be able to remove any book. Members should be able to add books (collaborative contribution) and remove books they personally added (undo mistakes), but not remove other members' contributions.

## Root Cause

Three layers all missing permission checks:

1. **UI** (`BookDetailScreen.kt:95`) — `ShelfActionsCard` visibility only checks `isSignedIn`, not ownership or attribution
2. **State** — `BookDetailsWithShelfStatus` and `BookDetailState` carry `isBookClub`/`clubCode` but never `clubCreatorId` or `addedByUserId`, so the UI has no permission info to check
3. **UseCase** — `RemoveBookFromShelfUseCaseImpl` loads the shelf (has `clubCreatorId`) but never validates the current user against it
4. **Data** — `BookshelfBookCrossRef` has no `addedByUserId` field, so there's no local record of who added a book to a shelf

## Product Rules

- Members CAN add books to club shelves (collaborative contribution)
- Members CAN remove books they personally added (undo mistakes)
- Only the owner CAN remove any book (moderation)
- Guests cannot interact with club shelves at all (existing behavior, preserved)

## Fix — 5 Layers

### Layer 1: Cross-Ref Attribution (track who added each book)

**`BookshelfBookCrossRef.kt`** — Add `addedByUserId` field:
```kotlin
@Entity(primaryKeys = ["shelfId", "bookId"])
data class BookshelfBookCrossRef(
    val shelfId: String,
    val bookId: String,
    val addedAt: Long,
    val addedByUserId: String? = null  // NEW — null for personal shelves
)
```

**`MyBookshelfRoomDatabase.kt`** — Bump version, destructive fallback (no existing users):
```kotlin
@Database(
    // ...
    version = 4,  // WAS 3
    exportSchema = true
)
```
Plus `fallbackToDestructiveMigration()` in the database builder (if not already present).

### Layer 2: Populate `addedByUserId` at write time

**`BookshelfRepository.kt`** — Widen `addBookToShelf` signature:
```kotlin
suspend fun addBookToShelf(shelfId: String, bookId: String, addedByUserId: String? = null): Result<Unit, DataError.Local>
```

**`BookshelfRepositoryImpl.kt:20`** — Pass through to cross-ref:
```kotlin
override suspend fun addBookToShelf(shelfId: String, bookId: String, addedByUserId: String?): Result<Unit, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        val now = timeProvider.currentTimeMillis()
        dao.upsertCrossRef(
            BookshelfBookCrossRef(
                shelfId = shelfId,
                bookId = bookId,
                addedAt = now,
                addedByUserId = addedByUserId
            )
        )
    }
}
```

**`AddBookToShelfUseCaseImpl.kt`** — Inject `CurrentUserProvider`, pass userId for club shelves:
```kotlin
class AddBookToShelfUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val clubOperations: ClubOperations,
    private val timeProvider: TimeProvider,
    private val currentUserProvider: CurrentUserProvider,  // NEW
) : AddBookToShelfUseCase {
    // ... existing logic ...

    // Line 77 — pass userId for club shelves:
    val userId = if (shelf.isBookClub) currentUserProvider.getCurrentUserId() else null
    when (val addResult = bookshelfRepository.addBookToShelf(shelfId, book.id, userId)) {
        // ...
    }
}
```

**Sync-from-Firestore paths** — Both create cross-refs and have `bookDto.addedBy` available:

`BookClubSyncRepositoryImpl.kt:142`:
```kotlin
val crossRef = BookshelfBookCrossRef(
    shelfId = localShelfId,
    bookId = book.id,
    addedAt = timeProvider.currentTimeMillis(),
    addedByUserId = bookDto.addedBy.ifEmpty { null }  // NEW
)
```

`BookClubRepositoryHelper.kt:116`:
```kotlin
val crossRef = BookshelfBookCrossRef(
    shelfId = shelfId,
    bookId = book.id,
    addedAt = timeProvider.currentTimeMillis(),
    addedByUserId = bookDto.addedBy.ifEmpty { null }  // NEW
)
```

### Layer 3: Data Flow (thread `clubCreatorId` + `addedByUserId` to UI)

**`BookshelfRepository.kt`** — Add query for cross-ref attribution:
```kotlin
suspend fun getAddedByUserId(shelfId: String, bookId: String): Result<String?, DataError.Local>
```

**`BookshelfRepositoryImpl.kt`** — Implement via DAO:
```kotlin
override suspend fun getAddedByUserId(shelfId: String, bookId: String): Result<String?, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        dao.getAddedByUserId(shelfId, bookId)
    }
}
```

**`CrossRefDao.kt`** — Add query:
```kotlin
@Query("SELECT addedByUserId FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND bookId = :bookId")
suspend fun getAddedByUserId(shelfId: String, bookId: String): String?
```

**`BookDetailsWithShelfStatus.kt`** — Add fields:
```kotlin
data class BookDetailsWithShelfStatus(
    val book: Book?,
    val isOnShelf: Boolean,
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    val clubCreatorId: String? = null,   // NEW
    val addedByUserId: String? = null    // NEW
)
```

**`GetBookDetailsUseCaseImpl.kt`** — Pass through (shelf already loaded at line 26, zero extra cost for `clubCreatorId`; one DAO query for `addedByUserId`).

**Known limitation:** `addedByUserId` is fetched once outside the reactive `combine` pipeline. If a background sync changes the cross-ref while the detail screen is open, the permission won't update until the screen is re-entered. Acceptable — the scenario (book removed and re-added by a different user while viewing) is extremely unlikely, and the UseCase guard (Layer 5) always reads fresh data as a backstop.
```kotlin
class GetBookDetailsUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookcaseRepository: BookcaseRepository
) : GetBookDetailsUseCase {

    override suspend operator fun invoke(bookId: String, shelfId: String?): Flow<BookDetailsWithShelfStatus> {
        val shelf = // ... existing ...
        val isBookClub = shelf?.isBookClub ?: false
        val clubCode = shelf?.clubCode
        val clubCreatorId = shelf?.clubCreatorId  // NEW

        // Get who added this book to the shelf (for club remove permissions)
        val addedByUserId = if (shelfId != null && isBookClub) {
            when (val result = bookshelfRepository.getAddedByUserId(shelfId, bookId)) {
                is Result.Success -> result.data
                is Result.Error -> null
            }
        } else null

        // ... existing combine logic ...
        BookDetailsWithShelfStatus(
            book = book,
            isOnShelf = isOnShelf,
            isBookClub = isBookClub,
            clubCode = clubCode,
            clubCreatorId = clubCreatorId,   // NEW
            addedByUserId = addedByUserId    // NEW
        )
    }
}
```

**`BookDetailState.kt`** — Add fields + derived properties:
```kotlin
data class BookDetailState(
    // ... existing fields ...
    val clubCreatorId: String? = null,   // NEW
    val addedByUserId: String? = null,   // NEW
) {
    val isSignedIn: Boolean get() = currentUserId != null
    val isClubOwner: Boolean get() = isBookClub && currentUserId != null && currentUserId == clubCreatorId
    val isAddedByCurrentUser: Boolean get() = isBookClub && currentUserId != null && addedByUserId != null && currentUserId == addedByUserId
    val isTutorialBook: Boolean get() = book?.id == BookDetailConstants.TUTORIAL_BOOK_ID
}
```

**`BookDetailViewModel.kt`** — Map in `loadBookDetails()` alongside existing fields:
```kotlin
currentState.copy(
    book = bookDetails.book,
    onShelf = bookDetails.isOnShelf,
    isBookClub = bookDetails.isBookClub,
    clubCode = bookDetails.clubCode,
    clubCreatorId = bookDetails.clubCreatorId,      // NEW
    addedByUserId = bookDetails.addedByUserId,      // NEW
    isLoading = false
)
```

### Layer 4: UI Gate (conditionally hide remove button, not the whole card)

Rather than hiding the entire `ShelfActionsCard` when a member can't remove, pass the permission
into the card and let it decide which buttons to show. This keeps single responsibility (the card
owns its own layout) and avoids future bugs if the card gains more actions.

**`BookDetailState.kt`** — Add derived property:
```kotlin
val canRemoveFromShelf: Boolean get() = when {
    !isBookClub -> true              // Personal shelf — always allow
    isClubOwner -> true              // Club owner — can remove any book
    isAddedByCurrentUser -> true     // Member who added this book — can undo
    else -> false                    // Other member — cannot remove
}
```

**`ShelfActionsCard.kt`** — Add `canRemove` parameter, hide remove button when false:
```kotlin
@Composable
fun ShelfActionsCard(
    book: Book,
    onShelf: Boolean,
    canRemove: Boolean = true,  // NEW — hides remove button when false
    onAddToShelf: (Book) -> Unit,
    onRemoveFromShelf: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    // Don't render the card at all if on shelf but can't remove (nothing to show)
    if (onShelf && !canRemove) return

    Card(/* ... existing ... */) {
        Column(/* ... existing ... */) {
            if (onShelf) {
                // Remove button — only reachable when canRemove is true (guarded above)
                OutlinedButton(onClick = { onRemoveFromShelf(book) }, /* ... */) { /* ... */ }
            } else {
                // Add button — always available for signed-in members
                Button(onClick = { onAddToShelf(book) }, /* ... */) { /* ... */ }
            }
        }
    }
}
```

**`BookDetailScreen.kt:95`** — Simplify condition and pass `canRemoveFromShelf`:
```kotlin
// BEFORE:
if (!isTutorialBook && state.hasShelfContext && !(state.isBookClub && !state.isSignedIn))

// AFTER:
if (!isTutorialBook && state.hasShelfContext && !(state.isBookClub && !state.isSignedIn)) {
    ShelfActionsCard(
        book = state.book,
        onShelf = state.onShelf,
        canRemove = state.canRemoveFromShelf,  // NEW
        onAddToShelf = { book -> onAction(BookDetailAction.OnAddBookClick(book)) },
        onRemoveFromShelf = { book -> onAction(BookDetailAction.OnRemoveBookClick(book)) }
    )
}
```

The existing guest check (`!(state.isBookClub && !state.isSignedIn)`) stays at the call site —
guests should never see the card at all. The `canRemove` param handles the finer-grained
owner/adder/member distinction.

### Layer 5: UseCase Guard (defense-in-depth)

**`RemoveBookFromShelfUseCaseImpl.kt`** — Add `CurrentUserProvider` and permission check:
```kotlin
class RemoveBookFromShelfUseCaseImpl(
    private val bookshelfRepository: BookshelfRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val clubOperations: ClubOperations,
    private val currentUserProvider: CurrentUserProvider,  // NEW
) : RemoveBookFromShelfUseCase {

    override suspend operator fun invoke(bookId: String, shelfId: String): Result<Unit, DataError.Local> {
        val shelf = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        // NEW: Permission check for club shelves
        if (shelf != null && shelf.isBookClub) {
            val currentUserId = currentUserProvider.getCurrentUserId()
            if (currentUserId == null) {
                Timber.tag(TAG).w("Unauthenticated user attempted to remove book %s from club shelf %s", bookId, shelfId)
                return Result.Error(DataError.Local.PERMISSION_DENIED)
            }
            // Owner can remove any book; members can only remove books they added
            if (shelf.clubCreatorId != currentUserId) {
                val addedBy = when (val result = bookshelfRepository.getAddedByUserId(shelfId, bookId)) {
                    is Result.Success -> result.data
                    is Result.Error -> null
                }
                if (addedBy != currentUserId) {
                    Timber.tag(TAG).w("Non-owner/non-adder attempted to remove book %s from club shelf %s", bookId, shelfId)
                    return Result.Error(DataError.Local.PERMISSION_DENIED)
                }
            }
        }

        // ... existing remove + sync logic unchanged ...
    }
}
```

No DI changes needed — `singleOf(::RemoveBookFromShelfUseCaseImpl)` and `singleOf(::AddBookToShelfUseCaseImpl)` auto-resolve `CurrentUserProvider` (already registered in `AuthModule`).

### Layer 6: Tests

**`RemoveBookFromShelfUseCaseTest`** — Add `StubCurrentUserProvider`, add tests:
- `owner can remove any book from club shelf` — returns `Success`
- `member can remove book they added from club shelf` — returns `Success`
- `member cannot remove book another member added` — returns `PERMISSION_DENIED`
- `guest cannot remove book from club shelf` — returns `PERMISSION_DENIED`
- Existing tests continue to pass (personal shelves, no permission check needed)

**`AddBookToShelfUseCaseTest`** — Verify `addedByUserId` is passed for club shelves, null for personal shelves.

**`BookDetailState` unit tests** — Parameterized tests for `canRemoveFromShelf` covering all combinations:
- Personal shelf (always true)
- Club owner viewing any book (true)
- Member viewing own addition (true)
- Member viewing another's book (false)
- Guest (false)
- Null `addedByUserId` on club shelf (false — fail closed)

## Key Decisions

| Decision | Rationale |
|----------|-----------|
| Use `CurrentUserProvider` not `AuthUseCases` | Narrow interface, already used by `book/data`, avoids pulling in aggregator |
| `PERMISSION_DENIED` not a new error type | Already exists in `DataError.Local` enum |
| `canRemove` param on `ShelfActionsCard`, not hidden card | Card owns its layout; screen owns visibility. Avoids suppressing future card actions |
| Derived `canRemoveFromShelf` on state | Keeps boolean logic out of Composable, testable, readable |
| `addedByUserId` on cross-ref, not on `Book` | Attribution is a relationship property (who added *this book to this shelf*), not a book property |
| Destructive migration v3→v4 | No existing users; avoids migration complexity |
| `addedByUserId` nullable | Personal shelves don't need attribution; null on club shelf = fail closed (treated as "not your book") |
| Widen `addBookToShelf` with default null | Non-breaking — existing callers (personal shelves) pass nothing, get null |
| No Firestore rules change | Separate hardening concern; flag as tracked follow-up, not vague TODO |

## Files Changed

| File | Change |
|------|--------|
| `core/data/database/entity/BookshelfBookCrossRef.kt` | +1 field (`addedByUserId`) |
| `core/data/database/MyBookshelfRoomDatabase.kt` | Version 3→4 |
| `core/data/database/dao/CrossRefDao.kt` | +1 query (`getAddedByUserId`) |
| `book/domain/repository/BookshelfRepository.kt` | Widen `addBookToShelf` signature, +1 method |
| `book/data/repository/BookshelfRepositoryImpl.kt` | Pass `addedByUserId` in cross-ref, +1 method impl |
| `book/domain/model/BookDetailsWithShelfStatus.kt` | +2 fields |
| `bookdetail/domain/usecase/GetBookDetailsUseCaseImpl.kt` | +1 query, pass through 2 fields |
| `bookdetail/presentation/BookDetailState.kt` | +2 fields, +3 derived properties |
| `bookdetail/presentation/BookDetailViewModel.kt` | +2 lines (map fields) |
| `bookdetail/presentation/BookDetailScreen.kt` | +1 param passed to `ShelfActionsCard` |
| `bookdetail/presentation/components/ShelfActionsCard.kt` | +1 param (`canRemove`), early return guard |
| `book/domain/usecase/AddBookToShelfUseCaseImpl.kt` | +1 dep, pass userId for club shelves |
| `book/domain/usecase/RemoveBookFromShelfUseCaseImpl.kt` | +1 dep, permission guard with adder check |
| `bookclub/data/repository/BookClubSyncRepositoryImpl.kt` | +1 field in cross-ref creation (line 142) |
| `bookclub/data/repository/BookClubRepositoryHelper.kt` | +1 field in cross-ref creation (line 116) |
| Tests: `RemoveBookFromShelfUseCaseTest` | +4 test cases, update setup |
| Tests: `AddBookToShelfUseCaseTest` | +2 test cases |
| Tests: `BookDetailState` tests | +6 parameterized cases |
| `MockBookshelfRepository` | Widen `addBookToShelf`, +1 method |

## Not In Scope

- Firestore rules hardening (tracked follow-up — tighten book delete to owner-only, book add to members-only)
- Firestore rules: create a tracked GitHub issue so this doesn't get lost
- Displaying "Added by [name]" in the UI (data is available via cross-ref if wanted later)
