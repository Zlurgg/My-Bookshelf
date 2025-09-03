package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Entity

@Entity(primaryKeys = ["shelfId", "bookId"])
data class BookshelfBookCrossRef(
    val shelfId: String,
    val bookId: String,
    val addedAt: Long
)
