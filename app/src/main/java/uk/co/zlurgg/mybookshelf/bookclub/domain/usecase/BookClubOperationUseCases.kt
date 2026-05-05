package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

/**
 * Aggregates book club operations use cases for club management and membership.
 * Injected into ClubOperationsImpl.
 */
class BookClubOperationUseCases(
    val createBookClub: CreateBookClubUseCase,
    val parseClubCode: ParseClubCodeUseCase,
    val getBookClubPreview: GetBookClubPreviewUseCase,
    val joinBookClub: JoinBookClubUseCase,
    val syncBookClub: SyncBookClubUseCase,
    val restoreBookClubMemberships: RestoreBookClubMembershipsUseCase,
    val leaveBookClub: LeaveBookClubUseCase,
    val validateMemberships: ValidateBookClubMembershipsUseCase
)
