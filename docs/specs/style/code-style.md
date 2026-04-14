# Code Style & Conventions

Code style rules, naming conventions, and testing standards.

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| UseCase | `VerbNounUseCase` | `SearchBooksUseCase`, `CreateShelfUseCase` |
| UseCases Aggregator | `FeatureUseCases` | `BookcaseUseCases`, `AuthUseCases` |
| ViewModel | `FeatureViewModel` | `BookcaseViewModel`, `BookDetailViewModel` |
| Repository Interface | `FeatureRepository` | `BookRepository`, `BookClubRepository` |
| Repository Impl | `FeatureRepositoryImpl` | `BookRepositoryImpl` |
| State | `FeatureState` | `BookcaseState`, `BookshelfState` |
| Action | `FeatureAction` | `BookcaseAction`, `BookshelfAction` |
| Screen (Root) | `FeatureScreenRoot` | `BookcaseScreenRoot` |
| Screen (Pure UI) | `FeatureScreen` | `BookcaseScreen` |
| Room Entity | `FeatureEntity` | `BookshelfEntity`, `SyncMetadataEntity` |
| Room DAO | `FeatureDao` | `BookshelfDao`, `SyncDao` |
| Koin Module | `featureModule` | `bookshelfModule`, `authModule` |
| Handler | `FeatureHandler` | `ShelfOperationsHandler` |

## Logging Pattern

Use Timber with TAG constants and varargs formatting (not string interpolation):

```kotlin
class BookcaseViewModel(...) : ViewModel() {

    private fun loadShelves() {
        Timber.tag(TAG).d("Loading shelves for user: %s", userId)
        // NOT: Timber.d("Loading shelves for user: $userId")
    }

    companion object {
        private const val TAG = "BookcaseVM"
    }
}
```

### Guidelines

- Always define `private const val TAG` in companion object
- Use `Timber.tag(TAG).d/w/e(...)` for all log calls
- Use format specifiers (`%s`, `%d`) instead of string interpolation
- This enables log filtering by class and improves performance

### When to Log

| Level | When | Example |
|-------|------|---------|
| `d` (debug) | Development info, operation start | `"Starting sync"` |
| `w` (warning) | Recoverable issues | `"Retry attempt %d"` |
| `e` (error) | Failures with exception | `"Database error", exception` |

**DO:** Log errors, critical operations, use varargs formatting, use tags
**DON'T:** Log happy-path, PII, use string interpolation, log in loops

**Existing tags:** BookSearch, BookshelfExport, BookshelfImport, DeepLinkImport, ErrorMapper

## Detekt Rules

All code must pass detekt checks. Run `./gradlew detekt` before committing.

| Rule | Limit | How to Fix |
|------|-------|------------|
| **LongParameterList** | Max 10 function / 15 constructor | Group into data classes |
| **LongMethod** | Max 100 lines | Extract helper functions |
| **ReturnCount** | Max 4 (guards excluded) | Use `when` or extract helpers |
| **MaxLineLength** | 120 characters | Wrap long lines |
| **MagicNumber** | Named constants only | Extract to companion object |
| **LargeClass** | 500 lines | Split responsibilities |
| **TooManyFunctions** | 25 per class/interface | Extract handler classes |

## Reducing Parameter Count

When a function has too many parameters, create wrapper data classes:

```kotlin
// BAD: Too many parameters
@Composable
fun BookCard(
    book: Book,
    shelf: Bookshelf,
    isSelected: Boolean,
    onBookClick: (Book) -> Unit,
    onEditClick: (Book) -> Unit,
    onDeleteClick: (Book) -> Unit,
    modifier: Modifier
)

// GOOD: Grouped into state + actions
data class BookCardActions(
    val onBookClick: (Book) -> Unit,
    val onEditClick: (Book) -> Unit,
    val onDeleteClick: (Book) -> Unit
)

@Composable
fun BookCard(
    book: Book,
    shelf: Bookshelf,
    isSelected: Boolean,
    actions: BookCardActions,
    modifier: Modifier
)
```

## Testing Standards

### Test Naming

```kotlin
// Pattern: `action - condition - expected result`
@Test
fun `createShelf - when name is valid - returns success with shelf`()

@Test
fun `searchBooks - when no internet - returns NoInternet error`()
```

### Test File Locations

| Test Type | Location | Purpose |
|-----------|----------|---------|
| Unit tests | `app/src/test/.../` | ViewModel, UseCase, pure logic |
| Integration tests | `app/src/androidTest/.../` | Room, real dependencies |
| Mock implementations | `app/src/test/.../testutil/mocks/` | Shared test doubles |
| Test builders | `app/src/test/.../testutil/builders/` | Test data builders |
| Test helpers | `app/src/test/.../testutil/helpers/` | StateFlow testing utils |

### Test Organization

```
app/src/test/java/uk/co/zlurgg/mybookshelf/
├── bookshelf/
│   ├── data/              # Repository, mapper tests
│   ├── domain/usecase/    # UseCase tests
│   └── presentation/      # ViewModel tests
├── core/domain/service/   # Core service tests
├── sync/                  # Sync engine tests
└── testutil/              # Shared test utilities
    ├── builders/          # TestShelfBuilder, TestBookBuilder
    ├── helpers/           # ViewModelTestHelper, TestIdGenerator
    └── mocks/             # Mock repositories, mock use cases
```

### Test Class Guidelines

- Keep test classes focused (<300 lines)
- Use `@Before`/`@After` for shared setup/teardown
- One assertion concept per test
- Use `advanceUntilIdle()` after coroutine actions
- Always collect StateFlow to trigger `onStart` initialization
- Use `testHelper(this)` for proper StateFlow testing

## Commit Style

Conventional commits with scope:

```
type(scope): description

feat(bookclub): add rating system
fix(ui): fix hardcoded strings
refactor(core): extract error mapping to ErrorMapper
test(bookcase): add ViewModel state tests
docs: update architecture documentation
build: migrate to AGP 9.1
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`
