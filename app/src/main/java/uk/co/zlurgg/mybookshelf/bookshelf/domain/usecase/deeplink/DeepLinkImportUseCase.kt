package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink

import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * UseCase interface for handling deep link bookshelf imports.
 * Abstracts the complexity of token validation and bookshelf import process.
 */
interface DeepLinkImportUseCase {
    suspend fun importBookshelfFromToken(token: String): Result<ImportResult, DataError.Local>
    suspend fun importBookshelfWithCustomName(jsonData: String, customName: String): Result<Unit, DataError.Local>
}