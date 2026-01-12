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
│   │   ├── bookclub/            # Book club UseCase tests (16 use cases)
│   │   ├── deeplink/            # Import/Export UseCase tests
│   │   └── export/              # Share UseCase tests
│   └── presentation/            # UI/ViewModel tests
│       ├── bookcase/           # BookcaseViewModel tests
│       ├── bookshelf/          # BookshelfViewModel tests
│       └── book_detail/        # BookDetailViewModel tests
├── core/domain/service/         # Core service tests
└── testutil/                    # Shared test utilities
    ├── builders/               # Test data builders (TestShelfBuilder, TestBookBuilder)
    ├── helpers/                # Test helpers and utilities
    │   ├── ViewModelTestHelper.kt  # StateFlow testing utilities
    │   ├── TestIdGenerator.kt      # Deterministic ID generation
    │   └── TestTimeProvider.kt     # Time control for tests
    └── mocks/                  # Reusable mock implementations
        ├── MockBookcaseRepository.kt # Configurable repository mock
        ├── MockBookRepository.kt     # Book repository mock
        ├── MockBookshelfRepository.kt # Bookshelf repository mock
        ├── MockBookClubRepository.kt # Book club repository mock
        └── Mock*UseCase.kt           # Individual UseCase mocks for ViewModels
```

## 🧪 Testing Principles

### 1. **Write Focused Unit Tests**
- **Unit scope**: Test one class/file in isolation, mock external dependencies
- **Not integration scope**: Don't test multiple classes together (save for integration tests)
- **Pragmatic assertions**: Multiple related assertions in one test are fine if testing the same behavior
- **Examples**:
  - ✅ GOOD: Test all error messages from ErrorFormatter in grouped tests (same class, same logic)
  - ✅ GOOD: Test shelf creation with multiple assertions (id, name, position, style) - all part of "creates shelf correctly"
  - ❌ BAD: Test UseCase + Repository + Database together (integration test - belongs elsewhere)
  - ❌ BAD: Test ViewModel that calls real UseCase that calls real Repository (too broad)
- Use descriptive test names that explain the scenario being tested
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

**Philosophy**: ViewModel tests focus on **UI state changes**, not business logic. Business logic is tested in the UseCase layer. Use minimal inline mocks that return success/error to trigger state changes.

**Pattern**: Test UI state changes with simplified inline mocks

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookshelfViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Simplified inline mocks for UI testing
    private val mockShareBookshelf = SimpleShareBookshelfUseCase()
    private val mockGetShelfById = MockGetShelfByIdUseCase()

    @After
    fun tearDown() {
        mockShareBookshelf.reset()
        mockGetShelfById.reset()
    }

    private fun createViewModel(shelfId: String = "test-shelf"): BookshelfViewModel {
        val bookshelfUseCases = BookshelfUseCases(
            searchBooks = SimpleSearchBooksUseCase(),
            getShelfBooks = SimpleGetShelfBooksUseCase(),
            addBookToShelf = SimpleAddBookToShelfUseCase(),
            removeBookFromShelf = SimpleRemoveBookFromShelfUseCase(),
            upsertBook = SimpleUpsertBookUseCase(),
            shareBookshelf = mockShareBookshelf
        )
        val bookcaseUseCases = BookcaseUseCases(
            getAllShelves = MockGetAllShelvesUseCase(),
            createShelf = MockCreateShelfUseCase(),
            deleteShelf = MockDeleteShelfUseCase(),
            reorderShelves = MockReorderShelvesUseCase(),
            getShelfById = mockGetShelfById
        )
        return BookshelfViewModel(bookshelfUseCases, bookcaseUseCases, shelfId)
    }

    @Test
    fun `share shelf success updates state correctly`() = runTest(testDispatcher) {
        // Given
        mockShareBookshelf.shouldSucceed = true
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterShare = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnShareShelf)
        }

        // Then
        assertTrue("Should set share success flag", stateAfterShare?.shareSuccess == true)
        assertFalse("Should clear loading flag", stateAfterShare?.isShareLoading == true)
        stateHelper.cleanup()
    }

    @Test
    fun `share shelf error updates error message`() = runTest(testDispatcher) {
        // Given
        mockShareBookshelf.shouldSucceed = false
        mockGetShelfById.shelfToReturn = TestShelfBuilder().build()
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // When
        val stateAfterShare = stateHelper.executeAndGetState {
            viewModel.onAction(BookshelfAction.OnShareShelf)
        }

        // Then
        assertNotNull("Should set error message", stateAfterShare?.errorMessage)
        assertFalse("Should clear loading flag", stateAfterShare?.isShareLoading == true)
        stateHelper.cleanup()
    }

    // Simplified inline mock for UI testing
    private class SimpleShareBookshelfUseCase : ShareBookshelfUseCase {
        var shouldSucceed = true

        override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)

        fun reset() {
            shouldSucceed = true
        }
    }
}
```

**Key ViewModel Testing Principles**:

1. **Focus on UI State**: Test state transitions, not business logic
2. **Minimal Mocks**: Use simple inline mocks that return success/error
3. **No Business Logic**: Business rules are tested in UseCase layer
4. **State Collection**: Use `testHelper(this)` for proper StateFlow testing
5. **Cleanup**: Always call `stateHelper.cleanup()` after assertions
6. **Test Coverage**: Test success paths, error paths, and loading states

**Common ViewModel Test Scenarios**:
- Initial state loading
- Action triggers state change
- Success state updates
- Error state updates
- Loading state management
- Dialog visibility toggles
- Navigation callbacks

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

### Service Layer Tests (Business Services)

**Pattern**: Test service logic with shared mock repositories and proper data structures

**Philosophy**: Service layer tests focus on **business logic coordination** (serialization, validation, orchestration). Mock repository dependencies and test the service's specific responsibilities.

```kotlin
class JsonBookshelfSerializerTest {
    private val mockTimeProvider = SimpleMockTimeProvider()
    private val mockIdGenerator = SimpleMockIdGenerator()
    private val exportMapper = BookshelfExportMapper(mockTimeProvider, mockIdGenerator)
    private val serializer = JsonBookshelfSerializer(exportMapper)

    @Before
    fun setup() {
        mockTimeProvider.currentTime = 1704067200000L
    }

    @Test
    fun `serialize converts shelf to valid JSON string`() {
        // Given - Use TestShelfBuilder for consistent test data
        val shelf = TestShelfBuilder()
            .withName("Fiction")
            .withStyle(ShelfStyle.DarkWood)
            .build()

        // When
        val result = serializer.serialize(shelf)

        // Then
        assertTrue("Should succeed", result is Result.Success)
        val jsonString = (result as Result.Success).data
        assertTrue("Should contain shelf name", jsonString.contains("Fiction"))
    }

    @Test
    fun `deserialize handles invalid JSON`() {
        // Given
        val invalidJson = "{ invalid json }"

        // When
        val result = serializer.deserialize(invalidJson)

        // Then
        assertTrue("Should fail", result is Result.Error)
        assertEquals("Should return serialization error",
            DataError.Local.SERIALIZATION_ERROR,
            (result as Result.Error).error)
    }

    // Simplified mocks for time/ID generation
    private class SimpleMockTimeProvider : TimeProvider {
        var currentTime = 0L
        override fun currentTimeMillis(): Long = currentTime
    }

    private class SimpleMockIdGenerator : IdGenerator {
        var nextId = "test-id"
        override fun generateId(): String = nextId
    }
}
```

**Key Service Layer Testing Principles**:

1. **Mock Repository Dependencies**: Use shared mocks from `testutil/mocks/`
2. **Test Business Logic**: Focus on validation, serialization, orchestration
3. **Use Proper Data Structures**: Create test data matching actual export formats (no fake fields)
4. **Test Error Handling**: Verify error scenarios and exception handling
5. **Simple Mocks for Utilities**: Inline mocks for TimeProvider, IdGenerator when needed
6. **Avoid Redundant Checks**: Don't check types that are already guaranteed by Result unwrapping

**Common Service Layer Test Scenarios**:

**Serialization Services**:
```kotlin
@Test
fun `serialize and deserialize round trip preserves data`() {
    val originalShelf = TestShelfBuilder().withName("Test").build()
    val jsonResult = serializer.serialize(originalShelf)
    val deserializeResult = serializer.deserialize((jsonResult as Result.Success).data)

    val exportData = (deserializeResult as Result.Success).data
    assertEquals("Should preserve name", "Test", exportData.bookshelf.name)
}
```

**Validation Services**:
```kotlin
@Test
fun `validateFormat rejects unsupported format version`() = runTest {
    // Given
    val futureVersion = createExportData(formatVersion = 2, shelfName = "Test")

    // When
    val result = validator.validateFormat(futureVersion)

    // Then
    assertTrue("Should fail", result is Result.Error)
    assertEquals("Should return unsupported format error",
        DataError.Local.UNSUPPORTED_FORMAT_VERSION,
        (result as Result.Error).error)
}

@Test
fun `checkNameConflict returns conflicting name when duplicate found`() = runTest {
    // Given - Use shared mock repository
    mockBookcaseRepository.configureShelves(listOf(
        TestShelfBuilder().withName("Fiction").build()
    ))

    // When
    val result = validator.checkNameConflict("Fiction")

    // Then
    assertTrue("Should succeed", result is Result.Success)
    assertEquals("Should find conflict", "Fiction", (result as Result.Success).data)
}
```

**Orchestration Services** (coordinating multiple repositories):
```kotlin
@Test
fun `loadShelfForExport loads shelf with books successfully`() = runTest {
    // Given - Configure multiple mock repositories
    val shelf = TestShelfBuilder().withId("shelf-1").build()
    val book1 = TestBookBuilder().withId("book-1").build()
    val book2 = TestBookBuilder().withId("book-2").build()

    mockBookcaseRepository.configureShelves(listOf(shelf))
    mockBookshelfRepository.configureBooksForShelf("shelf-1", listOf(book1, book2))

    // When
    val result = orchestrator.loadShelfForExport("shelf-1")

    // Then
    assertTrue("Should succeed", result is Result.Success)
    val loadedShelf = (result as Result.Success).data
    assertEquals("Should have 2 books", 2, loadedShelf.books.size)
}

@Test
fun `importShelfToDatabase coordinates all repositories correctly`() = runTest {
    // Given
    val book = TestBookBuilder().withId("book-1").build()
    val shelf = TestShelfBuilder()
        .withId("shelf-1")
        .withBooks(listOf(book))
        .build()

    // When
    val result = orchestrator.importShelfToDatabase(shelf)

    // Then
    assertTrue("Should succeed", result is Result.Success)
    assertEquals("Should save book", book, mockBookRepository.getBookById("book-1"))
    assertTrue("Should call addShelf", mockBookcaseRepository.addShelfCalled)

    // Verify relationships were created
    val relations = mockBookshelfRepository.getShelfBookRelations()
    assertTrue("Should link book to shelf", relations["shelf-1"]?.contains("book-1") == true)
}
```

**Exception Handling in Services**:
```kotlin
@Test
fun `service handles repository exception gracefully`() = runTest {
    // Given - Configure mock to throw exception
    mockBookcaseRepository.shouldThrowException = true

    // When
    val result = service.performOperation("test-id")

    // Then
    assertTrue("Should fail", result is Result.Error)
    // Note: No need to check if error is DataError.Local - it always will be
    // after ErrorMapper.mapExceptionToDataError() processing
}
```

### Instrumented Tests (Android Device/Emulator Tests)

**Philosophy**: Instrumented tests run on real Android devices/emulators and are used for testing Android-specific APIs that cannot be accurately simulated with Robolectric. Use sparingly for critical infrastructure that requires real Android behavior.

**Location**: `app/src/androidTest/java/uk/co/zlurgg/mybookshelf/`

**When to Use Instrumented Tests**:
- ✅ Database migrations with real SQLite (MigrationTestHelper)
- ✅ Android platform APIs (android.util.Base64, android.os.*, etc.)
- ✅ UI tests requiring real rendering (Compose UI tests)
- ❌ Business logic (use unit tests)
- ❌ ViewModels (use unit tests with Robolectric)
- ❌ Repositories (use unit tests with in-memory Room)

**Key Differences from Unit Tests**:
1. **Test Runner**: Use `@RunWith(AndroidJUnit4::class)` instead of `@RunWith(RobolectricTestRunner::class)`
2. **Function Names**: Use camelCase (e.g., `testMigrationPreservesData()`) - **backticks not supported**
3. **Execution**: Requires device/emulator (`./gradlew connectedAndroidTest`)
4. **Speed**: Slower (~30 seconds vs ~8 seconds for unit tests)
5. **Dependencies**: Use `androidTestImplementation` in gradle

**Pattern**: Database migration testing with MigrationTestHelper

```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB_NAME = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BookshelfDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3RemovesOnShelfColumnPreservesBookData() {
        // Given - Create database at version 2 with sample book
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            execSQL("""
                INSERT INTO BookEntity (id, title, description, imageUrl, languages, authors,
                firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions,
                purchased, onShelf, affiliateLink, spineColor)
                VALUES ('book-1', 'Test Book', 'Test description', 'https://example.com/cover.jpg',
                '["en"]', '["Test Author"]', '2020', 4.5, 100, 300, 5, 0, 1,
                'https://example.com/buy', -16711936)
            """.trimIndent())
            close()
        }

        // When - Migrate to version 3
        val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, MIGRATION_2_3)

        // Then - Verify onShelf column removed and data preserved
        val cursor = db.query("SELECT * FROM BookEntity WHERE id = 'book-1'")
        assertTrue("Should have book data", cursor.moveToFirst())
        assertEquals("onShelf column should be removed", -1, cursor.getColumnIndex("onShelf"))
        assertEquals("book-1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Test Book", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        cursor.close()
    }

    @Test
    fun migrateAll2To5PreservesCompleteDataIntegrity() {
        // Given - Create database at version 2 with comprehensive test data
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            // Insert book, shelf, and cross-reference
            execSQL("INSERT INTO BookEntity (...) VALUES (...)")
            execSQL("INSERT INTO BookshelfEntity (...) VALUES (...)")
            execSQL("INSERT INTO BookshelfBookCrossRef (...) VALUES (...)")
            close()
        }

        // When - Migrate through all versions 2→3→4→5
        val db = helper.runMigrationsAndValidate(
            TEST_DB_NAME, 5, true,
            MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
        )

        // Then - Verify complete data integrity across all tables
        // Verify book data, shelf data, and relationships survived migrations
    }
}
```

**Pattern**: Testing Android platform APIs

```kotlin
@RunWith(AndroidJUnit4::class)
class Base64EncoderTest {
    @Test
    fun encodeSimpleStringAndDecodeBackReturnsOriginal() {
        // Given
        val original = "Hello, World!"

        // When
        val encoded = Base64Encoder.encode(original)
        val decoded = Base64Encoder.decode(encoded)

        // Then
        assertEquals(original, decoded)
    }

    @Test
    fun encodeProducesUrlSafeCharactersOnly() {
        // Given
        val testData = "Test data with special chars: +/=[]{}()"

        // When
        val encoded = Base64Encoder.encode(testData)

        // Then - Uses real android.util.Base64.URL_SAFE implementation
        assertFalse("Encoded string should not contain '+'", encoded.contains('+'))
        assertFalse("Encoded string should not contain '/'", encoded.contains('/'))
        assertEquals(testData, Base64Encoder.decode(encoded))
    }
}
```

**Required Dependencies** (app/build.gradle.kts):
```kotlin
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
androidTestImplementation(libs.room.testing)  // For MigrationTestHelper
```

**Key Instrumented Testing Principles**:

1. **Function Naming**: Always use camelCase - backticks cause compilation errors
2. **Minimal Scope**: Only test what requires real Android (migrations, platform APIs)
3. **Real Behavior**: Test with actual Android implementations, not mocks
4. **Data Integrity**: Focus on data preservation across migrations
5. **Fast Unit Tests First**: Use instrumented tests as supplement, not replacement

**Common Instrumented Test Scenarios**:

**Database Migrations**:
```kotlin
@Test
fun migrate3To4AddsPositionColumnWithDefaultValueZero() {
    helper.createDatabase(TEST_DB_NAME, 3).apply {
        execSQL("INSERT INTO BookshelfEntity (id, name, shelfMaterial) VALUES ('shelf-1', 'Test', 'DARK_WOOD')")
        close()
    }

    val db = helper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, MIGRATION_3_4)

    val cursor = db.query("SELECT * FROM BookshelfEntity WHERE id = 'shelf-1'")
    assertTrue(cursor.moveToFirst())
    assertEquals(0, cursor.getInt(cursor.getColumnIndex("position")))
    cursor.close()
}
```

**Android Platform APIs**:
```kotlin
@Test
fun encodeCompressesDataSignificantly() {
    // Given - Test real GZip compression with android.util.Base64
    val repetitiveData = "AAAAAAAA".repeat(100)

    // When - Uses real Android compression
    val encoded = Base64Encoder.encode(repetitiveData)

    // Then - Verify actual compression ratio
    assertTrue(encoded.length < repetitiveData.length / 4)
}
```

### Integration Tests (Medium-Scope Tests - 20% of Test Pyramid)

**Philosophy**: Integration tests verify interactions between 2-3 components with real implementations. They test data flow through multiple layers but stop short of full E2E workflows.

**Location**: `app/src/androidTest/java/uk/co/zlurgg/mybookshelf/`

**Scope**: According to Google's testing pyramid (70/20/10), integration tests should represent ~20% of total tests. They verify component interactions without testing complete user workflows.

**When to Use Integration Tests**:
- ✅ Testing Repository + Real Room Database
- ✅ Testing Service + Multiple Repositories
- ✅ Testing Serialization + Mapper + Data structures
- ✅ Testing validation logic with database queries
- ❌ Complete user workflows (use E2E tests)
- ❌ Single class behavior (use unit tests)
- ❌ UI rendering (use Compose UI tests)

**Key Characteristics**:
1. **Real Implementations**: Use actual Room database, real services, real mappers
2. **Limited Scope**: Test 2-3 components together, not entire stack
3. **Fast Execution**: Faster than E2E tests (~2-3 seconds per test)
4. **Focused Testing**: Verify specific integration points, not workflows
5. **Stub External Dependencies**: Mock network, external services

**Pattern**: Service + Repositories Integration

```kotlin
@RunWith(AndroidJUnit4::class)
class DataOrchestratorIntegrationTest {

    private lateinit var database: BookshelfDatabase
    private lateinit var orchestrator: DatabaseBookshelfDataOrchestrator
    private lateinit var bookcaseRepository: BookcaseRepositoryImpl
    private lateinit var bookshelfRepository: BookshelfRepositoryImpl
    private lateinit var bookRepository: BookRepositoryImpl

    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1000L
    }

    // Stub RemoteBookDataSource - not used in these tests
    private val stubRemoteDataSource = object : RemoteBookDataSource {
        override suspend fun searchBooks(/*...*/): Result<SearchResponseDto, DataError.Remote> {
            throw NotImplementedError("Not used in integration tests")
        }
        override suspend fun getBookDetails(/*...*/): Result<BookWorkDto, DataError.Remote> {
            throw NotImplementedError("Not used in integration tests")
        }
    }

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        bookcaseRepository = BookcaseRepositoryImpl(database.bookshelfDao)
        bookshelfRepository = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
        bookRepository = BookRepositoryImpl(stubRemoteDataSource, database.bookshelfDao)

        orchestrator = DatabaseBookshelfDataOrchestrator(
            bookcaseRepository,
            bookshelfRepository,
            bookRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadShelfForExportWithBooks() = runTest {
        // Given - Shelf with books in database
        val shelfId = "shelf-1"
        val book1 = createTestBook("book-1", "Book One")
        val book2 = createTestBook("book-2", "Book Two")

        val shelf = Bookshelf(
            id = shelfId,
            name = "Test Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )
        bookcaseRepository.addShelf(shelf)
        bookRepository.upsertBook(book1)
        bookRepository.upsertBook(book2)
        bookshelfRepository.addBookToShelf(shelfId, book1.id)
        bookshelfRepository.addBookToShelf(shelfId, book2.id)

        // When - Load for export
        val result = orchestrator.loadShelfForExport(shelfId)

        // Then - Should succeed with shelf and books
        assertTrue("Load should succeed", result is Result.Success)
        val loadedShelf = (result as Result.Success).data
        assertEquals("Test Shelf", loadedShelf.name)
        assertEquals(2, loadedShelf.books.size)
    }
}
```

**Pattern**: Serialization + Mapper Integration

```kotlin
@RunWith(AndroidJUnit4::class)
class SerializationIntegrationTest {

    private lateinit var serializer: JsonBookshelfSerializer
    private lateinit var mapper: BookshelfExportMapper

    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1000L
    }

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-id-${counter++}"
    }

    @Before
    fun setup() {
        mapper = BookshelfExportMapper(testTimeProvider, testIdGenerator)
        serializer = JsonBookshelfSerializer(mapper)
    }

    @Test
    fun serializeDeserializeRoundTrip() = runTest {
        // Given - Bookshelf with books
        val book = Book(
            id = "book-1",
            title = "Test Book",
            authors = listOf("Test Author"),
            // ... other fields
        )
        val shelf = Bookshelf(
            id = "shelf-1",
            name = "Fiction",
            books = listOf(book),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )

        // When - Serialize
        val serializeResult = serializer.serialize(shelf)
        assertTrue("Serialize should succeed", serializeResult is Result.Success)
        val jsonString = (serializeResult as Result.Success).data

        // Then - JSON should be valid and contain expected data
        assertTrue("Should contain shelf name", jsonString.contains("Fiction"))
        assertTrue("Should contain book title", jsonString.contains("Test Book"))

        // And - Deserialize should reconstruct data
        val deserializeResult = serializer.deserialize(jsonString)
        assertTrue("Deserialize should succeed", deserializeResult is Result.Success)
        val exportData = (deserializeResult as Result.Success).data
        assertEquals("Fiction", exportData.bookshelf.name)
        assertEquals(1, exportData.bookshelf.books.size)
    }
}
```

**Pattern**: Validation + Repository Integration

```kotlin
@RunWith(AndroidJUnit4::class)
class ValidatorIntegrationTest {

    private lateinit var database: BookshelfDatabase
    private lateinit var bookcaseRepository: BookcaseRepositoryImpl
    private lateinit var validator: BookshelfImportValidatorImpl

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        bookcaseRepository = BookcaseRepositoryImpl(database.bookshelfDao)
        validator = BookshelfImportValidatorImpl(bookcaseRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun checkNameConflictReturnsNameWhenConflictExists() = runTest {
        // Given - Database with existing shelf
        val existingShelf = Bookshelf(
            id = "shelf-1",
            name = "Fiction",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )
        bookcaseRepository.addShelf(existingShelf)

        // When - Check for same name
        val result = validator.checkNameConflict("Fiction")

        // Then - Should return conflicting name
        assertTrue("Check should succeed", result is Result.Success)
        assertEquals("Fiction", (result as Result.Success).data)
    }
}
```

**Key Integration Testing Principles**:

1. **Real Database**: Always use Room in-memory database for integration tests
2. **Stub External Services**: Mock network calls, but use real repositories
3. **Test Data Setup**: Create test data through repository methods (realistic flow)
4. **Cleanup**: Always close database in `@After` tearDown
5. **Focus on Boundaries**: Test where components interact, not internal logic
6. **Multiple Assertions**: Verify data flows through all layers correctly

**Common Integration Test Scenarios**:

**Repository + Database**:
```kotlin
@Test
fun addShelfPersistsToRealDatabase() = runTest {
    // Given
    val shelf = Bookshelf(/*...*/)

    // When
    repository.addShelf(shelf)

    // Then - Verify via database
    val retrieved = repository.getShelfById(shelf.id)
    assertEquals(shelf.name, retrieved?.name)
}
```

**Service Orchestration**:
```kotlin
@Test
fun importShelfToDatabaseWithBooks() = runTest {
    // Given - Import data
    val book = createTestBook("book-1", "Test")
    val shelf = Bookshelf(id = "shelf-1", name = "Test", books = listOf(book), /*...*/)

    // When - Import through orchestrator
    val result = orchestrator.importShelfToDatabase(shelf)

    // Then - Verify all components updated
    assertTrue(result is Result.Success)
    assertNotNull(bookcaseRepository.getShelfById("shelf-1"))
    assertNotNull(bookRepository.getBookById("book-1"))
    val booksInShelf = bookshelfRepository.getBooksForShelf("shelf-1").first()
    assertEquals(1, booksInShelf.size)
}
```

**Validation + Query Integration**:
```kotlin
@Test
fun validateFormatRejectsBlankShelfName() = runTest {
    // Given
    val exportData = BookshelfExportData(
        formatVersion = 1,
        exportedAt = "2024-01-01T00:00:00",
        appName = "My Bookshelf",
        bookshelf = ExportedBookshelf(
            name = "   ",  // Blank
            shelfStyle = ShelfStyle.DarkWood,
            books = emptyList()
        )
    )

    // When
    val result = validator.validateFormat(exportData)

    // Then
    assertTrue(result is Result.Error)
    assertEquals(DataError.Local.VALIDATION_ERROR, (result as Result.Error).error)
}
```

### E2E Tests (Large-Scope Tests - 10% of Test Pyramid)

**Philosophy**: E2E (End-to-End) tests verify complete user workflows from ViewModel through UseCase, Repository, to Database. They use real implementations for all components except external dependencies.

**Location**: `app/src/androidTest/java/uk/co/zlurgg/mybookshelf/`

**Scope**: According to Google's testing pyramid (70/20/10), E2E tests should represent ~10% of total tests. Google calls them "Release Candidate" or "Large" tests - they verify critical user workflows work end-to-end.

**When to Use E2E Tests**:
- ✅ Critical user workflows (create shelf, add book, delete shelf)
- ✅ Complete feature flows from UI action to database persistence
- ✅ Integration of all layers: ViewModel → UseCase → Repository → Database
- ✅ Regression prevention for core features
- ❌ UI rendering (use Compose UI tests instead)
- ❌ Business logic details (use unit tests)
- ❌ Component interactions (use integration tests)

**Key Characteristics**:
1. **Full Stack**: Test ViewModel → UseCases → Repositories → Database
2. **Real Implementations**: Use actual components, not mocks (except external services)
3. **User Workflows**: Test complete user actions from start to finish
4. **State + Persistence**: Verify both UI state updates AND database persistence
5. **Slow But Comprehensive**: ~5-10 seconds per test, but provides high confidence

**Pattern**: Complete User Workflow E2E Test

```kotlin
@RunWith(AndroidJUnit4::class)
class ShelfCreationE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var viewModel: BookcaseViewModel

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-shelf-${counter++}"
    }

    @Before
    fun setup() {
        // Setup real database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        // Setup repository
        val repository = BookcaseRepositoryImpl(database.bookshelfDao)

        // Setup use cases
        val useCases = BookcaseUseCases(
            getAllShelves = GetAllShelvesUseCaseImpl(repository),
            createShelf = CreateShelfUseCaseImpl(repository, testIdGenerator),
            deleteShelf = DeleteShelfUseCaseImpl(repository),
            reorderShelves = ReorderShelvesUseCaseImpl(repository),
            getShelfById = GetShelfByIdUseCaseImpl(repository)
        )

        // Setup ViewModel with full dependency chain
        viewModel = BookcaseViewModel(useCases)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createShelfUpdatesStateAndPersistsToDatabase() = runTest {
        // Given - Initial state with no shelves
        val initialState = viewModel.state.first()
        assertEquals(0, initialState.bookshelves.size)
        assertFalse(initialState.showAddDialog)

        // When - User creates a new shelf
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Fiction", ShelfStyle.DarkWood))

        // Then - ViewModel state should update
        val updatedState = viewModel.state.first()
        assertEquals(1, updatedState.bookshelves.size)
        assertEquals("Fiction", updatedState.bookshelves[0].name)
        assertEquals(ShelfStyle.DarkWood, updatedState.bookshelves[0].shelfStyle)
        assertTrue(updatedState.operationSuccess)
        assertNull(updatedState.errorMessage)

        // And - Shelf should persist in database
        val persistedShelf = database.bookshelfDao.getShelfById("test-shelf-0")
        assertEquals("Fiction", persistedShelf?.name)
        assertEquals("DarkWood", persistedShelf?.shelfMaterial)
    }

    @Test
    fun createMultipleShelvesAssignsCorrectPositions() = runTest {
        // Given - No existing shelves
        val initialState = viewModel.state.first()
        assertEquals(0, initialState.bookshelves.size)

        // When - User creates three shelves
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Fiction", ShelfStyle.DarkWood))
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Non-Fiction", ShelfStyle.SilverMetal))
        viewModel.onAction(BookcaseAction.OnAddBookshelfClick("Science", ShelfStyle.WhiteMetal))

        // Then - All shelves should be in state with correct positions
        val state = viewModel.state.first()
        assertEquals(3, state.bookshelves.size)
        assertEquals(0, state.bookshelves[0].position)
        assertEquals(1, state.bookshelves[1].position)
        assertEquals(2, state.bookshelves[2].position)

        // And - Order should match in database
        val allShelves = database.bookshelfDao.getAllShelves().first()
        assertEquals(3, allShelves.size)
        assertEquals("Fiction", allShelves[0].name)
        assertEquals("Non-Fiction", allShelves[1].name)
        assertEquals("Science", allShelves[2].name)
    }
}
```

**Pattern**: Book Addition E2E Test

```kotlin
@RunWith(AndroidJUnit4::class)
class BookAdditionE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var bookshelfViewModel: BookshelfViewModel
    private val testShelfId = "test-shelf-1"

    // Stub external dependencies only
    private val stubRemoteDataSource = object : RemoteBookDataSource {
        override suspend fun searchBooks(/*...*/): Result<SearchResponseDto, DataError.Remote> {
            throw NotImplementedError("Not used in E2E tests")
        }
        override suspend fun getBookDetails(/*...*/): Result<BookWorkDto, DataError.Remote> {
            throw NotImplementedError("Not used in E2E tests")
        }
    }

    private val stubExportService = object : BookshelfExportService {
        override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
            throw NotImplementedError("Not used in E2E tests")
        }
        // ... other interface methods
    }

    @Before
    fun setup() {
        // Setup real database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        // Setup real repositories
        val bookcaseRepo = BookcaseRepositoryImpl(database.bookshelfDao)
        val bookshelfRepo = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
        val bookRepo = BookRepositoryImpl(stubRemoteDataSource, database.bookshelfDao)

        // Create test shelf
        runTest {
            val testShelf = Bookshelf(
                id = testShelfId,
                name = "Test Shelf",
                books = emptyList(),
                shelfStyle = ShelfStyle.DarkWood,
                position = 0
            )
            bookcaseRepo.addShelf(testShelf)
        }

        // Setup use cases
        val bookshelfUseCases = BookshelfUseCases(
            searchBooks = SearchBooksUseCaseImpl(stubRemoteDataSource, BookSorter()),
            getShelfBooks = GetShelfBooksUseCaseImpl(bookshelfRepo),
            addBookToShelf = AddBookToShelfUseCaseImpl(bookRepo, bookshelfRepo),
            removeBookFromShelf = RemoveBookFromShelfUseCaseImpl(bookshelfRepo),
            upsertBook = UpsertBookUseCaseImpl(bookRepo),
            shareBookshelf = ShareBookshelfUseCaseImpl(stubExportService)
        )

        val bookcaseUseCases = BookcaseUseCases(/* ... */)

        // Setup ViewModel with full dependency chain
        bookshelfViewModel = BookshelfViewModel(bookshelfUseCases, bookcaseUseCases, testShelfId)
    }

    @Test
    fun addBookToShelfUpdatesStateAndPersistsToDatabase() = runTest {
        // Given - A book to add
        val book = createTestBook("book-1", "Test Book")

        // When - User adds book to shelf
        bookshelfViewModel.onAction(BookshelfAction.OnAddBookClick(book))

        // Then - ViewModel state should update
        val state = bookshelfViewModel.state.first()
        assertEquals(1, state.books.size)
        assertEquals("Test Book", state.books[0].title)
        assertEquals(null, state.errorMessage)

        // And - Book should persist in database
        val booksInShelf = database.bookshelfDao.getBooksForShelf(testShelfId).first()
        assertEquals(1, booksInShelf.size)
        assertEquals("book-1", booksInShelf[0].id)
    }
}
```

**Key E2E Testing Principles**:

1. **Full Dependency Chain**: Instantiate complete ViewModel → UseCases → Repositories → Database
2. **Real Implementations**: Use actual classes, not mocks (except network/external services)
3. **Test User Actions**: Simulate real user interactions via ViewModel actions
4. **Verify State + Persistence**: Check both UI state updates AND database changes
5. **Stub External Only**: Only stub network calls, external services - everything else is real
6. **Clean Setup/Teardown**: Always close database, use test utilities for deterministic IDs/time
7. **Focus on Workflows**: Test complete feature flows, not individual methods
8. **Minimal Test Count**: Only test critical user workflows (~5-10 E2E tests total)

**Common E2E Test Workflows**:

1. **Shelf Creation** - User creates shelf → State updates → Database persists
2. **Book Addition** - User adds book → Book upserted → Shelf-book relation created
3. **Book Removal** - User removes book → State updates → Relation deleted
4. **Shelf Deletion** - User deletes shelf → State updates → Shelf + relations deleted
5. **Shelf Reordering** - User reorders shelves → Positions recalculated → Database updated

**E2E Test Structure**:

```kotlin
@RunWith(AndroidJUnit4::class)
class SomeWorkflowE2ETest {

    // Real components
    private lateinit var database: BookshelfDatabase
    private lateinit var viewModel: SomeViewModel

    // Test utilities for deterministic behavior
    private val testIdGenerator = object : IdGenerator { /* ... */ }
    private val testTimeProvider = object : TimeProvider { /* ... */ }

    // Stubs for external dependencies only
    private val stubNetworkService = object : NetworkService { /* ... */ }

    @Before
    fun setup() {
        // 1. Create real in-memory database
        database = Room.inMemoryDatabaseBuilder(/* ... */).build()

        // 2. Create real repositories with real database
        val repository = RepositoryImpl(database.dao, testTimeProvider)

        // 3. Create real use cases with real repositories
        val useCases = UseCases(/* real use case implementations */)

        // 4. Create ViewModel with complete real dependency chain
        viewModel = ViewModel(useCases)

        // 5. Optionally setup initial test data
        runTest {
            repository.addInitialData(/* ... */)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeUserWorkflow() = runTest {
        // Given - Initial state
        val initialState = viewModel.state.first()
        // Assert initial conditions

        // When - User performs action
        viewModel.onAction(SomeAction(/* ... */))

        // Then - Verify state updates
        val updatedState = viewModel.state.first()
        // Assert state changes

        // And - Verify database persistence
        val persistedData = database.dao.getData()
        // Assert database changes
    }
}
```

**Differences from Integration Tests**:

| Aspect | Integration Test | E2E Test |
|--------|-----------------|----------|
| **Scope** | 2-3 components | Full stack (4+ components) |
| **Entry Point** | Service/Repository | ViewModel |
| **Focus** | Component boundaries | User workflows |
| **Duration** | ~2-3 seconds | ~5-10 seconds |
| **Quantity** | ~20% of tests | ~10% of tests |
| **Example** | Serializer + Mapper | Shelf creation flow |

**When to Add E2E Tests**:
- ✅ New critical user feature
- ✅ Complex workflow with multiple steps
- ✅ Feature that touches many layers
- ✅ Regression prevention for core flows
- ❌ Minor UI tweaks
- ❌ Simple CRUD operations (use integration tests)
- ❌ Business rule variations (use unit tests)

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

## ⚠️ Common Pitfalls to Avoid (Lessons Learned)

### ❌ Flow Exception Handling in Mocks
```kotlin
// ❌ BAD: Flow doesn't check shouldThrowException flag
override fun getAllShelves(): Flow<List<Bookshelf>> = flowOf(shelvesToReturn)

// This won't throw even when shouldThrowException = true!
// The Flow is created immediately, not when collected

// ✅ GOOD: Use flow builder to check exception flag during collection
override fun getAllShelves(): Flow<List<Bookshelf>> = flow {
    if (shouldThrowException) throw RuntimeException("Test exception")
    emit(shelvesToReturn)
}
```

**Why this matters**: Tests that expect exceptions from Flow-returning methods will silently fail if the mock doesn't use the `flow { }` builder. Always use `flow { }` for mocks that need to support exception throwing.

### ❌ Redundant Instance Checks After Type Casting
```kotlin
// ❌ BAD: Redundant check - error is already typed as DataError.Local
assertTrue("Should return error", result is Result.Error)
val error = (result as Result.Error).error
assertTrue("Should be DataError.Local", error is DataError.Local) // ← Always true!

// ✅ GOOD: Remove redundant check or add clarifying comment
assertTrue("Should return error", result is Result.Error)
// Error will always be DataError.Local due to ErrorMapper implementation
```

**Compiler Warning**: `Check for instance is always 'true'` - indicates redundant type checking after unwrapping a typed Result.

### ❌ Unused Result Variables
```kotlin
// ❌ BAD: result assigned but never used
val result = useCase.execute(shelfId)
assertTrue("Should call export service", mockService.called)

// ✅ GOOD: Don't assign if not needed
useCase.execute(shelfId)
assertTrue("Should call export service", mockService.called)
```

### ❌ Missing Annotations
```kotlin
// ❌ BAD: Missing required annotations
class SomeViewModelTest {
    @Test
    fun `test something`() = runTest { /* ... */ }
}

// ✅ GOOD: Include all required annotations
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SomeViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @After
    fun tearDown() {
        // Reset mocks
    }

    @Test
    fun `test something`() = runTest(testDispatcher) { /* ... */ }
}
```

### ❌ Inline Mocks in Shared Utilities
```kotlin
// ❌ BAD: Creating inline mock when shared mock exists
@Test
fun `test something`() = runTest {
    val mockService = object : SomeService {
        override suspend fun doSomething() = Result.Success(Unit)
    }
    // ...
}

// ✅ GOOD: Use shared mock from testutil/mocks
@Test
fun `test something`() = runTest {
    val mockService = MockSomeService()
    mockService.shouldSucceed = true
    // ...
}
```

### ❌ Using Wrong Enum Values or Model Fields
```kotlin
// ❌ BAD: Using non-existent enum values
val shelf = TestShelfBuilder()
    .withStyle(ShelfStyle.MetalGray)  // Doesn't exist!
    .build()

// ❌ BAD: Using old/removed Book constructor fields
val book = Book(
    id = "book-1",
    title = "Test",
    isbn = "123",           // ← Field removed
    openLibraryId = "OL1",  // ← Field removed
    coverUrl = "url"        // ← Field removed
)

// ✅ GOOD: Use actual ShelfStyle enum values
val shelf = TestShelfBuilder()
    .withStyle(ShelfStyle.GreyMetal)  // Correct: GreyMetal, not MetalGray
    .build()

// Available ShelfStyle values:
// - DarkWood
// - SilverMetal
// - WhiteMetal
// - GreyMetal
// - DarkGreyMetal

// ✅ GOOD: Use current Book model structure
val book = Book(
    id = "book-1",
    title = "Test Book",
    authors = emptyList(),
    imageUrl = "",
    description = null,
    languages = emptyList(),
    firstPublishYear = null,
    averageRating = null,
    ratingCount = null,
    numPages = null,
    numEditions = 0,
    purchased = false,
    spineColor = 0xFF8B4513.toInt()
)

// OR: Use TestBookBuilder for simpler test data creation
val book = TestBookBuilder()
    .withId("book-1")
    .withTitle("Test Book")
    .build()
```

**Why this matters**: Using old field names or wrong enum values causes compilation errors. Always check the actual model/enum definition before creating test data, or use TestBuilders which are kept up-to-date.

## ✅ Testing Checklist

### For UseCase Tests:
- [ ] Test success scenarios with valid inputs
- [ ] Test error scenarios (exceptions, invalid data)
- [ ] Test business rules and edge cases
- [ ] Test with different input combinations
- [ ] Mock repository dependencies only
- [ ] Use descriptive test names
- [ ] Include `@OptIn(ExperimentalCoroutinesApi::class)`
- [ ] Include `@After tearDown()` with mock resets
- [ ] No redundant instance checks after casting
- [ ] No unused result variables

### For ViewModel Tests:
- [ ] Test initial state
- [ ] Test each user action
- [ ] Test state updates after actions
- [ ] Test error state handling
- [ ] Use StateFlow collection pattern with `testHelper`
- [ ] Use minimal inline UseCase mocking
- [ ] Include `@RunWith(RobolectricTestRunner::class)`
- [ ] Include `InstantTaskExecutorRule`
- [ ] Include `@After tearDown()` with mock resets
- [ ] Always call `stateHelper.cleanup()` after assertions

### For Repository Tests:
- [ ] Test data persistence
- [ ] Test data retrieval
- [ ] Test error handling
- [ ] Test data mapping
- [ ] Use real Room/database when possible

### For Service Layer Tests:
- [ ] Use shared mock repositories from `testutil/mocks/`
- [ ] Test core business logic (validation, serialization, orchestration)
- [ ] Test with proper data structures (match actual export formats)
- [ ] Test round-trip operations (serialize → deserialize)
- [ ] Test error handling and invalid inputs
- [ ] Use inline mocks for utilities (TimeProvider, IdGenerator)
- [ ] Avoid redundant type checks after Result unwrapping
- [ ] Test multi-repository coordination for orchestrators
- [ ] Verify all repository interactions for orchestration services
- [ ] Include `@Before setup()` for mock initialization if needed
- [ ] Use correct enum values (check actual enums, not assumptions)
- [ ] Use current model structures (check for removed/renamed fields)

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

**Mock Repository Enhancements (Phase 3)**:

```kotlin
// MockBookcaseRepository - Flow exception support
class MockBookcaseRepository {
    var shouldThrowException = false

    // ✅ Properly supports exception throwing via flow builder
    override fun getAllShelves(): Flow<List<Bookshelf>> = flow {
        if (shouldThrowException) throw RuntimeException("Test exception")
        emit(shelvesToReturn)
    }
}

// MockBookshelfRepository - Convenience method
class MockBookshelfRepository {
    // ✅ Convenience method for common setup pattern
    fun configureBooksForShelf(shelfId: String, books: List<Book>) {
        books.forEach { configureBook(it) }
        configureShelfWithBooks(shelfId, books.map { it.id })
    }

    // Use in tests:
    mockBookshelfRepository.configureBooksForShelf("shelf-1", listOf(book1, book2))
}
```

### ViewModel Test Pattern (Current Approach)
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SomeViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Simplified inline mocks
    private val mockSomeUseCase = SimpleSomeUseCase()

    @After
    fun tearDown() {
        mockSomeUseCase.reset()
    }

    private fun createViewModel(): SomeViewModel {
        val useCases = SomeUseCases(someUseCase = mockSomeUseCase)
        return SomeViewModel(useCases)
    }

    @Test
    fun `test ViewModel state changes`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val stateHelper = viewModel.state.testHelper(this)

        // Execute action and get resulting state
        val newState = stateHelper.executeAndGetState {
            viewModel.onAction(SomeAction)
        }

        // Assert state changes
        assertEquals("Expected state", expectedValue, newState?.someProperty)
        stateHelper.cleanup()
    }

    // Inline mock at bottom of file
    private class SimpleSomeUseCase : SomeUseCase {
        var shouldSucceed = true
        override suspend fun execute(): Result<Unit, DataError.Local> =
            if (shouldSucceed) Result.Success(Unit) else Result.Error(DataError.Local.UNKNOWN)
        fun reset() { shouldSucceed = true }
    }
}
```

## 🚀 Current Test Status

- **All tests passing**: ✅ **597 tests total** (542 unit tests + 55 instrumented tests)
  - Unit tests (91%): 542 tests in `app/src/test/` (~60 seconds)
  - Instrumented tests (9%): 55 tests in `app/src/androidTest/` (~25 seconds)
- **Test execution time**: ⚡ ~85 seconds total for full test suite
- **Clean package structure**: ✅ Mirrors source architecture (test/ + androidTest/)
- **Shared utilities**: ✅ Reusable mocks, builders, and helpers
- **Testing patterns established**: ✅ Complete pyramid: Unit + Integration + E2E
- **Zero technical debt**: ✅ No failing or outdated tests
- **Zero duplication**: ✅ Shared utilities eliminate code duplication
- **Enhanced mocks**: ✅ Flow exception support, convenience methods
- **Book Club coverage**: ✅ 16 use cases with comprehensive test coverage

## 📈 Current Test Coverage

### Test Organization Completed ✅
- **Package Structure**: Clean architecture mirroring with proper testutil organization
- **Shared Utilities**: Complete set of builders, helpers, and mocks packages
- **Pattern Compliance**: All existing tests refactored to use shared utilities
- **Builder Pattern**: TestBookBuilder, TestSearchedBookDtoBuilder, TestShelfBuilder for consistent test data
- **Mock Implementations**: Enhanced MockBookcaseRepository, MockBookshelfRepository, MockBookRepository
- **Test Helpers**: ViewModelTestHelper (testHelper extension), TestIdGenerator, TestTimeProvider

### Phase 3 Service Layer Coverage Completed ✅

**Added Tests (23 new test cases)**:
1. **JsonBookshelfSerializerTest** - JSON serialization/deserialization logic
2. **BookshelfImportValidatorImplTest** - Import validation and conflict detection
3. **DatabaseBookshelfDataOrchestratorTest** - Multi-repository orchestration

**Mock Infrastructure Enhancements**:
- ✅ Fixed `MockBookcaseRepository.getAllShelves()` for proper Flow exception support
- ✅ Added `MockBookshelfRepository.configureBooksForShelf()` convenience method
- ✅ Verified all mocks properly implement current repository interfaces

### Phase 4 Integration & E2E Tests Completed ✅

**Integration Tests (4 test files, 17 test cases)**:
1. **BookcaseRepositoryIntegrationTest** - Repository + Real Room Database (7 tests)
2. **DataOrchestratorIntegrationTest** - Service + Multiple Repositories (5 tests)
3. **SerializationIntegrationTest** - Serialization + Mapper + Data structures (7 tests)
4. **ValidatorIntegrationTest** - Validation logic with database queries (5 tests)

**E2E Tests (5 test files, 26 test cases)**:
1. **ShelfCreationE2ETest** - Complete shelf creation workflow (6 tests)
2. **BookAdditionE2ETest** - Book addition to shelf workflow (4 tests)
3. **BookRemovalE2ETest** - Book removal from shelf workflow (4 tests)
4. **ShelfDeletionE2ETest** - Shelf deletion workflow (5 tests)
5. **ShelfReorderE2ETest** - Shelf reordering workflow (7 tests)

**Key Achievements**:
- ✅ Google 70/20/10 pyramid compliance (94 unit / 17 integration / 26 E2E)
- ✅ All critical user workflows covered end-to-end
- ✅ Complete integration test coverage for service layer
- ✅ Real Room database testing for data integrity
- ✅ Full stack testing: ViewModel → UseCase → Repository → Database

### Coverage Summary

- **Domain Layer (UseCases)**: 100% - All 50 UseCases tested (including 16 book club use cases)
- **Presentation Layer (ViewModels)**: 100% - All 6 ViewModels tested
- **Service Layer**: 100% - All export/import services tested
- **Data Layer**: ~90% - Repositories, mappers, and DAOs tested (including integration tests)
- **Database Layer**: 100% critical paths - Migrations + DAO operations + Integration tests
- **Book Club Layer**: 100% - Full coverage of collaborative features (create, join, leave, reviews, comments)
- **Utilities**: Base64 encoding with real Android APIs
- **Workflows**: 100% critical paths - Major user workflows covered E2E
- **Overall**: ~35% file coverage (55 unit test files + 8 instrumented test files / ~280 production files)

**Focus**: Quality over quantity - testing business-critical components with focused unit tests + instrumented tests for critical Android APIs

### Test Quality Standards ✅
- **Zero duplication**: All tests use shared utilities from testutil packages
- **Consistent patterns**: Given-When-Then structure with descriptive naming
- **Proper mocking**: Fakes over mocks, configurable behavior for realistic testing
- **StateFlow testing**: Proper collection patterns with cleanup and lifecycle management

Follow these established patterns for consistent, maintainable testing across the entire codebase.