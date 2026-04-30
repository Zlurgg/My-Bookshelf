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
    val duplicateShelf: DuplicateShelfUseCase,
    val shareShelf: ShareBookshelfUseCase
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

### Simple UseCases: Use ErrorMapper.safeSuspendCall()

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

## Sync After Mutation

All mutating use cases (create, update, delete) must trigger an immediate sync after a successful local mutation so changes push to Firestore without waiting for the 15-minute periodic sync.

### Pattern

```kotlin
class CreateShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val syncSchedulerService: SyncSchedulerService,
) : CreateShelfUseCase {

    override suspend operator fun invoke(...): Result<Bookshelf, DataError.Local> {
        // ... perform mutation ...

        Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: CreateShelf")
        syncSchedulerService.triggerImmediateSync()

        return Result.Success(newShelf)
    }
}
```

### Rules

- Inject `SyncSchedulerService` and call `triggerImmediateSync()` after the successful mutation path
- Use `SyncConstants.TAG_SYNC_TRIGGER` as the Timber log tag for all sync trigger logs
- Only trigger sync on success — error paths should not trigger sync
- **Building-block use cases** (e.g., `UpsertBookUseCaseImpl`) that are only called by parent use cases which handle sync themselves may skip the sync trigger, but must document this with a KDoc warning
- **Conditional sync**: If a use case handles both personal and club data, only trigger sync for personal data (club data pushes to Firestore directly and is excluded from the sync engine)

### Tech Debt

This is a manual convention with no compile-time enforcement. Every new mutating use case must remember to add this call. Future options to reduce risk:
- Repository write observer that auto-triggers sync on Room mutations
- UseCase decorator/wrapper that adds sync after any successful mutation
