package uk.co.zlurgg.mybookshelf.bookcase.presentation.components

data class ShelfDisplayState(
    val isReorderMode: Boolean = false,
    val isTutorialShelf: Boolean = false,
    val bookCountOverride: Int? = null,
    val currentUserId: String? = null,
    val memberCount: Int? = null
)
