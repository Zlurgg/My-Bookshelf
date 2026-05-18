package uk.co.zlurgg.mybookshelf.bookclub.data.mappers

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookClubMembershipEntity
import uk.co.zlurgg.mybookshelf.bookclub.data.dto.BookClubBookDto
import uk.co.zlurgg.mybookshelf.bookclub.data.dto.BookClubMetadataDto

// ========== BookClub (DTO -> Domain) ==========

/**
 * Converts Firestore metadata DTO to domain BookClub model.
 */
fun BookClubMetadataDto.toBookClub(): BookClub = BookClub(
    code = code,
    name = name,
    style = ShelfStyle.entries.find { it.name == shelfStyle } ?: ShelfStyle.DarkWood,
    createdAt = createdAt?.time ?: 0L,
    createdBy = createdBy,
    createdByName = createdByName,
    bookCount = bookCount,
    memberCount = memberCount
)

// ========== BookClubMembership (Entity <-> Domain) ==========

/**
 * Converts domain BookClubMembership to Room entity.
 */
fun BookClubMembership.toMembershipEntity(id: String): BookClubMembershipEntity = BookClubMembershipEntity(
    id = id,
    clubCode = clubCode,
    localShelfId = localShelfId,
    joinedAt = joinedAt,
)

/**
 * Converts Room entity to domain BookClubMembership.
 */
fun BookClubMembershipEntity.toMembership(): BookClubMembership = BookClubMembership(
    clubCode = clubCode,
    localShelfId = localShelfId,
    joinedAt = joinedAt,
)

// ========== BookClubBook (DTO <-> Domain) ==========

/**
 * Converts Firestore BookClubBookDto to domain Book model.
 * Note: Personal metadata (notes, personal rating, reading status) are not included
 * as they're not stored in the club's book collection.
 */
fun BookClubBookDto.toBookDomain(): Book = Book(
    id = id,
    title = title,
    authors = authors,
    imageUrl = coverUrl ?: "",
    isbn = isbn,
    firstPublishYear = firstPublishYear?.toString(),
    numPages = pageCount,
    averageRating = averageRating?.toDouble(),
    ratingCount = ratingCount,
    // Remaining fields use defaults
    description = null,
    languages = emptyList(),
    numEditions = 0,
    purchased = false,
    spineColor = if (spineColor != 0) spineColor else BookColorGenerator.generateSpineColor()
)

/**
 * Converts domain Book to Firestore BookClubBookDto.
 * @param addedBy The user ID of who added this book
 * @param addedByName The display name of who added this book
 */
fun Book.toBookClubBookDto(
    addedBy: String,
    addedByName: String
): BookClubBookDto = BookClubBookDto(
    id = id,
    title = title,
    authors = authors,
    coverUrl = imageUrl,
    isbn = isbn,
    workId = id, // OpenLibrary work ID is the book ID
    firstPublishYear = firstPublishYear?.toIntOrNull(),
    pageCount = numPages,
    averageRating = averageRating?.toFloat(),
    ratingCount = ratingCount,
    spineColor = spineColor,
    addedBy = addedBy,
    addedByName = addedByName
)
