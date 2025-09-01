package uk.co.zlurgg.mybookshelf.bookshelf.presenation.util

import androidx.compose.runtime.mutableStateListOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import java.util.UUID

val bookshelves =
    mutableStateListOf(
        Bookshelf(id = UUID.randomUUID().toString(), name = "Favorites", books = sampleBooks, shelfMaterial = ShelfMaterial.DarkWood),
        Bookshelf(id = UUID.randomUUID().toString(), name = "Sci-Fi", books = sampleBooks, shelfMaterial = ShelfMaterial.DarkGreyMetal),
        Bookshelf(id = UUID.randomUUID().toString(), name = "To Read", books = sampleBooks, shelfMaterial = ShelfMaterial.GreyMetal)
    )


val bookshelf = Bookshelf(id = UUID.randomUUID().toString(), name = "Favorites", books = sampleBooks, shelfMaterial = ShelfMaterial.SliverMetal)
