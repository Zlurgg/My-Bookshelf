package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.builders.TestSearchedBookDtoBuilder

class BookMappersTest {

    @Test
    fun `toBook maps SearchedBookDto with coverKey`() {
        val dto = TestSearchedBookDtoBuilder()
            .withId("/works/OL123W")
            .withTitle("Test Title")
            .withCoverKey("OLCOVER1M")
            .withCoverAlternativeKey(123)
            .withAuthorNames(listOf("Author One"))
            .withLanguages(listOf("eng"))
            .withFirstPublishYear(1999)
            .withRatingsAverage(4.5)
            .withRatingsCount(10)
            .withNumPagesMedian(321)
            .withNumEditions(2)
            .build()

        val book = dto.toBook()

        assertEquals("OL123W", book.id)
        assertEquals("Test Title", book.title)
        assertTrue(book.imageUrl.contains("olid/OLCOVER1M"))
        assertEquals(listOf("Author One"), book.authors)
        assertEquals(listOf("eng"), book.languages)
        assertEquals("1999", book.firstPublishYear)
        assertEquals(4.5, book.averageRating!!, 0.0)
        assertEquals(10, book.ratingCount)
        assertEquals(321, book.numPages)
        assertEquals(2, book.numEditions)
        // spineColor is placeholder (0) for search results - actual color generated when added to shelf
        assertEquals(0, book.spineColor)
    }

    @Test
    fun `toBook maps SearchedBookDto without coverKey uses alternative`() {
        val dto = TestSearchedBookDtoBuilder()
            .withId("/works/OL999W")
            .withTitle("No Cover Key")
            .withCoverKey(null)
            .withCoverAlternativeKey(555)
            .withAuthorNames(null)
            .withLanguages(null)
            .withFirstPublishYear(2001)
            .withRatingsAverage(3.0)
            .withRatingsCount(0)
            .withNumPagesMedian(100)
            .withNumEditions(null)
            .build()

        val book = dto.toBook()

        assertTrue(book.imageUrl.contains("/b/id/555-"))
        assertEquals(emptyList<String>(), book.authors)
        assertEquals(emptyList<String>(), book.languages)
        assertEquals(0, book.numEditions)
    }

    @Test
    fun `book roundtrip entity mapping`() {
        val original = TestBookBuilder()
            .withId("ID1")
            .withTitle("Title")
            .withImageUrl("http://example.com/img.jpg")
            .withAuthors(listOf("A1", "A2"))
            .withDescription("Desc")
            .withLanguages(listOf("eng"))
            .withFirstPublishYear("1988")
            .withAverageRating(4.0)
            .withRatingCount(42)
            .withNumPages(250)
            .withNumEditions(3)
            .withPurchased(true)
            .withSpineColor(0xFF112233.toInt())
            .build()

        val entity: BookEntity = original.toBookEntity()
        val mappedBack = entity.toBook()

        assertEquals(original.id, mappedBack.id)
        assertEquals(original.title, mappedBack.title)
        assertEquals(original.imageUrl, mappedBack.imageUrl)
        assertEquals(original.authors, mappedBack.authors)
        assertEquals(original.description, mappedBack.description)
        assertEquals(original.languages, mappedBack.languages)
        assertEquals(original.firstPublishYear, mappedBack.firstPublishYear)
        assertEquals(original.averageRating!!, mappedBack.averageRating!!, 0.0)
        assertEquals(original.ratingCount, mappedBack.ratingCount)
        assertEquals(original.numPages, mappedBack.numPages)
        assertEquals(original.numEditions, mappedBack.numEditions)
        assertEquals(original.purchased, mappedBack.purchased)
        assertEquals(original.spineColor, mappedBack.spineColor)
    }
}
