package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial

data class BookshelfState(
    val shelfId: String,
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val isSearchDialogVisible: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Book> = emptyList(),
    val isSearchLoading: Boolean = false,
    val recentlyDeleted: Book? = null,
    val errorMessage: String? = null,
    val shelfMaterial: ShelfMaterial = ShelfMaterial.DarkWood
)