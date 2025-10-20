package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.BookshelfImportValidatorImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.DatabaseBookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.JsonBookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.UrlEncodedShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.result.getOrThrow
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * E2E test for shelf import workflow via deep links.
 * Tests complete flow: DeepLink → ViewModel → UseCases → Services → Database
 *
 * This is a large-scope test (Google's 10% E2E test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class ShelfImportE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var viewModel: DeepLinkViewModel
    private lateinit var shareTokenService: UrlEncodedShareTokenService
    private lateinit var bookcaseRepository: BookcaseRepositoryImpl
    private lateinit var bookshelfRepository: BookshelfRepositoryImpl

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-id-${counter++}"
    }

    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1234567890L
    }

    private val stubRemoteDataSource = object : RemoteBookDataSource {
        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            sort: String?
        ): uk.co.zlurgg.mybookshelf.core.domain.result.Result<SearchResponseDto, uk.co.zlurgg.mybookshelf.core.domain.error.DataError.Remote> =
            uk.co.zlurgg.mybookshelf.core.domain.result.Result.Error(uk.co.zlurgg.mybookshelf.core.domain.error.DataError.Remote.NO_INTERNET)

        override suspend fun getBookDetails(bookWorkId: String): uk.co.zlurgg.mybookshelf.core.domain.result.Result<BookWorkDto, uk.co.zlurgg.mybookshelf.core.domain.error.DataError.Remote> =
            uk.co.zlurgg.mybookshelf.core.domain.result.Result.Error(uk.co.zlurgg.mybookshelf.core.domain.error.DataError.Remote.NO_INTERNET)
    }

    @Before
    fun setup() {
        // Setup real Room database (in-memory)
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        // Setup repositories with real database
        bookcaseRepository = BookcaseRepositoryImpl(database.bookshelfDao)
        bookshelfRepository = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
        val bookRepository = BookRepositoryImpl(stubRemoteDataSource, database.bookshelfDao)

        // Setup services with real implementations
        val exportMapper = BookshelfExportMapper(testIdGenerator, stubRemoteDataSource)
        val serializer = JsonBookshelfSerializer(exportMapper)
        val validator = BookshelfImportValidatorImpl(bookcaseRepository)
        val dataOrchestrator = DatabaseBookshelfDataOrchestrator(
            bookcaseRepository,
            bookshelfRepository,
            bookRepository
        )
        shareTokenService = UrlEncodedShareTokenService()

        // Setup use cases with real dependencies
        val checkImportConflictUseCase = CheckImportConflictUseCaseImpl(serializer, validator)
        val importBookshelfUseCase = ImportBookshelfUseCaseImpl(
            serializer,
            validator,
            dataOrchestrator,
            exportMapper
        )
        val deepLinkImportUseCase = DeepLinkImportUseCaseImpl(
            shareTokenService,
            checkImportConflictUseCase,
            importBookshelfUseCase
        )

        // Setup ViewModel with full dependency chain
        viewModel = DeepLinkViewModel(deepLinkImportUseCase)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importShelfSuccessfully() = runBlocking {
        // Given - A valid share token for a shelf
        val shelf = createTestShelf("Fiction", listOf(createTestBook("book-1", "1984")))
        val serializer = JsonBookshelfSerializer(BookshelfExportMapper(testIdGenerator, stubRemoteDataSource))
        val jsonData = serializer.serialize(shelf).getOrThrow()
        val shareToken = shareTokenService.generateToken(jsonData).getOrThrow()

        // When - User imports shelf via token
        viewModel.onAction(DeepLinkAction.ImportFromToken(shareToken))
        delay(500) // Allow async operation to complete (real delay, not test time)

        // Then - State should show success
        val state = viewModel.state.first()
        assertTrue("Should set import successful flag", state.importSuccessful)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have error", state.error)
        assertNull("Should not have conflict", state.conflictExistingName)

        // And - Shelf should exist in database
        val shelves = bookcaseRepository.getAllShelves().first()
        assertEquals("Should have 1 shelf", 1, shelves.size)
        assertEquals("Should have correct name", "Fiction", shelves[0].name)

        // Validate books using bookshelfRepository
        val books = bookshelfRepository.getBooksForShelf(shelves[0].id).first()
        assertEquals("Should have 1 book", 1, books.size)
        assertEquals("Should have correct book title", "1984", books[0].title)
    }

    @Test
    fun importShelfWithNameConflictShowsConflictState() = runBlocking {
        // Given - Existing shelf with same name
        val existingShelf = createTestShelf("Fiction", emptyList())
        bookcaseRepository.addShelf(existingShelf)

        val shelfToImport = createTestShelf("Fiction", listOf(createTestBook("book-1", "1984")))
        val serializer = JsonBookshelfSerializer(BookshelfExportMapper(testIdGenerator, stubRemoteDataSource))
        val jsonData = serializer.serialize(shelfToImport).getOrThrow()
        val shareToken = shareTokenService.generateToken(jsonData).getOrThrow()

        // When - User imports shelf with conflicting name
        viewModel.onAction(DeepLinkAction.ImportFromToken(shareToken))
        delay(500) // Allow async operation to complete (real delay, not test time)

        // Then - State should show conflict
        val state = viewModel.state.first()
        assertNotNull("Should have conflict existing name", state.conflictExistingName)
        assertNotNull("Should have conflict JSON data", state.conflictJsonData)
        assertEquals("Should show correct conflicting name", "Fiction", state.conflictExistingName)
        assertFalse("Should not be loading", state.isLoading)
        assertFalse("Should not show success", state.importSuccessful)

        // And - Database should still have only 1 shelf
        val shelves = bookcaseRepository.getAllShelves().first()
        assertEquals("Should still have only 1 shelf", 1, shelves.size)

        // Validate original shelf still has no books
        val books = bookshelfRepository.getBooksForShelf(shelves[0].id).first()
        assertEquals("Should be the original shelf with no books", 0, books.size)
    }

    @Test
    fun resolveNameConflictWithCustomNameSucceeds() = runBlocking {
        // Given - Existing shelf and conflict detected
        val existingShelf = createTestShelf("Fiction", emptyList())
        bookcaseRepository.addShelf(existingShelf)

        val shelfToImport = createTestShelf("Fiction", listOf(createTestBook("book-1", "1984")))
        val serializer = JsonBookshelfSerializer(BookshelfExportMapper(testIdGenerator, stubRemoteDataSource))
        val jsonDataForImport = serializer.serialize(shelfToImport).getOrThrow()
        val shareToken = shareTokenService.generateToken(jsonDataForImport).getOrThrow()

        // Trigger conflict detection first
        viewModel.onAction(DeepLinkAction.ImportFromToken(shareToken))
        delay(500) // Allow async operation to complete (real delay, not test time)
        val conflictState = viewModel.state.first()
        val jsonData = conflictState.conflictJsonData!!

        // When - User resolves with custom name
        viewModel.onAction(DeepLinkAction.ResolveNameConflictWithNewName(jsonData, "Fiction 2"))
        delay(500) // Allow async operation to complete (real delay, not test time)

        // Then - State should show success
        val state = viewModel.state.first()
        assertTrue("Should set import successful flag", state.importSuccessful)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should clear conflict existing name", state.conflictExistingName)
        assertNull("Should clear conflict JSON data", state.conflictJsonData)
        assertNull("Should not have error", state.error)

        // And - Both shelves should exist in database
        val shelves = bookcaseRepository.getAllShelves().first()
        assertEquals("Should have 2 shelves", 2, shelves.size)

        // Validate Fiction shelf has no books
        val fictionShelf = shelves.find { it.name == "Fiction" }
        assertNotNull("Should have original Fiction shelf", fictionShelf)
        val fictionBooks = bookshelfRepository.getBooksForShelf(fictionShelf!!.id).first()
        assertEquals("Fiction shelf should have no books", 0, fictionBooks.size)

        // Validate Fiction 2 shelf has 1 book
        val fiction2Shelf = shelves.find { it.name == "Fiction 2" }
        assertNotNull("Should have new Fiction 2 shelf", fiction2Shelf)
        val fiction2Books = bookshelfRepository.getBooksForShelf(fiction2Shelf!!.id).first()
        assertEquals("Fiction 2 shelf should have 1 book", 1, fiction2Books.size)
    }

    @Test
    fun importShelfWithInvalidTokenShowsError() = runBlocking {
        // Given - Invalid/corrupted share token
        val invalidToken = "invalid-garbage-token-xyz123"

        // When - User tries to import invalid token
        viewModel.onAction(DeepLinkAction.ImportFromToken(invalidToken))
        delay(500) // Allow async operation to complete (real delay, not test time)

        // Then - State should show error
        val state = viewModel.state.first()
        assertNotNull("Should have error message", state.error)
        assertFalse("Should not show success", state.importSuccessful)
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should not have conflict", state.conflictExistingName)

        // And - Database should be unchanged
        val shelves = bookcaseRepository.getAllShelves().first()
        assertEquals("Should have no shelves", 0, shelves.size)
    }

    @Test
    fun importShelfWithMultipleBooksPreservesAllData() = runBlocking {
        // Given - Shelf with multiple books
        val testBooks = listOf(
            createTestBook("book-1", "1984"),
            createTestBook("book-2", "Animal Farm"),
            createTestBook("book-3", "Brave New World")
        )
        val shelf = createTestShelf("Dystopian", testBooks)
        val serializer = JsonBookshelfSerializer(BookshelfExportMapper(testIdGenerator, stubRemoteDataSource))
        val jsonData = serializer.serialize(shelf).getOrThrow()
        val shareToken = shareTokenService.generateToken(jsonData).getOrThrow()

        // When - User imports shelf
        viewModel.onAction(DeepLinkAction.ImportFromToken(shareToken))
        delay(500) // Allow async operation to complete (real delay, not test time)

        // Then - State should show success
        val state = viewModel.state.first()
        assertTrue("Should set import successful flag", state.importSuccessful)
        assertNull("Should not have error", state.error)

        // And - All books should be imported
        val shelves = bookcaseRepository.getAllShelves().first()
        assertEquals("Should have 1 shelf", 1, shelves.size)
        assertEquals("Should have correct name", "Dystopian", shelves[0].name)

        // Validate all 3 books were imported
        val books = bookshelfRepository.getBooksForShelf(shelves[0].id).first()
        assertEquals("Should have 3 books", 3, books.size)

        val bookTitles = books.map { it.title }
        assertTrue("Should contain 1984", bookTitles.contains("1984"))
        assertTrue("Should contain Animal Farm", bookTitles.contains("Animal Farm"))
        assertTrue("Should contain Brave New World", bookTitles.contains("Brave New World"))
    }

    // Helper functions to create test data

    private fun createTestShelf(name: String, books: List<Book>): Bookshelf {
        return Bookshelf(
            id = testIdGenerator.generateId(),
            name = name,
            books = books,
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )
    }

    private fun createTestBook(id: String, title: String): Book {
        return Book(
            id = id,
            title = title,
            authors = listOf("Test Author"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Test description",
            languages = listOf("en"),
            firstPublishYear = "2024",
            averageRating = 4.5,
            ratingCount = 100,
            numPages = 300,
            numEditions = 5,
            purchased = false,
            spineColor = 0xFF8B4513.toInt()
        )
    }
}
