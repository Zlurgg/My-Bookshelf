package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.sync.domain.SyncConstants
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

class DuplicateShelfUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val idGenerator: IdGenerator,
    private val syncSchedulerService: SyncSchedulerService
) : DuplicateShelfUseCase {

    override suspend fun execute(shelfId: String): Result<Bookshelf, DataError.Local> {
        return ErrorMapper.safeCall {
            // Get the original shelf
            val originalShelf = bookcaseRepository.getShelfById(shelfId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            // Get all books from the original shelf
            val books = bookshelfRepository.getBooksForShelf(shelfId).first()

            // Get existing shelf names to ensure uniqueness
            val allShelves = bookcaseRepository.getAllShelves().first()
            val existingNames = allShelves
                .filter { shelf -> !shelf.isBookClub }
                .map { shelf -> shelf.name.lowercase() }
                .toSet()

            // Generate unique name
            val baseName = if (originalShelf.isBookClub) originalShelf.name else "Copy of ${originalShelf.name}"
            val uniqueName = generateUniqueName(baseName, existingNames)

            // Create duplicated shelf with new ID and name
            // Always create as personal shelf (reset book club properties)
            val duplicatedShelf = originalShelf.copy(
                id = idGenerator.generateId(),
                name = uniqueName,
                books = books,
                position = Int.MAX_VALUE, // Will be positioned at the end
                isBookClub = false,
                clubCode = null,
                clubCreatorId = null
            )

            // Add the duplicated shelf
            bookcaseRepository.addShelf(duplicatedShelf)

            // Add all books to the duplicated shelf
            books.forEach { book ->
                bookshelfRepository.addBookToShelf(duplicatedShelf.id, book.id)
            }

            // Trigger sync to upload the new personal shelf to Firebase
            Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: DuplicateShelf")
            syncSchedulerService.triggerImmediateSync()

            duplicatedShelf
        }
    }

    /**
     * Generates a unique shelf name by appending a number suffix if the base name already exists.
     * Example: "My Shelf" -> "My Shelf (2)" -> "My Shelf (3)" etc.
     */
    private fun generateUniqueName(baseName: String, existingNames: Set<String>): String {
        val baseNameLower = baseName.lowercase()
        if (baseNameLower !in existingNames) {
            return baseName
        }

        var counter = 2
        while (true) {
            val candidateName = "$baseName ($counter)"
            if (candidateName.lowercase() !in existingNames) {
                return candidateName
            }
            counter++
        }
    }
}
