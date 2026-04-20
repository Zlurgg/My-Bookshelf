package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Database implementation of BookshelfDataOrchestrator.
 * Coordinates repository operations with proper Result<T, E> propagation.
 */
class DatabaseBookshelfDataOrchestrator(
    private val bookcaseRepository: BookcaseRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val bookRepository: BookRepository
) : BookshelfDataOrchestrator {

    companion object {
        private const val TAG = "BookshelfDataOrchestrator"
    }

    override suspend fun loadShelfForExport(shelfId: String): Result<Bookshelf, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val allShelves = bookcaseRepository.getAllShelves().first()
            val shelf = allShelves.find { it.id == shelfId }
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            val books = bookshelfRepository.getBooksForShelf(shelfId).first()
            shelf.copy(books = books)
        }
    }

    override suspend fun importShelfToDatabase(shelf: Bookshelf): Result<Unit, DataError.Local> {
        // Add all books to the repository first
        for (book in shelf.books) {
            when (val upsertResult = bookRepository.upsertBook(book)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return upsertResult
            }
        }

        // Then add the shelf
        when (val addShelfResult = bookcaseRepository.addShelf(shelf)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return addShelfResult
        }

        // Link each book to the imported shelf
        for (book in shelf.books) {
            when (val linkResult = bookshelfRepository.addBookToShelf(shelf.id, book.id)) {
                is Result.Success -> { /* continue */ }
                is Result.Error -> return linkResult
            }
        }

        return Result.Success(Unit)
    }
}
