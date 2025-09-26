package uk.co.zlurgg.mybookshelf.core.data.image

import android.content.Context
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

object ImageLoaderFactory {
    fun create(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = HttpClient(Android) {
                            install(HttpTimeout) {
                                connectTimeoutMillis = 5_000L
                                requestTimeoutMillis = 10_000L
                                socketTimeoutMillis = 10_000L
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