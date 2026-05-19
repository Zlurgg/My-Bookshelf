package uk.co.zlurgg.mybookshelf.bookshelf.presentation

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.presentation.searchcomponents.BookSearchState
import uk.co.zlurgg.mybookshelf.book.presentation.util.ShelfMaterial

data class BookshelfState(
    val shelfId: String,
    val shelfName: String = "",
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val isSearchDialogVisible: Boolean = false,
    val bookSearchState: BookSearchState = BookSearchState(),
    val recentlyDeleted: Book? = null,
    val errorMessage: String? = null,
    val shelfMaterial: ShelfMaterial = ShelfMaterial.DarkWood,
    val isTidyMode: Boolean = false,
    val isTutorialShelf: Boolean = false,

    // Book Club state (read-only for display)
    val isBookClub: Boolean = false,
    val clubCode: String? = null,

    // Auth state (for gating book club creation)
    val isSignedIn: Boolean = false
)
