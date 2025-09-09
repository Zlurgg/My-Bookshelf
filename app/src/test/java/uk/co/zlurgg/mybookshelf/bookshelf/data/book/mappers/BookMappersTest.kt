package uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchedBookDto
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

class BookMappersTest {

    @Test
    fun `toBook maps SearchedBookDto with coverKey`() {
        val dto = SearchedBookDto(
            id = "/works/OL123W",
            title = "Test Title",
            coverKey = "OLCOVER1M",
            coverAlternativeKey = 123,
            authorNames = listOf("Author One"),
            languages = listOf("eng"),
            firstPublishYear = 1999,
            ratingsAverage = 4.5,
            ratingsCount = 10,
            numPagesMedian = 321,
            numEditions = 2,
        )

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
        // spineColor is random; just ensure it's a valid color int by checking it's non-zero
        assertTrue(book.spineColor != 0)
    }

    @Test
    fun `toBook maps SearchedBookDto without coverKey uses alternative`() {
        val dto = SearchedBookDto(
            id = "/works/OL999W",
            title = "No Cover Key",
            coverKey = null,
            coverAlternativeKey = 555,
            authorNames = null,
            languages = null,
            firstPublishYear = 2001,
            ratingsAverage = 3.0,
            ratingsCount = 0,
            numPagesMedian = 100,
            numEditions = null,
        )

        val book = dto.toBook()

        assertTrue(book.imageUrl.contains("/b/id/555-"))
        assertEquals(emptyList<String>(), book.authors)
        assertEquals(emptyList<String>(), book.languages)
        assertEquals(0, book.numEditions)
    }

    @Test
    fun `book roundtrip entity mapping`() {
        val original = Book(
            id = "ID1",
            title = "Title",
            imageUrl = "http://example.com/img.jpg",
            authors = listOf("A1", "A2"),
            description = "Desc",
            languages = listOf("eng"),
            firstPublishYear = "1988",
            averageRating = 4.0,
            ratingCount = 42,
            numPages = 250,
            numEditions = 3,
            purchased = true,
            affiliateLink = "http://buy",
            spineColor = 0xFF112233.toInt()
        )

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
        assertEquals(original.affiliateLink, mappedBack.affiliateLink)
        assertEquals(original.spineColor, mappedBack.spineColor)
    }
}
