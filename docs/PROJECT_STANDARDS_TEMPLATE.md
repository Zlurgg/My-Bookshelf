# CLAUDE.md Template

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

<!-- TODO: Add project description here -->

---

## Architecture Overview

### Clean Architecture Pattern

The app follows Clean Architecture with clear separation of concerns:

- **Domain Layer**: Core business logic, entities, and repository interfaces
  - Contains domain models and repository contracts
  - **Pure domain code** - Zero framework dependencies
  - Business logic lives here, not in ViewModels

- **Data Layer**: Database, network, and repository implementations
  - Database entities, DAOs, and type converters
  - Network clients and API services
  - Repository implementations of domain contracts

- **Presentation Layer**: UI components, ViewModels, and state management
  - MVVM pattern with ViewModels handling UI logic
  - Unidirectional data flow

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

UseCases are the gatekeepers of business logic:

```kotlin
// ✅ CORRECT: ViewModel depends on UseCase
class BookshelfViewModel(
    private val getBooks: GetBooksUseCase,
    private val deleteBook: DeleteBookUseCase
) : ViewModel()

// ❌ WRONG: ViewModel depends on Repository
class BookshelfViewModel(
    private val bookRepository: BookRepository  // NEVER DO THIS
) : ViewModel()
```

**UseCase Guidelines:**
- One UseCase per business operation
- Consistent `Result<T, Error>` return types
- UseCases can depend on multiple repositories
- UseCases should be testable in isolation

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

ViewModels manage UI state with StateFlow:

```kotlin
class BookshelfViewModel(...) : ViewModel() {
    private val _state = MutableStateFlow(BookshelfState())
    val state: StateFlow<BookshelfState> = _state.asStateFlow()

    fun onAction(action: BookshelfAction) {
        when (action) {
            is BookshelfAction.DeleteBook -> deleteBook(action.bookId)
            is BookshelfAction.RefreshBooks -> refreshBooks()
        }
    }
}
```

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
| UseCase | `VerbNounUseCase` | `GetBooksUseCase`, `DeleteBookUseCase` |
| ViewModel | `FeatureViewModel` | `BookshelfViewModel`, `SettingsViewModel` |
| Repository Interface | `FeatureRepository` | `BookRepository`, `UserRepository` |
| Repository Impl | `FeatureRepositoryImpl` | `BookRepositoryImpl` |
| State | `FeatureState` | `BookshelfState`, `LoginState` |
| Action | `FeatureAction` | `BookshelfAction`, `LoginAction` |
| Entity (Room) | `FeatureEntity` | `BookEntity`, `UserEntity` |
| DTO (Network) | `FeatureDto` | `BookDto`, `UserResponseDto` |
| Mapper | `FeatureMapper` or extension functions | `BookMapper`, `Book.toEntity()` |

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
│   │   ├── database/             # Entities, DAOs
│   │   ├── network/              # DTOs, API services
│   │   ├── repository/           # Repository implementations
│   │   └── mappers/              # DTO ↔ Entity ↔ Domain
│   ├── domain/
│   │   ├── model/                # Domain models
│   │   ├── repository/           # Repository interfaces
│   │   └── usecase/              # UseCases
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
| Events (one-time) | `SharedFlow<T>` or Channel |
| Combining multiple sources | `combine()`, `zip()` |
| Transforming streams | `map()`, `flatMapLatest()` |

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

**Side Effects:**
```kotlin
// One-time events (navigation, snackbars)
LaunchedEffect(state.navigateToDetail) {
    state.navigateToDetail?.let { bookId ->
        navController.navigate("detail/$bookId")
        viewModel.onAction(ClearNavigation)
    }
}

// Lifecycle-aware collection
val state by viewModel.state.collectAsStateWithLifecycle()
```

### Room Database

**Migration Strategy:**
- Pre-release: Destructive migrations OK (`fallbackToDestructiveMigration()`)
- Post-release: Always write migrations, never lose user data

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

---

## When to Break the Rules

### Skip UseCase When:
- **Trivial pass-through**: If UseCase just calls one repository method with no logic, consider combining related operations or allowing direct access for truly simple cases
- **Never skip for**: Anything with business logic, validation, or multiple repository calls

### Direct Repository Access When:
- Simple CRUD with no business rules
- Internal data layer operations
- **But**: Document why the exception exists

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

*Template Version*: 2.0
*Based on*: Production Android/Kotlin project standards
