package uk.co.zlurgg.mybookshelf.bookclub.presentation.handlers

import uk.co.zlurgg.mybookshelf.book.domain.model.BookComment
import uk.co.zlurgg.mybookshelf.book.domain.model.BookReview
import uk.co.zlurgg.mybookshelf.book.domain.service.BookReviewProvider
import uk.co.zlurgg.mybookshelf.bookclub.domain.usecase.BookClubReviewUseCases
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of BookReviewProvider that delegates to book club review use cases.
 * Maps BookClubReview/BookClubComment to slim BookReview/BookComment at the boundary.
 */
class BookReviewProviderImpl(
    private val reviewUseCases: BookClubReviewUseCases
) : BookReviewProvider {

    override suspend fun getReviews(
        clubCode: String,
        bookId: String
    ): Result<List<BookReview>, DataError.Sync> {
        return when (val result = reviewUseCases.getBookClubReviews(clubCode, bookId)) {
            is Result.Success -> Result.Success(
                result.data.map { review ->
                    BookReview(
                        id = review.id,
                        bookId = review.bookId,
                        userId = review.userId,
                        displayName = review.displayName,
                        rating = review.rating,
                        reviewText = review.reviewText,
                        createdAt = review.createdAt,
                        updatedAt = review.updatedAt
                    )
                }
            )
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun upsertReview(
        clubCode: String,
        bookId: String,
        rating: Float,
        reviewText: String
    ): Result<Unit, DataError.Sync> {
        return reviewUseCases.upsertBookClubReview(clubCode, bookId, rating, reviewText)
    }

    override suspend fun deleteReview(
        clubCode: String,
        bookId: String
    ): Result<Unit, DataError.Sync> {
        return reviewUseCases.deleteBookClubReview(clubCode, bookId)
    }

    override suspend fun getComments(
        clubCode: String,
        bookId: String
    ): Result<List<BookComment>, DataError.Sync> {
        return when (val result = reviewUseCases.getBookClubComments(clubCode, bookId)) {
            is Result.Success -> Result.Success(
                result.data.map { comment ->
                    BookComment(
                        id = comment.id,
                        bookId = comment.bookId,
                        userId = comment.userId,
                        displayName = comment.displayName,
                        text = comment.text,
                        createdAt = comment.createdAt,
                        updatedAt = comment.updatedAt
                    )
                }
            )
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun addComment(
        clubCode: String,
        bookId: String,
        text: String
    ): Result<Unit, DataError.Sync> {
        return when (val result = reviewUseCases.addBookClubComment(clubCode, bookId, text)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun editComment(
        clubCode: String,
        bookId: String,
        commentId: String,
        newText: String
    ): Result<Unit, DataError.Sync> {
        return reviewUseCases.editBookClubComment(clubCode, bookId, commentId, newText)
    }

    override suspend fun deleteComment(
        clubCode: String,
        bookId: String,
        commentId: String
    ): Result<Unit, DataError.Sync> {
        return reviewUseCases.deleteBookClubComment(clubCode, bookId, commentId)
    }
}
