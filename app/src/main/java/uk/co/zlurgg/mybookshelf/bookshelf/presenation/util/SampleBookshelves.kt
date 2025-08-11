package uk.co.zlurgg.mybookshelf.bookshelf.presenation.util

import androidx.compose.runtime.mutableStateListOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf.Bookshelf
import java.util.UUID

val bookshelves =
    mutableStateListOf(
        Bookshelf(id = UUID.randomUUID().toString(), name = "Favorites", bookCount = 12),
        Bookshelf(id = UUID.randomUUID().toString(), name = "Sci-Fi", bookCount = 5),
        Bookshelf(id = UUID.randomUUID().toString(), name = "To Read", bookCount = 8)
    )


val bookshelf = Bookshelf(id = UUID.randomUUID().toString(), name = "Favorites", bookCount = 12)
