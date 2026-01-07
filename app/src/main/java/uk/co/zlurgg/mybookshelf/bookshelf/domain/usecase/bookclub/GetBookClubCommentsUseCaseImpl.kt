package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of GetBookClubCommentsUseCase.
 */
class GetBookClubCommentsUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : GetBookClubCommentsUseCase {

    override suspend fun invoke(
        clubCode: String,
        bookId: String
    ): Result<List<BookClubComment>, DataError.Sync> {
        Timber.tag(TAG).d("Getting comments for book %s in club %s", bookId, clubCode)

        return when (val result = bookClubRepository.getBookComments(clubCode, bookId)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Found %d comments", result.data.size)
                Result.Success(result.data)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get comments: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    companion object {
        private const val TAG = "GetClubComments"
    }
}
