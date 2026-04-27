package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class GetShelfByIdUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository
) : GetShelfByIdUseCase {

    override suspend operator fun invoke(shelfId: String): Result<Bookshelf?, DataError.Local> {
        return bookcaseRepository.getShelfById(shelfId)
    }
}
