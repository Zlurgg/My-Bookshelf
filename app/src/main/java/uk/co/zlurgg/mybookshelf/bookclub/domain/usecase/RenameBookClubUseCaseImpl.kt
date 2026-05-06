package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class RenameBookClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : RenameBookClubUseCase {
    override suspend fun invoke(code: String, newName: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.renameBookClub(code, newName)
    }
}
