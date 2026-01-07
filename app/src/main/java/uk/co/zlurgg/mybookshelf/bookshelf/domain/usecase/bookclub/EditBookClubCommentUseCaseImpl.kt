package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of EditBookClubCommentUseCase.
 *
 * Validates that the new text is not empty before updating.
 * Ownership validation is enforced by Firestore security rules.
 */
class EditBookClubCommentUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : EditBookClubCommentUseCase {

    override suspend fun invoke(
        clubCode: String,
        bookId: String,
        commentId: String,
        newText: String
    ): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Editing comment %s for book %s in club %s", commentId, bookId, clubCode)

        // Validate non-empty text
        val trimmedText = newText.trim()
        if (trimmedText.isEmpty()) {
            Timber.tag(TAG).w("Comment text is empty")
            return Result.Error(DataError.Sync.INVALID_INPUT)
        }

        // Validate max length
        if (trimmedText.length > MAX_COMMENT_LENGTH) {
            Timber.tag(TAG).w("Comment text exceeds max length: %d", trimmedText.length)
            return Result.Error(DataError.Sync.INVALID_INPUT)
        }

        return when (val result = bookClubRepository.editBookComment(clubCode, bookId, commentId, trimmedText)) {
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

    companion object {
        private const val TAG = "EditClubComment"
        private const val MAX_COMMENT_LENGTH = 1000
    }
}
