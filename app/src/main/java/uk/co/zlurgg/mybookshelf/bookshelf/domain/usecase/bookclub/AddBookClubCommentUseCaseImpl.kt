package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of AddBookClubCommentUseCase.
 *
 * Validates that the comment text is not empty before adding.
 */
class AddBookClubCommentUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : AddBookClubCommentUseCase {

    override suspend fun invoke(
        clubCode: String,
        bookId: String,
        text: String
    ): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Adding comment for book %s in club %s", bookId, clubCode)

        // Validate non-empty text
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            Timber.tag(TAG).w("Comment text is empty")
            return Result.Error(DataError.Sync.INVALID_INPUT)
        }

        // Validate max length (1000 characters as per plan)
        if (trimmedText.length > MAX_COMMENT_LENGTH) {
            Timber.tag(TAG).w("Comment text exceeds max length: %d", trimmedText.length)
            return Result.Error(DataError.Sync.INVALID_INPUT)
        }

        return when (val result = bookClubRepository.addBookComment(clubCode, bookId, trimmedText)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Comment added with ID: %s", result.data)
                Result.Success(result.data)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to add comment: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    companion object {
        private const val TAG = "AddClubComment"
        private const val MAX_COMMENT_LENGTH = 1000
    }
}
