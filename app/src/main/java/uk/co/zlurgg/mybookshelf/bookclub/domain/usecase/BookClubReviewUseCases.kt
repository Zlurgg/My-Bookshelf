package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

/**
 * Aggregates book club review and comment use cases.
 * Injected into BookDetailViewModel.
 */
class BookClubReviewUseCases(
    val getBookClubReviews: GetBookClubReviewsUseCase,
    val upsertBookClubReview: UpsertBookClubReviewUseCase,
    val deleteBookClubReview: DeleteBookClubReviewUseCase,
    val getBookClubComments: GetBookClubCommentsUseCase,
    val addBookClubComment: AddBookClubCommentUseCase,
    val editBookClubComment: EditBookClubCommentUseCase,
    val deleteBookClubComment: DeleteBookClubCommentUseCase
)
