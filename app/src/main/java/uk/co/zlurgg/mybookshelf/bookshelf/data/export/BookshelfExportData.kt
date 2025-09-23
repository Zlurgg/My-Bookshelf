package uk.co.zlurgg.mybookshelf.bookshelf.data.export

import kotlinx.serialization.Serializable

@Serializable
data class BookshelfExportData(
    val formatVersion: Int = 1,
    val exportedAt: String,
    val appName: String = "My Bookshelf",
    val bookshelf: ExportedBookshelf
)