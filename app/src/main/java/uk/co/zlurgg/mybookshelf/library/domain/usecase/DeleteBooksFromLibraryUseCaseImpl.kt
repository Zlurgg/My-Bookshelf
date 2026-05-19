package uk.co.zlurgg.mybookshelf.library.domain.usecase

import kotlinx.coroutines.flow.first
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class DeleteBooksFromLibraryUseCaseImpl(
    private val bookRepository: BookRepository,
) : DeleteBooksFromLibraryUseCase {

    override suspend operator fun invoke(
        bookIds: List<String>,
    ): Result<Unit, DataError.Local> {
        val nonRemovable = bookRepository.getNonRemovableBookIds().first()
        val blocked = bookIds.filter { it in nonRemovable }
        if (blocked.isNotEmpty()) {
            Timber.tag(TAG).w("Blocked deletion of %d non-removable books", blocked.size)
            return Result.Error(DataError.Local.PROTECTED_RESOURCE)
        }
        return bookRepository.deleteBooks(bookIds)
    }

    companion object {
        private const val TAG = "DeleteBooksUC"
    }
}
