package uk.co.zlurgg.mybookshelf.bookshelf.presenation.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf

data class BookcaseState(
    val bookshelves: List<Bookshelf> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val recentlyDeleted: Bookshelf? = null,
    val operationSuccess: Boolean = false
)