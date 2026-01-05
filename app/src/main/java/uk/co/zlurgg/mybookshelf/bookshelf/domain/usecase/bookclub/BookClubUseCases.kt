package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

/**
 * Facade aggregating all Book Club related UseCases.
 * Simplifies ViewModel constructor dependencies and provides clean separation of concerns.
 */
class BookClubUseCases(
    // Phase 1: Create Book Club
    val createBookClub: CreateBookClubUseCase,
    val generateInviteLink: GenerateInviteLinkUseCase,

    // Phase 2: Join Book Club
    val parseClubCode: ParseClubCodeUseCase,
    val getBookClubPreview: GetBookClubPreviewUseCase,
    val joinBookClub: JoinBookClubUseCase,

    // Phase 3: Sync
    val syncBookClub: SyncBookClubUseCase,

    // Membership Management
    val restoreBookClubMemberships: RestoreBookClubMembershipsUseCase,
    val leaveBookClub: LeaveBookClubUseCase,
    val validateMemberships: ValidateBookClubMembershipsUseCase
)
