package uk.co.zlurgg.mybookshelf.bookclub.data.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookclub.data.dto.BookClubBookDto
import uk.co.zlurgg.mybookshelf.bookclub.data.dto.BookClubCommentDto
import uk.co.zlurgg.mybookshelf.bookclub.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.bookclub.data.dto.BookClubMetadataDto
import uk.co.zlurgg.mybookshelf.bookclub.data.dto.BookClubReviewDto
import uk.co.zlurgg.mybookshelf.bookclub.data.remote.BookClubRemoteDataSource
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubCode
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class BookClubCodeGeneratorImplTest {

    // ========== Successful Generation Tests ==========

    @Test
    fun `generateUniqueCode - when code does not exist - returns success`() = runTest {
        // Given
        val stub = StubBookClubRemoteDataSource(
            metadataResults = listOf(Result.Success(null))
        )
        val generator = BookClubCodeGeneratorImpl(stub)

        // When
        val result = generator.generateUniqueCode()

        // Then
        assertTrue("Should return success", result is Result.Success)
    }

    @Test
    fun `generateUniqueCode - generated codes have correct length and valid characters`() = runTest {
        // Given — accept non-determinism, run multiple iterations for statistical confidence
        val stub = StubBookClubRemoteDataSource(
            metadataResults = List(100) { Result.Success(null) }
        )
        val generator = BookClubCodeGeneratorImpl(stub)

        // When / Then
        repeat(100) {
            val result = generator.generateUniqueCode()
            assertTrue("Should return success", result is Result.Success)
            val code = (result as Result.Success).data
            assertEquals("Code should be ${BookClubCode.CODE_LENGTH} characters", BookClubCode.CODE_LENGTH, code.length)
            assertTrue(
                "All characters should be in VALID_CHARS, got: $code",
                code.all { it in BookClubCode.VALID_CHARS }
            )
        }
    }

    // ========== Retry Tests ==========

    @Test
    fun `generateUniqueCode - when first code exists - retries and succeeds`() = runTest {
        // Given — first attempt finds existing club, second attempt succeeds
        val existingMetadata = BookClubMetadataDto(code = "EXISTS")
        val stub = StubBookClubRemoteDataSource(
            metadataResults = listOf(
                Result.Success(existingMetadata),
                Result.Success(null)
            )
        )
        val generator = BookClubCodeGeneratorImpl(stub)

        // When
        val result = generator.generateUniqueCode()

        // Then
        assertTrue("Should return success after retry", result is Result.Success)
        assertEquals("Should have checked metadata twice", 2, stub.getMetadataCallCount)
    }

    @Test
    fun `generateUniqueCode - succeeds on last attempt - returns success`() = runTest {
        // Given — attempts 1-4 collide, attempt 5 succeeds (boundary test)
        val existingMetadata = BookClubMetadataDto(code = "EXISTS")
        val stub = StubBookClubRemoteDataSource(
            metadataResults = listOf(
                Result.Success(existingMetadata),
                Result.Success(existingMetadata),
                Result.Success(existingMetadata),
                Result.Success(existingMetadata),
                Result.Success(null)
            )
        )
        val generator = BookClubCodeGeneratorImpl(stub)

        // When
        val result = generator.generateUniqueCode()

        // Then
        assertTrue("Should return success on last attempt", result is Result.Success)
        assertEquals("Should have checked metadata 5 times", 5, stub.getMetadataCallCount)
    }

    @Test
    fun `generateUniqueCode - when all retries exhausted - returns GENERATION_FAILED`() = runTest {
        // Given — all 5 attempts find existing clubs
        val existingMetadata = BookClubMetadataDto(code = "EXISTS")
        val stub = StubBookClubRemoteDataSource(
            metadataResults = List(5) { Result.Success(existingMetadata) }
        )
        val generator = BookClubCodeGeneratorImpl(stub)

        // When
        val result = generator.generateUniqueCode()

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.GENERATION_FAILED, (result as Result.Error).error)
        assertEquals("Should have checked metadata 5 times", 5, stub.getMetadataCallCount)
    }

    // ========== Error Propagation Tests ==========

    @Test
    fun `generateUniqueCode - when network error on first attempt - propagates immediately`() = runTest {
        // Given
        val stub = StubBookClubRemoteDataSource(
            metadataResults = listOf(Result.Error(DataError.Sync.NETWORK_ERROR))
        )
        val generator = BookClubCodeGeneratorImpl(stub)

        // When
        val result = generator.generateUniqueCode()

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
        assertEquals("Should not retry after network error", 1, stub.getMetadataCallCount)
    }

    @Test
    fun `generateUniqueCode - when network error after collisions - propagates error`() = runTest {
        // Given — attempts 1-2 collide, attempt 3 hits network error
        val existingMetadata = BookClubMetadataDto(code = "EXISTS")
        val stub = StubBookClubRemoteDataSource(
            metadataResults = listOf(
                Result.Success(existingMetadata),
                Result.Success(existingMetadata),
                Result.Error(DataError.Sync.NETWORK_ERROR)
            )
        )
        val generator = BookClubCodeGeneratorImpl(stub)

        // When
        val result = generator.generateUniqueCode()

        // Then
        assertTrue("Should return error", result is Result.Error)
        assertEquals(DataError.Sync.NETWORK_ERROR, (result as Result.Error).error)
        assertEquals("Should have checked 3 times", 3, stub.getMetadataCallCount)
    }
}

// ========== Test Stubs ==========

/**
 * Focused stub for BookClubRemoteDataSource — only getBookClubMetadata is implemented.
 * Results are returned in sequence; if more calls are made than results provided,
 * the last result is repeated.
 */
private class StubBookClubRemoteDataSource(
    private val metadataResults: List<Result<BookClubMetadataDto?, DataError.Sync>>
) : BookClubRemoteDataSource {

    var getMetadataCallCount = 0
        private set

    override suspend fun getBookClubMetadata(code: String): Result<BookClubMetadataDto?, DataError.Sync> {
        val index = getMetadataCallCount.coerceAtMost(metadataResults.lastIndex)
        getMetadataCallCount++
        return metadataResults[index]
    }

    // Unused methods — not called by BookClubCodeGeneratorImpl
    override suspend fun createBookClub(code: String, metadata: BookClubMetadataDto) = nie()
    override suspend fun addBookClubMember(code: String, member: BookClubMemberDto) = nie()
    override suspend fun removeBookClubMember(code: String, userId: String) = nie()
    override suspend fun getBookClubMembers(code: String) = nie()
    override suspend fun isMember(code: String, userId: String) = nie()
    override suspend fun addBookToClub(code: String, book: BookClubBookDto) = nie()
    override suspend fun removeBookFromClub(code: String, bookId: String) = nie()
    override suspend fun getClubBooks(code: String) = nie()
    override suspend fun updateBookClubCounts(code: String, bookCount: Int, memberCount: Int) = nie()
    override suspend fun updateBookClubName(code: String, name: String, lastModifiedAt: Long) = nie()
    override suspend fun updateBookClubStyle(code: String, style: String, lastModifiedAt: Long) = nie()
    override suspend fun deleteBookClub(code: String) = nie()
    override suspend fun addClubMembership(userId: String, clubCode: String) = nie()
    override suspend fun removeClubMembership(userId: String, clubCode: String) = nie()
    override suspend fun getBookReviews(clubCode: String, bookId: String) = nie()
    override suspend fun upsertBookReview(clubCode: String, bookId: String, review: BookClubReviewDto) = nie()
    override suspend fun deleteBookReview(clubCode: String, bookId: String, userId: String) = nie()
    override suspend fun getBookComments(clubCode: String, bookId: String) = nie()
    override suspend fun addBookComment(clubCode: String, bookId: String, comment: BookClubCommentDto) = nie()
    override suspend fun editBookComment(clubCode: String, bookId: String, commentId: String, newText: String) = nie()
    override suspend fun deleteBookComment(clubCode: String, bookId: String, commentId: String) = nie()
    override suspend fun getClubsCreatedByUser(userId: String) = nie()
    override suspend fun getClubMembershipsForUser(userId: String) = nie()
    override suspend fun removeUserFromClub(clubCode: String, userId: String) = nie()

    private fun nie(): Nothing = throw NotImplementedError("Not used in BookClubCodeGeneratorImpl tests")
}
