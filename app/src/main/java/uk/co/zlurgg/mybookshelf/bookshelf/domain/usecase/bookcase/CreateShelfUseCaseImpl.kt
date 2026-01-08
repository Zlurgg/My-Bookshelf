package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class CreateShelfUseCaseImpl(
    private val repository: BookcaseRepository,
    private val idGenerator: IdGenerator,
    private val getOrCreateTutorialBook: GetOrCreateTutorialBookUseCase,
    private val syncSchedulerService: SyncSchedulerService,
) : CreateShelfUseCase {
    override suspend fun execute(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>,
    ): Result<Bookshelf, DataError.Local> {
        return ErrorMapper.safeCall {
            val nextPosition = existingShelves.maxOfOrNull { it.position }?.plus(1) ?: 0
            val newShelf =
                Bookshelf(
                    id = idGenerator.generateId(),
                    name = name,
                    books = emptyList(),
                    shelfStyle = style,
                    position = nextPosition,
                )

            repository.addShelf(newShelf)

            // If this is the tutorial shelf, ensure the tutorial book is added
            if (name == BookshelfConstants.TUTORIAL_SHELF_NAME) {
                getOrCreateTutorialBook.execute(newShelf.id)
            }

            // Trigger sync after successful shelf creation
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: CreateShelf")
            syncSchedulerService.triggerImmediateSync()

            newShelf
        }
    }
}
