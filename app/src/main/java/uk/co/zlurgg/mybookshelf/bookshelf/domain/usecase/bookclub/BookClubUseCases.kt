package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

/**
 * Facade aggregating all Book Club related UseCases.
 * Simplifies ViewModel constructor dependencies and provides clean separation of concerns.
 */
class BookClubUseCases(
    val createBookClub: CreateBookClubUseCase,
    val generateInviteLink: GenerateInviteLinkUseCase
)
