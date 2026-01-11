package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ShareBookshelfUseCaseImpl(
    private val bookshelfExportService: BookshelfExportService
) : ShareBookshelfUseCase {

    override suspend operator fun invoke(shelfId: String): Result<Unit, DataError.Local> {
        return bookshelfExportService.shareBookshelf(shelfId)
    }
}
