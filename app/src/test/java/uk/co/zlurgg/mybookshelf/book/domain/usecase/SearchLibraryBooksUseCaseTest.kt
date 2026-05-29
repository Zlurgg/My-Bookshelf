package uk.co.zlurgg.mybookshelf.book.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

class SearchLibraryBooksUseCaseTest {

    private val mockBookRepository = MockBookRepository()
    private val useCase = SearchLibraryBooksUseCaseImpl(mockBookRepository)

    private val dune = TestBookBuilder()
        .withId("dune")
        .withTitle("Dune")
        .withAuthors(listOf("Frank Herbert"))
        .build()
    private val foundation = TestBookBuilder()
        .withId("foundation")
        .withTitle("Foundation")
        .withAuthors(listOf("Isaac Asimov"))
        .build()
    private val hobbit = TestBookBuilder()
        .withId("hobbit")
        .withTitle("The Hobbit")
        .withAuthors(listOf("J.R.R. Tolkien"))
        .build()

    @After
    fun tearDown() {
        mockBookRepository.reset()
    }

    @Test
    fun `empty query returns the whole library`() = runTest {
        mockBookRepository.setPersonalBooks(listOf(dune, foundation, hobbit))

        val result = useCase(query = "", searchByTitle = true, searchByAuthor = false)

        assertTrue(result is Result.Success)
        assertEquals(listOf(dune, foundation, hobbit), (result as Result.Success).data)
    }

    @Test
    fun `whitespace-only query returns the whole library`() = runTest {
        mockBookRepository.setPersonalBooks(listOf(dune, foundation))

        val result = useCase(query = "   ", searchByTitle = true, searchByAuthor = false)

        assertEquals(listOf(dune, foundation), (result as Result.Success).data)
    }

    @Test
    fun `title-only filter matches case-insensitively on title`() = runTest {
        mockBookRepository.setPersonalBooks(listOf(dune, foundation, hobbit))

        val result = useCase(query = "DUNE", searchByTitle = true, searchByAuthor = false)

        assertEquals(listOf(dune), (result as Result.Success).data)
    }

    @Test
    fun `author-only filter matches case-insensitively on author`() = runTest {
        mockBookRepository.setPersonalBooks(listOf(dune, foundation, hobbit))

        val result = useCase(query = "asimov", searchByTitle = false, searchByAuthor = true)

        assertEquals(listOf(foundation), (result as Result.Success).data)
    }

    @Test
    fun `both filters use OR semantics`() = runTest {
        // "Frank" matches dune.authors; nothing in foundation/hobbit
        mockBookRepository.setPersonalBooks(listOf(dune, foundation, hobbit))

        val result = useCase(query = "frank", searchByTitle = true, searchByAuthor = true)

        assertEquals(listOf(dune), (result as Result.Success).data)
    }

    @Test
    fun `both filters off falls back to title search`() = runTest {
        // Defensive fallback: previously-persisted "Subject only" with library
        // scope on shouldn't yield zero results — title is the default.
        mockBookRepository.setPersonalBooks(listOf(dune, foundation, hobbit))

        val result = useCase(query = "hobbit", searchByTitle = false, searchByAuthor = false)

        assertEquals(listOf(hobbit), (result as Result.Success).data)
    }

    @Test
    fun `partial title substring matches`() = runTest {
        mockBookRepository.setPersonalBooks(listOf(hobbit))

        val result = useCase(query = "hobb", searchByTitle = true, searchByAuthor = false)

        assertEquals(listOf(hobbit), (result as Result.Success).data)
    }

    @Test
    fun `no matches returns empty list, still success`() = runTest {
        mockBookRepository.setPersonalBooks(listOf(dune, foundation))

        val result = useCase(query = "xenocide", searchByTitle = true, searchByAuthor = true)

        assertTrue(result is Result.Success)
        assertEquals(emptyList<Any>(), (result as Result.Success).data)
    }

    @Test
    fun `empty library returns empty list`() = runTest {
        mockBookRepository.setPersonalBooks(emptyList())

        val result = useCase(query = "anything", searchByTitle = true, searchByAuthor = true)

        assertEquals(emptyList<Any>(), (result as Result.Success).data)
    }
}
