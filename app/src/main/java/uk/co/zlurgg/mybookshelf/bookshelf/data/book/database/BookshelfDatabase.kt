package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import uk.co.zlurgg.mybookshelf.sync.data.database.SyncDao
import uk.co.zlurgg.mybookshelf.sync.data.database.SyncMetadataEntity

@Database(
    entities = [
        BookEntity::class,
        BookshelfEntity::class,
        BookshelfBookCrossRef::class,
        SyncMetadataEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(
    StringListTypeConverter::class
)
abstract class BookshelfDatabase : RoomDatabase() {
    abstract val bookshelfDao: BookshelfDao
    abstract val syncDao: SyncDao

    companion object {
        const val DB_NAME = "my_bookshelf.db"
    }
}