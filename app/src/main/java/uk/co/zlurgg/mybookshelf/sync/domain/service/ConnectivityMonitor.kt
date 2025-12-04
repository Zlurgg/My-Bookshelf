package uk.co.zlurgg.mybookshelf.sync.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * Service interface for monitoring network connectivity.
 *
 * This is in the domain layer - implementations provide platform-specific behavior.
 */
interface ConnectivityMonitor {

    /**
     * Checks if the device currently has network connectivity.
     *
     * @return true if connected to network
     */
    fun isConnected(): Boolean

    /**
     * Observes connectivity changes.
     *
     * @return Flow that emits true when connected, false when disconnected
     */
    fun observeConnectivity(): Flow<Boolean>
}
