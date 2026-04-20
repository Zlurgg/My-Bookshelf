package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

/**
 * Aggregates book club operations use cases for club management and membership.
 * Injected into BookClubOperationsHandler.
 */
class BookClubOperationUseCases(
    val createBookClub: CreateBookClubUseCase,
    val generateInviteLink: GenerateInviteLinkUseCase,
    val parseClubCode: ParseClubCodeUseCase,
    val getBookClubPreview: GetBookClubPreviewUseCase,
    val joinBookClub: JoinBookClubUseCase,
    val syncBookClub: SyncBookClubUseCase,
    val restoreBookClubMemberships: RestoreBookClubMembershipsUseCase,
    val leaveBookClub: LeaveBookClubUseCase,
    val validateMemberships: ValidateBookClubMembershipsUseCase
)
