package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for importing a bookshelf from JSON data.
 */
interface ImportBookshelfUseCase {
    suspend operator fun invoke(jsonData: String, customName: String? = null): Result<Unit, DataError.Local>
}
