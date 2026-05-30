package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository

/**
 * Replaces the preview cache with [books] so a subsequent detail-screen lookup
 * via `peekPreview` can render a tapped row without a DB write.
 *
 * Owned by the VM under pagination: callers must pass the accumulated list, not
 * a per-page batch — `BookRepository.cacheSearchPreviews` clears the cache
 * before writing, so passing only the page-2 books would invalidate page-1
 * entries for the back-from-detail round trip.
 */
interface CacheSearchPreviewsUseCase {
    operator fun invoke(books: List<Book>)
}

class CacheSearchPreviewsUseCaseImpl(
    private val bookRepository: BookRepository,
) : CacheSearchPreviewsUseCase {
    override fun invoke(books: List<Book>) {
        bookRepository.cacheSearchPreviews(books)
    }
}
