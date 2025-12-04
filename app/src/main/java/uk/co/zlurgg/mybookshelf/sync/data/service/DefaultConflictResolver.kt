package uk.co.zlurgg.mybookshelf.sync.data.service

import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictResolution
import uk.co.zlurgg.mybookshelf.sync.domain.model.ConflictStrategy
import uk.co.zlurgg.mybookshelf.sync.domain.model.SyncConflict
import uk.co.zlurgg.mybookshelf.sync.domain.service.ConflictResolver

/**
 * Default implementation of ConflictResolver.
 *
 * Uses a configurable strategy for automatic conflict resolution.
 * Default strategy is LAST_WRITE_WINS.
 */
class DefaultConflictResolver(
    override val strategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS
) : ConflictResolver {

    override fun resolve(conflict: SyncConflict): ConflictResolution? {
        return when (strategy) {
            ConflictStrategy.LOCAL_WINS -> ConflictResolution.KeepLocal
            ConflictStrategy.REMOTE_WINS -> ConflictResolution.KeepRemote
            ConflictStrategy.LAST_WRITE_WINS -> resolveByTimestamp(conflict)
            ConflictStrategy.ASK_USER -> null // Requires manual resolution
        }
    }

    override fun canAutoResolve(conflict: SyncConflict): Boolean {
        return strategy != ConflictStrategy.ASK_USER
    }

    /**
     * Resolves conflict using last-write-wins strategy.
     * Compares timestamps and keeps the version with the later modification time.
     */
    private fun resolveByTimestamp(conflict: SyncConflict): ConflictResolution {
        return when {
            conflict.localTimestamp > conflict.remoteTimestamp -> ConflictResolution.KeepLocal
            conflict.remoteTimestamp > conflict.localTimestamp -> ConflictResolution.KeepRemote
            // If timestamps are equal, prefer local (user's changes)
            else -> ConflictResolution.KeepLocal
        }
    }

    companion object {
        /**
         * Creates a resolver that always keeps local changes.
         */
        fun localWins(): DefaultConflictResolver =
            DefaultConflictResolver(ConflictStrategy.LOCAL_WINS)

        /**
         * Creates a resolver that always keeps remote changes.
         */
        fun remoteWins(): DefaultConflictResolver =
            DefaultConflictResolver(ConflictStrategy.REMOTE_WINS)

        /**
         * Creates a resolver that uses last-write-wins (default).
         */
        fun lastWriteWins(): DefaultConflictResolver =
            DefaultConflictResolver(ConflictStrategy.LAST_WRITE_WINS)

        /**
         * Creates a resolver that requires manual resolution.
         */
        fun askUser(): DefaultConflictResolver =
            DefaultConflictResolver(ConflictStrategy.ASK_USER)
    }
}
