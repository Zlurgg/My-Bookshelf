package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class RemoveBookFromClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : RemoveBookFromClubUseCase {
    override suspend fun invoke(code: String, bookId: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.removeBookFromClub(code, bookId)
    }
}
