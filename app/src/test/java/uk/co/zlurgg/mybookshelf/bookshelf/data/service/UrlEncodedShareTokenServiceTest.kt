package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

@RunWith(RobolectricTestRunner::class)
class UrlEncodedShareTokenServiceTest {
    private val service = UrlEncodedShareTokenService()

    @Test
    fun `generateToken with simple JSON returns success with encoded string`() =
        runTest {
            // Given
            val simpleJson = """{"name":"Test"}"""

            // When
            val result = service.generateToken(simpleJson)

            // Then
            assertTrue("Result should be success", result is Result.Success)
            val token = (result as Result.Success).data
            assertNotNull("Token should not be null", token)
            assertTrue("Token should not be empty", token.isNotEmpty())
        }

    @Test
    fun `generateToken with realistic bookshelf JSON returns success`() =
        runTest {
            // Given
            val bookshelfJson =
                """
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
            val result = service.generateToken(bookshelfJson)

            // Then
            assertTrue("Result should be success", result is Result.Success)
            val token = (result as Result.Success).data
            assertTrue(
                "Token should be shorter than original JSON due to compression",
                token.length < bookshelfJson.length,
            )
        }

    @Test
    fun `getShelfDataByToken with valid token returns original JSON`() =
        runTest {
            // Given
            val originalJson = """{"name":"Test Shelf","books":[{"title":"Book1"}]}"""

            // When
            val generateResult = service.generateToken(originalJson)
            assertTrue("Generate should succeed", generateResult is Result.Success)
            val token = (generateResult as Result.Success).data

            val retrieveResult = service.getShelfDataByToken(token)

            // Then
            assertTrue("Retrieve should succeed", retrieveResult is Result.Success)
            val retrievedJson = (retrieveResult as Result.Success).data
            assertEquals("Retrieved JSON should match original", originalJson, retrievedJson)
        }

    @Test
    fun `getShelfDataByToken with invalid token returns error`() =
        runTest {
            // Given
            val invalidToken = "This is not a valid Base64-encoded GZip token!@#$%"

            // When
            val result = service.getShelfDataByToken(invalidToken)

            // Then
            assertTrue("Result should be error", result is Result.Error)
        }

    @Test
    fun `getShelfDataByToken with corrupted token returns error`() =
        runTest {
            // Given - Valid Base64 but not valid GZip data
            val corruptedToken =
                android.util.Base64.encodeToString(
                    "Not GZip data".toByteArray(),
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
                )

            // When
            val result = service.getShelfDataByToken(corruptedToken)

            // Then
            assertTrue("Result should be error", result is Result.Error)
        }

    @Test
    fun `generateToken with empty string returns success`() =
        runTest {
            // Given
            val emptyJson = ""

            // When
            val result = service.generateToken(emptyJson)

            // Then
            assertTrue("Result should be success", result is Result.Success)
            val token = (result as Result.Success).data
            assertTrue("Token should not be empty (GZip header)", token.isNotEmpty())
        }

    @Test
    fun `getShelfDataByToken with empty token returns error`() =
        runTest {
            // Given
            val emptyToken = ""

            // When
            val result = service.getShelfDataByToken(emptyToken)

            // Then
            assertTrue("Result should be error", result is Result.Error)
        }

    @Test
    fun `cleanupExpiredTokens always returns success`() =
        runTest {
            // Given - no setup needed

            // When
            val result = service.cleanupExpiredTokens()

            // Then
            assertTrue("Result should be success (no-op)", result is Result.Success)
        }

    @Test
    fun `round trip with Unicode characters preserves data`() =
        runTest {
            // Given
            val jsonWithUnicode = """{"title":"日本語 書籍","author":"村上春樹"}"""

            // When
            val generateResult = service.generateToken(jsonWithUnicode)
            assertTrue("Generate should succeed", generateResult is Result.Success)
            val token = (generateResult as Result.Success).data

            val retrieveResult = service.getShelfDataByToken(token)

            // Then
            assertTrue("Retrieve should succeed", retrieveResult is Result.Success)
            val retrievedJson = (retrieveResult as Result.Success).data
            assertEquals("Unicode characters should be preserved", jsonWithUnicode, retrievedJson)
        }

    @Test
    fun `round trip with large bookshelf preserves data`() =
        runTest {
            // Given - 10 books
            val largeJson =
                buildString {
                    append("""{"formatVersion":1,"bookshelf":{"name":"Large","books":[""")
                    repeat(10) { i ->
                        if (i > 0) append(",")
                        append("""{"id":"OL${i}W","title":"Book $i","authors":["Author $i"]}""")
                    }
                    append("]}}")
                }

            // When
            val generateResult = service.generateToken(largeJson)
            assertTrue("Generate should succeed", generateResult is Result.Success)
            val token = (generateResult as Result.Success).data

            val retrieveResult = service.getShelfDataByToken(token)

            // Then
            assertTrue("Retrieve should succeed", retrieveResult is Result.Success)
            val retrievedJson = (retrieveResult as Result.Success).data
            assertEquals("Large JSON should be preserved", largeJson, retrievedJson)
        }

    @Test
    fun `generateToken produces consistent results for same input`() =
        runTest {
            // Given
            val json = """{"name":"Consistency Test"}"""

            // When
            val result1 = service.generateToken(json)
            val result2 = service.generateToken(json)

            // Then
            assertTrue("Both results should be success", result1 is Result.Success && result2 is Result.Success)
            val token1 = (result1 as Result.Success).data
            val token2 = (result2 as Result.Success).data
            assertEquals("Same input should produce same token", token1, token2)
        }

    @Test
    fun `token does not contain problematic URL characters`() =
        runTest {
            // Given
            val json = """{"name":"Test with special chars: +/=[]{}()"}"""

            // When
            val result = service.generateToken(json)

            // Then
            assertTrue("Result should be success", result is Result.Success)
            val token = (result as Result.Success).data
            assertFalse("Token should not contain '+'", token.contains('+'))
            assertFalse("Token should not contain '/'", token.contains('/'))
            // Note: '=' padding may exist but won't break URLs in hash fragments
        }
}
