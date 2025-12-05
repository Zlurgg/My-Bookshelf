package uk.co.zlurgg.mybookshelf.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

class DatabaseFactory(
    private val context: Context
) {
    fun create(): RoomDatabase.Builder<MyBookshelfRoomDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(MyBookshelfRoomDatabase.DB_NAME)

        return Room.databaseBuilder(
                appContext,
                MyBookshelfRoomDatabase::class.java,
                dbFile.absolutePath,
            )
    }
}
