package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.AuthService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubReview
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubReviewRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubCommentDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubReviewDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.RemoteSyncDataSource

internal class BookClubReviewRepositoryImpl(
    private val remoteDataSource: RemoteSyncDataSource,
    private val authService: AuthService,
    private val timeProvider: TimeProvider,
) : BookClubReviewRepository {

    override suspend fun getBookReviews(
        code: String,
        bookId: String,
    ): Result<List<BookClubReview>, DataError.Sync> {
        authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Getting reviews for book %s in club %s", bookId, code)

        return when (val result = remoteDataSource.getBookReviews(code, bookId)) {
            is Result.Success -> {
                val reviews = result.data.map { dto ->
                    BookClubReview(
                        id = dto.id,
                        bookId = dto.bookId,
                        userId = dto.userId,
                        displayName = dto.displayName,
                        rating = dto.rating,
                        reviewText = dto.reviewText,
                        createdAt = dto.createdAt?.time ?: 0L,
                        updatedAt = dto.updatedAt?.time ?: 0L
                    )
                }
                Timber.tag(TAG).d("Got %d reviews for book %s", reviews.size, bookId)
                Result.Success(reviews)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get reviews: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun upsertBookReview(
        code: String,
        bookId: String,
        rating: Float,
        reviewText: String,
    ): Result<Unit, DataError.Sync> {
        val user = authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Upserting review for book %s in club %s", bookId, code)

        val now = java.util.Date(timeProvider.currentTimeMillis())
        val reviewDto = BookClubReviewDto(
            id = user.userId,
            bookId = bookId,
            userId = user.userId,
            displayName = user.username ?: "Anonymous",
            rating = rating,
            reviewText = reviewText,
            createdAt = now,
            updatedAt = now
        )

        return when (val result = remoteDataSource.upsertBookReview(code, bookId, reviewDto)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Review upserted successfully")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to upsert review: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun deleteBookReview(
        code: String,
        bookId: String,
    ): Result<Unit, DataError.Sync> {
        val user = authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Deleting review for book %s in club %s", bookId, code)

        return when (val result = remoteDataSource.deleteBookReview(code, bookId, user.userId)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Review deleted successfully")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to delete review: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun getBookComments(
        code: String,
        bookId: String,
    ): Result<List<BookClubComment>, DataError.Sync> {
        authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Getting comments for book %s in club %s", bookId, code)

        return when (val result = remoteDataSource.getBookComments(code, bookId)) {
            is Result.Success -> {
                val comments = result.data.map { it.toDomain() }
                Timber.tag(TAG).d("Got %d comments for book %s", comments.size, bookId)
                Result.Success(comments)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get comments: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun addBookComment(
        code: String,
        bookId: String,
        text: String,
    ): Result<String, DataError.Sync> {
        val user = authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Adding comment for book %s in club %s", bookId, code)

        val commentDto = BookClubCommentDto(
            id = "",
            bookId = bookId,
            userId = user.userId,
            displayName = user.username ?: "Anonymous",
            text = text,
            createdAt = java.util.Date(timeProvider.currentTimeMillis()),
            updatedAt = java.util.Date(timeProvider.currentTimeMillis())
        )

        return when (val result = remoteDataSource.addBookComment(code, bookId, commentDto)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Comment added successfully with ID: %s", result.data)
                Result.Success(result.data)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to add comment: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun editBookComment(
        code: String,
        bookId: String,
        commentId: String,
        newText: String,
    ): Result<Unit, DataError.Sync> {
        authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Editing comment %s for book %s in club %s", commentId, bookId, code)

        return when (val result = remoteDataSource.editBookComment(code, bookId, commentId, newText)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Comment edited successfully")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to edit comment: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    override suspend fun deleteBookComment(
        code: String,
        bookId: String,
        commentId: String,
    ): Result<Unit, DataError.Sync> {
        authService.getSignedInUser()
            ?: return Result.Error(DataError.Sync.NOT_SIGNED_IN)

        Timber.tag(TAG).d("Deleting comment %s for book %s in club %s", commentId, bookId, code)

        return when (val result = remoteDataSource.deleteBookComment(code, bookId, commentId)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Comment deleted successfully")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to delete comment: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    companion object {
        private const val TAG = "BookClubReviewRepo"
    }
}
