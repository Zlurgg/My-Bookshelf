package uk.co.zlurgg.mybookshelf.core.util

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Utility for encoding/decoding strings with GZip compression and Base64 encoding.
 *
 * This is specifically designed for encoding bookshelf data into URL-safe strings
 * that can be embedded in share links. The two-step process:
 * 1. GZip compression (reduces JSON size by 70-80%)
 * 2. URL-safe Base64 encoding (avoids problematic URL characters)
 *
 * This allows typical 5-book shelves to fit under 2KB URL length limits.
 *
 * Security: Decompression has a 10MB size limit to prevent ZIP bomb attacks.
 */
object Base64Encoder {

    private const val MAX_DECOMPRESSED_SIZE = 10 * 1024 * 1024 // 10MB

    /**
     * Encodes a string with GZip compression and URL-safe Base64 encoding.
     *
     * @param data The string to encode (typically JSON data)
     * @return URL-safe Base64 encoded string
     * @throws Exception if compression or encoding fails
     */
    fun encode(data: String): String {
        // Step 1: GZip compression (70-80% size reduction)
        val compressed = ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { gzip ->
                gzip.write(data.toByteArray(Charsets.UTF_8))
            }
            bos.toByteArray()
        }

        // Step 2: URL-safe Base64 encoding
        return android.util.Base64.encodeToString(
            compressed,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )
    }

    /**
     * Decodes a Base64 encoded and GZip compressed string back to the original.
     *
     * Security: Enforces a 10MB decompression limit to prevent ZIP bomb attacks.
     *
     * @param encoded The URL-safe Base64 encoded string
     * @return The original uncompressed string
     * @throws IllegalArgumentException if Base64 decoding fails or size limit exceeded
     * @throws Exception if GZip decompression fails
     */
    fun decode(encoded: String): String {
        // Step 1: Base64 decode
        val compressed = android.util.Base64.decode(
            encoded,
            android.util.Base64.URL_SAFE
        )

        // Step 2: GZip decompression with size limit (ZIP bomb protection)
        return GZIPInputStream(compressed.inputStream()).use { gzip ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var totalRead = 0
            var bytesRead: Int

            while (gzip.read(chunk).also { bytesRead = it } != -1) {
                totalRead += bytesRead

                // Check size limit before writing
                if (totalRead > MAX_DECOMPRESSED_SIZE) {
                    throw IllegalArgumentException(
                        "Decompressed data exceeds $MAX_DECOMPRESSED_SIZE byte limit (potential ZIP bomb)"
                    )
                }

                buffer.write(chunk, 0, bytesRead)
            }

            buffer.toByteArray().toString(Charsets.UTF_8)
        }
    }
}
