package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookSorter
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.map

/**
 * UseCase responsible for searching books with proper business logic separation.
 * Combines network data retrieval with domain-specific sorting algorithms.
 */
class SearchBooksUseCase(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val bookSorter: BookSorter
) {
    suspend fun execute(
        query: String,
        sortBy: BookSearchSort,
        language: String? = null,
        authorFilter: String? = null,
        titleFilter: String? = null
    ): Result<List<Book>, DataError.Remote> {
        // Determine server-side sort parameter
        val serverSort = if (sortBy.useServerSide) sortBy.serverSortParam else null

        return remoteBookDataSource.searchBooks(
            query = query,
            language = language,
            authorFilter = authorFilter,
            titleFilter = titleFilter,
            sort = serverSort
        ).map { dto ->
            val books = dto.results.map { it.toBook() }
            // Apply client-side sorting only if not handled server-side
            if (sortBy.isClientSide) {
                bookSorter.sortBooks(books, sortBy, query)
            } else {
                books // Server already sorted
            }
        }
    }
}