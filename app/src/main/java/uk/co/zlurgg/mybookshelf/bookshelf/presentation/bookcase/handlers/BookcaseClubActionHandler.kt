package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookClubPreview
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseAction
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseState
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

internal class BookcaseClubActionHandler(
    private val state: MutableStateFlow<BookcaseState>,
    private val bookClubOperations: ClubOperations,
    private val shelfOperations: ShelfOperationsHandler,
    private val scope: CoroutineScope,
) {

    fun handleAction(action: BookcaseAction) {
        when (action) {
            is BookcaseAction.OnCreateBookClub -> createBookClub(action.shelf)
            is BookcaseAction.OnInviteToClub -> showInviteForExistingClub(action.shelf)
            is BookcaseAction.DismissInviteLink -> {
                state.update { it.copy(bookClubInviteLink = null, bookClubCode = null, bookClubName = null) }
            }
            is BookcaseAction.ShowDeleteBookClubDialog -> {
                state.update { it.copy(showDeleteBookClubDialog = true, shelfToDelete = action.bookshelf) }
            }
            is BookcaseAction.DismissDeleteBookClubDialog -> {
                state.update { it.copy(showDeleteBookClubDialog = false, shelfToDelete = null) }
            }
            is BookcaseAction.ConfirmDeleteBookClub -> deleteBookClub()
            is BookcaseAction.ShowLeaveBookClubDialog -> {
                state.update { it.copy(showLeaveBookClubDialog = true, shelfToLeave = action.bookshelf) }
            }
            is BookcaseAction.DismissLeaveBookClubDialog -> {
                state.update { it.copy(showLeaveBookClubDialog = false, shelfToLeave = null) }
            }
            is BookcaseAction.ConfirmLeaveBookClub -> leaveBookClub()
            is BookcaseAction.ShowJoinBookClubDialog -> {
                state.update { it.copy(showJoinBookClubDialog = true, joinLookupError = null) }
            }
            is BookcaseAction.DismissJoinBookClubDialog -> {
                state.update {
                    it.copy(showJoinBookClubDialog = false, joinLookupError = null, pendingInviteCode = null)
                }
                bookClubOperations.clearLookupState()
            }
            is BookcaseAction.OnLookupBookClub -> lookupBookClub(action.codeOrUrl)
            is BookcaseAction.DismissBookClubPreview -> {
                state.update { it.copy(bookClubPreview = null, showJoinBookClubDialog = true) }
            }
            is BookcaseAction.OnConfirmJoinBookClub -> confirmJoinBookClub()
            is BookcaseAction.DismissJoinSuccess -> {
                state.update { it.copy(joinBookClubSuccess = null) }
            }
            is BookcaseAction.HandleInviteLink -> handleInviteLink(action.code)
            is BookcaseAction.DismissDeletedBookClubsNotification -> {
                state.update { it.copy(deletedBookClubNames = emptyList()) }
            }
            is BookcaseAction.DismissBookClubLimitDialog -> {
                state.update { it.copy(showBookClubLimitDialog = false) }
            }
            else -> error("Unhandled club action: $action")
        }
    }

    private fun createBookClub(shelf: Bookshelf) {
        scope.launch {
            state.update { it.copy(isCreatingBookClub = true, errorMessage = null) }

            when (val createResult = bookClubOperations.createBookClub(shelf.id, shelf.name)) {
                is Result.Success -> {
                    state.update {
                        it.copy(
                            isCreatingBookClub = false,
                            bookClubCode = createResult.data.clubCode,
                            bookClubInviteLink = createResult.data.inviteLink,
                            bookClubName = shelf.name,
                            isNewlyCreatedBookClub = true,
                            switchToBookClubsTab = true
                        )
                    }
                }
                is Result.Error -> {
                    if (createResult.error == DataError.Sync.MAX_BOOK_CLUBS_REACHED) {
                        state.update {
                            it.copy(isCreatingBookClub = false, showBookClubLimitDialog = true)
                        }
                    } else {
                        state.update {
                            it.copy(
                                isCreatingBookClub = false,
                                errorMessage = ErrorFormatter.formatDataErrorMessage(
                                    createResult.error,
                                    "create book club"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showInviteForExistingClub(shelf: Bookshelf) {
        val clubCode = shelf.clubCode ?: return
        val inviteLink = bookClubOperations.generateInviteLink(clubCode, shelf.name)
        state.update {
            it.copy(
                bookClubCode = clubCode,
                bookClubInviteLink = inviteLink,
                bookClubName = shelf.name,
                isNewlyCreatedBookClub = false
            )
        }
    }

    private fun deleteBookClub() {
        val shelfToDelete = state.value.shelfToDelete ?: return

        state.update {
            it.copy(
                showDeleteBookClubDialog = false,
                bookshelves = it.bookshelves - shelfToDelete,
                recentlyDeleted = shelfToDelete,
                shelfToDelete = null
            )
        }

        scope.launch {
            when (val deleteResult = shelfOperations.deleteShelf(shelfToDelete.id)) {
                is Result.Success -> {
                    state.update { it.copy(recentlyDeleted = null) }
                }
                is Result.Error -> {
                    state.update {
                        it.copy(
                            bookshelves = it.bookshelves + shelfToDelete,
                            recentlyDeleted = null,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                deleteResult.error,
                                "delete book club"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun leaveBookClub() {
        val shelfToLeave = state.value.shelfToLeave ?: return

        state.update {
            it.copy(
                showLeaveBookClubDialog = false,
                bookshelves = it.bookshelves - shelfToLeave,
                recentlyDeleted = shelfToLeave,
                shelfToLeave = null
            )
        }

        scope.launch {
            when (val leaveResult = bookClubOperations.leaveBookClub(shelfToLeave.id)) {
                is Result.Success -> {
                    state.update { it.copy(recentlyDeleted = null) }
                }
                is Result.Error -> {
                    state.update {
                        it.copy(
                            bookshelves = it.bookshelves + shelfToLeave,
                            recentlyDeleted = null,
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                leaveResult.error,
                                "leave book club"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun lookupBookClub(codeOrUrl: String) {
        scope.launch {
            state.update { it.copy(joinLookupLoading = true, joinLookupError = null) }

            when (val lookupResult = bookClubOperations.lookupBookClub(codeOrUrl)) {
                is ClubOperations.LookupResult.Found -> {
                    val preview = BookClubPreview(
                        clubName = lookupResult.clubName,
                        clubCode = lookupResult.clubCode,
                        memberCount = lookupResult.memberCount
                    )
                    state.update {
                        it.copy(
                            joinLookupLoading = false,
                            showJoinBookClubDialog = false,
                            bookClubPreview = preview
                        )
                    }
                }
                is ClubOperations.LookupResult.NotFound -> {
                    state.update {
                        it.copy(
                            joinLookupLoading = false,
                            joinLookupError = ErrorFormatter.formatDataErrorMessage(
                                lookupResult.error,
                                "find book club"
                            )
                        )
                    }
                }
                is ClubOperations.LookupResult.InvalidCode -> {
                    state.update {
                        it.copy(
                            joinLookupLoading = false,
                            joinLookupError = ErrorFormatter.formatDataErrorMessage(
                                lookupResult.error,
                                "validate code"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun confirmJoinBookClub() {
        scope.launch {
            state.update { it.copy(joinInProgress = true) }

            when (val joinResult = bookClubOperations.joinBookClub()) {
                is Result.Success -> {
                    when (val result = joinResult.data) {
                        is ClubOperations.JoinResult.Success -> {
                            state.update {
                                it.copy(
                                    joinInProgress = false,
                                    bookClubPreview = null,
                                    joinBookClubSuccess = result.shelfName
                                )
                            }
                        }
                        is ClubOperations.JoinResult.AlreadyMember -> {
                            state.update {
                                it.copy(
                                    joinInProgress = false,
                                    bookClubPreview = null,
                                    errorMessage = ErrorFormatter.formatDataErrorMessage(
                                        DataError.Sync.ALREADY_MEMBER,
                                        "join book club"
                                    )
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    if (joinResult.error == DataError.Sync.MAX_BOOK_CLUBS_REACHED) {
                        state.update {
                            it.copy(
                                joinInProgress = false,
                                bookClubPreview = null,
                                showBookClubLimitDialog = true
                            )
                        }
                    } else {
                        state.update {
                            it.copy(
                                joinInProgress = false,
                                bookClubPreview = null,
                                errorMessage = ErrorFormatter.formatDataErrorMessage(
                                    joinResult.error,
                                    "join book club"
                                )
                            )
                        }
                    }
                }
            }

            bookClubOperations.clearLookupState()
        }
    }

    private fun handleInviteLink(code: String) {
        state.update {
            it.copy(
                pendingInviteCode = code,
                showJoinBookClubDialog = true,
                joinLookupError = null
            )
        }
        lookupBookClub(code)
    }
}
