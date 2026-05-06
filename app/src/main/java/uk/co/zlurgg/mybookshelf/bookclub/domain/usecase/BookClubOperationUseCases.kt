package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

/**
 * Aggregates book club operations use cases for club management and membership.
 * Injected into ClubOperationsImpl.
 */
@Suppress("LongParameterList")
class BookClubOperationUseCases(
    val createBookClub: CreateBookClubUseCase,
    val parseClubCode: ParseClubCodeUseCase,
    val getBookClubPreview: GetBookClubPreviewUseCase,
    val joinBookClub: JoinBookClubUseCase,
    val syncBookClub: SyncBookClubUseCase,
    val restoreBookClubMemberships: RestoreBookClubMembershipsUseCase,
    val leaveBookClub: LeaveBookClubUseCase,
    val validateMemberships: ValidateBookClubMembershipsUseCase,
    val deleteBookClub: DeleteBookClubUseCase,
    val syncBookToClub: SyncBookToClubUseCase,
    val removeBookFromClub: RemoveBookFromClubUseCase,
    val updateClubStyle: UpdateClubStyleUseCase,
    val clearClubMemberships: ClearClubMembershipsUseCase,
    val renameBookClub: RenameBookClubUseCase,
    val getClubsCreatedByUser: GetClubsCreatedByUserUseCase,
    val getClubMembershipsForUser: GetClubMembershipsForUserUseCase,
    val removeUserFromClub: RemoveUserFromClubUseCase
)
