package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.components

data class ShelfDisplayState(
    val isReorderMode: Boolean = false,
    val isTutorialShelf: Boolean = false,
    val bookCountOverride: Int? = null
)
