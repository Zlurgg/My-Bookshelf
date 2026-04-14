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
