package uk.co.zlurgg.mybookshelf.book.domain.usecase

import kotlinx.coroutines.flow.first
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * In-memory filter over [BookRepository.getAllPersonalBooks]. Reads the
 * current Flow snapshot via `.first()` and filters case-insensitively. SQL-
 * level filtering is unnecessary at typical library sizes (tens to low
 * hundreds of books); revisit with a DAO `LIKE` query if libraries grow
 * to thousands.
 *
 * Filter semantics: when both `searchByTitle` and `searchByAuthor` are set,
 * a book matches if **either** field contains the query (OR). This differs
 * slightly from the remote search's "both checked → general q=" shape, but
 * matches the natural user expectation when filtering a local list.
 */
class SearchLibraryBooksUseCaseImpl(
    private val bookRepository: BookRepository,
) : SearchLibraryBooksUseCase {
    override suspend operator fun invoke(
        query: String,
        searchByTitle: Boolean,
        searchByAuthor: Boolean,
    ): Result<List<Book>, DataError.Local> {
        val books = bookRepository.getAllPersonalBooks().first()
        val trimmedQuery = query.trim()
        // Defensive fallback: persisted preferences may legitimately leave
        // both title and author unchecked (e.g. user previously had Subject
        // only). Library scope hides Subject, so we'd silently match nothing
        // — default to title-only in that corner.
        val effectiveByTitle = searchByTitle || (!searchByTitle && !searchByAuthor)
        val filtered = if (trimmedQuery.isEmpty()) {
            books
        } else {
            val q = trimmedQuery.lowercase()
            books.filter { book ->
                (effectiveByTitle && book.title.lowercase().contains(q)) ||
                    (searchByAuthor && book.authors.any { it.lowercase().contains(q) })
            }
        }
        return Result.Success(filtered)
    }
}
