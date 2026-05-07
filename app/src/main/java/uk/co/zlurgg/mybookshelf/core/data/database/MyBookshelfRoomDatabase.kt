package uk.co.zlurgg.mybookshelf.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookClubDao
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookClubMembershipEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity

@Database(
    entities = [
        BookEntity::class,
        BookshelfEntity::class,
        BookshelfBookCrossRef::class,
        BookClubMembershipEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(
    StringListTypeConverter::class
)
abstract class MyBookshelfRoomDatabase : RoomDatabase() {
    abstract val bookshelfDao: BookshelfDao
    abstract val bookClubDao: BookClubDao

    companion object {
        const val DB_NAME = "my_bookshelf.db"
    }
}
