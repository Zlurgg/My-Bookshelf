package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

import androidx.compose.runtime.Immutable

@Immutable
data class ShelfDisplayState(
    val isReorderMode: Boolean = false,
    val isTutorialShelf: Boolean = false,
    val bookCountOverride: Int? = null,
    val currentUserId: String? = null,
)
