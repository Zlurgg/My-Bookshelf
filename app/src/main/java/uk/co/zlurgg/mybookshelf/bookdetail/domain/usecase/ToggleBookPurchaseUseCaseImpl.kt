package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map

class ToggleBookPurchaseUseCaseImpl(
    private val bookRepository: BookRepository,
) : ToggleBookPurchaseUseCase {

    override suspend operator fun invoke(book: Book, purchased: Boolean): Result<Book, DataError.Local> {
        return bookRepository.updatePurchased(book.id, purchased)
            .map { book.copy(purchased = purchased) }
    }
}
