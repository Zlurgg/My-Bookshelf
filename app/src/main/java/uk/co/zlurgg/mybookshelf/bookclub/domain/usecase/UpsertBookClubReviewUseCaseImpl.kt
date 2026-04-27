package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of UpsertBookClubReviewUseCase.
 *
 * Validates input and delegates to repository.
 */
class UpsertBookClubReviewUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : UpsertBookClubReviewUseCase {

    override suspend fun invoke(
        clubCode: String,
        bookId: String,
        rating: Float,
        reviewText: String
    ): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Upserting review for book %s in club %s", bookId, clubCode)

        // Validate rating (0 = no rating, 1-5 = rated)
        if (rating < 0 || rating > MAX_RATING) {
            Timber.tag(TAG).e("Invalid rating: %f", rating)
            return Result.Error(DataError.Sync.INVALID_INPUT)
        }

        // Validate review text length
        if (reviewText.length > MAX_REVIEW_LENGTH) {
            Timber.tag(TAG).e("Review text too long: %d chars", reviewText.length)
            return Result.Error(DataError.Sync.INVALID_INPUT)
        }

        return when (
            val result = bookClubRepository.upsertBookReview(
                code = clubCode,
                bookId = bookId,
                rating = rating,
                reviewText = reviewText.trim()
            )
        ) {
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

    companion object {
        private const val TAG = "UpsertClubReview"
        private const val MAX_RATING = 5f
        private const val MAX_REVIEW_LENGTH = 2000
    }
}
