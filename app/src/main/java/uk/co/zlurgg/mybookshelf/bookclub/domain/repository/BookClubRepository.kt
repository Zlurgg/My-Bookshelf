package uk.co.zlurgg.mybookshelf.bookclub.domain.repository

/**
 * Composite repository for all Book Club operations.
 * Extends focused interfaces for better separation of concerns while maintaining
 * a single injection point for consumers.
 *
 * Individual interfaces:
 * - [BookClubManagementRepository]: Club lifecycle (7 functions)
 * - [BookClubMembershipRepository]: Membership operations (7 functions)
 * - [BookClubSyncRepository]: Book sync (4 functions)
 * - [BookClubReviewRepository]: Reviews and comments (7 functions)
 */
interface BookClubRepository :
    BookClubManagementRepository,
    BookClubMembershipRepository,
    BookClubSyncRepository,
    BookClubReviewRepository

/**
 * Result of syncing books from a club.
 */
data class SyncResult(
    val booksAdded: Int,
    val booksRemoved: Int
)
