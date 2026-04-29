package uk.co.zlurgg.mybookshelf.sync.data.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubBookDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubCommentDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMetadataDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubReviewDto

/**
 * Remote book club operations: CRUD, membership, books, reviews, comments, and account deletion.
 */
@Suppress("TooManyFunctions") // Covers CRUD, membership, books, reviews, comments, and account deletion
interface BookClubRemoteDataSource {
    suspend fun createBookClub(code: String, metadata: BookClubMetadataDto): Result<Unit, DataError.Sync>
    suspend fun getBookClubMetadata(code: String): Result<BookClubMetadataDto?, DataError.Sync>
    suspend fun addBookClubMember(code: String, member: BookClubMemberDto): Result<Unit, DataError.Sync>
    suspend fun removeBookClubMember(code: String, userId: String): Result<Unit, DataError.Sync>
    suspend fun getBookClubMembers(code: String): Result<List<BookClubMemberDto>, DataError.Sync>
    suspend fun isMember(code: String, userId: String): Result<Boolean, DataError.Sync>
    suspend fun addBookToClub(code: String, book: BookClubBookDto): Result<Unit, DataError.Sync>
    suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync>
    suspend fun getClubBooks(code: String): Result<List<BookClubBookDto>, DataError.Sync>
    suspend fun updateBookClubCounts(code: String, bookCount: Int, memberCount: Int): Result<Unit, DataError.Sync>
    suspend fun updateBookClubName(code: String, name: String, lastModifiedAt: Long): Result<Unit, DataError.Sync>
    suspend fun updateBookClubStyle(code: String, style: String, lastModifiedAt: Long): Result<Unit, DataError.Sync>
    suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync>
    suspend fun addClubMembership(userId: String, clubCode: String): Result<Unit, DataError.Sync>
    suspend fun removeClubMembership(userId: String, clubCode: String): Result<Unit, DataError.Sync>
    suspend fun getBookReviews(clubCode: String, bookId: String): Result<List<BookClubReviewDto>, DataError.Sync>
    suspend fun upsertBookReview(
        clubCode: String,
        bookId: String,
        review: BookClubReviewDto,
    ): Result<Unit, DataError.Sync>
    suspend fun deleteBookReview(clubCode: String, bookId: String, userId: String): Result<Unit, DataError.Sync>
    suspend fun getBookComments(clubCode: String, bookId: String): Result<List<BookClubCommentDto>, DataError.Sync>
    suspend fun addBookComment(
        clubCode: String,
        bookId: String,
        comment: BookClubCommentDto,
    ): Result<String, DataError.Sync>
    suspend fun editBookComment(
        clubCode: String,
        bookId: String,
        commentId: String,
        newText: String,
    ): Result<Unit, DataError.Sync>
    suspend fun deleteBookComment(clubCode: String, bookId: String, commentId: String): Result<Unit, DataError.Sync>
    suspend fun getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync>
    suspend fun getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync>
    suspend fun removeUserFromClub(clubCode: String, userId: String): Result<Unit, DataError.Sync>
}
