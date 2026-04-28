package uk.co.zlurgg.mybookshelf.sharing.data.export

import kotlinx.serialization.Serializable
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle

@Serializable
data class ExportedBookshelf(
    val name: String,
    val shelfStyle: ShelfStyle,
    val bookIds: List<BookIdentifier>
)
