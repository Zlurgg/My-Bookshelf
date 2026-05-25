package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for persisting a fetched book description.
 *
 * Delegates to [uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository.updateDescription],
 * which issues a column-targeted UPDATE — intentionally NOT an upsert, so concurrent
 * personal-metadata writes (notes/rating/status) are not clobbered.
 */
interface UpdateBookDescriptionUseCase {
    suspend operator fun invoke(bookId: String, description: String?): Result<Unit, DataError.Local>
}
