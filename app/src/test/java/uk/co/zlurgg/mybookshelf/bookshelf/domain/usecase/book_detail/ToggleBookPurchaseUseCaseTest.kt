package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

class ToggleBookPurchaseUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val useCase = ToggleBookPurchaseUseCaseImpl(mockBookRepository)

    @After
    fun tearDown() {
        mockBookRepository.reset()
    }

    @Test
    fun `execute marks unpurchased book as purchased`() = runTest {
        // Given
        val unpurchasedBook = TestBookBuilder()
            .withId("test-book")
            .withTitle("Test Book")
            .withPurchased(false)
            .build()

        // When
        val result = useCase.execute(unpurchasedBook, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Book should be marked as purchased", updatedBook.purchased)
        assertEquals("Should call upsertBook once", 1, mockBookRepository.upsertBookCallCount)

        val upsertedBook = mockBookRepository.lastUpsertedBook!!
        assertTrue("Upserted book should be purchased", upsertedBook.purchased)
        assertEquals("Should preserve book ID", unpurchasedBook.id, upsertedBook.id)
        assertEquals("Should preserve book title", unpurchasedBook.title, upsertedBook.title)
    }

    @Test
    fun `execute marks purchased book as unpurchased`() = runTest {
        // Given
        val purchasedBook = TestBookBuilder()
            .withId("purchased-book")
            .withTitle("Already Purchased Book")
            .withPurchased(true)
            .build()

        // When
        val result = useCase.execute(purchasedBook, false)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertFalse("Book should be marked as unpurchased", updatedBook.purchased)
        assertEquals("Should call upsertBook once", 1, mockBookRepository.upsertBookCallCount)

        val upsertedBook = mockBookRepository.lastUpsertedBook!!
        assertFalse("Upserted book should be unpurchased", upsertedBook.purchased)
    }

    @Test
    fun `execute keeps purchase status same when toggling to current status`() = runTest {
        // Given
        val purchasedBook = TestBookBuilder()
            .withId("already-purchased")
            .withPurchased(true)
            .build()

        // When - Set to purchased again (same status)
        val result = useCase.execute(purchasedBook, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Book should remain purchased", updatedBook.purchased)
        assertEquals("Should still call upsertBook", 1, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `execute preserves all book data except purchase status`() = runTest {
        // Given
        val originalBook = TestBookBuilder.completeBook()

        // When
        val result = useCase.execute(originalBook, !originalBook.purchased)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data

        // All fields should be preserved except purchase status
        assertEquals("Should preserve ID", originalBook.id, updatedBook.id)
        assertEquals("Should preserve title", originalBook.title, updatedBook.title)
        assertEquals("Should preserve image URL", originalBook.imageUrl, updatedBook.imageUrl)
        assertEquals("Should preserve authors", originalBook.authors, updatedBook.authors)
        assertEquals("Should preserve description", originalBook.description, updatedBook.description)
        assertEquals("Should preserve languages", originalBook.languages, updatedBook.languages)
        assertEquals("Should preserve publish year", originalBook.firstPublishYear, updatedBook.firstPublishYear)
        assertEquals("Should preserve rating", originalBook.averageRating, updatedBook.averageRating)
        assertEquals("Should preserve rating count", originalBook.ratingCount, updatedBook.ratingCount)
        assertEquals("Should preserve page count", originalBook.numPages, updatedBook.numPages)
        assertEquals("Should preserve edition count", originalBook.numEditions, updatedBook.numEditions)
        assertEquals("Should preserve spine color", originalBook.spineColor, updatedBook.spineColor)

        // Only purchase status should change
        assertEquals("Should toggle purchase status", !originalBook.purchased, updatedBook.purchased)
    }

    @Test
    fun `execute handles book with minimal data`() = runTest {
        // Given
        val minimalBook = TestBookBuilder.minimalBook()

        // When
        val result = useCase.execute(minimalBook, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Should mark minimal book as purchased", updatedBook.purchased)
        assertEquals("Should preserve minimal book structure", minimalBook.title, updatedBook.title)
    }

    @Test
    fun `execute handles book with null rating`() = runTest {
        // Given
        val bookWithNullRating = TestBookBuilder()
            .withId("no-rating-book")
            .withAverageRating(null)
            .withPurchased(false)
            .build()

        // When
        val result = useCase.execute(bookWithNullRating, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Should mark book as purchased", updatedBook.purchased)
        assertEquals("Should preserve null rating", null, updatedBook.averageRating)
    }

    @Test
    fun `execute handles book with empty authors list`() = runTest {
        // Given
        val bookWithNoAuthors = TestBookBuilder()
            .withId("anonymous-book")
            .withAuthors(emptyList())
            .withPurchased(false)
            .build()

        // When
        val result = useCase.execute(bookWithNoAuthors, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Should mark book as purchased", updatedBook.purchased)
        assertTrue("Should preserve empty authors list", updatedBook.authors.isEmpty())
    }

    @Test
    fun `execute returns error when repository fails`() = runTest {
        // Given
        val book = TestBookBuilder().withId("test-book").build()
        mockBookRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        // When
        val result = useCase.execute(book, true)

        // Then
        assertTrue("Should return error", result is Result.Error)
        val error = (result as Result.Error).error
        assertEquals(DataError.Local.DATABASE_ERROR, error)
        // getBookById returns error first, so upsertBook is never called
        assertEquals("Should not call upsertBook", 0, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `execute handles books with special characters in data`() = runTest {
        // Given
        val bookWithSpecialChars = TestBookBuilder()
            .withId("special-book-éñ")
            .withTitle("Book with Émojis 📚 and Special Çhars")
            .withAuthors(listOf("Authör Namé", "José María"))
            .withDescription("Description with <HTML> & special chars: αβγ")
            .withPurchased(false)
            .build()

        // When
        val result = useCase.execute(bookWithSpecialChars, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Should mark book as purchased", updatedBook.purchased)
        assertEquals("Should preserve special chars in title", bookWithSpecialChars.title, updatedBook.title)
        assertEquals("Should preserve special chars in authors", bookWithSpecialChars.authors, updatedBook.authors)
        assertEquals(
            "Should preserve special chars in description",
            bookWithSpecialChars.description,
            updatedBook.description
        )
    }

    @Test
    fun `execute works with extreme values`() = runTest {
        // Given
        val bookWithExtremeValues = TestBookBuilder()
            .withId("extreme-book")
            .withRatingCount(Int.MAX_VALUE)
            .withNumPages(0)
            .withNumEditions(999999)
            .withAverageRating(5.0)
            .withPurchased(false)
            .build()

        // When
        val result = useCase.execute(bookWithExtremeValues, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data
        assertTrue("Should mark book as purchased", updatedBook.purchased)
        assertEquals("Should preserve extreme rating count", Int.MAX_VALUE, updatedBook.ratingCount)
        assertEquals("Should preserve zero pages", 0, updatedBook.numPages)
        assertEquals("Should preserve many editions", 999999, updatedBook.numEditions)
    }

    @Test
    fun `execute multiple toggles maintain data integrity`() = runTest {
        // Given
        val originalBook = TestBookBuilder()
            .withId("toggle-test")
            .withTitle("Toggle Test Book")
            .withPurchased(false)
            .build()

        // When - Toggle to purchased
        val firstResult = useCase.execute(originalBook, true)
        assertTrue("First toggle should succeed", firstResult is Result.Success)
        val firstUpdatedBook = (firstResult as Result.Success).data

        // When - Toggle back to unpurchased
        val secondResult = useCase.execute(firstUpdatedBook, false)
        assertTrue("Second toggle should succeed", secondResult is Result.Success)
        val secondUpdatedBook = (secondResult as Result.Success).data

        // When - Toggle to purchased again
        val thirdResult = useCase.execute(secondUpdatedBook, true)

        // Then
        assertTrue("Third toggle should succeed", thirdResult is Result.Success)
        val finalBook = (thirdResult as Result.Success).data

        assertTrue("Final book should be purchased", finalBook.purchased)
        assertEquals("Should preserve original data through multiple toggles", originalBook.title, finalBook.title)
        assertEquals("Should preserve original ID", originalBook.id, finalBook.id)
        assertEquals("Should call upsertBook three times", 3, mockBookRepository.upsertBookCallCount)
    }

    @Test
    fun `execute preserves personal metadata when toggling existing book`() = runTest {
        // Given - Book already exists with personal metadata
        val existingBook = TestBookBuilder()
            .withId("book-with-metadata")
            .withTitle("Old Title")
            .withPersonalRating(4.5f)
            .withPersonalNotes("Amazing read!")
            .withReadingStatus(uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.READ)
            .withDateAdded(1609459200000L)
            .withPurchaseDate(1609545600000L)
            .withPurchased(false)
            .build()
        mockBookRepository.addBook(existingBook)

        // Fresh book from API (same ID, updated title, NO personal data)
        val freshBookFromApi = TestBookBuilder()
            .withId("book-with-metadata") // Same ID
            .withTitle("Updated Title from API")
            .withPersonalRating(0f) // API doesn't have this
            .withPersonalNotes("") // API doesn't have this
            .withReadingStatus(uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.WANT_TO_READ) // Default
            .withDateAdded(null) // API doesn't track this
            .withPurchaseDate(null) // API doesn't track this
            .withPurchased(false)
            .build()

        // When - Toggle purchased on fresh API book
        val result = useCase.execute(freshBookFromApi, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data

        // API data should be updated
        assertEquals("Should update title from API", "Updated Title from API", updatedBook.title)
        assertTrue("Should toggle purchased status", updatedBook.purchased)

        // Personal metadata should be preserved from existing book
        assertEquals("Should preserve personal rating", 4.5f, updatedBook.personalRating, 0.01f)
        assertEquals("Should preserve personal notes", "Amazing read!", updatedBook.personalNotes)
        assertEquals(
            "Should preserve reading status",
            uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.READ,
            updatedBook.readingStatus
        )
        assertEquals("Should preserve dateAdded", 1609459200000L, updatedBook.dateAdded)
        assertEquals("Should preserve purchaseDate", 1609545600000L, updatedBook.purchaseDate)
    }

    @Test
    fun `execute works normally for new books without existing metadata`() = runTest {
        // Given - Book does NOT exist in repository
        val newBookFromApi = TestBookBuilder()
            .withId("new-book")
            .withTitle("Brand New Book")
            .withPersonalRating(0f)
            .withPersonalNotes("")
            .withReadingStatus(uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.WANT_TO_READ)
            .withDateAdded(null)
            .withPurchaseDate(null)
            .withPurchased(false)
            .build()

        // When - Toggle purchased on new book
        val result = useCase.execute(newBookFromApi, true)

        // Then
        assertTrue("Should return success", result is Result.Success)
        val updatedBook = (result as Result.Success).data

        // Should use API data as-is
        assertEquals("Should use API title", "Brand New Book", updatedBook.title)
        assertTrue("Should set purchased to true", updatedBook.purchased)
        assertEquals("Should use default rating", 0f, updatedBook.personalRating, 0.01f)
        assertEquals("Should use default notes", "", updatedBook.personalNotes)
        assertEquals(
            "Should use default reading status",
            uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus.WANT_TO_READ,
            updatedBook.readingStatus
        )
    }
}
