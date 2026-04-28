package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase

import uk.co.zlurgg.mybookshelf.sharing.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

// Depends on sharing/ via interface — bookshelf triggers export but sharing owns the logic.
// This cross-feature dependency is intentional and injected via Koin.
class ShareBookshelfUseCaseImpl(
    private val bookshelfExportService: BookshelfExportService
) : ShareBookshelfUseCase {

    override suspend operator fun invoke(shelfId: String): Result<Unit, DataError.Local> {
        return bookshelfExportService.shareBookshelf(shelfId)
    }
}
