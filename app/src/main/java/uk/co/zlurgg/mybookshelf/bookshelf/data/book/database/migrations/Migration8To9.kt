package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from database version 8 to 9.
 * Adds sync metadata fields to support Firestore cloud sync.
 *
 * Changes:
 * - BookEntity: adds ownerId, lastModifiedAt, syncStatus, cloudId, version
 * - BookshelfEntity: adds ownerId, lastModifiedAt, syncStatus, cloudId, version, isShared, shareCode
 * - BookshelfBookCrossRef: adds syncStatus, lastModifiedAt
 * - New table: SyncMetadataEntity for tracking sync state per user
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ========== BookEntity sync fields ==========
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN ownerId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN cloudId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN version INTEGER NOT NULL DEFAULT 1")

        // ========== BookshelfEntity sync fields ==========
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN ownerId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN cloudId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN isShared INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN shareCode TEXT DEFAULT NULL")

        // ========== BookshelfBookCrossRef sync fields ==========
        db.execSQL("ALTER TABLE BookshelfBookCrossRef ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        db.execSQL("ALTER TABLE BookshelfBookCrossRef ADD COLUMN lastModifiedAt INTEGER NOT NULL DEFAULT 0")

        // ========== New SyncMetadataEntity table ==========
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS SyncMetadataEntity (
                userId TEXT PRIMARY KEY NOT NULL,
                lastSyncTimestamp INTEGER NOT NULL,
                syncInProgress INTEGER NOT NULL DEFAULT 0,
                lastSyncError TEXT,
                pendingOperationsCount INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // ========== Indexes for efficient sync queries ==========
        db.execSQL("CREATE INDEX IF NOT EXISTS index_BookEntity_syncStatus ON BookEntity(syncStatus)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_BookEntity_ownerId ON BookEntity(ownerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_BookshelfEntity_syncStatus ON BookshelfEntity(syncStatus)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_BookshelfEntity_ownerId ON BookshelfEntity(ownerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_BookshelfEntity_shareCode ON BookshelfEntity(shareCode)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_BookshelfBookCrossRef_syncStatus ON BookshelfBookCrossRef(syncStatus)")
    }
}
