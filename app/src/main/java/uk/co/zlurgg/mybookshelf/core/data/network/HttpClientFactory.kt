package uk.co.zlurgg.mybookshelf.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    private const val MAX_RETRIES = 3

    fun create(
        engine: HttpClientEngine,
        enableLogging: Boolean = false,
    ): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    json =
                        Json {
                            ignoreUnknownKeys = true
                        },
                )
            }
            install(HttpTimeout) {
                socketTimeoutMillis = ApiConfig.Http.socketTimeout
                requestTimeoutMillis = ApiConfig.Http.requestTimeout
                connectTimeoutMillis = ApiConfig.Http.connectTimeout
            }

            install(HttpRequestRetry) {
                maxRetries = MAX_RETRIES
                retryIf { _, httpResponse ->
                    // Retry on server errors (5xx) and rate limiting (429)
                    httpResponse.status == HttpStatusCode.InternalServerError ||
                        httpResponse.status == HttpStatusCode.BadGateway ||
                        httpResponse.status == HttpStatusCode.ServiceUnavailable ||
                        httpResponse.status == HttpStatusCode.GatewayTimeout ||
                        httpResponse.status == HttpStatusCode.TooManyRequests
                }
                retryOnExceptionIf { _, cause ->
                    // Retry on network-related exceptions
                    cause is java.net.SocketTimeoutException ||
                        cause is java.net.UnknownHostException ||
                        cause is java.net.ConnectException ||
                        cause is io.ktor.client.network.sockets.SocketTimeoutException ||
                        cause is io.ktor.util.network.UnresolvedAddressException
                }
                exponentialDelay(
                    base = 1.0,
                    maxDelayMs = 10_000L,
                )
            }
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            println(message)
                        }
                    }
                level = if (enableLogging) LogLevel.ALL else LogLevel.NONE
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
                userAgent(ApiConfig.Http.USER_AGENT)
            }
        }
    }
}
