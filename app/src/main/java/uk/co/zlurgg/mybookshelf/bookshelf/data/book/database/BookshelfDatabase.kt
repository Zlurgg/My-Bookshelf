package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BookEntity::class],
    version = 1
)
@TypeConverters(
    StringListTypeConverter::class,
    ColorConverters::class
)
abstract class BookshelfDatabase : RoomDatabase() {
    abstract val bookshelfDao: BookshelfDao

    companion object {
        const val DB_NAME = "my_bookshelf.db"
    }
}