package uk.co.zlurgg.mybookshelf.sync.domain.service

import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictStrategy
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncConflict

/**
 * Service interface for resolving sync conflicts.
 *
 * This is in the domain layer - implementations should not have Android dependencies.
 */
interface ConflictResolver {

    /**
     * The current conflict resolution strategy.
     */
    val strategy: ConflictStrategy

    /**
     * Resolves a conflict based on the current strategy.
     *
     * @param conflict The conflict to resolve
     * @return The resolution to apply, or null if manual resolution is required
     */
    fun resolve(conflict: SyncConflict): ConflictResolution?

    /**
     * Checks if a conflict can be auto-resolved with the current strategy.
     *
     * @param conflict The conflict to check
     * @return true if the conflict can be auto-resolved
     */
    fun canAutoResolve(conflict: SyncConflict): Boolean
}
