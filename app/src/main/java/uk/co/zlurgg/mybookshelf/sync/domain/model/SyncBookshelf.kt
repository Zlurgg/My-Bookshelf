package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Domain model for bookshelf sync operations.
 *
 * This represents a bookshelf in the sync context - pure domain without
 * any Firestore or database annotations.
 */
data class SyncBookshelf(
    val id: String,
    val name: String,
    val shelfMaterial: String,
    val position: Int,
    val isTidyMode: Boolean,
    val bookIds: List<String>,

    // Sharing metadata
    val isShared: Boolean,
    val shareCode: String?,

    // Sync metadata
    val version: Long,
    val lastModifiedAt: Long
)
