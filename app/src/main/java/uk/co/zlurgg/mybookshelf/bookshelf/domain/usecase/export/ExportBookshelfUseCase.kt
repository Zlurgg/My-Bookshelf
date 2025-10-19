package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ShareData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for exporting a bookshelf and generating a shareable token.
 */
interface ExportBookshelfUseCase {
    suspend fun execute(shelfId: String): Result<ShareData, DataError.Local>
}
