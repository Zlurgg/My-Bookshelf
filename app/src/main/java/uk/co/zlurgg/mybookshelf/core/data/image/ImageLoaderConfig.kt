package uk.co.zlurgg.mybookshelf.core.data.image

/**
 * Configuration constants for image loading.
 *
 * Timeout Strategy:
 * - OpenLibrary redirects all cover requests to Archive.org CDN
 * - Archive.org has unreliable performance (some images load <1s, others timeout >20s)
 * - These increased timeouts give slow Archive.org responses more time to complete
 * - See IMAGE_LOADING_INVESTIGATION.md for detailed analysis
 */
object ImageLoaderConfig {
    /** Connection timeout - time to establish connection (10 seconds) */
    const val CONNECT_TIMEOUT_MS = 10_000L

    /** Request timeout - total time for request completion (30 seconds) */
    const val REQUEST_TIMEOUT_MS = 30_000L

    /** Socket timeout - time between data packets (30 seconds) */
    const val SOCKET_TIMEOUT_MS = 30_000L
}
