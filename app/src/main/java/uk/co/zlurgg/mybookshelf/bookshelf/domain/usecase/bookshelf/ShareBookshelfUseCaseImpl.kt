package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

class ShareBookshelfUseCaseImpl(
    private val bookshelfExportService: BookshelfExportService
) : ShareBookshelfUseCase {

    override suspend fun execute(shelfId: String): Result<Unit, DataError.Local> {
        return bookshelfExportService.shareBookshelf(shelfId)
    }
}