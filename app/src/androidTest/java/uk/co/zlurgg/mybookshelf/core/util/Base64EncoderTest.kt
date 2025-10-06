package uk.co.zlurgg.mybookshelf.core.util

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Base64EncoderTest {

    @Test
    fun `encode simple string and decode back returns original`() {
        // Given
        val original = "Hello, World!"

        // When
        val encoded = Base64Encoder.encode(original)
        val decoded = Base64Encoder.decode(encoded)

        // Then
        assertEquals(original, decoded)
    }

    @Test
    fun `encode empty string returns valid Base64`() {
        // Given
        val original = ""

        // When
        val encoded = Base64Encoder.encode(original)
        val decoded = Base64Encoder.decode(encoded)

        // Then
        assertEquals(original, decoded)
        assertTrue(encoded.isNotEmpty()) // GZip header still produces output
    }

    @Test
    fun `encode JSON data and decode back returns original`() {
        // Given
        val jsonData = """
            {
              "formatVersion": 1,
              "exportedAt": "2025-10-01T16:29:00Z",
              "appName": "My Bookshelf",
              "bookshelf": {
                "name": "Test Shelf",
                "shelfStyle": "OAK",
                "books": [
                  {
                    "id": "OL123456W",
                    "title": "Test Book",
                    "authors": ["Test Author"],
                    "imageUrl": "https://example.com/image.jpg",
                    "description": "Test description",
                    "languages": ["eng"],
                    "firstPublishYear": "2024",
                    "averageRating": 4.5,
                    "ratingCount": 100,
                    "numPages": 300,
                    "numEditions": 10,
                    "purchased": false,
                    "spineColor": -8355712
                  }
                ]
              }
            }
        """.trimIndent()

        // When
        val encoded = Base64Encoder.encode(jsonData)
        val decoded = Base64Encoder.decode(encoded)

        // Then
        assertEquals(jsonData, decoded)
    }

    @Test
    fun `encode large JSON data and decode back returns original`() {
        // Given - 5 books similar to URL_ENCODING_PLAN example
        val largeJson = buildString {
            append("""{"formatVersion":1,"exportedAt":"2025-10-01T16:29:00Z","appName":"My Bookshelf","bookshelf":{"name":"Large Shelf","shelfStyle":"OAK","books":[""")
            repeat(5) { i ->
                if (i > 0) append(",")
                append("""{"id":"OL${i}W","title":"Book Title $i","authors":["Author $i"],"imageUrl":"https://covers.openlibrary.org/b/id/$i-L.jpg","description":"Description for book $i","languages":["eng"],"firstPublishYear":"2024","averageRating":4.5,"ratingCount":1000,"numPages":300,"numEditions":50,"purchased":false,"spineColor":-8355712}""")
            }
            append("]}}")
        }

        // When
        val encoded = Base64Encoder.encode(largeJson)
        val decoded = Base64Encoder.decode(encoded)

        // Then
        assertEquals(largeJson, decoded)
    }

    @Test
    fun `encode produces URL-safe characters only`() {
        // Given
        val testData = "Test data with special chars: +/=[]{}()"

        // When
        val encoded = Base64Encoder.encode(testData)

        // Then
        assertFalse("Encoded string should not contain '+'", encoded.contains('+'))
        assertFalse("Encoded string should not contain '/'", encoded.contains('/'))
        // Note: URL_SAFE with NO_WRAP may still produce '=' padding in Robolectric
        // The important part is no '+' or '/' which would break URLs

        // Verify it decodes correctly (most important test)
        assertEquals(testData, Base64Encoder.decode(encoded))
    }

    @Test
    fun `encode with Unicode characters preserves data`() {
        // Given
        val unicodeData = "Test with émojis 😀🎉 and spëcial chäracters: 日本語, 中文, العربية"

        // When
        val encoded = Base64Encoder.encode(unicodeData)
        val decoded = Base64Encoder.decode(encoded)

        // Then
        assertEquals(unicodeData, decoded)
    }

    @Test
    fun `encode compresses data significantly`() {
        // Given - Highly repetitive data (very compressible)
        val repetitiveData = "AAAAAAAA".repeat(100) // 800 bytes of repeated 'A'

        // When
        val encoded = Base64Encoder.encode(repetitiveData)

        // Then
        // GZip should compress 800 bytes of 'A' to <100 bytes
        // Base64 expands by ~33%, so expect <133 chars
        assertTrue(
            "Compressed + Base64 should be much smaller than original: ${encoded.length} vs ${repetitiveData.length}",
            encoded.length < repetitiveData.length / 4
        )
    }

    @Test
    fun `encode JSON with pretty print vs minified shows compression benefit`() {
        // Given
        val minifiedJson = """{"name":"Test","books":[{"title":"Book1"}]}"""
        val prettyJson = """
            {
              "name": "Test",
              "books": [
                {
                  "title": "Book1"
                }
              ]
            }
        """.trimIndent()

        // When
        val encodedMinified = Base64Encoder.encode(minifiedJson)
        val encodedPretty = Base64Encoder.encode(prettyJson)

        // Then
        // Both should decode correctly
        assertEquals(minifiedJson, Base64Encoder.decode(encodedMinified))
        assertEquals(prettyJson, Base64Encoder.decode(encodedPretty))

        // Minified should produce shorter encoded output
        assertTrue(
            "Minified JSON should produce shorter Base64: ${encodedMinified.length} vs ${encodedPretty.length}",
            encodedMinified.length < encodedPretty.length
        )
    }

    @Test(expected = Exception::class)
    fun `decode invalid Base64 throws exception`() {
        // Given
        val invalidBase64 = "This is not valid Base64!@#$%"

        // When
        Base64Encoder.decode(invalidBase64)

        // Then - exception should be thrown (either IllegalArgumentException or ZipException)
    }

    @Test(expected = Exception::class)
    fun `decode corrupted GZip data throws exception`() {
        // Given - Valid Base64 but not valid GZip
        val notGZipData = android.util.Base64.encodeToString(
            "Not GZip data".toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
        )

        // When
        Base64Encoder.decode(notGZipData)

        // Then - exception should be thrown
    }

    @Test
    fun `encode then decode multiple times produces consistent results`() {
        // Given
        val original = "Test data for consistency"

        // When
        val encoded1 = Base64Encoder.encode(original)
        val encoded2 = Base64Encoder.encode(original)
        val decoded1 = Base64Encoder.decode(encoded1)
        val decoded2 = Base64Encoder.decode(encoded2)

        // Then
        assertEquals(encoded1, encoded2)
        assertEquals(decoded1, decoded2)
        assertEquals(original, decoded1)
    }

    @Test
    fun `realistic 5-book shelf stays under 2KB URL limit`() {
        // Given - Realistic 5-book shelf JSON (minified)
        val fiveBookShelf = buildString {
            append("""{"formatVersion":1,"exportedAt":"2025-10-01T16:29:00Z","appName":"My Bookshelf","bookshelf":{"name":"My Reading List","shelfStyle":"WALNUT","books":[""")
            repeat(5) { i ->
                if (i > 0) append(",")
                append("""{"id":"OL${100000 + i}W","title":"The Lord of the Rings Volume $i","authors":["J.R.R. Tolkien","Christopher Tolkien"],"imageUrl":"https://covers.openlibrary.org/b/id/12345678-L.jpg","description":"Epic fantasy novel that changed the genre forever. A masterpiece of world-building and storytelling.","languages":["eng"],"firstPublishYear":"1954","averageRating":4.52,"ratingCount":125000,"numPages":1178,"numEditions":500,"purchased":false,"spineColor":-8355712}""")
            }
            append("]}}")
        }

        // When
        val encoded = Base64Encoder.encode(fiveBookShelf)
        val fullUrl = "https://zlurgg.github.io/My-Bookshelf/share/?name=My+Reading+List#$encoded"

        // Then
        println("5-book shelf JSON size: ${fiveBookShelf.length} bytes")
        println("Encoded Base64 size: ${encoded.length} bytes")
        println("Full URL length: ${fullUrl.length} chars")

        assertTrue(
            "Full URL should be under 2000 chars for browser compatibility: ${fullUrl.length}",
            fullUrl.length < 2000
        )

        // Verify decoding works
        assertEquals(fiveBookShelf, Base64Encoder.decode(encoded))
    }
}
