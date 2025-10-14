package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.*
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * E2E test for book metadata update workflow.
 * Tests complete user flow: Add book → Update reading status → Rate → Add notes → Verify persistence.
 * Uses real Room database and full implementation stack (no mocks except external services).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BookMetadataUpdateE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var bookRepository: BookRepository
    private lateinit var bookshelfRepository: BookshelfRepository
    private lateinit var bookcaseRepository: BookcaseRepository
    private lateinit var bookDetailViewModel: BookDetailViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testShelfId = "test-shelf-e2e"
    private val testBookId = "test-book-e2e"

    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1000L
    }

    // Stub RemoteBookDataSource (not used in this E2E test)
    private val stubRemoteDataSource = object : uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource {
        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            sort: String?
        ): Result<uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto, uk.co.zlurgg.mybookshelf.core.domain.error.DataError.Remote> {
            throw UnsupportedOperationException("Not used in this test")
        }

        override suspend fun getBookDetails(bookWorkId: String): Result<uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto, uk.co.zlurgg.mybookshelf.core.domain.error.DataError.Remote> {
            throw UnsupportedOperationException("Not used in this test")
        }
    }

    @Before
    fun setup() = runTest(testDispatcher) {
        // Create real in-memory database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        // Create real repositories with correct constructors
        bookRepository = BookRepositoryImpl(stubRemoteDataSource, database.bookshelfDao)
        bookshelfRepository = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
        bookcaseRepository = BookcaseRepositoryImpl(database.bookshelfDao)

        // Create test shelf using bookcase repository
        val testShelf = Bookshelf(
            id = testShelfId,
            name = "Test Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 0
        )
        bookcaseRepository.addShelf(testShelf)

        // Create and add test book to shelf
        val initialBook = Book(
            id = testBookId,
            title = "Test Book for Metadata",
            authors = listOf("Test Author"),
            imageUrl = "https://example.com/cover.jpg",
            description = "Test description",
            languages = listOf("en"),
            firstPublishYear = "2024",
            averageRating = 4.0,
            ratingCount = 100,
            numPages = 300,
            numEditions = 5,
            purchased = false,
            spineColor = 0xFF8B4513.toInt(),
            // Personal metadata - defaults
            readingStatus = ReadingStatus.WANT_TO_READ,
            personalRating = 0f,
            personalNotes = "",
            dateAdded = null,
            purchaseDate = null,
            // Enhanced metadata
            isbn = null,
            publisher = null,
            publishDate = null,
            internetArchiveId = null
        )

        // Add book to database and shelf
        bookRepository.upsertBook(initialBook)
        bookshelfRepository.addBookToShelf(testShelfId, testBookId)

        // Create real UseCases with real implementations
        val getBookDetailsUseCase = GetBookDetailsUseCaseImpl(bookRepository, bookshelfRepository)
        val addBookToShelfUseCase = AddBookToShelfUseCaseImpl(bookRepository, bookshelfRepository)
        val removeBookFromShelfUseCase = RemoveBookFromShelfUseCaseImpl(bookshelfRepository)
        val upsertBookUseCase = UpsertBookUseCaseImpl(bookRepository)
        val toggleBookPurchaseUseCase = ToggleBookPurchaseUseCaseImpl(bookRepository)
        val updateBookMetadataUseCase = UpdateBookMetadataUseCaseImpl(bookRepository, testTimeProvider)

        val useCases = BookDetailUseCases(
            getBookDetails = getBookDetailsUseCase,
            addBookToShelf = addBookToShelfUseCase,
            removeBookFromShelf = removeBookFromShelfUseCase,
            upsertBook = upsertBookUseCase,
            toggleBookPurchase = toggleBookPurchaseUseCase,
            updateBookMetadata = updateBookMetadataUseCase
        )

        // Create ViewModel with real implementation
        bookDetailViewModel = BookDetailViewModel(useCases, testBookId, testShelfId)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeMetadataWorkflow_updatesBookCorrectly() = runTest(testDispatcher) {
        // Wait for initial book load
        val initialState = bookDetailViewModel.state.first { !it.isLoading }
        assertEquals(ReadingStatus.WANT_TO_READ, initialState.book!!.readingStatus)
        assertEquals(0f, initialState.book!!.personalRating)
        assertEquals("", initialState.book!!.personalNotes)

        // When - User updates metadata: Step 1: Change status to CURRENTLY_READING
        bookDetailViewModel.onAction(BookDetailAction.OnReadingStatusChange(ReadingStatus.CURRENTLY_READING))

        // Then - Verify state updated
        val stateAfterStatus = bookDetailViewModel.state.first { it.book?.readingStatus == ReadingStatus.CURRENTLY_READING }
        assertEquals(ReadingStatus.CURRENTLY_READING, stateAfterStatus.book!!.readingStatus)

        // AND - Verify database persistence
        val bookAfterStatus = bookRepository.getBookById(testBookId)
        assertEquals(ReadingStatus.CURRENTLY_READING, bookAfterStatus?.readingStatus)

        // When - Step 2: Add personal rating
        bookDetailViewModel.onAction(BookDetailAction.OnPersonalRatingChange(4.5f))

        // Then - Verify state updated
        val stateAfterRating = bookDetailViewModel.state.first { it.book?.personalRating == 4.5f }
        assertEquals(4.5f, stateAfterRating.book!!.personalRating)

        // AND - Verify database persistence
        val bookAfterRating = bookRepository.getBookById(testBookId)
        assertEquals(4.5f, bookAfterRating?.personalRating)

        // When - Step 3: Add personal notes
        bookDetailViewModel.onAction(BookDetailAction.OnPersonalNotesChange("Great book! Highly recommend."))

        // Then - Verify state updated
        val stateAfterNotes = bookDetailViewModel.state.first { it.book?.personalNotes == "Great book! Highly recommend." }
        assertEquals("Great book! Highly recommend.", stateAfterNotes.book!!.personalNotes)

        // AND - Verify database persistence
        val finalBook = bookRepository.getBookById(testBookId)
        assertNotNull("Book should exist in database", finalBook)
        assertEquals("Reading status should persist", ReadingStatus.CURRENTLY_READING, finalBook?.readingStatus)
        assertEquals("Personal rating should persist", 4.5f, finalBook?.personalRating)
        assertEquals("Personal notes should persist", "Great book! Highly recommend.", finalBook?.personalNotes)
        assertNotNull("dateAdded should be auto-set", finalBook?.dateAdded)
    }

    @Test
    fun clearPersonalRating_setsRatingTo0f() = runTest(testDispatcher) {
        // Given - Book with rating already set
        val bookWithRating = bookRepository.getBookById(testBookId)?.copy(
            personalRating = 4.5f,
            dateAdded = 1000L
        )
        assertNotNull(bookWithRating)
        bookRepository.upsertBook(bookWithRating!!)

        // Wait for ViewModel to load updated book
        val initialState = bookDetailViewModel.state.first { it.book?.personalRating == 4.5f }
        assertEquals(4.5f, initialState.book!!.personalRating)

        // When - User clears rating (sets to 0f)
        bookDetailViewModel.onAction(BookDetailAction.OnPersonalRatingChange(0f))

        // Then - Verify state updated
        val stateAfterClear = bookDetailViewModel.state.first { it.book?.personalRating == 0f }
        assertEquals("State personal rating should be 0f", 0f, stateAfterClear.book!!.personalRating)

        // AND - Verify database persistence
        val bookAfterClear = bookRepository.getBookById(testBookId)
        assertNotNull("Book should still exist", bookAfterClear)
        assertEquals("Database personal rating should be 0f", 0f, bookAfterClear?.personalRating)
    }

    @Test
    fun personalMetadata_persistsWhenAddingBookToMultipleShelves() = runTest(testDispatcher) {
        // Given - User updates personal metadata
        bookDetailViewModel.onAction(BookDetailAction.OnPersonalRatingChange(4.5f))
        bookDetailViewModel.onAction(BookDetailAction.OnPersonalNotesChange("Loved it!"))
        bookDetailViewModel.onAction(BookDetailAction.OnReadingStatusChange(ReadingStatus.READ))

        // Wait for state updates
        val stateAfterUpdates = bookDetailViewModel.state.first {
            it.book?.personalRating == 4.5f && it.book?.personalNotes == "Loved it!" && it.book?.readingStatus == ReadingStatus.READ
        }
        assertEquals(4.5f, stateAfterUpdates.book!!.personalRating)
        assertEquals("Loved it!", stateAfterUpdates.book!!.personalNotes)
        assertEquals(ReadingStatus.READ, stateAfterUpdates.book!!.readingStatus)

        // When - User adds book to another shelf (this triggered the bug)
        // Create second shelf
        val secondShelf = Bookshelf(
            id = "second-shelf-e2e",
            name = "Second Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.DarkWood,
            position = 1
        )
        bookcaseRepository.addShelf(secondShelf)

        // Add book to second shelf via AddBookToShelfUseCase (simulates the bug scenario)
        val currentBook = bookRepository.getBookById(testBookId)
        assertNotNull("Book should exist", currentBook)

        val addResult = AddBookToShelfUseCaseImpl(bookRepository, bookshelfRepository)
            .execute(currentBook!!, "second-shelf-e2e")
        assertTrue("Add should succeed", addResult is Result.Success)

        // Then - Verify personal metadata persists in database
        val finalBook = bookRepository.getBookById(testBookId)
        assertNotNull("Book should still exist", finalBook)
        assertEquals("Rating should persist", 4.5f, finalBook?.personalRating)
        assertEquals("Notes should persist", "Loved it!", finalBook?.personalNotes)
        assertEquals("Status should persist", ReadingStatus.READ, finalBook?.readingStatus)
    }
}
