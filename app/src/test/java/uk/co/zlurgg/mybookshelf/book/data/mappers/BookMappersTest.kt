package uk.co.zlurgg.mybookshelf.book.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.MaturityRating
import uk.co.zlurgg.mybookshelf.book.domain.model.PrintType
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
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
            .withNumPagesMedian(321)
            .withSubjects(listOf("Fiction", "Adventure"))
            .build()

        val book = dto.toBook()

        assertEquals("OL123W", book.id)
        assertEquals("Test Title", book.title)
        assertTrue(book.imageUrl.contains("olid/OLCOVER1M"))
        assertEquals(listOf("Author One"), book.authors)
        assertEquals(listOf("eng"), book.languages)
        assertEquals("1999", book.firstPublishYear)
        assertEquals(321, book.numPages)
        assertEquals(BookProvider.OPEN_LIBRARY, book.provider)
        // spineColor is placeholder (0) for search results - actual color generated when added to shelf
        assertEquals(0, book.spineColor)
        assertEquals(listOf("Fiction", "Adventure"), book.subjects)
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
            .withNumPagesMedian(100)
            .build()

        val book = dto.toBook()

        assertTrue(book.imageUrl.contains("/b/id/555-"))
        assertEquals(emptyList<String>(), book.authors)
        assertEquals(emptyList<String>(), book.languages)
        assertEquals(BookProvider.OPEN_LIBRARY, book.provider)
        assertEquals(emptyList<String>(), book.subjects)
    }

    @Test
    fun `book roundtrip entity mapping`() {
        val original = TestBookBuilder()
            .withId("ID1")
            .withTitle("Title")
            .withSubtitle("A Subtitle")
            .withImageUrl("http://example.com/img.jpg")
            .withAuthors(listOf("A1", "A2"))
            .withDescription("Desc")
            .withLanguages(listOf("eng"))
            .withFirstPublishYear("1988")
            .withNumPages(250)
            .withPurchased(true)
            .withSpineColor(0xFF112233.toInt())
            .withProvider(BookProvider.GOOGLE_BOOKS)
            .withSubjects(listOf("History", "Biography"))
            .withPreviewLink("https://books.google.com/preview")
            .withInfoLink("https://books.google.com/info")
            .withMaturityRating(MaturityRating.NOT_MATURE)
            .withPrintType(PrintType.BOOK)
            .build()

        val entity: BookEntity = original.toBookEntity()
        val mappedBack = entity.toBook()

        assertEquals(original.id, mappedBack.id)
        assertEquals(original.title, mappedBack.title)
        assertEquals(original.subtitle, mappedBack.subtitle)
        assertEquals(original.imageUrl, mappedBack.imageUrl)
        assertEquals(original.authors, mappedBack.authors)
        assertEquals(original.description, mappedBack.description)
        assertEquals(original.languages, mappedBack.languages)
        assertEquals(original.firstPublishYear, mappedBack.firstPublishYear)
        assertEquals(original.numPages, mappedBack.numPages)
        assertEquals(original.purchased, mappedBack.purchased)
        assertEquals(original.spineColor, mappedBack.spineColor)
        assertEquals(original.provider, mappedBack.provider)
        assertEquals(original.subjects, mappedBack.subjects)
        assertEquals(original.previewLink, mappedBack.previewLink)
        assertEquals(original.infoLink, mappedBack.infoLink)
        assertEquals(original.maturityRating, mappedBack.maturityRating)
        assertEquals(original.printType, mappedBack.printType)
    }

    @Test
    fun `book roundtrip entity mapping preserves OL provider`() {
        val original = TestBookBuilder()
            .withId("OL123W")
            .withProvider(BookProvider.OPEN_LIBRARY)
            .build()

        val entity: BookEntity = original.toBookEntity()
        val mappedBack = entity.toBook()

        assertEquals(BookProvider.OPEN_LIBRARY, mappedBack.provider)
    }
}
