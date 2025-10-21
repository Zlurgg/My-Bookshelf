package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ShareData
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.flatMap
import uk.co.zlurgg.mybookshelf.core.domain.result.map

/**
 * Implementation of ExportBookshelfUseCase.
 * Orchestrates the export workflow using pure Result chains.
 */
class ExportBookshelfUseCaseImpl(
    private val dataOrchestrator: BookshelfDataOrchestrator,
    private val serializer: BookshelfSerializer,
    private val shareTokenService: ShareTokenService
) : ExportBookshelfUseCase {

    companion object {
        private const val TAG = "BookshelfExport"
    }

    override suspend fun execute(shelfId: String): Result<ShareData, DataError.Local> {
        Timber.tag(TAG).d("Starting bookshelf export for shelf: %s", shelfId)

        val result = dataOrchestrator.loadShelfForExport(shelfId)
            .flatMap { shelf ->
                Timber.tag(TAG).d("Loaded shelf '%s' with %d books, serializing...", shelf.name, shelf.books.size)
                serializer.serialize(shelf).map { jsonString -> shelf to jsonString }
            }
            .flatMap { (shelf, jsonString) ->
                Timber.tag(TAG).d("Serialization successful (%d chars), generating share token...", jsonString.length)
                shareTokenService.generateToken(jsonString)
                    .map { token ->
                        Timber.tag(TAG).d("Token generated successfully")
                        ShareData(token, shelf.name)
                    }
            }

        return when (result) {
            is Result.Success -> {
                Timber.tag(TAG).d("Export completed successfully for shelf: %s", result.data.shelfName)
                result
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Export failed for shelf %s: %s", shelfId, result.error)
                result
            }
        }
    }
}