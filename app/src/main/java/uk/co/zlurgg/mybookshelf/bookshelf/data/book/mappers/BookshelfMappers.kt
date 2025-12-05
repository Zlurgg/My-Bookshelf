package uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers

import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

fun BookshelfEntity.toDomain(): Bookshelf = Bookshelf(
    id = id,
    name = name,
    books = emptyList(),
    shelfStyle = ShelfStyle.valueOf(shelfMaterial),
    position = position,
    isTidyMode = isTidyMode
)

fun Bookshelf.toEntity(): BookshelfEntity = BookshelfEntity(
    id = id,
    name = name,
    shelfMaterial = shelfStyle.name,
    position = position,
    isTidyMode = isTidyMode
)
