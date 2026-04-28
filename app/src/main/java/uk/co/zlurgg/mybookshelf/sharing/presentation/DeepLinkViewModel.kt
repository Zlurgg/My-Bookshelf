package uk.co.zlurgg.mybookshelf.sharing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.sharing.domain.usecase.ImportResult
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorFormatter
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class DeepLinkViewModel(
    private val deepLinkImportUseCase: DeepLinkImportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DeepLinkState())
    val state: StateFlow<DeepLinkState> = _state.asStateFlow()

    fun onAction(action: DeepLinkAction) {
        when (action) {
            is DeepLinkAction.ImportFromToken -> importFromToken(action.token)
            is DeepLinkAction.OnDismissError -> dismissError()
            is DeepLinkAction.OnDismissSuccess -> dismissSuccess()
            is DeepLinkAction.OnDismissNameConflict -> dismissNameConflict()
            is DeepLinkAction.ResolveNameConflictWithNewName -> resolveNameConflict(action.jsonData, action.newName)
            is DeepLinkAction.ReceiveBookClubInvite -> receiveBookClubInvite(action.code)
            is DeepLinkAction.ClearBookClubInvite -> clearBookClubInvite()
        }
    }

    private fun receiveBookClubInvite(code: String) {
        _state.update { it.copy(pendingClubCode = code) }
    }

    private fun clearBookClubInvite() {
        _state.update { it.copy(pendingClubCode = null) }
    }

    private fun importFromToken(token: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    conflictExistingName = null,
                    conflictJsonData = null
                )
            }

            when (val result = deepLinkImportUseCase.importBookshelfFromToken(token)) {
                is Result.Success -> {
                    when (val importResult = result.data) {
                        is ImportResult.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    importSuccessful = true
                                )
                            }
                        }
                        is ImportResult.NameConflict -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    conflictExistingName = importResult.existingName,
                                    conflictJsonData = importResult.jsonData
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = ErrorFormatter.formatDataErrorMessage(result.error, "import bookshelf")
                        )
                    }
                }
            }
        }
    }

    private fun resolveNameConflict(jsonData: String, newName: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    conflictError = null // Clear previous error
                )
            }

            when (val result = deepLinkImportUseCase.importBookshelfWithCustomName(jsonData, newName)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            importSuccessful = true,
                            conflictExistingName = null,
                            conflictJsonData = null,
                            conflictError = null
                        )
                    }
                }
                is Result.Error -> {
                    // Check if it's a name conflict error (inline error) or general error (dismiss dialog)
                    if (result.error == DataError.Local.NAME_CONFLICT) {
                        // Show inline error in dialog, keep dialog open
                        _state.update {
                            it.copy(
                                isLoading = false,
                                conflictError = ErrorFormatter.formatDataErrorMessage(result.error, "import bookshelf")
                            )
                        }
                    } else {
                        // General error - dismiss dialog and show error dialog
                        _state.update {
                            it.copy(
                                isLoading = false,
                                conflictExistingName = null,
                                conflictJsonData = null,
                                conflictError = null,
                                error = ErrorFormatter.formatDataErrorMessage(result.error, "import bookshelf")
                            )
                        }
                    }
                }
            }
        }
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun dismissSuccess() {
        _state.update { it.copy(importSuccessful = false) }
    }

    private fun dismissNameConflict() {
        _state.update {
            it.copy(
                conflictExistingName = null,
                conflictJsonData = null,
                conflictError = null // Clear inline error when dismissing
            )
        }
    }
}
