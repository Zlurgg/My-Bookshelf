package uk.co.zlurgg.mybookshelf.book.data.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class FallbackRemoteBookDataSource(
    private val primary: GoogleBooksRemoteBookDataSource,
    private val fallback: OpenLibraryRemoteBookDataSource,
) : RemoteBookDataSource {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?
    ): Result<BookSearchResponse, DataError.Remote> {
        val result = primary.searchBooks(
            query = query,
            resultLimit = resultLimit,
            language = language,
            authorFilter = authorFilter,
            titleFilter = titleFilter,
            subjectFilter = subjectFilter,
            sort = sort,
        )

        return when {
            result is Result.Error && shouldFallback(result.error) -> {
                Timber.tag(TAG).w(
                    "Google Books unavailable (%s), falling back to OpenLibrary",
                    result.error
                )
                fallback.searchBooks(
                    query = query,
                    resultLimit = resultLimit,
                    language = language,
                    authorFilter = authorFilter,
                    titleFilter = titleFilter,
                    subjectFilter = subjectFilter,
                    sort = sort,
                )
            }
            else -> result
        }
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        return when (provider) {
            BookProvider.GOOGLE_BOOKS -> primary.getBookDescription(bookId, provider)
            BookProvider.OPEN_LIBRARY -> fallback.getBookDescription(bookId, provider)
        }
    }

    private fun shouldFallback(error: DataError.Remote): Boolean {
        return error == DataError.Remote.TOO_MANY_REQUESTS ||
            error == DataError.Remote.FORBIDDEN
    }

    companion object {
        private const val TAG = "BookSearchFallback"
    }
}
