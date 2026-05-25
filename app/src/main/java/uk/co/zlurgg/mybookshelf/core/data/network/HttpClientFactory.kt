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

// URL query parameter names that carry secrets. To add a new provider whose
// credential rides in the URL, append its parameter name here.
private val SENSITIVE_URL_PARAMS = listOf("key")

// Header names whose values must be redacted from logs. To add a new provider
// whose credential rides in a header, append its header name here.
private val SENSITIVE_HEADER_NAMES = listOf("X-Goog-Api-Key")

private val URL_PARAM_PATTERN = Regex(
    "([?&])(${SENSITIVE_URL_PARAMS.joinToString("|")})=[^&\\s]*",
    RegexOption.IGNORE_CASE,
)

private val SENSITIVE_HEADER_PATTERNS: List<Regex> = SENSITIVE_HEADER_NAMES.map { name ->
    Regex("(${Regex.escape(name)}:\\s*)\\S+", RegexOption.IGNORE_CASE)
}

/**
 * Strips credential values from a single log line. Covers two leak vectors:
 *  - URL query parameters (e.g. `?key=…`) — defense in depth; the codebase
 *    currently sends Google Books credentials via header, not URL.
 *  - Sensitive request/response headers.
 *
 * To extend for a new provider, add the parameter name to SENSITIVE_URL_PARAMS
 * and/or the header name to SENSITIVE_HEADER_NAMES above. No other changes needed.
 */
internal fun redactSensitiveValues(message: String): String {
    var redacted = message.replace(URL_PARAM_PATTERN) { match ->
        "${match.groupValues[1]}${match.groupValues[2]}=REDACTED"
    }
    SENSITIVE_HEADER_PATTERNS.forEach { pattern ->
        redacted = redacted.replace(pattern, "\$1REDACTED")
    }
    return redacted
}

object HttpClientFactory {

    private const val MAX_RETRIES = 3

    fun create(engine: HttpClientEngine, enableLogging: Boolean = false): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        ignoreUnknownKeys = true
                    }
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
                    // Retry on server errors (5xx) only.
                    // 429 not retried — Google Books returns 429 for daily quota exhaustion (not transient).
                    // FallbackRemoteBookDataSource handles this by switching providers.
                    httpResponse.status == HttpStatusCode.InternalServerError ||
                        httpResponse.status == HttpStatusCode.BadGateway ||
                        httpResponse.status == HttpStatusCode.ServiceUnavailable ||
                        httpResponse.status == HttpStatusCode.GatewayTimeout
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
                    maxDelayMs = ApiConfig.Http.MAX_RETRY_DELAY_MS
                )
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println(redactSensitiveValues(message))
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
