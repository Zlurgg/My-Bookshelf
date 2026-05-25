package uk.co.zlurgg.mybookshelf.book.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleBookItemDto
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleImageLinksDto
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleIndustryIdentifierDto
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleSearchInfoDto
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleVolumeInfoDto
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.MaturityRating
import uk.co.zlurgg.mybookshelf.book.domain.model.PrintType

@RunWith(RobolectricTestRunner::class)
class GoogleBookMappersTest {

    @Test
    fun `toBook maps complete GoogleBookItemDto`() {
        val dto = GoogleBookItemDto(
            id = "dXz5DwAAQBAJ",
            volumeInfo = GoogleVolumeInfoDto(
                title = "Kotlin in Action",
                subtitle = "Second Edition",
                authors = listOf("Dmitry Jemerov", "Svetlana Isakova"),
                publisher = "Manning",
                publishedDate = "2017-02-03",
                description = "A <b>great</b> book about Kotlin",
                pageCount = 360,
                categories = listOf("Computers"),
                imageLinks = GoogleImageLinksDto(
                    thumbnail = "http://books.google.com/books/content?id=dXz5DwAAQBAJ&printsec=frontcover&img=1&zoom=1"
                ),
                language = "en",
                previewLink = "https://books.google.com/preview",
                infoLink = "https://books.google.com/info",
                maturityRating = "NOT_MATURE",
                printType = "BOOK",
                industryIdentifiers = listOf(
                    GoogleIndustryIdentifierDto("ISBN_13", "9781617293290"),
                    GoogleIndustryIdentifierDto("ISBN_10", "1617293296"),
                ),
            ),
        )

        val book = dto.toBook()

        assertEquals("dXz5DwAAQBAJ", book.id)
        assertEquals("Kotlin in Action", book.title)
        assertEquals("Second Edition", book.subtitle)
        assertEquals(listOf("Dmitry Jemerov", "Svetlana Isakova"), book.authors)
        assertEquals("Manning", book.publisher)
        assertEquals("2017-02-03", book.publishDate)
        assertEquals("2017", book.firstPublishYear)
        assertEquals(360, book.numPages)
        assertEquals(listOf("Computers"), book.subjects)
        assertEquals(listOf("en"), book.languages)
        assertEquals(BookProvider.GOOGLE_BOOKS, book.provider)
        assertEquals(MaturityRating.NOT_MATURE, book.maturityRating)
        assertEquals(PrintType.BOOK, book.printType)
        assertEquals("https://books.google.com/preview", book.previewLink)
        assertEquals("https://books.google.com/info", book.infoLink)
    }

    @Test
    fun `toBook prefers ISBN_13 over ISBN_10`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(
                industryIdentifiers = listOf(
                    GoogleIndustryIdentifierDto("ISBN_10", "1617293296"),
                    GoogleIndustryIdentifierDto("ISBN_13", "9781617293290"),
                ),
            ),
        )

        assertEquals("9781617293290", dto.toBook().isbn)
    }

    @Test
    fun `toBook falls back to first identifier when no ISBN_13`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(
                industryIdentifiers = listOf(
                    GoogleIndustryIdentifierDto("ISBN_10", "1617293296"),
                ),
            ),
        )

        assertEquals("1617293296", dto.toBook().isbn)
    }

    @Test
    fun `toBook forces HTTPS on image URLs`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(
                imageLinks = GoogleImageLinksDto(
                    thumbnail = "http://books.google.com/books/content?id=test&zoom=1"
                ),
            ),
        )

        assertTrue(dto.toBook().imageUrl.startsWith("https://"))
    }

    @Test
    fun `toBook handles null volumeInfo`() {
        val dto = GoogleBookItemDto(id = "test", volumeInfo = null)

        val book = dto.toBook()

        assertEquals("test", book.id)
        assertEquals("", book.title)
        assertNull(book.subtitle)
        assertEquals(emptyList<String>(), book.authors)
        assertEquals("", book.imageUrl)
        assertNull(book.description)
        assertEquals(BookProvider.GOOGLE_BOOKS, book.provider)
        assertEquals(MaturityRating.UNKNOWN, book.maturityRating)
        assertEquals(PrintType.UNKNOWN, book.printType)
    }

    @Test
    fun `toBook maps unknown maturityRating to UNKNOWN`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(maturityRating = "SOME_NEW_VALUE"),
        )

        assertEquals(MaturityRating.UNKNOWN, dto.toBook().maturityRating)
    }

    @Test
    fun `toBook maps MATURE maturityRating`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(maturityRating = "MATURE"),
        )

        assertEquals(MaturityRating.MATURE, dto.toBook().maturityRating)
    }

    @Test
    fun `toBook maps MAGAZINE printType`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(printType = "MAGAZINE"),
        )

        assertEquals(PrintType.MAGAZINE, dto.toBook().printType)
    }

    @Test
    fun `toBook coerces non-HTTPS previewLink and infoLink to null`() {
        val hostileSchemes = listOf(
            "http://books.google.com/preview",
            "mailto:attacker@example.com",
            "intent://books.google.com/preview#Intent;scheme=https;end",
            "javascript:alert(1)",
            "tel:+15551234567",
            "",
        )
        hostileSchemes.forEach { value ->
            val dto = GoogleBookItemDto(
                id = "test",
                volumeInfo = GoogleVolumeInfoDto(
                    previewLink = value,
                    infoLink = value,
                ),
            )

            val book = dto.toBook()

            assertNull("previewLink should be null for input '$value'", book.previewLink)
            assertNull("infoLink should be null for input '$value'", book.infoLink)
        }
    }

    @Test
    fun `toBook leaves previewLink and infoLink null when DTO values are null`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(previewLink = null, infoLink = null),
        )

        val book = dto.toBook()

        assertNull(book.previewLink)
        assertNull(book.infoLink)
    }

    @Test
    fun `toBook maps searchInfo textSnippet into searchSnippet with HTML stripped`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(description = null),
            searchInfo = GoogleSearchInfoDto(
                textSnippet = "A <b>great</b> snippet &amp; preview"
            ),
        )

        val book = dto.toBook()

        assertEquals("A great snippet & preview", book.searchSnippet)
    }

    @Test
    fun `toBook leaves searchSnippet null when searchInfo is absent`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(description = "Real description"),
            searchInfo = null,
        )

        assertNull(dto.toBook().searchSnippet)
    }

    @Test
    fun `toBook leaves searchSnippet null when textSnippet is null`() {
        val dto = GoogleBookItemDto(
            id = "test",
            volumeInfo = GoogleVolumeInfoDto(),
            searchInfo = GoogleSearchInfoDto(textSnippet = null),
        )

        assertNull(dto.toBook().searchSnippet)
    }

    @Test
    fun `stripHtml removes HTML tags and decodes entities`() {
        assertEquals("Hello World", stripHtml("<b>Hello</b> <i>World</i>"))
        assertEquals("Tom & Jerry", stripHtml("Tom &amp; Jerry"))
    }

    @Test
    fun `stripHtml returns null for null input`() {
        assertNull(stripHtml(null))
    }
}
