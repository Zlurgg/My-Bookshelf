package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface SyncBookToClubUseCase {
    suspend operator fun invoke(code: String, book: Book): Result<Unit, DataError.Sync>
}
