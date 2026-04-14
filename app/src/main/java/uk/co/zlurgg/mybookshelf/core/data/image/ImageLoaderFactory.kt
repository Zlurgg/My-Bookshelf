package uk.co.zlurgg.mybookshelf.core.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

/**
 * Factory for creating Coil ImageLoader with optimized settings for book cover loading.
 *
 * @see ImageLoaderConfig for timeout configuration and rationale
 */
object ImageLoaderFactory {
    @OptIn(ExperimentalCoilApi::class)
    fun create(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = HttpClient(Android) {
                            install(HttpTimeout) {
                                connectTimeoutMillis = ImageLoaderConfig.CONNECT_TIMEOUT_MS
                                requestTimeoutMillis = ImageLoaderConfig.REQUEST_TIMEOUT_MS
                                socketTimeoutMillis = ImageLoaderConfig.SOCKET_TIMEOUT_MS
                            }
                        }
                    )
                )
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
