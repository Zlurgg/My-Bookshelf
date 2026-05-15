package uk.co.zlurgg.mybookshelf.core.data.database.entity

import androidx.room.Entity

@Entity(primaryKeys = ["shelfId", "bookId"])
data class BookshelfBookCrossRef(
    val shelfId: String,
    val bookId: String,
    val addedAt: Long,
    val addedByUserId: String? = null,
)
