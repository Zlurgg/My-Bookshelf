package uk.co.zlurgg.mybookshelf.book.domain.service

import uk.co.zlurgg.mybookshelf.book.domain.model.BookComment
import uk.co.zlurgg.mybookshelf.book.domain.model.BookReview
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Interface for book review/comment operations consumed by BookDetailViewModel.
 * Provides a bridge between bookshelf and bookclub without direct dependency.
 */
interface BookReviewProvider {

    suspend fun getReviews(clubCode: String, bookId: String): Result<List<BookReview>, DataError.Sync>

    suspend fun upsertReview(
        clubCode: String,
        bookId: String,
        rating: Float,
        reviewText: String
    ): Result<Unit, DataError.Sync>

    suspend fun deleteReview(clubCode: String, bookId: String): Result<Unit, DataError.Sync>

    suspend fun getComments(clubCode: String, bookId: String): Result<List<BookComment>, DataError.Sync>

    suspend fun addComment(
        clubCode: String,
        bookId: String,
        text: String
    ): Result<Unit, DataError.Sync>

    suspend fun editComment(
        clubCode: String,
        bookId: String,
        commentId: String,
        newText: String
    ): Result<Unit, DataError.Sync>

    suspend fun deleteComment(
        clubCode: String,
        bookId: String,
        commentId: String
    ): Result<Unit, DataError.Sync>
}
