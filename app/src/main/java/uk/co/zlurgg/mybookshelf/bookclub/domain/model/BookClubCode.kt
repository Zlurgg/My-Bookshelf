package uk.co.zlurgg.mybookshelf.bookclub.domain.model

/**
 * Shared constants for book club invite code generation and validation.
 *
 * Character set excludes confusing characters (0/O, 1/I/L) for better readability
 * when codes need to be manually entered.
 */
object BookClubCode {
    const val CODE_LENGTH = 12
    const val VALID_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
}
