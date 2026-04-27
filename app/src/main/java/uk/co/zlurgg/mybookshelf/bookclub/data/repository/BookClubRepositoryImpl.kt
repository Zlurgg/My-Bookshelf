package uk.co.zlurgg.mybookshelf.bookclub.data.repository

import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubManagementRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubMembershipRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubReviewRepository
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubSyncRepository

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
