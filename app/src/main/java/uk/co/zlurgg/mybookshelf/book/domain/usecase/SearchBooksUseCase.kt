package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

data class SearchResult(
    val books: List<Book>,
    val filteredCount: Int
)

/**
 * UseCase interface for searching books.
 *
 * Backed by Google Books as the primary provider with an OpenLibrary fallback
 * (see [uk.co.zlurgg.mybookshelf.book.data.network.FallbackRemoteBookDataSource]).
 * Results are sorted by the active provider's default relevance algorithm.
 */
interface SearchBooksUseCase {
    suspend operator fun invoke(
        query: String,
        resultLimit: Int? = null,
        language: String? = null,
        authorFilter: String? = null,
        titleFilter: String? = null,
        subjectFilter: String? = null,
        safeSearchEnabled: Boolean = true
    ): Result<SearchResult, DataError.Remote>
}
