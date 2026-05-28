package uk.co.zlurgg.mybookshelf.book.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface BookRepository {
    // Book CRUD operations
    /**
     * Reads a book strictly from local storage. Returns `Result.Success(null)`
     * when no row exists for [bookId].
     *
     * Does NOT fall back to the in-memory preview cache — callers that legitimately
     * want a previewed book must compose explicitly with [peekPreview]. Routing
     * the cache through here previously promoted previewed books into the library
     * any time an update use case did `getBookById` → modify → `upsertBook`.
     */
    suspend fun getBookById(bookId: String): Result<Book?, DataError.Local>
    suspend fun upsertBook(book: Book): Result<Unit, DataError.Local>

    // Book metadata operations
    suspend fun getBookDescription(bookId: String, provider: BookProvider): Result<String?, DataError.Remote>

    /**
     * Persists a fetched book description via a targeted column UPDATE.
     *
     * Does NOT use `upsertBook` — that would write the entire row and clobber
     * any concurrent personal-metadata writes (notes/rating/status) the user
     * may have queued while the network fetch was in flight.
     */
    suspend fun updateDescription(bookId: String, description: String?): Result<Unit, DataError.Local>

    /**
     * Targeted column UPDATE for personal metadata (reading status, rating, notes).
     * Null parameters mean "leave this column alone." All-or-none atomicity is
     * preserved at the DAO via a `@Transaction` orchestrator.
     *
     * UPDATE on a missing row is a SQLite no-op — previewed books (cache-only)
     * never get promoted into storage.
     */
    suspend fun updatePersonalMetadata(
        bookId: String,
        readingStatus: String? = null,
        personalRating: Float? = null,
        personalNotes: String? = null,
    ): Result<Unit, DataError.Local>

    /**
     * Targeted column UPDATE for the purchased flag. No-op on a missing row.
     */
    suspend fun updatePurchased(bookId: String, purchased: Boolean): Result<Unit, DataError.Local>

    /**
     * Upserts a system book (e.g., tutorial book) with SystemOwnerIds.TUTORIAL as owner.
     * System books are visible to all users and not synced to cloud.
     */
    suspend fun upsertSystemBook(book: Book): Result<Unit, DataError.Local>

    /**
     * Caches search-result books in memory so a subsequent [peekPreview] can
     * return them without a DB write. Used by the search → detail-screen path
     * to avoid polluting the local DB (and therefore the Library view) with
     * books the user only previewed.
     *
     * The cache is process-scoped and lost on process death. Detail screens
     * opened after a process restart fall back to the empty-state behaviour.
     */
    fun cacheSearchPreviews(books: List<Book>)

    /**
     * In-memory lookup against the preview cache. Intentionally deviates from
     * the repository's all-`Result` convention: the lookup cannot fail (no I/O,
     * no parsing), so a `Result` wrapper would add cost without information.
     *
     * The single legitimate consumer is the detail-screen render path, which
     * composes `getBookById ?: peekPreview` explicitly so the cache fallback is
     * visible at the call site.
     */
    fun peekPreview(bookId: String): Book?

    // Library
    fun getAllPersonalBooks(): Flow<List<Book>>

    // Deletion
    suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local>
    fun getNonRemovableBookIds(): Flow<Set<String>>
}
