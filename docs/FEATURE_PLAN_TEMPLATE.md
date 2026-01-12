# [Feature Name] Implementation Plan

## Overview

Brief description of the feature and its purpose.

**Scope**: List the main capabilities this feature will provide.

---

## Files to Create

### Domain Layer

| File | Description |
|------|-------------|
| `feature/domain/model/ModelName.kt` | Domain entity description |
| `feature/domain/repository/FeatureRepository.kt` | Repository interface (dependency inversion boundary) |
| `feature/domain/usecase/FeatureUseCase.kt` | Concrete UseCase class (no interface needed) |

### Data Layer

| File | Description |
|------|-------------|
| `feature/data/repository/FeatureRepositoryImpl.kt` | Repository implementation |
| `feature/data/datasource/FeatureLocalDataSource.kt` | Local data source (wraps DAO) |
| `feature/data/datasource/FeatureRemoteDataSource.kt` | Remote data source (wraps API) |

### Presentation Layer

| File | Description |
|------|-------------|
| `feature/presentation/FeatureState.kt` | UI state with data, isLoading, error fields |
| `feature/presentation/FeatureAction.kt` | Sealed interface for user actions including Retry |
| `feature/presentation/FeatureViewModel.kt` | ViewModel with activeJob pattern, Timber logging |
| `feature/presentation/FeatureScreen.kt` | Screen with loading/error/empty/content states |
| `feature/presentation/components/ComponentName.kt` | Reusable UI components |
| `feature/presentation/util/PreviewData.kt` | Preview data for composable previews |

### DI & Tests

| File | Description |
|------|-------------|
| `feature/di/FeatureModule.kt` | Koin module for repository, usecases, viewmodel |
| `test/.../FeatureViewModelTest.kt` | ViewModel unit tests |
| `test/.../FeatureUseCaseTest.kt` | UseCase unit tests |
| `test/.../FeatureRepositoryImplTest.kt` | Repository unit tests |
| `androidTest/.../MigrationTest.kt` | Database migration tests (if adding entities) |

---

## Files to Modify

| File | Change |
|------|--------|
| `di/AppModule.kt` | Add `includes(featureModule)` |
| `core/presentation/navigation/SoleMateNavHost.kt` | Add navigation route and screen |
| `core/domain/error/DataError.kt` | Add feature-specific error types if needed |
| `core/presentation/util/ErrorMessages.kt` | Add error message mappings |
| `res/values/strings.xml` | Add feature strings |

---

## Implementation Steps

### Step 1: Domain Models
- Create domain entities
- Ensure pure Kotlin (no framework dependencies)
- Add displayName property to enums if applicable

**Commit: `feat(feature): Add domain models`**

### Step 2: Repository Interface
- Define repository interface in domain layer (dependency inversion boundary)
- All methods return `Result<T, DataError>`
- Interface stays in domain so presentation never imports from data layer

### Step 3: Use Cases
- Create concrete use case classes (no interface needed)
- Naming: `VerbNoun` + `UseCase` (e.g., `GetLatestNewsUseCase`, `FormatDateUseCase`)
- Use `operator fun invoke()` for callable syntax
- Use cases can depend on repositories, domain services, or other use cases
- Must be main-safe; use `withContext(dispatcher)` for background work

### Step 4: Repository Implementation
- Implement repository interface in data layer
- Inject data sources (local/remote) - never access DAOs directly
- **Design to NEVER throw exceptions** - handle errors internally and return Result

**Commit: `feat(feature): Add repository and usecases`**

### Step 5: State & Actions
- Create state data class with: data, isLoading, error fields
- Create sealed interface for actions including Retry

### Step 6: ViewModel
- Follow activeJob pattern for cancellation
- Add Timber logging at operation boundaries
- Handle both Success and Error from Result

### Step 7: UI Components
- Create reusable components in `presentation/components/`
- Follow existing component patterns

### Step 8: Screen
- Handle all states: loading, error, empty, content
- Use `@PreviewAllModes` for main preview
- Use `@PreviewLightDark` for state-specific previews
- Add landscape-aware spacing if applicable

### Step 9: Accessibility
- Add `contentDescription` to all interactive icons/images
- Ensure minimum 48dp touch targets
- Use `semantics(mergeDescendants = true)` for list items
- Verify with TalkBack

**Accessibility Checklist:**
- [ ] All IconButtons have contentDescription (from string resources)
- [ ] Decorative images have `contentDescription = null`
- [ ] Touch targets ≥ 48dp
- [ ] Text uses `sp` units (scales with system settings)
- [ ] Colors have sufficient contrast

**Commit: `feat(feature): Add presentation layer`**

### Step 10: Preview Data
- Extract preview data to `presentation/util/PreviewData.kt`
- Use `internal` visibility

### Step 11: DI Module
- Create Koin module
- Register repository, use cases, viewmodel
- Update AppModule to include new module

**Commit: `feat(feature): Add DI module`**

### Step 12: Database Migration (if adding entities)
- Increment database version
- Add AutoMigration or manual Migration
- Test migration with MigrationTestHelper

```kotlin
// In AppDatabase
@Database(
    version = X,  // Increment
    autoMigrations = [AutoMigration(from = X-1, to = X)]
)
```

**Commit: `feat(feature): Add database migration`**

### Step 13: Tests
- ViewModel tests: state transitions, error handling, actions
- UseCase tests: business logic, error propagation
- Repository tests: data operations, edge cases
- Migration tests (if applicable)

**Commit: `test(feature): Add unit tests`**

### Step 14: Integration
- Update navigation
- Add error message mappings
- Add string resources (including accessibility descriptions)

**Commit: `feat(feature): Wire navigation and add resources`**

### Step 15: Build & Verify
- Run `./gradlew assembleDebug`
- Run `./gradlew test`
- Run `./gradlew detekt` (if configured)
- Test with TalkBack enabled
- Fix any issues

### Step 16: Code Review
- Check pattern compliance
- Review for Clean Architecture violations
- Verify error handling completeness
- Verify accessibility compliance

**Commit (if fixes needed): `fix(feature): Address code review issues`**

---

## Key Patterns to Follow

### State Pattern
```kotlin
data class FeatureState(
    val data: DataType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,       // Persistent error (inline display)
    val userMessage: String? = null,        // Transient message (snackbar)
    val navigationEvent: NavigationEvent? = null
)

// State-based navigation - survives configuration changes
sealed interface NavigationEvent {
    data object Back : NavigationEvent
    // Add other destinations as needed
}
```

### Action Pattern
```kotlin
sealed interface FeatureAction {
    data object Retry : FeatureAction
    data object MessageShown : FeatureAction       // Clears userMessage
    data object NavigationHandled : FeatureAction  // Clears navigationEvent
    data class UpdateField(val value: String) : FeatureAction
}
```

### ViewModel Pattern
```kotlin
class FeatureViewModel(
    private val featureUseCase: FeatureUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(FeatureState())
    val state: StateFlow<FeatureState> = _state.asStateFlow()

    fun onAction(action: FeatureAction) {
        when (action) {
            is FeatureAction.Delete -> deleteItem(action.id)
            is FeatureAction.MessageShown -> {
                _state.update { it.copy(userMessage = null) }
            }
            is FeatureAction.NavigationHandled -> {
                _state.update { it.copy(navigationEvent = null) }
            }
            // ... other actions
        }
    }

    private fun deleteItem(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            featureUseCase(id)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            navigationEvent = NavigationEvent.Back
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            userMessage = ErrorFormatter.formatDataErrorMessage(error, "delete item")
                        )
                    }
                }
        }
    }
}
```

### Screen Pattern
```kotlin
@Composable
fun FeatureScreenRoot(
    viewModel: FeatureViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // State-based navigation (consume and clear)
    LaunchedEffect(state.navigationEvent) {
        when (state.navigationEvent) {
            is NavigationEvent.Back -> onNavigateBack()
            null -> { /* no-op */ }
        }
        if (state.navigationEvent != null) {
            viewModel.onAction(FeatureAction.NavigationHandled)
        }
    }

    // Transient messages (consume and clear)
    state.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(FeatureAction.MessageShown)
        }
    }

    FeatureContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@Composable
private fun FeatureContent(
    state: FeatureState,
    snackbarHostState: SnackbarHostState,
    onAction: (FeatureAction) -> Unit
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(message = state.errorMessage, onRetry = { onAction(FeatureAction.Retry) })
            state.data == null -> EmptyState()
            else -> { /* Main content */ }
        }
    }
}
```

### Error Handling Pattern (ENFORCED)

**Repository Implementation:**
```kotlin
// ✅ CORRECT: Repository never throws, always returns Result
class FeatureRepositoryImpl(...) : FeatureRepository {
    override suspend fun getData(): Result<Data, DataError.Local> {
        return try {
            Result.Success(dao.getData())
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "getData failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }
}
```

**UseCase Implementation:**
```kotlin
// ✅ Concrete use case class (no interface needed)
class GetDataUseCase(
    private val repository: FeatureRepository
) {
    suspend operator fun invoke(): Result<Data, DataError.Local> {
        return repository.getData()
    }
}

// ✅ Use case with multiple dependencies
class GetNewsWithAuthorsUseCase(
    private val newsRepository: NewsRepository,
    private val authorsRepository: AuthorsRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend operator fun invoke(): List<ArticleWithAuthor> =
        withContext(defaultDispatcher) {
            val news = newsRepository.fetchLatestNews()
            news.map { article ->
                val author = authorsRepository.getAuthor(article.authorId)
                ArticleWithAuthor(article, author)
            }
        }
}

// ✅ Use case depending on other use cases
class ProcessDataUseCase(
    private val getDataUseCase: GetDataUseCase,
    private val formatDataUseCase: FormatDataUseCase
) {
    suspend operator fun invoke(): Result<FormattedData, DataError> {
        return getDataUseCase().map { data ->
            formatDataUseCase(data)
        }
    }
}
```

---

## Test Cases

### FeatureViewModelTest
1. `initial state loads data successfully`
2. `initial state shows error on failure`
3. `retry reloads data after error`
4. `action updates state correctly`
5. `navigation event set on success`
6. `user message shown on error`
7. ... (add specific test cases)

### FeatureUseCaseTest
1. `invoke returns success when repository succeeds`
2. `invoke returns error when repository fails`
3. `business logic transforms data correctly`
4. ... (add specific test cases)

```kotlin
// Example UseCase test
class DeleteBookUseCaseTest {
    private val repository: BookRepository = mockk()
    private val useCase = DeleteBookUseCase(repository)

    @Test
    fun `invoke - when repository succeeds - returns Success`() = runTest {
        coEvery { repository.deleteBook("1") } returns Result.Success(Unit)

        val result = useCase("1")

        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `invoke - when repository fails - returns Error`() = runTest {
        coEvery { repository.deleteBook("1") } returns Result.Error(DataError.Local.NOT_FOUND)

        val result = useCase("1")

        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).error).isEqualTo(DataError.Local.NOT_FOUND)
    }
}
```

### FeatureRepositoryImplTest
1. `operation returns success with valid data`
2. `operation handles edge cases`
3. `operation catches exceptions and returns typed error`
4. ... (add specific test cases)

---

## String Resources to Add

### Naming Convention
Use `feature_component_description` pattern:
- Screen titles: `feature_title`
- Buttons: `feature_action_verb` (e.g., `book_action_delete`)
- Labels: `feature_label_description`
- Errors: `error_feature_description`
- Accessibility: `feature_cd_description` (cd = content description)

### UI Strings
```xml
<!-- Screen -->
<string name="feature_title">Feature Title</string>
<string name="feature_empty_title">No Items</string>
<string name="feature_empty_message">Add your first item to get started.</string>

<!-- Actions -->
<string name="feature_action_save">Save</string>
<string name="feature_action_delete">Delete</string>
<string name="feature_action_retry">Retry</string>

<!-- Labels -->
<string name="feature_label_name">Name</string>
<string name="feature_label_description">Description</string>
```

### Formatted Strings
```xml
<!-- Use positional arguments for reordering in translations -->
<string name="feature_welcome">Welcome, %1$s!</string>
<string name="feature_item_count">%1$d items in %2$s</string>
```

**Usage:**
```kotlin
stringResource(R.string.feature_welcome, userName)
stringResource(R.string.feature_item_count, count, shelfName)
```

### Plurals
```xml
<plurals name="feature_books_count">
    <item quantity="one">%d book</item>
    <item quantity="other">%d books</item>
</plurals>
```

**Usage:**
```kotlin
pluralStringResource(R.plurals.feature_books_count, count, count)
```

### Error Messages
```xml
<string name="error_feature_not_found">Item not found</string>
<string name="error_feature_save_failed">Failed to save item</string>
<string name="error_feature_network">Unable to connect. Check your internet connection.</string>
```

### Accessibility Strings
```xml
<!-- Content descriptions for icons/images -->
<string name="feature_cd_delete_button">Delete item</string>
<string name="feature_cd_edit_button">Edit item</string>
<string name="feature_cd_close_button">Close</string>

<!-- State announcements -->
<string name="feature_cd_loading">Loading items</string>
<string name="feature_cd_item_selected">Item selected</string>
```

**Key Rules:**
- Never hardcode user-visible text
- Use format arguments (`%1$s`) instead of string concatenation
- Always provide `quantity="one"` and `quantity="other"` for plurals
- Pass count twice for formatted plurals: `getQuantityString(id, count, count)`

---

## Dependencies

List any dependencies on other features or shared components:
- Reuses: `ErrorState`, `LoadingState`, `SoleMateTopAppBar`
- Depends on: (list other feature use cases if needed)

---

## Notes

Any additional implementation notes, considerations, or edge cases to handle.

---

## References

- [Guide to app architecture](https://developer.android.com/topic/architecture) - Core architecture principles
- [UI layer](https://developer.android.com/topic/architecture/ui-layer) - ViewModel, StateFlow, UDF patterns
- [Domain layer](https://developer.android.com/topic/architecture/domain-layer) - UseCase patterns
- [Data layer](https://developer.android.com/topic/architecture/data-layer) - Repository and DataSource patterns
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events) - Handling user actions and state
- [String resources](https://developer.android.com/guide/topics/resources/string-resource) - Formatting, plurals, accessibility
