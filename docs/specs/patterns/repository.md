# Repository Pattern

Patterns for implementing data access through repositories.

## Interface in Domain

Repository interfaces live in the domain layer and define the contract:

```kotlin
// In bookshelf/domain/repository/
interface BookRepository {
    suspend fun getBookById(id: String): Book?
    suspend fun upsertBook(book: Book)
    suspend fun deleteBook(id: String)
    fun getBooksByShelfId(shelfId: String): Flow<List<Book>>
}
```

### Guidelines

- Interfaces in `domain/repository/`
- Use domain models, not entities
- Return `Result<T, DataError>` for fallible operations
- Use `Flow<T>` for reactive/observable data
- Keep methods focused (SRP)

## Implementation in Data

Repository implementations live in the data layer:

```kotlin
// In bookshelf/data/repository/
class BookRepositoryImpl(
    private val dao: BookshelfDao
) : BookRepository {

    override suspend fun getBookById(id: String): Book? {
        return dao.getBookById(id)?.toDomain()
    }

    override suspend fun upsertBook(book: Book) {
        dao.upsertBook(book.toEntity())
    }

    override fun getBooksByShelfId(shelfId: String): Flow<List<Book>> {
        return dao.getBooksByShelfId(shelfId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    companion object {
        private const val TAG = "BookRepository"
    }
}
```

## Error Handling with ErrorMapper

Use `ErrorMapper.safeSuspendCall()` to wrap database operations:

```kotlin
override suspend fun getShelfById(id: String): Result<Bookshelf?, DataError.Local> {
    return ErrorMapper.safeSuspendCall(TAG) {
        dao.getShelfById(id)?.toDomain()
    }
}
```

## Entity to Domain Mapping

Keep mappers as extension functions:

```kotlin
// In bookshelf/data/mapper/
fun BookshelfEntity.toDomain(): Bookshelf {
    return Bookshelf(
        id = id,
        name = name,
        position = position,
        shelfStyle = ShelfStyle.valueOf(shelfStyle)
    )
}

fun Bookshelf.toEntity(): BookshelfEntity {
    return BookshelfEntity(
        id = id,
        name = name,
        position = position,
        shelfStyle = shelfStyle.name
    )
}
```

## DI Registration (Koin)

```kotlin
val bookshelfModule = module {
    // Repositories
    single<BookRepository> { BookRepositoryImpl(get()) }
    single<BookcaseRepository> { BookcaseRepositoryImpl(get(), get(), get()) }
}
```

## Testing with Mocks

Create mock implementations for testing:

```kotlin
// In testutil/mocks/MockBookcaseRepository.kt
class MockBookcaseRepository : BookcaseRepository {
    var addShelfCalled = false
    private val shelves = mutableListOf<Bookshelf>()

    override suspend fun addShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        addShelfCalled = true
        shelves.add(shelf)
        return Result.Success(Unit)
    }

    fun configureShelves(list: List<Bookshelf>) {
        shelves.clear()
        shelves.addAll(list)
    }

    fun reset() {
        addShelfCalled = false
        shelves.clear()
    }
}
```
