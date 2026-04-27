package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Deletes a comment from a book in a book club.
 *
 * Only the comment author can delete their comment.
 * Firestore security rules enforce ownership validation.
 */
interface DeleteBookClubCommentUseCase {
    suspend operator fun invoke(
        clubCode: String,
        bookId: String,
        commentId: String
    ): Result<Unit, DataError.Sync>
}
