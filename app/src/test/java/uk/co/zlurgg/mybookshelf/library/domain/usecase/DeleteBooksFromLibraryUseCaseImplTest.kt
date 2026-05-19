package uk.co.zlurgg.mybookshelf.library.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder
import uk.co.zlurgg.mybookshelf.testutil.mocks.MockBookRepository

class DeleteBooksFromLibraryUseCaseImplTest {

    private lateinit var mockBookRepository: MockBookRepository
    private lateinit var useCase: DeleteBooksFromLibraryUseCaseImpl

    @Before
    fun setUp() {
        mockBookRepository = MockBookRepository()
        useCase = DeleteBooksFromLibraryUseCaseImpl(mockBookRepository)
    }

    @Test
    fun `valid IDs delegate to repository`() = runTest {
        val book1 = TestBookBuilder().withId("book-1").build()
        val book2 = TestBookBuilder().withId("book-2").build()
        mockBookRepository.addBook(book1)
        mockBookRepository.addBook(book2)

        val result = useCase(listOf("book-1", "book-2"))

        assertTrue(result is Result.Success)
        assertEquals(1, mockBookRepository.deleteBooksCallCount)
        assertEquals(listOf("book-1", "book-2"), mockBookRepository.lastDeletedBookIds)
    }

    @Test
    fun `rejects non-removable book IDs with PROTECTED_RESOURCE`() = runTest {
        mockBookRepository.setNonRemovableBookIds(setOf("club-book-1"))

        val result = useCase(listOf("book-1", "club-book-1"))

        assertTrue(result is Result.Error)
        assertEquals(
            DataError.Local.PROTECTED_RESOURCE,
            (result as Result.Error).error
        )
        assertEquals(0, mockBookRepository.deleteBooksCallCount)
    }

    @Test
    fun `all non-removable IDs returns PROTECTED_RESOURCE`() = runTest {
        mockBookRepository.setNonRemovableBookIds(setOf("club-1", "club-2"))

        val result = useCase(listOf("club-1", "club-2"))

        assertTrue(result is Result.Error)
        assertEquals(
            DataError.Local.PROTECTED_RESOURCE,
            (result as Result.Error).error
        )
    }

    @Test
    fun `empty list delegates to repository`() = runTest {
        val result = useCase(emptyList())

        assertTrue(result is Result.Success)
        assertEquals(1, mockBookRepository.deleteBooksCallCount)
    }

    @Test
    fun `all valid IDs when no non-removable books exist`() = runTest {
        val result = useCase(listOf("book-1", "book-2"))

        assertTrue(result is Result.Success)
        assertEquals(1, mockBookRepository.deleteBooksCallCount)
    }

    @Test
    fun `repository error is propagated`() = runTest {
        mockBookRepository.errorToReturn = DataError.Local.DATABASE_ERROR

        val result = useCase(listOf("book-1"))

        assertTrue(result is Result.Error)
        assertEquals(
            DataError.Local.DATABASE_ERROR,
            (result as Result.Error).error
        )
    }
}
