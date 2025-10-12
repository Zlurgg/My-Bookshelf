package uk.co.zlurgg.mybookshelf.bookshelf.domain.model

/**
 * Represents the user's reading intention/goal for a book.
 *
 * IMPORTANT: This is NOT actual reading progress tracking - users don't have the actual books.
 * This is a recommendation organizer (Goodreads-style), not a reading tracker (Kindle-style).
 */
enum class ReadingStatus {
    /** Default - books you're interested in recommending */
    WANT_TO_READ,

    /** Goal/intention to read (not actual tracking) */
    CURRENTLY_READING,

    /** Finished, can recommend */
    READ
}
