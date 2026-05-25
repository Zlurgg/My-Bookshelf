package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Pure delegation to [BookRepository.updateDescription]. The repository already
 * returns [Result] with its own error handling — no exception wrapping needed.
 */
class UpdateBookDescriptionUseCaseImpl(
    private val bookRepository: BookRepository,
) : UpdateBookDescriptionUseCase {
    override suspend operator fun invoke(
        bookId: String,
        description: String?,
    ): Result<Unit, DataError.Local> {
        return bookRepository.updateDescription(bookId, description)
    }
}
