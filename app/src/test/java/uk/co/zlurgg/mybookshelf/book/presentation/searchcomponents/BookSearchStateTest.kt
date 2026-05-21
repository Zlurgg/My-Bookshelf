package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder

class BookSearchStateTest {

    @Test
    fun `withLoading sets loading and clears typing and error`() {
        val state = BookSearchState(isTyping = true, errorMessage = "old error")

        val result = state.withLoading()

        assertTrue(result.isLoading)
        assertFalse(result.isTyping)
        assertNull(result.errorMessage)
    }

    @Test
    fun `withResults sets results and clears loading and error`() {
        val books = listOf(TestBookBuilder().withId("book-1").build())
        val state = BookSearchState(isLoading = true, errorMessage = "old error")

        val result = state.withResults(books)

        assertFalse(result.isLoading)
        assertTrue(result.hasSearched)
        assertNull(result.errorMessage)
        assertEquals(books, result.results)
    }

    @Test
    fun `withBelowMinLength clears results when query is empty`() {
        val books = listOf(TestBookBuilder().withId("book-1").build())
        val state = BookSearchState(query = "", results = books, isLoading = true, isTyping = true)

        val result = state.withBelowMinLength()

        assertFalse(result.isLoading)
        assertFalse(result.isTyping)
        assertNull(result.errorMessage)
        assertTrue(result.results.isEmpty())
    }

    @Test
    fun `withBelowMinLength clears results when query is whitespace only`() {
        val books = listOf(TestBookBuilder().withId("book-1").build())
        val state = BookSearchState(query = "   ", results = books)

        val result = state.withBelowMinLength()

        assertTrue(result.results.isEmpty())
    }

    @Test
    fun `withBelowMinLength preserves results when query has content`() {
        val books = listOf(TestBookBuilder().withId("book-1").build())
        val state = BookSearchState(query = "k", results = books)

        val result = state.withBelowMinLength()

        assertEquals(books, result.results)
    }

    @Test
    fun `toSearchParams returns general when both filters checked`() {
        val state = BookSearchState(
            query = " kotlin ",
            searchByTitle = true,
            searchByAuthor = true
        )

        val params = state.toSearchParams()

        assertEquals("kotlin", params.general)
        assertNull(params.title)
        assertNull(params.author)
    }

    @Test
    fun `toSearchParams returns title when only title checked`() {
        val state = BookSearchState(
            query = "kotlin",
            searchByTitle = true,
            searchByAuthor = false
        )

        val params = state.toSearchParams()

        assertNull(params.general)
        assertEquals("kotlin", params.title)
        assertNull(params.author)
    }

    @Test
    fun `toSearchParams returns author when only author checked`() {
        val state = BookSearchState(
            query = "kotlin",
            searchByTitle = false,
            searchByAuthor = true
        )

        val params = state.toSearchParams()

        assertNull(params.general)
        assertNull(params.title)
        assertEquals("kotlin", params.author)
    }

    @Test
    fun `toSearchParams falls back to general when both unchecked`() {
        val state = BookSearchState(
            query = "kotlin",
            searchByTitle = false,
            searchByAuthor = false
        )

        val params = state.toSearchParams()

        assertEquals("kotlin", params.general)
        assertNull(params.title)
        assertNull(params.author)
    }

    @Test
    fun `toSearchParams trims query`() {
        val state = BookSearchState(query = "  kotlin  ", searchByTitle = true, searchByAuthor = false)

        val params = state.toSearchParams()

        assertEquals("kotlin", params.title)
    }
}
