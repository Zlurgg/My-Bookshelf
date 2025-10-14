package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase interface for searching books via OpenLibrary API.
 * Results are sorted by the API's default relevance algorithm.
 */
interface SearchBooksUseCase {
    suspend fun execute(
        query: String,
        resultLimit: Int? = null,
        language: String? = null,
        authorFilter: String? = null,
        titleFilter: String? = null
    ): Result<List<Book>, DataError.Remote>
}