package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator

class DuplicateShelfUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val bookshelfRepository: BookshelfRepository,
    private val idGenerator: IdGenerator
) : DuplicateShelfUseCase {

    override suspend fun execute(shelfId: String): Result<Bookshelf, DataError.Local> {
        return ErrorMapper.safeCall {
            // Get the original shelf
            val originalShelf = bookcaseRepository.getShelfById(shelfId)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            // Get all books from the original shelf
            val books = bookshelfRepository.getBooksForShelf(shelfId).first()

            // Create duplicated shelf with new ID and name
            val duplicatedShelf = originalShelf.copy(
                id = idGenerator.generateId(),
                name = "Copy of ${originalShelf.name}",
                books = books,
                position = Int.MAX_VALUE // Will be positioned at the end
            )

            // Add the duplicated shelf
            bookcaseRepository.addShelf(duplicatedShelf)

            // Add all books to the duplicated shelf
            books.forEach { book ->
                bookshelfRepository.addBookToShelf(duplicatedShelf.id, book.id)
            }

            duplicatedShelf
        }
    }
}
