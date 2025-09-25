package uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.ImportResult
import uk.co.zlurgg.mybookshelf.core.domain.Result

class DeepLinkViewModel(
    private val deepLinkImportUseCase: DeepLinkImportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DeepLinkState())
    val state: StateFlow<DeepLinkState> = _state.asStateFlow()

    fun onAction(action: DeepLinkAction) {
        when (action) {
            is DeepLinkAction.ImportFromToken -> importFromToken(action.token)
            is DeepLinkAction.DismissError -> dismissError()
            is DeepLinkAction.DismissSuccess -> dismissSuccess()
            is DeepLinkAction.DismissNameConflict -> dismissNameConflict()
            is DeepLinkAction.ResolveNameConflictWithNewName -> resolveNameConflict(action.jsonData, action.newName)
        }
    }

    private fun importFromToken(token: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, nameConflict = null)

            when (val result = deepLinkImportUseCase.importBookshelfFromToken(token)) {
                is Result.Success -> {
                    when (val importResult = result.data) {
                        is ImportResult.Success -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                importSuccessful = true
                            )
                        }
                        is ImportResult.NameConflict -> {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                nameConflict = NameConflictData(
                                    existingName = importResult.existingName,
                                    jsonData = importResult.jsonData
                                )
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to import bookshelf. The link may be expired or invalid."
                    )
                }
            }
        }
    }

    private fun resolveNameConflict(jsonData: String, newName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, nameConflict = null)

            when (val result = deepLinkImportUseCase.importBookshelfWithCustomName(jsonData, newName)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        importSuccessful = true
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to import bookshelf with the new name."
                    )
                }
            }
        }
    }

    private fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun dismissSuccess() {
        _state.value = _state.value.copy(importSuccessful = false)
    }

    private fun dismissNameConflict() {
        _state.value = _state.value.copy(nameConflict = null)
    }
}