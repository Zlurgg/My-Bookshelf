package uk.co.zlurgg.mybookshelf.book.domain.model

import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle

data class Bookshelf(
    val id: String,
    val name: String,
    val books: List<Book>,
    val shelfStyle: ShelfStyle,
    val position: Int = 0,
    val isTidyMode: Boolean = false,
    val isBookClub: Boolean = false,
    val clubCode: String? = null,
    val clubCreatorId: String? = null
)
