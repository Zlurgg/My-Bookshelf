package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Result of migrating local data to a user account.
 *
 * Migration happens when a user signs in for the first time
 * and has existing local data that needs to be associated
 * with their account.
 */
data class MigrationResult(
    /** Number of books that were assigned to the user */
    val booksAssigned: Int = 0,

    /** Number of shelves that were assigned to the user */
    val shelvesAssigned: Int = 0,

    /** Whether any data was migrated */
    val hadDataToMigrate: Boolean = false,

    /** Whether initial sync was triggered */
    val syncTriggered: Boolean = false
) {
    /** Total number of entities migrated */
    val totalMigrated: Int
        get() = booksAssigned + shelvesAssigned

    companion object {
        /** Result when no migration was needed */
        val NO_MIGRATION_NEEDED = MigrationResult(
            booksAssigned = 0,
            shelvesAssigned = 0,
            hadDataToMigrate = false,
            syncTriggered = false
        )
    }
}
