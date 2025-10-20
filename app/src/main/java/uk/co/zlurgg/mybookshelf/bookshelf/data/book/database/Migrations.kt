package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Remove the onShelf column from BookEntity table
        db.execSQL("CREATE TABLE BookEntity_new (id TEXT NOT NULL, title TEXT NOT NULL, description TEXT, imageUrl TEXT NOT NULL, languages TEXT NOT NULL, authors TEXT NOT NULL, firstPublishYear TEXT, ratingsAverage REAL, ratingsCount INTEGER, numPagesMedian INTEGER, numEditions INTEGER NOT NULL, purchased INTEGER NOT NULL, affiliateLink TEXT NOT NULL, spineColor INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("INSERT INTO BookEntity_new (id, title, description, imageUrl, languages, authors, firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions, purchased, affiliateLink, spineColor) SELECT id, title, description, imageUrl, languages, authors, firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions, purchased, affiliateLink, spineColor FROM BookEntity")
        db.execSQL("DROP TABLE BookEntity")
        db.execSQL("ALTER TABLE BookEntity_new RENAME TO BookEntity")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add position column to BookshelfEntity table
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Remove affiliateLink column from BookEntity table
        db.execSQL("CREATE TABLE BookEntity_new (id TEXT NOT NULL, title TEXT NOT NULL, description TEXT, imageUrl TEXT NOT NULL, languages TEXT NOT NULL, authors TEXT NOT NULL, firstPublishYear TEXT, ratingsAverage REAL, ratingsCount INTEGER, numPagesMedian INTEGER, numEditions INTEGER NOT NULL, purchased INTEGER NOT NULL, spineColor INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("INSERT INTO BookEntity_new (id, title, description, imageUrl, languages, authors, firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions, purchased, spineColor) SELECT id, title, description, imageUrl, languages, authors, firstPublishYear, ratingsAverage, ratingsCount, numPagesMedian, numEditions, purchased, spineColor FROM BookEntity")
        db.execSQL("DROP TABLE BookEntity")
        db.execSQL("ALTER TABLE BookEntity_new RENAME TO BookEntity")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add personal metadata columns (NOT exported for privacy)
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN readingStatus TEXT NOT NULL DEFAULT 'WANT_TO_READ'")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN personalRating REAL")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN personalNotes TEXT")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN dateAdded INTEGER")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN purchaseDate INTEGER")

        // Add enhanced metadata columns (shareable)
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN isbn TEXT")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN publisher TEXT")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN publishDate TEXT")
        db.execSQL("ALTER TABLE BookEntity ADD COLUMN internetArchiveId TEXT")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Convert nullable personalRating and personalNotes to non-null with defaults
        // SQLite doesn't support modifying column types directly, so we need table recreation

        // Step 1: Create new table with non-null columns
        db.execSQL("""
            CREATE TABLE BookEntity_new (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT,
                imageUrl TEXT NOT NULL,
                languages TEXT NOT NULL,
                authors TEXT NOT NULL,
                firstPublishYear TEXT,
                ratingsAverage REAL,
                ratingsCount INTEGER,
                numPagesMedian INTEGER,
                numEditions INTEGER NOT NULL,
                purchased INTEGER NOT NULL,
                spineColor INTEGER NOT NULL,
                readingStatus TEXT NOT NULL DEFAULT 'WANT_TO_READ',
                personalRating REAL NOT NULL DEFAULT 0.0,
                personalNotes TEXT NOT NULL DEFAULT '',
                dateAdded INTEGER,
                purchaseDate INTEGER,
                isbn TEXT,
                publisher TEXT,
                publishDate TEXT,
                internetArchiveId TEXT
            )
        """.trimIndent())

        // Step 2: Copy data, converting NULL to defaults
        db.execSQL("""
            INSERT INTO BookEntity_new
            SELECT
                id, title, description, imageUrl, languages, authors, firstPublishYear,
                ratingsAverage, ratingsCount, numPagesMedian, numEditions, purchased, spineColor,
                readingStatus,
                COALESCE(personalRating, 0.0) as personalRating,
                COALESCE(personalNotes, '') as personalNotes,
                dateAdded, purchaseDate, isbn, publisher, publishDate, internetArchiveId
            FROM BookEntity
        """.trimIndent())

        // Step 3: Drop old table
        db.execSQL("DROP TABLE BookEntity")

        // Step 4: Rename new table
        db.execSQL("ALTER TABLE BookEntity_new RENAME TO BookEntity")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add isTidyMode column to BookshelfEntity table
        db.execSQL("ALTER TABLE BookshelfEntity ADD COLUMN isTidyMode INTEGER NOT NULL DEFAULT 0")
    }
}