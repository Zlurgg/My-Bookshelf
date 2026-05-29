package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Column-scoped flip of the purchased flag for a single book row.
 *
 * Takes just the id and the new value — the ViewModel already has the full
 * Book in state, so the use case doesn't need to round-trip it. Returns
 * [Unit] on success; the caller updates its own state optimistically.
 */
interface ToggleBookPurchaseUseCase {
    suspend operator fun invoke(bookId: String, purchased: Boolean): Result<Unit, DataError.Local>
}
