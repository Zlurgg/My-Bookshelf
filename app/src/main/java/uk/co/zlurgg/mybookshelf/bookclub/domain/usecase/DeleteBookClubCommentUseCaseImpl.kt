package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of DeleteBookClubCommentUseCase.
 *
 * Ownership validation is enforced by Firestore security rules.
 */
class DeleteBookClubCommentUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : DeleteBookClubCommentUseCase {

    override suspend fun invoke(
        clubCode: String,
        bookId: String,
        commentId: String
    ): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Deleting comment %s for book %s in club %s", commentId, bookId, clubCode)

        return when (val result = bookClubRepository.deleteBookComment(clubCode, bookId, commentId)) {
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
        private const val TAG = "DeleteClubComment"
    }
}
