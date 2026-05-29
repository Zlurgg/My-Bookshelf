package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map

class ToggleBookPurchaseUseCaseImpl(
    private val bookRepository: BookRepository,
) : ToggleBookPurchaseUseCase {

    override suspend operator fun invoke(
        bookId: String,
        purchased: Boolean
    ): Result<Unit, DataError.Local> {
        return bookRepository.updatePurchased(bookId, purchased).map { }
    }
}
