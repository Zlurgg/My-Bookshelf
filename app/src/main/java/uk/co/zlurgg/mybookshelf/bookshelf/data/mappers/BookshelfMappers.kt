package uk.co.zlurgg.mybookshelf.bookshelf.data.mappers

import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

fun BookshelfEntity.toDomain(): Bookshelf = Bookshelf(
    id = id,
    name = name,
    books = emptyList(),
    shelfStyle = ShelfStyle.valueOf(shelfMaterial),
    position = position,
    isTidyMode = isTidyMode,
    isBookClub = isBookClub,
    clubCode = clubCode
)

/**
 * Converts domain Bookshelf to entity with default ownerId (null).
 */
fun Bookshelf.toEntity(): BookshelfEntity = BookshelfEntity(
    id = id,
    name = name,
    shelfMaterial = shelfStyle.name,
    position = position,
    isTidyMode = isTidyMode,
    isBookClub = isBookClub,
    clubCode = clubCode
)

/**
 * Converts domain Bookshelf to entity with specified ownerId.
 * Use this for system entities (e.g., tutorial shelf with SystemOwnerIds.TUTORIAL).
 *
 * @param ownerId The owner ID to set (null for guest, system ID for system entities)
 */
fun Bookshelf.toEntity(ownerId: String?): BookshelfEntity = BookshelfEntity(
    id = id,
    name = name,
    shelfMaterial = shelfStyle.name,
    position = position,
    isTidyMode = isTidyMode,
    ownerId = ownerId,
    isBookClub = isBookClub,
    clubCode = clubCode
)
