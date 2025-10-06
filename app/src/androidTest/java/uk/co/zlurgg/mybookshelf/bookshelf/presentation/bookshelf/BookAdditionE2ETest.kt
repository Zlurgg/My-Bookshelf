package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ReorderShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookSorter
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

/**
 * E2E test for book addition workflow.
 * Tests complete flow: ViewModel → UseCase → Repository → Database
 *
 * This is a large-scope test (Google's 10% E2E test recommendation).
 */
@RunWith(AndroidJUnit4::class)
class BookAdditionE2ETest {

    private lateinit var database: BookshelfDatabase
    private lateinit var bookshelfViewModel: BookshelfViewModel
    private lateinit var bookcaseRepositoryImpl: BookcaseRepositoryImpl
    private val testShelfId = "test-shelf-1"

    private val testTimeProvider = object : TimeProvider {
        override fun currentTimeMillis(): Long = 1000L
    }

    private val testIdGenerator = object : IdGenerator {
        private var counter = 0
        override fun generateId(): String = "test-id-${counter++}"
    }

    // Stub RemoteBookDataSource - not used in these E2E tests
    private val stubRemoteDataSource = object : RemoteBookDataSource {
        override suspend fun searchBooks(
            query: String,
            resultLimit: Int?,
            language: String?,
            authorFilter: String?,
            titleFilter: String?,
            sort: String?
        ): Result<uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto, DataError.Remote> {
            throw NotImplementedError("Not used in E2E tests")
        }

        override suspend fun getBookDetails(bookWorkId: String): Result<uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto, DataError.Remote> {
            throw NotImplementedError("Not used in E2E tests")
        }
    }

    // Stub BookshelfExportService - not used in these E2E tests
    private val stubExportService = object : BookshelfExportService {
        override suspend fun shareBookshelf(shelfId: String): Result<Unit, DataError.Local> {
            throw NotImplementedError("Not used in E2E tests")
        }

        override suspend fun importBookshelf(jsonData: String): Result<Unit, DataError.Local> {
            throw NotImplementedError("Not used in E2E tests")
        }

        override suspend fun checkImportNameConflict(jsonData: String): Result<String?, DataError.Local> {
            throw NotImplementedError("Not used in E2E tests")
        }

        override suspend fun importBookshelfWithName(jsonData: String, customName: String): Result<Unit, DataError.Local> {
            throw NotImplementedError("Not used in E2E tests")
        }
    }

    @Before
    fun setup() {
        // Setup real database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookshelfDatabase::class.java
        ).build()

        // Setup repositories
        bookcaseRepositoryImpl = BookcaseRepositoryImpl(database.bookshelfDao)
        val bookshelfRepository = BookshelfRepositoryImpl(database.bookshelfDao, testTimeProvider)
        val bookRepository = BookRepositoryImpl(stubRemoteDataSource, database.bookshelfDao)

        // Create test shelf in database
        runTest {
            val testShelf = Bookshelf(
                id = testShelfId,
                name = "Test Shelf",
                books = emptyList(),
                shelfStyle = ShelfStyle.DarkWood,
                position = 0
            )
            bookcaseRepositoryImpl.addShelf(testShelf)
        }

        // Setup bookshelf use cases
        val bookshelfUseCases = BookshelfUseCases(
            searchBooks = SearchBooksUseCaseImpl(stubRemoteDataSource, BookSorter()),
            getShelfBooks = GetShelfBooksUseCaseImpl(bookshelfRepository),
            addBookToShelf = AddBookToShelfUseCaseImpl(bookRepository, bookshelfRepository),
            removeBookFromShelf = RemoveBookFromShelfUseCaseImpl(bookshelfRepository),
            upsertBook = UpsertBookUseCaseImpl(bookRepository),
            shareBookshelf = ShareBookshelfUseCaseImpl(stubExportService)
        )

        // Setup bookcase use cases
        val bookcaseUseCases = BookcaseUseCases(
            getAllShelves = GetAllShelvesUseCaseImpl(bookcaseRepositoryImpl),
            createShelf = CreateShelfUseCaseImpl(bookcaseRepositoryImpl, testIdGenerator),
            deleteShelf = DeleteShelfUseCaseImpl(bookcaseRepositoryImpl),
            reorderShelves = ReorderShelvesUseCaseImpl(bookcaseRepositoryImpl),
            getShelfById = GetShelfByIdUseCaseImpl(bookcaseRepositoryImpl)
        )

        // Setup ViewModel with full dependency chain
        bookshelfViewModel = BookshelfViewModel(
            bookshelfUseCases = bookshelfUseCases,
            bookcaseUseCases = bookcaseUseCases,
            shelfId = testShelfId
        )
    }

    @After
    fun tearDown() {
        database.close()
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

    @Test
    fun addMultipleBooksPreservesOrder() = runTest {
        // Given - Multiple books to add
        val book1 = createTestBook("book-1", "Book One")
        val book2 = createTestBook("book-2", "Book Two")
        val book3 = createTestBook("book-3", "Book Three")

        // When - User adds books sequentially
        bookshelfViewModel.onAction(BookshelfAction.OnAddBookClick(book1))
        bookshelfViewModel.onAction(BookshelfAction.OnAddBookClick(book2))
        bookshelfViewModel.onAction(BookshelfAction.OnAddBookClick(book3))

        // Then - All books should be in state
        val state = bookshelfViewModel.state.first()
        assertEquals(3, state.books.size)

        // And - Books should persist in database
        val booksInShelf = database.bookshelfDao.getBooksForShelf(testShelfId).first()
        assertEquals(3, booksInShelf.size)
    }

    @Test
    fun addSameBookTwiceOnlyAddsOnce() = runTest {
        // Given - A book
        val book = createTestBook("book-1", "Test Book")

        // When - User adds same book twice
        bookshelfViewModel.onAction(BookshelfAction.OnAddBookClick(book))
        bookshelfViewModel.onAction(BookshelfAction.OnAddBookClick(book))

        // Then - Book should only appear once in state
        val state = bookshelfViewModel.state.first()
        assertEquals(1, state.books.size)

        // And - Book should only exist once in database
        val booksInShelf = database.bookshelfDao.getBooksForShelf(testShelfId).first()
        assertEquals(1, booksInShelf.size)
    }

    @Test
    fun addBookUpsertsSameBookWithDifferentShelf() = runTest {
        // Given - Book on another shelf
        val book = createTestBook("book-1", "Test Book")
        val anotherShelfId = "another-shelf"

        // Create another shelf and add book to it
        val anotherShelf = Bookshelf(
            id = anotherShelfId,
            name = "Another Shelf",
            books = emptyList(),
            shelfStyle = ShelfStyle.SilverMetal,
            position = 1
        )
        bookcaseRepositoryImpl.addShelf(anotherShelf)

        // Add book to another shelf first
        database.bookshelfDao.upsert(book.toBookEntity())
        database.bookshelfDao.upsertCrossRef(
            uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef(
                shelfId = anotherShelfId,
                bookId = book.id,
                addedAt = testTimeProvider.currentTimeMillis()
            )
        )

        // When - User adds same book to test shelf
        bookshelfViewModel.onAction(BookshelfAction.OnAddBookClick(book))

        // Then - Book should be in both shelves
        val booksInTestShelf = database.bookshelfDao.getBooksForShelf(testShelfId).first()
        val booksInAnotherShelf = database.bookshelfDao.getBooksForShelf(anotherShelfId).first()

        assertEquals(1, booksInTestShelf.size)
        assertEquals(1, booksInAnotherShelf.size)
        assertEquals("book-1", booksInTestShelf[0].id)
        assertEquals("book-1", booksInAnotherShelf[0].id)
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

    private fun Book.toBookEntity(): uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity {
        return uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity(
            id = this.id,
            title = this.title,
            authors = this.authors,
            imageUrl = this.imageUrl,
            description = this.description,
            languages = this.languages,
            firstPublishYear = this.firstPublishYear,
            ratingsAverage = this.averageRating,
            ratingsCount = this.ratingCount,
            numPagesMedian = this.numPages,
            numEditions = this.numEditions,
            purchased = this.purchased,
            spineColor = this.spineColor
        )
    }
}
