package uk.co.zlurgg.mybookshelf.core.presentation

import androidx.compose.runtime.mutableStateListOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

val bookshelves =
    mutableStateListOf(
        Bookshelf(id = "sample-shelf-favorites", name = "Favorites", books = sampleBooks, shelfStyle = ShelfStyle.DarkWood),
        Bookshelf(id = "sample-shelf-scifi", name = "Sci-Fi", books = sampleBooks, shelfStyle = ShelfStyle.DarkGreyMetal),
        Bookshelf(id = "sample-shelf-toread", name = "To Read", books = sampleBooks, shelfStyle = ShelfStyle.GreyMetal)
    )


val bookshelf = Bookshelf(id = "sample-shelf-single", name = "Favorites", books = sampleBooks, shelfStyle = ShelfStyle.SliverMetal)
