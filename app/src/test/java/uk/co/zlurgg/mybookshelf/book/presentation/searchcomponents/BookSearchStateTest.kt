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
    fun `withBelowMinLength resets pagination state so stale Load More button is hidden`() {
        // Regression: after a successful paginated search, clearing the query
        // (X tap or backspace-to-empty) ran withBelowMinLength, which cleared
        // results but left canLoadMore=true. The dialog's footer render checks
        // (canLoadMore || isLoadingMore), so the button rendered over an empty
        // list — tap silently swallowed by the VM's min-length guard.
        val books = listOf(TestBookBuilder().withId("book-1").build())
        val state = BookSearchState(
            query = "",
            results = books,
            canLoadMore = true,
            nextStartIndex = 20,
            isLoadingMore = true,
        )

        val result = state.withBelowMinLength()

        assertFalse("canLoadMore must reset so the footer disappears", result.canLoadMore)
        assertFalse("isLoadingMore must reset", result.isLoadingMore)
        assertEquals("nextStartIndex must reset", 0, result.nextStartIndex)
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

    // Subject search tests

    @Test
    fun `toSearchParams returns subject when subject checked`() {
        val state = BookSearchState(
            query = "dragons",
            searchByTitle = false,
            searchByAuthor = false,
            searchBySubject = true
        )

        val params = state.toSearchParams()

        assertNull(params.general)
        assertNull(params.title)
        assertNull(params.author)
        assertEquals("dragons", params.subject)
    }

    @Test
    fun `toSearchParams returns general plus subject when title+author+subject checked`() {
        val state = BookSearchState(
            query = "dragons",
            searchByTitle = true,
            searchByAuthor = true,
            searchBySubject = true
        )

        val params = state.toSearchParams()

        assertEquals("dragons", params.general)
        assertNull(params.title)
        assertNull(params.author)
        assertEquals("dragons", params.subject)
    }

    @Test
    fun `toSearchParams returns title plus subject when title+subject checked`() {
        val state = BookSearchState(
            query = "dragons",
            searchByTitle = true,
            searchByAuthor = false,
            searchBySubject = true
        )

        val params = state.toSearchParams()

        assertNull(params.general)
        assertEquals("dragons", params.title)
        assertNull(params.author)
        assertEquals("dragons", params.subject)
    }

    @Test
    fun `toSearchParams returns author plus subject when author+subject checked`() {
        val state = BookSearchState(
            query = "tolkien",
            searchByTitle = false,
            searchByAuthor = true,
            searchBySubject = true
        )

        val params = state.toSearchParams()

        assertNull(params.general)
        assertNull(params.title)
        assertEquals("tolkien", params.author)
        assertEquals("tolkien", params.subject)
    }

    @Test
    fun `toSearchParams returns general when title+author checked no subject - existing behavior`() {
        val state = BookSearchState(
            query = "kotlin",
            searchByTitle = true,
            searchByAuthor = true,
            searchBySubject = false
        )

        val params = state.toSearchParams()

        assertEquals("kotlin", params.general)
        assertNull(params.title)
        assertNull(params.author)
        assertNull(params.subject)
    }

    // canToggle three-way interaction tests

    @Test
    fun `canToggleTitle true when subject is checked`() {
        val state = BookSearchState(
            searchByTitle = true,
            searchByAuthor = false,
            searchBySubject = true
        )

        assertTrue(state.canToggleTitle)
    }

    @Test
    fun `canToggleSubject false when title and author both unchecked`() {
        val state = BookSearchState(
            searchByTitle = false,
            searchByAuthor = false,
            searchBySubject = true
        )

        assertFalse(state.canToggleSubject)
    }

    @Test
    fun `canToggleSubject true when title is checked`() {
        val state = BookSearchState(
            searchByTitle = true,
            searchByAuthor = false,
            searchBySubject = true
        )

        assertTrue(state.canToggleSubject)
    }

    @Test
    fun `canToggleAuthor true when subject is checked`() {
        val state = BookSearchState(
            searchByTitle = false,
            searchByAuthor = true,
            searchBySubject = true
        )

        assertTrue(state.canToggleAuthor)
    }

    // Defensive fallback still works with all three unchecked

    @Test
    fun `toSearchParams defensive fallback still returns general when all unchecked`() {
        val state = BookSearchState(
            query = "kotlin",
            searchByTitle = false,
            searchByAuthor = false,
            searchBySubject = false
        )

        val params = state.toSearchParams()

        assertEquals("kotlin", params.general)
        assertNull(params.subject)
    }
}
