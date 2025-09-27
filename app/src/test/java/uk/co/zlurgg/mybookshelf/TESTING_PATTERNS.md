# Clean Architecture Testing Patterns

This document outlines the testing patterns established for the MyBookshelf project. Follow these patterns for consistency and maintainability.

## 📁 Test Package Structure

```
app/src/test/java/uk/co/zlurgg/mybookshelf/
├── bookshelf/
│   ├── data/
│   │   ├── mappers/              # Data transformation tests
│   │   └── repository/           # Repository implementation tests
│   ├── domain/usecase/           # Business logic tests
│   │   ├── bookcase/            # Bookcase UseCase tests
│   │   ├── bookshelf/           # Bookshelf UseCase tests
│   │   ├── book_detail/         # Book detail UseCase tests
│   │   ├── deeplink/            # Import/Export UseCase tests
│   │   └── export/              # Share UseCase tests
│   └── presentation/            # UI/ViewModel tests
│       ├── bookcase/           # BookcaseViewModel tests
│       ├── bookshelf/          # BookshelfViewModel tests
│       └── book_detail/        # BookDetailViewModel tests
├── core/domain/service/         # Core service tests
└── testutil/                    # Shared test utilities
    ├── builders/               # Test data builders (TestShelfBuilder, TestBookcaseBuilder)
    ├── helpers/                # Test helpers and utilities
    │   ├── ViewModelTestHelper.kt  # StateFlow testing utilities
    │   ├── UseCaseTestHelper.kt    # UseCase mock configuration
    │   ├── TestIdGenerator.kt      # Deterministic ID generation
    │   └── TestTimeProvider.kt     # Time control for tests
    └── mocks/                  # Reusable mock implementations
        ├── MockBookcaseRepository.kt # Configurable repository mock
        └── MockUseCases.kt         # All UseCase mocks with tracking
```

## 🧪 Testing Principles

### 1. **Test One Thing at a Time**
- Each test method should focus on a single behavior
- Use descriptive test names: `creates shelf with correct data when no existing shelves`
- Clear Given-When-Then structure

### 2. **Use Reusable Mocks and Test Utilities**
- Use shared mock implementations from `testutil/mocks/`
- Use test data builders from `testutil/builders/` for consistent test data
- Use test helpers from `testutil/helpers/` for common patterns
- Avoid duplicating mock implementations across test classes

### 3. **Test Business Logic in Isolation**
- UseCase tests should mock repository dependencies
- Focus on business rules and edge cases
- Test error handling and success scenarios

### 4. **Test Presentation Logic Separately**
- ViewModel tests focus on UI state changes
- Use minimal UseCase mocks for UI testing
- Test user actions and state updates

## 🎯 Test Types and Examples

### UseCase Tests (Domain Layer)

**Pattern**: Test business logic with shared mock repositories and test builders

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

    @Test
    fun `calculates correct position when existing shelves present`() = runTest {
        // Given - Use TestShelfBuilder for consistent test data
        val existingShelves = listOf(
            TestShelfBuilder().withId("1").withPosition(0).build(),
            TestShelfBuilder().withId("2").withPosition(2).build(),
            TestShelfBuilder().withId("3").withPosition(1).build()
        )

        // When
        val result = useCase.execute("New Shelf", ShelfStyle.SilverMetal, existingShelves)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val shelf = (result as Result.Success).data
        assertEquals("Should position after highest existing shelf", 3, shelf.position)
    }
}
```

### ViewModel Tests (Presentation Layer)

**Pattern**: Test UI state changes with shared test helpers and UseCase mocks

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookcaseViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val useCaseHelper = UseCaseTestHelper()

    @After
    fun tearDown() {
        useCaseHelper.resetAll()
    }

    @Test
    fun `ShowAddDialog action toggles dialog visibility`() = runTest(testDispatcher) {
        // Given
        val viewModel = BookcaseViewModel(useCaseHelper.createBookcaseUseCases())
        val stateHelper = viewModel.state.testHelper(this)

        // When - show dialog
        val stateAfterShow = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowAddDialog(true))
        }

        // Then
        assertTrue("Should show dialog", stateAfterShow?.showAddDialog == true)

        // When - hide dialog
        val stateAfterHide = stateHelper.executeAndGetState {
            viewModel.onAction(BookcaseAction.ShowAddDialog(false))
        }

        // Then
        assertFalse("Should hide dialog", stateAfterHide?.showAddDialog == true)
        stateHelper.cleanup()
    }
}
```

### Repository Tests (Data Layer)

**Pattern**: Test data access logic with real Room/network behavior

```kotlin
@RunWith(RobolectricTestRunner::class)
class BookcaseRepositoryImplTest {
    // Test actual repository implementations
    // Use in-memory database for Room testing
    // Mock only external dependencies (network, etc.)
}
```

## 🛠️ Required Test Setup

### Dependencies
```kotlin
// Test configuration
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
```

### StateFlow Testing Pattern
```kotlin
@Test
fun `test state changes`() = runTest {
    var currentState: SomeState? = null
    val job = launch {
        viewModel.state.collect { currentState = it }
    }
    advanceUntilIdle()

    // Perform actions
    viewModel.onAction(SomeAction)
    advanceUntilIdle()

    // Assert state changes
    assertEquals("Expected state", expectedValue, currentState?.someProperty)
    job.cancel()
}
```

## ✅ Testing Checklist

### For UseCase Tests:
- [ ] Test success scenarios with valid inputs
- [ ] Test error scenarios (exceptions, invalid data)
- [ ] Test business rules and edge cases
- [ ] Test with different input combinations
- [ ] Mock repository dependencies only
- [ ] Use descriptive test names

### For ViewModel Tests:
- [ ] Test initial state
- [ ] Test each user action
- [ ] Test state updates after actions
- [ ] Test error state handling
- [ ] Use StateFlow collection pattern
- [ ] Use minimal UseCase mocking

### For Repository Tests:
- [ ] Test data persistence
- [ ] Test data retrieval
- [ ] Test error handling
- [ ] Test data mapping
- [ ] Use real Room/database when possible

## 🛠️ Shared Test Utilities

### Test Data Builders
```kotlin
// Create test shelves with fluent API
val shelf = TestShelfBuilder()
    .withId("fiction-1")
    .withName("Fiction")
    .withStyle(ShelfStyle.DarkWood)
    .withPosition(0)
    .build()

// Use predefined common test data
val commonShelves = TestShelfBuilder.createTestShelves(3)
val fictionShelf = TestShelfBuilder.fiction()
val emptyShelf = TestShelfBuilder.emptyShelf()

// Create test bookcases
val bookcase = TestBookcaseBuilder.withCommonShelves().build()
```

### Reusable Mock Repositories
```kotlin
class SomeUseCaseTest {
    private val mockRepository = MockBookcaseRepository()

    @Test
    fun `test with configured mock`() = runTest {
        // Configure mock behavior
        mockRepository.configureShelves(TestShelfBuilder.createTestShelves(2))
        mockRepository.shouldThrowException = false

        // Test your UseCase
        val result = useCase.execute()

        // Verify mock interactions
        assertTrue("Should call repository", mockRepository.addShelfCalled)
        assertEquals("Should add correct shelf", expectedShelf, mockRepository.lastAddedShelf)
    }
}
```

### ViewModel Test Helpers
```kotlin
class SomeViewModelTest {
    private val useCaseHelper = UseCaseTestHelper()

    @Test
    fun `test ViewModel state changes`() = runTest {
        val viewModel = SomeViewModel(useCaseHelper.createBookcaseUseCases())
        val stateHelper = viewModel.state.testHelper(this)

        // Execute action and get resulting state
        val newState = stateHelper.executeAndGetState {
            viewModel.onAction(SomeAction)
        }

        // Assert state changes
        assertEquals("Expected state", expectedValue, newState?.someProperty)
        stateHelper.cleanup()
    }
}
```

## 🚀 Current Test Status

- **All tests passing**: ✅ 7 tests total
- **Clean package structure**: ✅ Mirrors source architecture
- **Shared utilities**: ✅ Reusable mocks, builders, and helpers
- **Testing patterns established**: ✅ UseCase + ViewModel examples
- **Zero technical debt**: ✅ No failing or outdated tests
- **Zero duplication**: ✅ Shared utilities eliminate code duplication

## 📈 Next Steps for Test Expansion

1. **Add more UseCase tests**: Focus on business-critical UseCases
2. **Add repository tests**: Test data layer thoroughly
3. **Add integration tests**: Test complete workflows
4. **Add edge case tests**: Test error scenarios and boundary conditions

Follow these patterns for consistent, maintainable testing across the entire codebase.