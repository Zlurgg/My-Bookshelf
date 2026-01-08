package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Edits an existing comment's text.
 *
 * Only the comment author can edit their comment.
 * Firestore security rules enforce ownership validation.
 */
interface EditBookClubCommentUseCase {
    suspend operator fun invoke(
        clubCode: String,
        bookId: String,
        commentId: String,
        newText: String,
    ): Result<Unit, DataError.Sync>
}
