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
```

## 🧪 Testing Principles

### 1. **Test One Thing at a Time**
- Each test method should focus on a single behavior
- Use descriptive test names: `creates shelf with correct data when no existing shelves`
- Clear Given-When-Then structure

### 2. **Use Simple Mocks, Not Complex Fakes**
- Create focused mock implementations within test classes
- Mock only what you need for the specific test
- Avoid 15+ method fake implementations

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

**Pattern**: Test business logic with mocked repositories

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
        // ... more assertions
    }

    // Simple, focused mock
    private class MockBookcaseRepository : BookcaseRepository {
        var addShelfCalled = false
        override suspend fun addShelf(shelf: Bookshelf) {
            addShelfCalled = true
        }
        // ... minimal implementations
    }
}
```

### ViewModel Tests (Presentation Layer)

**Pattern**: Test UI state changes with minimal UseCase mocking

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookcaseViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `ShowAddDialog action toggles dialog visibility`() = runTest {
        // Given
        val viewModel = BookcaseViewModel(createMinimalUseCases())
        var currentState: BookcaseState? = null
        val job = launch {
            viewModel.state.collect { currentState = it }
        }

        // When
        viewModel.onAction(BookcaseAction.ShowAddDialog(true))
        advanceUntilIdle()

        // Then
        assertTrue("Should show dialog", currentState?.showAddDialog == true)
        job.cancel()
    }

    // Minimal mocking for UI testing
    private fun createMinimalUseCases() = BookcaseUseCases(...)
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

## 🚀 Current Test Status

- **All tests passing**: ✅ 7 tests total
- **Clean package structure**: ✅ Mirrors source architecture
- **Testing patterns established**: ✅ UseCase + ViewModel examples
- **Zero technical debt**: ✅ No failing or outdated tests

## 📈 Next Steps for Test Expansion

1. **Add more UseCase tests**: Focus on business-critical UseCases
2. **Add repository tests**: Test data layer thoroughly
3. **Add integration tests**: Test complete workflows
4. **Add edge case tests**: Test error scenarios and boundary conditions

Follow these patterns for consistent, maintainable testing across the entire codebase.