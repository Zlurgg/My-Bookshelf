package uk.co.zlurgg.mybookshelf.core.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the log-redaction helper used by HttpClientFactory's Logging plugin.
 * Extension contract: adding new provider credentials should be a one-line change
 * in HttpClientFactory.kt (SENSITIVE_URL_PARAMS or SENSITIVE_HEADER_NAMES) and the
 * existing redactor handles them automatically. These tests pin that behavior.
 */
class HttpClientFactoryTest {

    @Test
    fun `redactSensitiveValues strips X-Goog-Api-Key header value`() {
        val input = "-> X-Goog-Api-Key: AIzaSyExampleSecretValueXYZ"
        assertEquals("-> X-Goog-Api-Key: REDACTED", redactSensitiveValues(input))
    }

    @Test
    fun `redactSensitiveValues is case-insensitive on header name`() {
        val input = "x-goog-api-key: AIzaSyAnother"
        assertEquals("x-goog-api-key: REDACTED", redactSensitiveValues(input))
    }

    @Test
    fun `redactSensitiveValues strips key URL parameter when first`() {
        val input = "GET https://example.com/v1/volumes?key=AIzaSyFoo&q=test"
        assertEquals(
            "GET https://example.com/v1/volumes?key=REDACTED&q=test",
            redactSensitiveValues(input),
        )
    }

    @Test
    fun `redactSensitiveValues strips key URL parameter when not first`() {
        val input = "GET https://example.com/v1/volumes?q=test&key=AIzaSyBar"
        assertEquals(
            "GET https://example.com/v1/volumes?q=test&key=REDACTED",
            redactSensitiveValues(input),
        )
    }

    @Test
    fun `redactSensitiveValues leaves messages without sensitive values untouched`() {
        val input = "HTTP/1.1 200 OK\nContent-Type: application/json"
        assertEquals(input, redactSensitiveValues(input))
    }

    @Test
    fun `redactSensitiveValues redacts both URL and header forms in the same message`() {
        val input = """
            GET https://example.com/v1/volumes?key=AIzaSyURL
            -> X-Goog-Api-Key: AIzaSyHEADER
        """.trimIndent()
        val expected = """
            GET https://example.com/v1/volumes?key=REDACTED
            -> X-Goog-Api-Key: REDACTED
        """.trimIndent()
        assertEquals(expected, redactSensitiveValues(input))
    }

    @Test
    fun `redactSensitiveValues does not match unrelated query parameters containing 'key'`() {
        val input = "GET https://example.com/v1/search?keyword=cooking"
        assertEquals(input, redactSensitiveValues(input))
    }
}
