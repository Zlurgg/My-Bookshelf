package uk.co.zlurgg.mybookshelf.core.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

/**
 * Factory for creating Coil ImageLoader with optimized settings for book cover loading.
 *
 * Timeout Strategy:
 * - OpenLibrary redirects all cover requests to Archive.org CDN
 * - Archive.org has unreliable performance (some images load <1s, others timeout >20s)
 * - Increased timeouts give slow Archive.org responses more time to complete
 * - See IMAGE_LOADING_INVESTIGATION.md for detailed analysis
 */
object ImageLoaderFactory {
    // Timeout values for image loading (increased for slow Archive.org CDN)
    private const val CONNECT_TIMEOUT_MS = 10_000L
    private const val REQUEST_TIMEOUT_MS = 30_000L
    private const val SOCKET_TIMEOUT_MS = 30_000L

    fun create(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient =
                            HttpClient(Android) {
                                install(HttpTimeout) {
                                    connectTimeoutMillis = CONNECT_TIMEOUT_MS
                                    requestTimeoutMillis = REQUEST_TIMEOUT_MS
                                    socketTimeoutMillis = SOCKET_TIMEOUT_MS
                                }
                            },
                    ),
                )
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
