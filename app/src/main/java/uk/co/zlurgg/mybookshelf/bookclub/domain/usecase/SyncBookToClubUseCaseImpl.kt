package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class SyncBookToClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : SyncBookToClubUseCase {
    override suspend fun invoke(code: String, book: Book): Result<Unit, DataError.Sync> {
        return bookClubRepository.syncBookToClub(code, book)
    }
}
