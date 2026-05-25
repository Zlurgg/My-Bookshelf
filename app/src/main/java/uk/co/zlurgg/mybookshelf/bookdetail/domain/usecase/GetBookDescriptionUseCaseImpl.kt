package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Pure delegation to [BookRepository.getBookDescription]. The repository already
 * returns [Result] with its own error handling — no exception wrapping needed.
 */
class GetBookDescriptionUseCaseImpl(
    private val bookRepository: BookRepository,
) : GetBookDescriptionUseCase {
    override suspend operator fun invoke(
        bookId: String,
        provider: BookProvider,
    ): Result<String?, DataError.Remote> {
        return bookRepository.getBookDescription(bookId, provider)
    }
}
