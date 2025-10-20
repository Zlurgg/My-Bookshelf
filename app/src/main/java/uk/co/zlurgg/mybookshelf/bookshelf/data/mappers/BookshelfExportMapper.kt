package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.export.BookshelfExportData
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator

/**
 * Mapper for converting between Bookshelf domain models and BookshelfExportData.
 * Handles minimal export data creation for URL sharing.
 * For import, fetches full book details from the API using work IDs.
 */
class BookshelfExportMapper(
    private val idGenerator: IdGenerator,
    private val remoteBookDataSource: RemoteBookDataSource
) {

    fun toExportData(shelf: Bookshelf): BookshelfExportData {
        return BookshelfExportData(
            bookshelf = BookshelfMapper.toExportedBookshelf(shelf)
        )
    }

    suspend fun fromExportData(
        exportData: BookshelfExportData,
        customName: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Bookshelf, DataError> {
        val exportedShelf = exportData.bookshelf
        val finalName = customName ?: exportedShelf.name
        val bookIds = exportedShelf.bookIds

        // Fetch all books from API using search endpoint
        val books = mutableListOf<Book>()
        bookIds.forEachIndexed { index, bookId ->
            onProgress(index + 1, bookIds.size)

            // Use search API with work key to find the book
            // Format: "key:/works/OL123W"
            val searchQuery = "key:/works/${bookId.workId}"
            when (val searchResult = remoteBookDataSource.searchBooks(
                query = searchQuery,
                resultLimit = 1
            )) {
                is Result.Success -> {
                    val foundBook = searchResult.data.results.firstOrNull()
                    if (foundBook != null) {
                        books.add(foundBook.toBook())
                    } else {
                        // Book not found, fail import
                        return Result.Error(DataError.Remote.NOT_FOUND)
                    }
                }
                is Result.Error -> {
                    // If one book fails to fetch, fail the entire import
                    return Result.Error(searchResult.error)
                }
            }
        }

        return Result.Success(
            Bookshelf(
                id = generateNewId(),
                name = finalName,
                books = books,
                shelfStyle = exportedShelf.shelfStyle
            )
        )
    }

    private fun generateNewId(): String {
        return idGenerator.generateId()
    }
}