package uk.co.zlurgg.mybookshelf.bookshelf.presentation.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository

class SharedMyBookshelfViewModel(
    private val bookcaseRepository: BookcaseRepository
) : ViewModel() {

    private val _selectedShelfId = MutableStateFlow<String?>(null)
    val selectedShelfId: StateFlow<String?> = _selectedShelfId.asStateFlow()

    val shelves: StateFlow<List<Bookshelf>> =
        bookcaseRepository.getAllShelves()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedShelf: StateFlow<Bookshelf?> =
        combine(shelves, selectedShelfId) { list, id ->
            list.firstOrNull { it.id == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectShelf(id: String) {
        _selectedShelfId.value = id
    }
}
