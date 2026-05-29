package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Local-source counterpart to [SearchBooksUseCase] — filters the user's
 * existing personal-library books by query.
 *
 * Used by shelf-style search dialogs when the "My library only" toggle is on
 * so the user can re-shelve books they already own without re-querying Google
 * Books. Empty query returns the full library.
 *
 * `getAllPersonalBooks()` already excludes the tutorial book and book-club
 * shelves, so results are naturally limited to the user's curated personal
 * collection.
 */
interface SearchLibraryBooksUseCase {
    suspend operator fun invoke(
        query: String,
        searchByTitle: Boolean,
        searchByAuthor: Boolean,
    ): Result<List<Book>, DataError.Local>
}
