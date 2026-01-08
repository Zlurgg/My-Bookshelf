package uk.co.zlurgg.mybookshelf.sync.domain.model

/**
 * Represents the result of a sync operation.
 */
data class SyncResult(
    /** Number of entities pushed to cloud */
    val pushedCount: Int = 0,
    /** Number of entities pulled from cloud */
    val pulledCount: Int = 0,
    /** Number of conflicts detected */
    val conflictCount: Int = 0,
    /** Number of conflicts auto-resolved */
    val resolvedCount: Int = 0,
    /** Number of entities deleted */
    val deletedCount: Int = 0,
    /** IDs of entities with unresolved conflicts */
    val unresolvedConflictIds: List<String> = emptyList(),
    /** Timestamp when sync completed */
    val completedAt: Long = 0L,
) {
    /** Total number of changes processed */
    val totalChanges: Int
        get() = pushedCount + pulledCount + deletedCount

    /** Whether sync completed without unresolved conflicts */
    val isFullyResolved: Boolean
        get() = unresolvedConflictIds.isEmpty()

    companion object {
        val EMPTY = SyncResult()
    }
}
