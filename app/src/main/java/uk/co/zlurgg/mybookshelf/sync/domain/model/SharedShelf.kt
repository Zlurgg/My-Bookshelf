package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Domain model for shared shelf metadata in sync operations.
 *
 * This represents shared shelf information - pure domain without
 * any Firestore annotations.
 */
data class SharedShelf(
    val shareCode: String,
    val ownerId: String,
    val shelfId: String,
    val shelfName: String,
    val subscriberIds: List<String>,
    val createdAt: Long?,
    val bookCount: Int
)
