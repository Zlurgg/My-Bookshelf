package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

class DatabaseFactory(
    private val context: Context
) {
    fun create(): RoomDatabase.Builder<BookshelfDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(BookshelfDatabase.DB_NAME)

        return Room.databaseBuilder(
                appContext,
                BookshelfDatabase::class.java,
                dbFile.absolutePath,
            ).fallbackToDestructiveMigration(false)
            .addMigrations(MIGRATION_2_3)
    }
}