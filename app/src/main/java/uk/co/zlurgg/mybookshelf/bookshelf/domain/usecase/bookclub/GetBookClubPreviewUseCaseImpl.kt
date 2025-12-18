package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of GetBookClubPreviewUseCase.
 *
 * Delegates to BookClubRepository to fetch club metadata from Firestore.
 */
class GetBookClubPreviewUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : GetBookClubPreviewUseCase {

    override suspend fun invoke(code: String): Result<BookClub?, DataError.Sync> {
        return bookClubRepository.getBookClub(code)
    }
}
