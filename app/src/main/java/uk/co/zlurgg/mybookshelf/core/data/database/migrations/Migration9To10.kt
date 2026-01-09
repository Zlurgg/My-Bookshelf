package uk.co.zlurgg.mybookshelf.core.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database version 9 to 10.
 * Adds Book Club support for collaborative shelf sharing.
 *
 * Changes:
 * - BookshelfEntity: adds isBookClub, clubCode fields
 * - New table: book_club_memberships for tracking club memberships
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ========== BookshelfEntity book club fields ==========
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN isBookClub INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN clubCode TEXT DEFAULT NULL")

        // ========== New book_club_memberships table ==========
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS book_club_memberships (
                id TEXT PRIMARY KEY NOT NULL,
                clubCode TEXT NOT NULL,
                localShelfId TEXT NOT NULL,
                joinedAt INTEGER NOT NULL,
                lastSyncedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL DEFAULT 'PENDING'
            )
            """.trimIndent()
        )

        // ========== Indexes for efficient queries ==========
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_book_club_memberships_clubCode ON book_club_memberships(clubCode)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_book_club_memberships_localShelfId ON book_club_memberships(localShelfId)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_BookshelfEntity_clubCode ON BookshelfEntity(clubCode)")
    }
}
