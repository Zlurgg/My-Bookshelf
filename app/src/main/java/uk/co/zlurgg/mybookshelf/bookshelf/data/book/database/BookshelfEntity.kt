package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BookshelfEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shelfMaterial: String
)
