package uk.co.zlurgg.mybookshelf.core.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database version 10 to 11.
 * Adds creator tracking for book clubs.
 *
 * Changes:
 * - BookshelfEntity: adds clubCreatorId field to track who created the book club
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN clubCreatorId TEXT DEFAULT NULL")
    }
}
