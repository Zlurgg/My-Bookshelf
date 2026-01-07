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
| `feature/domain/repository/FeatureRepository.kt` | Repository interface with Result-wrapped methods |
| `feature/domain/usecase/FeatureUseCaseOne.kt` | UseCase description |
| `feature/domain/usecase/FeatureUseCases.kt` | Aggregator for multiple use cases |

### Data Layer

| File | Description |
|------|-------------|
| `feature/data/repository/FeatureRepositoryImpl.kt` | Repository implementation (mock data initially) |

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
| `test/.../FeatureRepositoryImplTest.kt` | Repository unit tests |

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
- Define repository interface in domain layer
- All methods return `Result<T, DataError>`

### Step 3: Use Cases
- Create individual use cases for each operation
- Create aggregator class if multiple use cases
- Use cases can depend on multiple repositories

### Step 4: Repository Implementation
- Create mock implementation with simulated delays
- Use UUID for generated IDs

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

**Commit: `feat(feature): Add presentation layer`**

### Step 9: Preview Data
- Extract preview data to `presentation/util/PreviewData.kt`
- Use `internal` visibility

### Step 10: DI Module
- Create Koin module
- Register repository, use cases, viewmodel
- Update AppModule to include new module

**Commit: `feat(feature): Add DI module`**

### Step 11: Tests
- ViewModel tests: state transitions, error handling, actions
- Repository tests: data operations, edge cases

**Commit: `test(feature): Add unit tests`**

### Step 12: Integration
- Update navigation
- Add error message mappings
- Add string resources

**Commit: `feat(feature): Wire navigation and add resources`**

### Step 13: Build & Verify
- Run `./gradlew assembleDebug`
- Run `./gradlew test`
- Fix any issues

### Step 14: Code Review
- Check pattern compliance
- Review for Clean Architecture violations
- Verify error handling completeness

**Commit (if fixes needed): `fix(feature): Address code review issues`**

---

## Key Patterns to Follow

### ViewModel Pattern
```kotlin
class FeatureViewModel(
    private val useCases: FeatureUseCases
) : ViewModel() {
    private val _state = MutableStateFlow(FeatureState())
    val state: StateFlow<FeatureState> = _state.asStateFlow()
    private var activeJob: Job? = null

    fun onAction(action: FeatureAction) { ... }
}
```

### State Pattern
```kotlin
data class FeatureState(
    val data: DataType? = null,
    val isLoading: Boolean = false,
    val error: DataError? = null
)
```

### Action Pattern
```kotlin
sealed interface FeatureAction {
    data object Retry : FeatureAction
    data class UpdateField(val value: String) : FeatureAction
    // ... other actions
}
```

### Screen Pattern
```kotlin
@Composable
fun FeatureScreen(...) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FeatureContent(
        state = state,
        onAction = { action ->
            when (action) {
                is FeatureAction.Navigate -> onNavigate()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
private fun FeatureContent(...) {
    when {
        state.isLoading -> LoadingState()
        state.error != null -> ErrorState(...)
        state.data == null -> EmptyState(...)
        else -> { /* Main content */ }
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
5. ... (add specific test cases)

### FeatureRepositoryImplTest
1. `operation returns success with valid data`
2. `operation handles edge cases`
3. ... (add specific test cases)

---

## String Resources to Add

```xml
<!-- Feature Screen -->
<string name="feature_title">Title</string>
<!-- ... add strings as needed -->

<!-- Error Messages -->
<string name="error_feature_specific_title">Error Title</string>
<string name="error_feature_specific_message">Error message</string>
```

---

## Dependencies

List any dependencies on other features or shared components:
- Reuses: `ErrorState`, `LoadingState`, `SoleMateTopAppBar`
- Depends on: (list other feature use cases if needed)

---

## Notes

Any additional implementation notes, considerations, or edge cases to handle.
