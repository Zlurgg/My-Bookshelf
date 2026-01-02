package uk.co.zlurgg.mybookshelf.core.data.database

import android.content.Context
import androidx.room.Room
import uk.co.zlurgg.mybookshelf.core.data.database.migrations.MIGRATION_8_9
import uk.co.zlurgg.mybookshelf.core.data.database.migrations.MIGRATION_9_10
import uk.co.zlurgg.mybookshelf.core.data.database.migrations.MIGRATION_10_11

class DatabaseFactory(
    private val context: Context
) {
    fun create(): MyBookshelfRoomDatabase {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(MyBookshelfRoomDatabase.DB_NAME)

        return Room.databaseBuilder(
                appContext,
                MyBookshelfRoomDatabase::class.java,
                dbFile.absolutePath,
            )
            .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .build()
    }
}
