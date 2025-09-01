package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.map

class BookshelfRepositoryImpl(
    val remoteBookDataSource: RemoteBookDataSource,
) : BookshelfRepository {
    // Temporary in-memory storage
    private val booksByShelf = mutableMapOf<String, MutableList<Book>>()

//    override suspend fun searchBooks(query: String): List<Book> {
//
//        val tempTestBooks = sampleBooks
//        // Implement actual search logic
//        return tempTestBooks
//    }

    override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
        return remoteBookDataSource
            .searchBooks(query)
            .map { dto ->
                dto.results.map { it.toBook() }
            }
    }

    override suspend fun addBookToShelf(shelfId: String, book: Book) {
        booksByShelf.getOrPut(shelfId) { mutableListOf() }.add(book)
    }

    override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
        booksByShelf[shelfId]?.removeIf { it.id == bookId }
    }

    override fun getBooksForShelf(shelfId: String): Flow<List<Book>> {
        return flowOf(booksByShelf[shelfId] ?: emptyList())
    }
}