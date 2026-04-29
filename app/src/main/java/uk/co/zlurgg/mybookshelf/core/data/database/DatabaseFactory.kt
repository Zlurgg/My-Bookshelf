package uk.co.zlurgg.mybookshelf.core.data.database

import android.content.Context
import androidx.room.Room

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
            .build()
    }
}
