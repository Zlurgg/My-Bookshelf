package uk.co.zlurgg.mybookshelf.bookshelf.presenation.util

import androidx.compose.runtime.mutableStateListOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.BookShelf
import java.util.UUID

val bookshelves =  {
    mutableStateListOf(
        BookShelf(id = UUID.randomUUID().toString(), name = "Favorites", bookCount = 12),
        BookShelf(id = UUID.randomUUID().toString(), name = "Sci-Fi", bookCount = 5),
        BookShelf(id = UUID.randomUUID().toString(), name = "To Read", bookCount = 8)
    )
}

val bookshelf = BookShelf(id = UUID.randomUUID().toString(), name = "Favorites", bookCount = 12)
