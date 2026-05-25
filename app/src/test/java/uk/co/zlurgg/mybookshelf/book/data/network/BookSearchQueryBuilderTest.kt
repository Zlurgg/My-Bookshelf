package uk.co.zlurgg.mybookshelf.book.data.network

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.co.zlurgg.mybookshelf.book.data.network.BookSearchQueryBuilder.FilterField

/**
 * Unit tests for [BookSearchQueryBuilder] — the shared query-string assembler
 * used by both `OpenLibraryRemoteBookDataSource` and
 * `GoogleBooksRemoteBookDataSource`.
 *
 * Both providers are exercised against the same construction rules to guarantee
 * the per-provider prefix maps are the only thing that varies.
 */
class BookSearchQueryBuilderTest {

    private val googleBooks = BookSearchQueryBuilder(
        mapOf(
            FilterField.AUTHOR to "inauthor",
            FilterField.TITLE to "intitle",
            FilterField.SUBJECT to "subject",
        )
    )

    private val openLibrary = BookSearchQueryBuilder(
        mapOf(
            FilterField.AUTHOR to "author",
            FilterField.TITLE to "title",
            FilterField.SUBJECT to "subject",
        )
    )

    @Test
    fun `base query alone is sanitized and returned verbatim`() {
        val result = googleBooks.build(baseQuery = "  kotlin  ")
        assertEquals("kotlin", result)
    }

    @Test
    fun `blank base query with no filters produces empty string`() {
        assertEquals("", googleBooks.build(baseQuery = "   "))
    }

    @Test
    fun `single-word author filter is not quoted for google`() {
        val result = googleBooks.build(baseQuery = "kotlin", authorFilter = "Bloch")
        assertEquals("kotlin inauthor:Bloch", result)
    }

    @Test
    fun `single-word author filter is not quoted for open library`() {
        val result = openLibrary.build(baseQuery = "kotlin", authorFilter = "Bloch")
        assertEquals("kotlin author:Bloch", result)
    }

    @Test
    fun `multi-word author filter is wrapped in quotes for google`() {
        val result = googleBooks.build(baseQuery = "kotlin", authorFilter = "Joshua Bloch")
        assertEquals("kotlin inauthor:\"Joshua Bloch\"", result)
    }

    @Test
    fun `multi-word author filter is wrapped in quotes for open library`() {
        val result = openLibrary.build(baseQuery = "kotlin", authorFilter = "Joshua Bloch")
        assertEquals("kotlin author:\"Joshua Bloch\"", result)
    }

    @Test
    fun `embedded double quotes in filter values are stripped`() {
        val result = googleBooks.build(
            baseQuery = "kotlin",
            titleFilter = "Effective \"Java\""
        )
        // Quotes are stripped, then the multi-word value is re-wrapped in one
        // outer pair so the API sees a clean phrase.
        assertEquals("kotlin intitle:\"Effective Java\"", result)
    }

    @Test
    fun `blank filter values are skipped`() {
        val result = googleBooks.build(
            baseQuery = "kotlin",
            authorFilter = "",
            titleFilter = "   ",
        )
        assertEquals("kotlin", result)
    }

    @Test
    fun `null filter values are skipped`() {
        val result = googleBooks.build(
            baseQuery = "kotlin",
            authorFilter = null,
            titleFilter = null,
            subjectFilter = null,
        )
        assertEquals("kotlin", result)
    }

    @Test
    fun `combined fields render in author-title-subject order for google`() {
        val result = googleBooks.build(
            baseQuery = "effective",
            authorFilter = "Joshua Bloch",
            titleFilter = "Effective Java",
            subjectFilter = "Programming",
        )
        assertEquals(
            "effective inauthor:\"Joshua Bloch\" intitle:\"Effective Java\" subject:Programming",
            result,
        )
    }

    @Test
    fun `combined fields render in author-title-subject order for open library`() {
        val result = openLibrary.build(
            baseQuery = "effective",
            authorFilter = "Joshua Bloch",
            titleFilter = "Effective Java",
            subjectFilter = "Programming",
        )
        assertEquals(
            "effective author:\"Joshua Bloch\" title:\"Effective Java\" subject:Programming",
            result,
        )
    }

    @Test
    fun `empty base query with only filters drops the leading space`() {
        val result = googleBooks.build(baseQuery = "  ", authorFilter = "Bloch")
        assertEquals("inauthor:Bloch", result)
    }

    @Test
    fun `unknown filter field is silently omitted when missing from prefix map`() {
        val minimal = BookSearchQueryBuilder(
            mapOf(FilterField.AUTHOR to "inauthor")
            // TITLE and SUBJECT omitted on purpose
        )
        val result = minimal.build(
            baseQuery = "kotlin",
            authorFilter = "Bloch",
            titleFilter = "Effective Java",
            subjectFilter = "Programming",
        )
        assertEquals("kotlin inauthor:Bloch", result)
    }
}
