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