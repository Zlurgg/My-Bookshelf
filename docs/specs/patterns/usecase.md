# UseCase Pattern

Patterns for implementing business logic through UseCases.

## UseCase Pattern

Interface + implementation with `operator fun invoke()` for callable syntax:

```kotlin
// Interface in domain/usecase/
interface SearchBooksUseCase {
    suspend operator fun invoke(
        query: String,
        resultLimit: Int? = null,
        language: String? = null,
        authorFilter: String? = null,
        titleFilter: String? = null
    ): Result<List<Book>, DataError.Remote>
}

// Implementation
class SearchBooksUseCaseImpl(
    private val remoteBookDataSource: RemoteBookDataSource
) : SearchBooksUseCase {
    override suspend operator fun invoke(...): Result<List<Book>, DataError.Remote> {
        // Business validation + delegation
    }
}

// Usage: useCase(query) instead of useCase.execute(query)
```

### Guidelines

- Naming: `VerbNoun` + `UseCase` (e.g., `CreateShelfUseCase`, `SearchBooksUseCase`)
- One UseCase per business operation
- Can depend on repositories, domain services, or other UseCases
- Must be main-safe; use `withContext(dispatcher)` for background work
- Inject dependencies for testability rather than using static calls

## UseCases Aggregator Pattern

When a ViewModel requires multiple UseCases, group them in a data class:

```kotlin
// In domain/usecase/
data class BookcaseUseCases(
    val getAllShelves: GetAllShelvesUseCase,
    val createShelf: CreateShelfUseCase,
    val deleteShelf: DeleteShelfUseCase,
    val reorderShelves: ReorderShelvesUseCase,
    val getShelfById: GetShelfByIdUseCase,
    val renameShelf: RenameShelfUseCase,
    val updateShelfStyle: UpdateShelfStyleUseCase,
    val duplicateShelf: DuplicateShelfUseCase
)

// ViewModel constructor
class BookcaseViewModel(
    private val bookcaseUseCases: BookcaseUseCases,
    ...
) : ViewModel()
```

### DI Registration (Koin)

```kotlin
factory {
    BookcaseUseCases(
        getAllShelves = get(),
        createShelf = get(),
        deleteShelf = get(),
        reorderShelves = get(),
        getShelfById = get(),
        renameShelf = get(),
        updateShelfStyle = get(),
        duplicateShelf = get(),
        shareShelf = get()
    )
}
```

## Error Handling in UseCases

### Pure Delegation UseCases: Direct return

When a UseCase delegates to a repository that already returns `Result<T, DataError>`, return the result directly. The repository has already caught exceptions internally — wrapping in `safeSuspendCall()` would double-handle errors and mask contract violations that should surface as crashes during development.

```kotlin
override suspend fun invoke(code: String): Result<Unit, DataError.Sync> {
    return bookClubRepository.deleteBookClub(code)
}
```

### Simple UseCases with raw data sources: Use ErrorMapper.safeSuspendCall()

When a UseCase calls a data source that may throw (e.g. raw DAO or network call not wrapped in `Result`), use `safeSuspendCall()` to catch exceptions at the UseCase boundary:

```kotlin
override suspend fun invoke(id: String): Result<Book?, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        repository.getById(id)
    }
}
```

### Complex UseCases: Use @Suppress + logging

```kotlin
@Suppress("TooGenericExceptionCaught") // Intentional: converts exceptions to Result.Error with logging
override suspend fun invoke(id: String): Result<Unit, DataError.Local> {
    return try {
        // Complex logic with multiple steps...
        Result.Success(Unit)
    } catch (e: Exception) {
        val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
            ?: DataError.Local.UNKNOWN
        Timber.tag(TAG).e(e, "Operation failed - Mapped to: %s", error)
        Result.Error(error)
    }
}
```

**Rules:**
- Always log caught exceptions with Timber
- Always add TAG constant for log filtering
- Use `@Suppress("TooGenericExceptionCaught")` with explaining comment
- Map to typed errors using `ErrorMapper.mapExceptionToDataError()`

## Testing UseCases

```kotlin
class CreateShelfUseCaseTest {
    private val mockRepository = MockBookcaseRepository()
    private val testIdGenerator = TestIdGenerator()
    private val useCase = CreateShelfUseCaseImpl(mockRepository, testIdGenerator)

    @Test
    fun `creates shelf with correct data when no existing shelves`() = runTest {
        // Given
        val name = "My Books"
        val style = ShelfStyle.DarkWood
        val existingShelves = emptyList<Bookshelf>()

        // When
        val result = useCase.execute(name, style, existingShelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val shelf = (result as Result.Success).data
        assertEquals("Should have correct name", name, shelf.name)
        assertTrue("Should call repository", mockRepository.addShelfCalled)
    }
}
```

## Sync After Mutation — Repository Decorator Pattern

Sync triggering is handled automatically by repository decorators using Kotlin `by` delegation. Use cases do NOT inject `SyncSchedulerService` or call `triggerImmediateSync()`.

### How It Works

Three decorator classes in `sync/domain/repository/` wrap the user-facing repository interfaces:

| Decorator | Wraps | Overridden Methods |
|-----------|-------|--------------------|
| `SyncingBookcaseRepository` | `BookcaseRepository` | `addShelf`, `removeShelf`, `updateShelf` |
| `SyncingBookRepository` | `BookRepository` | `upsertBook`, `deleteBook` |
| `SyncingBookshelfRepository` | `BookshelfRepository` | `addBookToShelf`, `removeBookFromShelf` |

Each overridden method calls the delegate, then on `Result.Success` logs with `SyncConstants.TAG_SYNC_TRIGGER` and calls `syncScheduler.triggerImmediateSync()`. On error, no sync is triggered.

All other methods (reads, Flows, system operations) delegate directly via `by` with zero overhead.

### DI Wiring

- `BookModule` registers concrete repository implementations (`BookRepositoryImpl`, etc.) without interface binding
- `SyncModule` wraps them with decorators and binds the interfaces:
  ```kotlin
  single<BookRepository> { SyncingBookRepository(get<BookRepositoryImpl>(), get()) }
  ```
- This keeps `book/` unaware of sync (correct dependency direction: `sync` → `book`)

### Deliberately NOT Overridden

| Method | Reason |
|--------|--------|
| `hardDeleteShelf` | Club-only; entity is gone from Room, sync engine excludes clubs |
| `addSystemShelf` | System shelves are not synced to cloud |
| `upsertSystemBook` | System books are not synced to cloud |
| `clearUserData` | Sign-out cleanup; no sync after wiping data |

### No Infinite Loop

The sync engine (`SyncWorker` → `SyncRepository` → `SyncEngine`) writes directly to DAOs, completely bypassing user-facing repositories. The decorators only wrap user-facing interfaces, so sync engine writes never pass through them.

### Exceptions (Manual Sync Required)

Three use cases still call `triggerImmediateSync()` directly because they write through non-decorated paths:

1. `ResumeSessionUseCaseImpl` — session setup, not a repository mutation
2. `MigrateLocalDataUseCaseImpl` — writes through `SyncRepository` → DAO directly
3. `ValidateBookClubMembershipsUseCaseImpl` — writes through `bookshelfDao.upsertShelf()` via `BookClubRepositoryHelper`

### Eventually Consistent

Multi-write use cases like `DuplicateShelfUseCase` fire multiple sync triggers (one per `addShelf` + N per `addBookToShelf`). `ExistingWorkPolicy.REPLACE` de-duplicates — only the last enqueue results in actual sync work. Intermediate state (e.g., shelf with zero books) may briefly appear server-side. Harmless.

### Architecture Test Enforcement

`SyncDecoratorCoverageTest` uses reflection with inclusion lists to verify all write methods are overridden. If a new write method is added to a repository interface, the test fails until the decorator is updated.
