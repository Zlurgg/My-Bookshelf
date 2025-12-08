package uk.co.zlurgg.mybookshelf.sync.data.usecase

import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.sync.domain.model.GuestDataInfo
import uk.co.zlurgg.mybookshelf.sync.domain.usecase.HasGuestDataUseCase

/**
 * Implementation of HasGuestDataUseCase.
 *
 * Checks the database for orphan (guest) data that could be imported
 * to a user's account.
 */
class HasGuestDataUseCaseImpl(
    private val bookshelfDao: BookshelfDao
) : HasGuestDataUseCase {

    override suspend fun execute(): GuestDataInfo {
        val bookCount = bookshelfDao.countOrphanBooks()
        val shelfCount = bookshelfDao.countOrphanShelves()

        return GuestDataInfo(
            bookCount = bookCount,
            shelfCount = shelfCount
        )
    }
}
