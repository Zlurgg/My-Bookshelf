package uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.ShelfStyle

fun BookshelfEntity.toDomain(): Bookshelf = Bookshelf(
    id = id,
    name = name,
    books = emptyList(),
    shelfStyle = ShelfStyle.valueOf(shelfMaterial)
)

fun Bookshelf.toEntity(): BookshelfEntity = BookshelfEntity(
    id = id,
    name = name,
    shelfMaterial = shelfStyle.name
)
