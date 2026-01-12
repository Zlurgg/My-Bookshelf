# CLAUDE.md Template

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

<!-- TODO: Add project description here -->

---

## Architecture Overview

### Clean Architecture Pattern

The app follows Clean Architecture with clear separation of concerns:

```
┌─────────────────────┐
│  Presentation Layer │ ──────┐
└─────────────────────┘       │
                              ▼
┌─────────────────────┐     ┌─────────────────────┐
│     Data Layer      │ ───▶│    Domain Layer     │
└─────────────────────┘     └─────────────────────┘
```

**Dependency Rule**: Both Presentation and Data layers depend on Domain. Domain depends on nothing.

- **Domain Layer**: Core business logic and contracts (dependency inversion boundary)
  - Domain models (pure Kotlin, no framework dependencies)
  - Repository interfaces (contracts that Data layer implements)
  - UseCases (concrete classes with business logic)

- **Data Layer**: Data management and persistence
  - Repository implementations (implement domain interfaces)
  - DataSources (wrap DAOs, APIs)
  - Entities, DTOs, and mappers

- **Presentation Layer**: UI and state management
  - ViewModels (depend on UseCases, never repositories)
  - UI State and Actions
  - Composables (consume state, emit actions)

---

## Design Patterns

### Layered Dependencies (ENFORCED)

```
UI (Composables) → ViewModels → UseCases → Repositories → DataSources
                                    ↓
                              Domain Models
```

**Critical Rules:**
- ViewModels **NEVER** depend on Repositories directly
- ViewModels **ONLY** depend on UseCases
- UseCases encapsulate all business logic
- Repositories are implementation details hidden from ViewModels

### UseCase Pattern

UseCases are concrete classes that encapsulate business logic. They use `operator fun invoke()` for callable syntax:

```kotlin
// ✅ CORRECT: Concrete UseCase class
class GetBooksUseCase(
    private val repository: BookRepository
) {
    suspend operator fun invoke(): Result<List<Book>, DataError> {
        return repository.getBooks()
    }
}

// ✅ CORRECT: UseCase with multiple dependencies
class GetBooksWithAuthorsUseCase(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(): List<BookWithAuthor> =
        withContext(defaultDispatcher) {
            // Business logic here
        }
}

// ✅ CORRECT: ViewModel depends on UseCases
class BookshelfViewModel(
    private val getBooks: GetBooksUseCase,
    private val deleteBook: DeleteBookUseCase
) : ViewModel()

// ❌ WRONG: ViewModel depends on Repository directly
class BookshelfViewModel(
    private val bookRepository: BookRepository  // NEVER DO THIS
) : ViewModel()
```

**UseCase Guidelines:**
- Concrete classes - no interface needed
- Use `operator fun invoke()` for callable syntax: `useCase(params)`
- Naming: `VerbNoun` + `UseCase` (e.g., `GetBooksUseCase`, `DeleteBookUseCase`)
- One UseCase per business operation (single responsibility)
- Can depend on repositories, domain services, or other use cases
- Must be main-safe; use `withContext(dispatcher)` for background work
- No lifecycle, no mutable state

### Repository Pattern

Repositories abstract data sources:

```kotlin
// Domain layer - interface only
interface BookRepository {
    fun getBooks(): Flow<List<Book>>
    suspend fun getBookById(id: String): Book?
    suspend fun insertBook(book: Book): Result<Unit, DataError>
    suspend fun deleteBook(id: String): Result<Unit, DataError>
}

// Data layer - implementation
class BookRepositoryImpl(
    private val localDataSource: BookLocalDataSource,
    private val remoteDataSource: BookRemoteDataSource
) : BookRepository
```

### Mapper Pattern

Mappers convert between layers:

```
DTO (Network) ←→ Entity (Database) ←→ Domain Model
```

- Extension functions preferred: `BookDto.toDomain()`, `Book.toEntity()`
- Located in `data/mappers/` directories
- Never leak layer-specific types across boundaries

### State Management Pattern

ViewModels expose a single immutable state via StateFlow:

```kotlin
data class BookshelfState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,        // Persistent error (inline display)
    val userMessage: String? = null,         // Transient message (snackbar, consumed and cleared)
    val navigationEvent: NavigationEvent? = null  // Navigation (consumed and cleared)
)

// State-based navigation - survives configuration changes
sealed interface NavigationEvent {
    data object Back : NavigationEvent
    data class ToDetail(val bookId: String) : NavigationEvent
}

class BookshelfViewModel(
    private val deleteBookUseCase: DeleteBookUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(BookshelfState())
    val state: StateFlow<BookshelfState> = _state.asStateFlow()

    fun onAction(action: BookshelfAction) {
        when (action) {
            is BookshelfAction.DeleteBook -> deleteBook(action.bookId)
            is BookshelfAction.MessageShown -> _state.update { it.copy(userMessage = null) }
            is BookshelfAction.NavigationHandled -> _state.update { it.copy(navigationEvent = null) }
        }
    }

    private fun deleteBook(bookId: String) {
        viewModelScope.launch {
            deleteBookUseCase(bookId)
                .onSuccess {
                    _state.update { it.copy(userMessage = "Book deleted") }
                }
                .onError { error ->
                    _state.update {
                        it.copy(userMessage = ErrorFormatter.formatDataErrorMessage(error, "delete book"))
                    }
                }
        }
    }
}
```

**Key Principles:**
- Single source of truth: One `StateFlow` per ViewModel
- Immutable state: Always use `copy()` to update
- No one-off events: Navigation and messages are state, consumed and cleared by UI

### Error Handling Pattern

```kotlin
// Domain layer - sealed error types
sealed interface DataError {
    enum class Remote : DataError { NO_NETWORK, SERVER_ERROR, TIMEOUT }
    enum class Local : DataError { DISK_FULL, NOT_FOUND }
}

// UseCase returns Result
suspend fun deleteBook(id: String): Result<Unit, DataError>

// ViewModel handles Result with NAMED variables
when (val deleteResult = deleteBookUseCase(id)) {
    is Result.Success -> updateState { it.copy(deleted = true) }
    is Result.Error -> updateState { it.copy(error = formatError(deleteResult.error)) }
}
```

### Error Handling Best Practices (ENFORCED)

**Repository Layer - The Exception Boundary:**
- Repositories should **never throw exceptions** to UseCases
- All repository methods that can fail should return `Result<T, DataError>`
- Catch exceptions at the repository level, log them, and convert to typed errors

```kotlin
// ✅ CORRECT: Repository handles exceptions internally
class BookRepositoryImpl(...) : BookRepository {
    @Suppress("TooGenericExceptionCaught") // Intentional: repository is the exception boundary
    override suspend fun getBookById(id: String): Result<Book?, DataError.Local> {
        return try {
            Result.Success(dao.getById(id)?.toDomain())
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "getBookById failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }
}

// ❌ WRONG: Repository throws exceptions
class BookRepositoryImpl(...) : BookRepository {
    override suspend fun getBookById(id: String): Book? {
        return dao.getById(id)?.toDomain() // Throws if database fails!
    }
}
```

**UseCase Layer - Clean Business Logic:**
- When repositories return Result, UseCases don't need try-catch
- UseCases are concrete classes (no interface needed)
- Must be main-safe; use `withContext(dispatcher)` for background work

```kotlin
// ✅ Simple UseCase - just delegates to repository
class DeleteBookUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(id: String): Result<Unit, DataError.Local> {
        return repository.deleteBook(id)
    }
}

// ✅ UseCase with business logic
class GetBookWithAuthorUseCase(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(bookId: String): Result<BookWithAuthor, DataError> =
        withContext(defaultDispatcher) {
            val book = bookRepository.getBookById(bookId)
                ?: return@withContext Result.Error(DataError.Local.NOT_FOUND)
            val author = authorRepository.getAuthor(book.authorId)
            Result.Success(BookWithAuthor(book, author))
        }
}

// ✅ UseCase depending on other use cases
class ProcessBookUseCase(
    private val getBookUseCase: GetBookUseCase,
    private val formatBookUseCase: FormatBookUseCase
) {
    suspend operator fun invoke(id: String): Result<FormattedBook, DataError> {
        return getBookUseCase(id).map { book ->
            formatBookUseCase(book)
        }
    }
}
```

**Key Rules:**
- ✅ Log ALL caught exceptions with Timber (include TAG for filtering)
- ✅ Map exceptions to typed errors using `ErrorMapper.mapExceptionToDataError()`
- ✅ Add `@Suppress("TooGenericExceptionCaught")` with explanatory comment
- ✅ Always add `companion object { private const val TAG = "ClassName" }`
- ❌ Never silently catch exceptions without logging
- ❌ Never let exceptions propagate from repository to UseCase

### Service Abstraction Pattern

Abstract system dependencies for testability:

```kotlin
// Interface
interface TimeProvider {
    fun currentTimeMillis(): Long
    fun now(): Instant
}

// Test implementation
class TestTimeProvider : TimeProvider {
    private var time = 0L
    fun setTime(time: Long) { this.time = time }
    fun advanceBy(duration: Duration) { time += duration.toMillis() }
    override fun currentTimeMillis() = time
    override fun now() = Instant.ofEpochMilli(time)
}
```

**Common abstractions:** `TimeProvider`, `IdGenerator`, `LanguageProvider`

---

## Naming Conventions

### Classes

| Type | Pattern | Example |
|------|---------|---------|
| UseCase | `VerbNounUseCase` (concrete) | `GetBooksUseCase`, `DeleteBookUseCase` |
| ViewModel | `FeatureViewModel` | `BookshelfViewModel`, `SettingsViewModel` |
| Repository Interface | `FeatureRepository` | `BookRepository`, `UserRepository` |
| Repository Impl | `FeatureRepositoryImpl` | `BookRepositoryImpl` |
| DataSource | `FeatureLocalDataSource`, `FeatureRemoteDataSource` | `BookLocalDataSource` |
| State | `FeatureState` | `BookshelfState`, `LoginState` |
| Action | `FeatureAction` | `BookshelfAction`, `LoginAction` |
| Entity (Room) | `FeatureEntity` | `BookEntity`, `UserEntity` |
| DTO (Network) | `FeatureDto` | `BookDto`, `UserResponseDto` |
| Mapper | Extension functions preferred | `Book.toEntity()`, `BookDto.toDomain()` |

### Functions

| Type | Pattern | Example |
|------|---------|---------|
| UseCase invoke | `operator fun invoke()` | `suspend operator fun invoke(id: String)` |
| ViewModel actions | `onActionName` | `onDeleteBook()`, `onRefresh()` |
| Repository reads | `getX`, `findX`, `observeX` | `getBooks()`, `findById()`, `observeAll()` |
| Repository writes | `insert`, `update`, `delete`, `upsert` | `insertBook()`, `deleteById()` |
| Mappers | `toX` | `toDomain()`, `toEntity()`, `toDto()` |

### Packages

| Layer | Pattern |
|-------|---------|
| Feature root | `feature_name/` |
| Domain | `feature_name/domain/model/`, `feature_name/domain/usecase/`, `feature_name/domain/repository/` |
| Data | `feature_name/data/repository/`, `feature_name/data/database/`, `feature_name/data/network/` |
| Presentation | `feature_name/presentation/` |

---

## Package Structure

### Feature-First Organization (Recommended)

```
app/src/main/java/com/example/app/
├── core/                           # Shared infrastructure (GENERIC ONLY)
│   ├── data/
│   │   ├── network/               # HttpClientFactory, ApiConfig
│   │   └── database/              # DatabaseFactory, base DAOs
│   ├── domain/
│   │   ├── error/                 # Result, DataError, ErrorFormatter
│   │   └── service/               # TimeProvider, IdGenerator interfaces
│   └── presentation/
│       ├── theme/                 # Colors, Typography, Theme
│       └── components/            # Shared UI components
│
├── feature_one/                    # Feature module
│   ├── data/
│   │   ├── datasource/           # Local and remote data sources
│   │   ├── database/             # Entities, DAOs
│   │   ├── network/              # DTOs, API interfaces
│   │   ├── repository/           # Repository implementations
│   │   └── mappers/              # DTO ↔ Entity ↔ Domain
│   ├── domain/
│   │   ├── model/                # Domain models
│   │   ├── repository/           # Repository interfaces (dependency inversion)
│   │   └── usecase/              # Concrete UseCase classes
│   └── presentation/
│       ├── FeatureScreen.kt
│       ├── FeatureViewModel.kt
│       ├── FeatureState.kt
│       └── components/           # Feature-specific UI components
│
├── feature_two/                    # Another feature
│   └── ...
│
├── di/                             # Dependency injection modules
│   └── AppModule.kt
│
└── App.kt                          # Application class
```

**Rules:**
- `core/` contains ONLY generic, reusable infrastructure
- Feature-specific code stays in feature packages
- No circular dependencies between features
- Features communicate through domain layer or navigation

---

## Coroutines & Flow Patterns

### When to Use What

| Use Case | Pattern |
|----------|---------|
| One-shot operation | `suspend fun` |
| Stream of values | `Flow<T>` |
| UI state from ViewModel | `StateFlow<T>` |
| One-time events (navigation, snackbars) | Include in UI state, consume and clear |
| Combining multiple sources | `combine()`, `zip()` |
| Transforming streams | `map()`, `flatMapLatest()` |

> **Note**: Avoid `Channel` or `SharedFlow` for one-time UI events. Model them as state that gets consumed and cleared. This ensures events aren't lost during configuration changes.

### Flow Best Practices

```kotlin
// ✅ CORRECT: Use stateIn for UI state
val state: StateFlow<UiState> = repository.observeItems()
    .map { items -> UiState(items = items) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

// ✅ CORRECT: Use flatMapLatest for dependent queries
val books: Flow<List<Book>> = selectedShelfId
    .flatMapLatest { shelfId -> repository.getBooksForShelf(shelfId) }

// ❌ WRONG: Collecting in init without lifecycle awareness
init {
    viewModelScope.launch {
        repository.observeItems().collect { /* ... */ }  // Never stops!
    }
}
```

### Coroutine Scope Rules

- **ViewModels**: Use `viewModelScope`
- **UseCases**: Don't create scopes; inherit from caller
- **Repositories**: Don't create scopes; use `withContext()` for dispatcher switching

---

## Android-Specific Patterns

### Jetpack Compose

**State Hoisting:**
```kotlin
// ✅ CORRECT: State hoisted to ViewModel
@Composable
fun BookshelfScreen(
    state: BookshelfState,
    onAction: (BookshelfAction) -> Unit
)

// ❌ WRONG: State managed in Composable
@Composable
fun BookshelfScreen() {
    var books by remember { mutableStateOf(emptyList<Book>()) }  // Don't do this for business state
}
```

**Use `remember` for:**
- UI-only state (scroll position, animation state, text field focus)
- Derived calculations that are expensive

**Use ViewModel state for:**
- Business data
- Anything that survives configuration changes
- State that triggers side effects

**Side Effects (Consume and Clear Pattern):**
```kotlin
@Composable
fun BookshelfScreenRoot(
    viewModel: BookshelfViewModel = koinViewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation events - consume and clear
    LaunchedEffect(state.navigationEvent) {
        when (val event = state.navigationEvent) {
            is NavigationEvent.Back -> onNavigateBack()
            is NavigationEvent.ToDetail -> onNavigateToDetail(event.bookId)
            null -> { /* no-op */ }
        }
        if (state.navigationEvent != null) {
            viewModel.onAction(BookshelfAction.NavigationHandled)
        }
    }

    // Transient messages - consume and clear
    state.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(BookshelfAction.MessageShown)
        }
    }

    BookshelfContent(state = state, snackbarHostState = snackbarHostState, onAction = viewModel::onAction)
}
```

> **Important**: Never use `Channel` or one-off event streams from ViewModel. Always model events as state that UI consumes and clears.

**Advanced: Queued Messages with `List<Message>`**

For apps that need to queue multiple messages (e.g., rapid operations), use unique IDs to ensure `LaunchedEffect` triggers for repeated messages:

```kotlin
data class Message(val id: Long, val text: String)

data class FeatureState(
    val userMessages: List<Message> = emptyList()
)

// In ViewModel
private var messageId = 0L

private fun showError(error: DataError, operation: String) {
    val message = Message(
        id = messageId++,
        text = ErrorFormatter.formatDataErrorMessage(error, operation)
    )
    _state.update { it.copy(userMessages = it.userMessages + message) }
}

fun messageShown(id: Long) {
    _state.update { it.copy(userMessages = it.userMessages.filterNot { it.id == id }) }
}

// In UI - unique ID ensures LaunchedEffect re-triggers for same message text
state.userMessages.firstOrNull()?.let { message ->
    LaunchedEffect(message.id) {
        snackbarHostState.showSnackbar(message.text)
        viewModel.messageShown(message.id)
    }
}
```

**When to use `List<Message>`:**
- Multiple messages can occur in quick succession
- Same message text might repeat (simple `String?` won't re-trigger `LaunchedEffect`)
- Need to queue messages instead of replacing

### Compose Previews

**Basic Preview:**
```kotlin
@Preview(showBackground = true)
@Composable
private fun BookCardPreview() {
    AppTheme {
        BookCard(book = previewBook, onClick = {})
    }
}
```

**Multi-Preview Annotations:**
```kotlin
// Built-in multi-previews
@PreviewLightDark        // Light and dark themes
@PreviewScreenSizes      // Multiple screen sizes
@PreviewFontScales       // Font scaling
@Composable
fun BookListPreview() {
    AppTheme {
        BookList(books = previewBooks, onBookClick = {})
    }
}
```

**Custom Multi-Preview:**
```kotlin
@Preview(name = "Light", uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES)
annotation class PreviewLightDark

@PreviewLightDark
@PreviewFontScales
@Composable
fun FeatureScreenPreview() { /* ... */ }
```

**Preview Data Pattern:**
```kotlin
// In presentation/preview/PreviewData.kt
internal val previewBook = Book(
    id = "preview-1",
    title = "Sample Book",
    author = "Sample Author"
)

internal val previewBooks = listOf(
    previewBook,
    previewBook.copy(id = "preview-2", title = "Another Book")
)

// Usage in preview
@Preview
@Composable
private fun BookListPreview() {
    BookList(books = previewBooks, onBookClick = {})
}
```

**Preview Best Practices:**
- ✅ Extract preview data to separate file (`PreviewData.kt`)
- ✅ Use `internal` visibility for preview data
- ✅ Preview stateless composables, not ViewModel-connected screens
- ✅ Wrap previews in `AppTheme` for accurate styling
- ❌ Don't include network/database calls in previews

> **Reference**: [Compose Previews](https://developer.android.com/develop/ui/compose/tooling/previews)

### Accessibility

All UI must be accessible. Follow these patterns:

**Content Descriptions:**
```kotlin
// ✅ Meaningful descriptions for non-text elements
Image(
    painter = painterResource(R.drawable.book_cover),
    contentDescription = stringResource(R.string.book_cover_description, book.title)
)

IconButton(onClick = { onAction(Action.Delete) }) {
    Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = stringResource(R.string.delete_book)  // Never null for interactive icons
    )
}

// ✅ Decorative images - explicitly null
Image(
    painter = painterResource(R.drawable.decorative_divider),
    contentDescription = null  // Screen readers skip this
)
```

**Touch Targets:**
```kotlin
// ✅ Minimum 48dp touch target (Google's recommended minimum)
IconButton(
    onClick = { /* action */ },
    modifier = Modifier.size(48.dp)  // Ensures adequate touch area
) {
    Icon(imageVector = Icons.Default.Add, contentDescription = "Add book")
}

// ✅ For smaller visual elements, expand touch area
Icon(
    imageVector = Icons.Default.Info,
    contentDescription = "More info",
    modifier = Modifier
        .size(24.dp)  // Visual size
        .clickable { /* action */ }
        .padding(12.dp)  // Expands touch target to 48dp
)
```

**Semantic Grouping:**
```kotlin
// ✅ Merge related elements for cleaner screen reader navigation
Row(
    modifier = Modifier
        .semantics(mergeDescendants = true) { }  // Read as single unit
        .clickable { onBookClick(book) }
) {
    BookCover(book)
    Column {
        Text(book.title)
        Text(book.author)
    }
}

// ✅ Custom content description for complex items
Card(
    modifier = Modifier.semantics {
        contentDescription = "${book.title} by ${book.author}, ${book.readingStatus.displayName}"
    }
) { /* card content */ }
```

**Accessibility Checklist:**
- [ ] All interactive elements have contentDescription
- [ ] Touch targets are at least 48dp
- [ ] Color is not the only indicator (add icons/text)
- [ ] Text scales with system font size (use `sp`)
- [ ] Sufficient color contrast (4.5:1 for text)
- [ ] Screen reader navigation order is logical

> **Reference**: [Compose Accessibility](https://developer.android.com/develop/ui/compose/accessibility)

### Room Database

**Query Patterns:**
```kotlin
// ✅ Return Flow for observable queries
@Query("SELECT * FROM books")
fun observeAll(): Flow<List<BookEntity>>

// ✅ Return suspend for one-shot reads
@Query("SELECT * FROM books WHERE id = :id")
suspend fun getById(id: String): BookEntity?

// ✅ Use @Transaction for related entities
@Transaction
@Query("SELECT * FROM shelves WHERE id = :id")
suspend fun getShelfWithBooks(id: String): ShelfWithBooks
```

**Migration Strategy:**

| Phase | Strategy | Rationale |
|-------|----------|-----------|
| Pre-release | `fallbackToDestructiveMigration()` | No user data to preserve |
| Post-release | Always write migrations | Never lose user data |

**AutoMigration (Simple Changes):**
```kotlin
@Database(
    version = 2,
    entities = [BookEntity::class, ShelfEntity::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2)  // Room handles simple additions
    ],
    exportSchema = true  // Required for migration testing
)
abstract class AppDatabase : RoomDatabase()
```

**AutoMigration with Spec (Renames/Deletes):**
```kotlin
@Database(
    version = 3,
    entities = [BookEntity::class],
    autoMigrations = [
        AutoMigration(from = 2, to = 3, spec = AppDatabase.Migration2To3::class)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    @RenameColumn(tableName = "books", fromColumnName = "pub_year", toColumnName = "publishedYear")
    class Migration2To3 : AutoMigrationSpec
}
```

**Manual Migration (Complex Changes):**
```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new column with default value
        db.execSQL("ALTER TABLE books ADD COLUMN rating REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table, copy data, drop old (for complex restructuring)
        db.execSQL("""
            CREATE TABLE books_new (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                author TEXT NOT NULL DEFAULT ''
            )
        """)
        db.execSQL("INSERT INTO books_new (id, title, author) SELECT id, title, author FROM books")
        db.execSQL("DROP TABLE books")
        db.execSQL("ALTER TABLE books_new RENAME TO books")
    }
}

// Register migrations
Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
    .build()
```

**Testing Migrations:**
```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate3To4() {
        // Create database at version 3
        helper.createDatabase("test-db", 3).apply {
            execSQL("INSERT INTO books (id, title) VALUES ('1', 'Test Book')")
            close()
        }

        // Run migration and validate
        helper.runMigrationsAndValidate("test-db", 4, true, MIGRATION_3_4)

        // Verify data integrity
        val db = helper.openDatabase("test-db", 4)
        val cursor = db.query("SELECT rating FROM books WHERE id = '1'")
        cursor.moveToFirst()
        assertThat(cursor.getFloat(0)).isEqualTo(0.0f)
    }
}
```

**Migration Best Practices:**
- ✅ Always set `exportSchema = true` for migration testing
- ✅ Test every migration path (including skipping versions: 1→3)
- ✅ Use raw SQL strings in migrations (not constants that might change)
- ✅ Keep schema JSON files in version control (`app/schemas/`)
- ❌ Never use `fallbackToDestructiveMigration()` in production releases
- ❌ Never skip testing migrations before release

> **Reference**: [Room Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)

### Koin Dependency Injection

**Module Organization:**
```kotlin
// Separate modules by layer/feature
val dataModule = module {
    single<BookRepository> { BookRepositoryImpl(get(), get()) }
    single { BookDatabase.create(androidContext()) }
}

val domainModule = module {
    factory { GetBooksUseCase(get()) }
    factory { DeleteBookUseCase(get()) }
}

val presentationModule = module {
    viewModel { BookshelfViewModel(get(), get()) }
    viewModel { params -> BookDetailViewModel(params.get(), get()) }  // With parameters
}

// In Application
startKoin {
    modules(dataModule, domainModule, presentationModule)
}
```

**Scoping:**
- `single`: One instance for app lifetime (databases, HTTP clients)
- `factory`: New instance each time (UseCases)
- `viewModel`: Scoped to ViewModel lifecycle

### Navigation (Type-Safe)

Use `@Serializable` routes for type-safe navigation:

**Route Definitions:**
```kotlin
// In navigation package
@Serializable
data object Home

@Serializable
data class BookDetail(val bookId: String, val shelfId: String)

@Serializable
data class BookClub(val clubCode: String)
```

**NavHost Setup:**
```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = Home
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Home> {
            HomeScreenRoot(
                onNavigateToDetail = { bookId, shelfId ->
                    navController.navigate(BookDetail(bookId, shelfId))
                }
            )
        }

        composable<BookDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<BookDetail>()
            BookDetailScreenRoot(
                bookId = route.bookId,
                shelfId = route.shelfId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

**Accessing Arguments in ViewModel:**
```kotlin
class BookDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val getBookUseCase: GetBookUseCase
) : ViewModel() {
    // Type-safe argument extraction
    private val route = savedStateHandle.toRoute<BookDetail>()
    private val bookId = route.bookId

    init {
        loadBook(bookId)
    }
}
```

**Deep Links:**
```kotlin
composable<BookDetail>(
    deepLinks = listOf(
        navDeepLink<BookDetail>(basePath = "https://myapp.com/book")
    )
) { /* ... */ }
```

**Navigation Best Practices:**
- ✅ Pass navigation lambdas to screens, not `NavController`
- ✅ Use `@Serializable` routes for type safety
- ✅ Pass only IDs, fetch data in ViewModel via `SavedStateHandle`
- ❌ Don't pass complex objects as navigation arguments

> **Reference**: [Compose Navigation](https://developer.android.com/develop/ui/compose/navigation)

---

## When to Use the Domain Layer

The domain layer is **optional** per Google's architecture guidance. Add UseCases when:

### Add UseCase When:
- **Complex business logic** that would clutter the ViewModel
- **Reusable logic** used by multiple ViewModels
- **Multiple repository coordination** (e.g., fetching from two sources)
- **Improves testability** by isolating business rules

### Skip UseCase When:
- **Simple pass-through** to a single repository method
- **No business logic** - just data fetching
- **Single consumer** - logic isn't reused anywhere

### Direct Repository Access When:
- ViewModels only need simple CRUD operations
- No business rules to enforce
- **But**: This violates strict Clean Architecture - use sparingly and document why

### Simpler State Management When:
- Screen has only 1-2 pieces of state
- No complex state transitions
- **But**: Be consistent within a feature

### Skip Abstraction When:
- You're 100% certain you'll never need to test it
- It's a one-liner with no side effects
- **But**: `TimeProvider` and `IdGenerator` are almost always worth it

---

## Coding Standards

### The Golden Rule

**Avoid over-engineering EXCEPT when it would violate best practices.**

This means:
- ✅ Abstract `TimeProvider` for testability (best practice: deterministic tests)
- ✅ Create UseCase even for simple operations (best practice: consistent architecture)
- ❌ Don't create `BookColorProviderFactory` for a one-time color lookup
- ❌ Don't add generic type parameters "for flexibility"

### General Principles

1. **DRY (Don't Repeat Yourself)**
   - Extract common patterns to shared utilities
   - Centralize error message formatting
   - But: Prefer duplication over wrong abstraction

2. **SOLID Principles**
   - Single Responsibility: Each class has one job
   - Open/Closed: Open for extension, closed for modification
   - Liskov Substitution: Subtypes must be substitutable
   - Interface Segregation: Prefer small, focused interfaces
   - Dependency Inversion: Depend on abstractions, not concretions

3. **Keep Changes Focused**
   - Only make changes that are directly requested or clearly necessary
   - A bug fix doesn't need surrounding code cleaned up
   - Don't add docstrings, comments, or type annotations to code you didn't change
   - Only add comments where the logic isn't self-evident

4. **No Backwards-Compatibility Hacks**
   - Don't rename unused `_vars`
   - Don't add `// removed` comments for deleted code
   - If something is unused, delete it completely

### Logging Best Practices

**DO:**
- Log errors and exceptions with context
- Log critical operation start/completion
- Use varargs formatting: `Timber.d("Value: %s", value)`
- Include relevant IDs: `"Processing item: %s", itemId`

**DON'T:**
- Log in normal happy-path operations
- Log user data or PII
- Use string interpolation: `"Value: $value"` (allocates even when disabled)
- Log in tight loops

---

## Testing Standards

### Test Pyramid

Target distribution:
- **Unit tests (70%)**: Fast, isolated, test one thing
- **Integration tests (20%)**: Test component interactions with real dependencies
- **E2E tests (10%)**: Critical user journeys only

Follow Google's "Just Say No to More End-to-End Tests" - prefer fast, reliable integration tests.

### Testing Patterns

**StateFlow ViewModels:**
```kotlin
@Test
fun `loading state emits correctly`() = runTest {
    val viewModel = createViewModel()

    // Must collect to trigger stateIn initialization
    val states = mutableListOf<UiState>()
    val job = launch { viewModel.state.collect { states.add(it) } }

    advanceUntilIdle()

    assertThat(states.first().isLoading).isTrue()
    job.cancel()
}
```

**Use Real Implementations:**
```kotlin
// ✅ CORRECT: Real Room database in integration tests
@get:Rule
val instantTaskRule = InstantTaskExecutorRule()

private lateinit var database: AppDatabase
private lateinit var repository: BookRepositoryImpl

@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    repository = BookRepositoryImpl(database.bookDao())
}
```

**Stub External Only:**
- ✅ Stub: Network APIs, system services, external SDKs
- ❌ Don't stub: Room database, your own repositories, UseCases

### Test Naming

```kotlin
// Pattern: `action - condition - expected result`
@Test
fun `deleteBook - when book exists - removes from database`()

@Test
fun `deleteBook - when book not found - returns NotFound error`()
```

### Always Research Before Writing Tests

1. **Search for actual class names** - they may differ from what you expect
2. **Check constructor parameters** - get the order and types right
3. **Verify method signatures** - don't assume methods exist
4. **Match field names exactly** - `averageRating` not `ratingsAverage`

### Testing Dependencies

```kotlin
// build.gradle.kts - Unit tests
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)  // runTest, advanceUntilIdle()
testImplementation(libs.androidx.arch.core.testing)  // InstantTaskExecutorRule
testImplementation(libs.robolectric)  // Android framework on JVM
testImplementation(libs.androidx.test.core.ktx)  // ApplicationProvider

// build.gradle.kts - Instrumented tests
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.ui.test.junit4)  // Compose testing
androidTestImplementation(libs.room.testing)  // MigrationTestHelper
```

| Library | Purpose |
|---------|---------|
| `kotlinx-coroutines-test` | `runTest`, `advanceUntilIdle()`, `TestDispatcher` |
| `robolectric` | Run Android-dependent tests on JVM (faster) |
| `room-testing` | `MigrationTestHelper` for database migration tests |
| `turbine` | Flow testing with `test()` extension (optional) |

---

## Static Analysis (Detekt + ktlint)

Use Detekt with the formatting module (wraps ktlint) for static analysis and code formatting in a single tool.

**Run Detekt:**
```bash
./gradlew detekt          # Check all code
./gradlew detektBaseline  # Generate baseline for existing issues
```

**Key Configuration Files:**
- `app/detekt.yml` - Detekt rule configuration
- `.editorconfig` - ktlint/formatting rules (NowInAndroid approach)

**Suppressing Rules:**
```kotlin
@Suppress("TooGenericExceptionCaught")  // Intentional: repository is exception boundary
override suspend fun getData(): Result<Data, DataError> { ... }

@Suppress("MagicNumber")  // Preview dimensions
@Preview fun MyPreview() { ... }
```

> **Full Setup Guide**: See [DETEKT_SETUP_PLAN.md](DETEKT_SETUP_PLAN.md) for complete configuration including pre-commit hooks.

---

## Git & Code Review Standards

### Commit Messages

```
<type>: <short description>

<optional body explaining why>

<optional footer>
```

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

```
feat: Add book deletion with confirmation dialog

Users requested ability to remove books from shelves.
Added confirmation to prevent accidental deletions.

Closes #123
```

### Branch Naming

```
feature/add-book-search
fix/crash-on-empty-shelf
refactor/extract-book-repository
```

### Pull Request Standards

- **Size**: Aim for <400 lines changed; split larger changes
- **One concern per PR**: Don't mix refactoring with features
- **Tests required**: No PR without tests for new functionality
- **Description**: Explain what AND why

### Code Review Checklist

- [ ] Does it follow the layered architecture?
- [ ] Are UseCases used (not direct repository access)?
- [ ] Is error handling consistent with patterns?
- [ ] Are there tests for new functionality?
- [ ] Is the naming consistent with conventions?
- [ ] No unnecessary changes outside the PR scope?

---

## Working Style Preferences

- **Be willing to disagree**: Engage in genuine technical debate about architectural trade-offs
- **Tell me when I'm wrong**: Defend your position with reasoning rather than just agreeing
- **Technical honesty**: Don't back down from legitimate technical concerns when challenged
- **Principled architecture**: Defend Clean Architecture, SOLID, DRY when they're being violated

---

## Security Practices

- Never commit secrets or API keys
- Use BuildConfig for environment-specific configurations
- ProGuard enabled for release builds
- Validate at system boundaries (user input, external APIs)

---

## HTTP Infrastructure

- **Configuration-driven**: API endpoints via BuildConfig (no hardcoded URLs)
- **Retry policies**: Automatic retries for 5xx errors, timeouts, network failures
- **Exponential backoff**: Prevent thundering herd on failures
- **Proper timeouts**: Connect, read, write timeouts configured

---

## Version Catalog (libs.versions.toml)

Centralize dependency versions in `gradle/libs.versions.toml`:

**Structure:**
```toml
[versions]
kotlin = "2.0.0"
compose-bom = "2024.06.00"
room = "2.6.1"
koin = "3.5.6"

[libraries]
# Format: group-artifact = { group = "...", name = "...", version.ref = "..." }
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

# BOM for version alignment
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }

[plugins]
android-application = { id = "com.android.application", version = "8.5.0" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.0-1.0.22" }
```

**Usage in build.gradle.kts:**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Room (grouped by version)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
```

**Adding New Dependencies:**
1. Add version to `[versions]` if shared
2. Add library to `[libraries]` with `version.ref`
3. Sync Gradle
4. Use `libs.library.name` in dependencies

> **Reference**: [Migrate to Version Catalogs](https://developer.android.com/build/migrate-to-catalogs)

---

## References

- [Guide to app architecture](https://developer.android.com/topic/architecture) - Core architecture principles
- [UI layer](https://developer.android.com/topic/architecture/ui-layer) - ViewModel, StateFlow, UDF patterns
- [Domain layer](https://developer.android.com/topic/architecture/domain-layer) - UseCase patterns
- [Data layer](https://developer.android.com/topic/architecture/data-layer) - Repository and DataSource patterns
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events) - Handling user actions and state

---

*Template Version*: 3.0
*Based on*: [Android Developer Architecture Guidelines](https://developer.android.com/topic/architecture)
