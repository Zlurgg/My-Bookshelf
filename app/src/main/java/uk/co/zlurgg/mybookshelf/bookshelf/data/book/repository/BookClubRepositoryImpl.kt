package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubManagementRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubMembershipRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubReviewRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubSyncRepository

/**
 * Composite repository that delegates to focused implementations.
 */
internal class BookClubRepositoryImpl(
    management: BookClubManagementRepository,
    membership: BookClubMembershipRepository,
    sync: BookClubSyncRepository,
    review: BookClubReviewRepository,
) : BookClubRepository,
    BookClubManagementRepository by management,
    BookClubMembershipRepository by membership,
    BookClubSyncRepository by sync,
    BookClubReviewRepository by review
