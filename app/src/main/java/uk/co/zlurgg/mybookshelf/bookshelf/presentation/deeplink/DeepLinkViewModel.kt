package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.ImportResult
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
        }
    }

    private fun importFromToken(token: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                isLoading = true,
                error = null,
                conflictExistingName = null,
                conflictJsonData = null
            ) }

            when (val result = deepLinkImportUseCase.importBookshelfFromToken(token)) {
                is Result.Success -> {
                    when (val importResult = result.data) {
                        is ImportResult.Success -> {
                            _state.update { it.copy(
                                isLoading = false,
                                importSuccessful = true
                            ) }
                        }
                        is ImportResult.NameConflict -> {
                            _state.update { it.copy(
                                isLoading = false,
                                conflictExistingName = importResult.existingName,
                                conflictJsonData = importResult.jsonData
                            ) }
                        }
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        isLoading = false,
                        error = ErrorFormatter.formatDataErrorMessage(result.error, "import bookshelf")
                    ) }
                }
            }
        }
    }

    private fun resolveNameConflict(jsonData: String, newName: String) {
        viewModelScope.launch {
            _state.update { it.copy(
                isLoading = true,
                conflictExistingName = null,
                conflictJsonData = null
            ) }

            when (val result = deepLinkImportUseCase.importBookshelfWithCustomName(jsonData, newName)) {
                is Result.Success -> {
                    _state.update { it.copy(
                        isLoading = false,
                        importSuccessful = true
                    ) }
                }
                is Result.Error -> {
                    _state.update { it.copy(
                        isLoading = false,
                        error = ErrorFormatter.formatDataErrorMessage(result.error, "import bookshelf")
                    ) }
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
        _state.update { it.copy(
            conflictExistingName = null,
            conflictJsonData = null
        ) }
    }
}