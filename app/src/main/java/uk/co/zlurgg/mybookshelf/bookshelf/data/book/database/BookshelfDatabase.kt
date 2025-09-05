package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BookEntity::class, BookshelfEntity::class, BookshelfBookCrossRef::class],
    version = 2
)
@TypeConverters(
    StringListTypeConverter::class
)
abstract class BookshelfDatabase : RoomDatabase() {
    abstract val bookshelfDao: BookshelfDao

    companion object {
        const val DB_NAME = "my_bookshelf.db"
    }
}