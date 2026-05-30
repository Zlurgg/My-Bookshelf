package uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder

class BookSearchStateTest {

    @Test
    fun `withLoading sets loading and clears error`() {
        val state = BookSearchState(errorMessage = "old error")

        val result = state.withLoading()

        assertTrue(result.isLoading)
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
    fun `resetForDialogClose clears query lastSubmittedQuery and result state`() {
        val books = listOf(TestBookBuilder().withId("book-1").build())
        val state = BookSearchState(
            query = "harry",
            lastSubmittedQuery = "harry",
            results = books,
            hasSearched = true,
            isLoading = true,
            isLoadingMore = true,
            canLoadMore = true,
            nextStartIndex = 20,
            filteredCount = 1,
            errorMessage = "boom",
        )

        val result = state.resetForDialogClose()

        assertEquals("", result.query)
        assertEquals("", result.lastSubmittedQuery)
        assertTrue(result.results.isEmpty())
        assertFalse(result.hasSearched)
        assertFalse(result.isLoading)
        assertFalse(result.isLoadingMore)
        assertFalse("canLoadMore must reset so stale footer disappears", result.canLoadMore)
        assertEquals(0, result.nextStartIndex)
        assertEquals(0, result.filteredCount)
        assertNull(result.errorMessage)
    }

    @Test
    fun `resetForDialogClose preserves filter prefs and existingBookIds`() {
        val state = BookSearchState(
            query = "harry",
            lastSubmittedQuery = "harry",
            searchByTitle = false,
            searchByAuthor = true,
            searchBySubject = true,
            safeSearchEnabled = false,
            libraryScopeEnabled = true,
            existingBookIds = setOf("book-1", "book-2"),
        )

        val result = state.resetForDialogClose()

        assertFalse(result.searchByTitle)
        assertTrue(result.searchByAuthor)
        assertTrue(result.searchBySubject)
        assertFalse(result.safeSearchEnabled)
        assertTrue(result.libraryScopeEnabled)
        assertEquals(setOf("book-1", "book-2"), result.existingBookIds)
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
