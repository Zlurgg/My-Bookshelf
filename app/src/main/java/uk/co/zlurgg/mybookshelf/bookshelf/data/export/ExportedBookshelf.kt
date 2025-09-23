package uk.co.zlurgg.mybookshelf.bookshelf.data.export

import kotlinx.serialization.Serializable
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

@Serializable
data class ExportedBookshelf(
    val name: String,
    val shelfStyle: ShelfStyle,
    val books: List<ExportedBook>
)