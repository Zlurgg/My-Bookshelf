package uk.co.zlurgg.mybookshelf.core.presentation

import androidx.compose.runtime.mutableStateListOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle
import java.util.UUID

val bookshelves =
    mutableStateListOf(
        Bookshelf(id = UUID.randomUUID().toString(), name = "Favorites", books = sampleBooks, shelfStyle = ShelfStyle.DarkWood),
        Bookshelf(id = UUID.randomUUID().toString(), name = "Sci-Fi", books = sampleBooks, shelfStyle = ShelfStyle.DarkGreyMetal),
        Bookshelf(id = UUID.randomUUID().toString(), name = "To Read", books = sampleBooks, shelfStyle = ShelfStyle.GreyMetal)
    )


val bookshelf = Bookshelf(id = UUID.randomUUID().toString(), name = "Favorites", books = sampleBooks, shelfStyle = ShelfStyle.SliverMetal)
