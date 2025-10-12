package uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.ReadingStatus

data class BookDetailState(
    val isLoading: Boolean = true,
    val rating: Int? = null,
    val isPurchased: Boolean = false,
    val book: Book? = null,
    val onShelf: Boolean = false,
    val errorMessage: String? = null,

    // Personal metadata (NOT exported for privacy)
    val readingStatus: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val personalRating: Float? = null,
    val personalNotes: String? = null
)
