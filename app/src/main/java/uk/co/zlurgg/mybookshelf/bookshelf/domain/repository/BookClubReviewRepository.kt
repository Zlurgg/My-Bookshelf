package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Review and comment operations for book clubs.
 */
interface BookClubReviewRepository {
    suspend fun getBookReviews(code: String, bookId: String): Result<List<BookClubReview>, DataError.Sync>
    suspend fun upsertBookReview(
        code: String,
        bookId: String,
        rating: Float,
        reviewText: String
    ): Result<Unit, DataError.Sync>
    suspend fun deleteBookReview(code: String, bookId: String): Result<Unit, DataError.Sync>
    suspend fun getBookComments(code: String, bookId: String): Result<List<BookClubComment>, DataError.Sync>
    suspend fun addBookComment(code: String, bookId: String, text: String): Result<String, DataError.Sync>
    suspend fun editBookComment(
        code: String,
        bookId: String,
        commentId: String,
        newText: String
    ): Result<Unit, DataError.Sync>
    suspend fun deleteBookComment(code: String, bookId: String, commentId: String): Result<Unit, DataError.Sync>
}
