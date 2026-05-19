package uk.co.zlurgg.mybookshelf.library.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface DeleteBooksFromLibraryUseCase {
    suspend operator fun invoke(bookIds: List<String>): Result<Unit, DataError.Local>
}
