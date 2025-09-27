package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ShareData
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.flatMap
import uk.co.zlurgg.mybookshelf.core.domain.result.map

/**
 * Use case for exporting a bookshelf and generating a shareable token.
 * Orchestrates the export workflow using pure Result chains.
 */
class ExportBookshelfUseCase(
    private val dataOrchestrator: BookshelfDataOrchestrator,
    private val serializer: BookshelfSerializer,
    private val shareTokenService: ShareTokenService
) {

    suspend fun execute(shelfId: String): Result<ShareData, DataError.Local> {
        return dataOrchestrator.loadShelfForExport(shelfId)
            .flatMap { shelf ->
                serializer.serialize(shelf).map { jsonString -> shelf to jsonString }
            }
            .flatMap { (shelf, jsonString) ->
                shareTokenService.generateToken(jsonString)
                    .map { token -> ShareData(token, shelf.name) }
            }
    }
}