package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * UseCase interface for searching books with proper business logic separation.
 * Abstracts the complexity of combining network data retrieval with domain-specific sorting algorithms.
 */
interface SearchBooksUseCase {
    suspend fun execute(
        query: String,
        sortBy: BookSearchSort = BookSearchSort.BEST_MATCH,
        language: String? = null,
        authorFilter: String? = null,
        titleFilter: String? = null
    ): Result<List<Book>, DataError.Remote>
}