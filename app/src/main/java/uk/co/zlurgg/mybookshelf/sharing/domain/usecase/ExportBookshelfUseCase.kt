package uk.co.zlurgg.mybookshelf.sharing.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.ShareData
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for exporting a bookshelf and generating a shareable token.
 */
interface ExportBookshelfUseCase {
    suspend operator fun invoke(shelfId: String): Result<ShareData, DataError.Local>
}
