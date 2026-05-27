package uk.co.zlurgg.mybookshelf.book.presentation.util

import androidx.compose.ui.platform.UriHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleBooksUrlTest {

    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun `allowlisted https Google Books URL is opened`() {
        val handler = RecordingUriHandler()

        openGoogleBooksUrl(handler, "https://books.google.com/books?id=abc")

        assertEquals(listOf("https://books.google.com/books?id=abc"), handler.opened)
    }

    @Test
    fun `allowlisted Play Books URL is opened`() {
        val handler = RecordingUriHandler()
        val url = "https://play.google.com/store/books/details?id=3ZDVEAAAQBAJ&source=gbs_api"

        openGoogleBooksUrl(handler, url)

        assertEquals(listOf(url), handler.opened)
    }

    @Test
    fun `hostile schemes are not opened`() {
        val hostile = listOf(
            "http://books.google.com/preview",
            "https://evil.example.com/",
            "mailto:attacker@example.com",
            "intent://books.google.com/preview#Intent;scheme=https;end",
            "javascript:alert(1)",
            "tel:+15551234567",
            "",
        )

        hostile.forEach { url ->
            val handler = RecordingUriHandler()

            openGoogleBooksUrl(handler, url)

            assertTrue("URL '$url' should not be opened", handler.opened.isEmpty())
        }
    }

    @Test
    fun `isPlayBooksUrl classifies Play Books store links`() {
        assertTrue(isPlayBooksUrl("https://play.google.com/store/books/details?id=abc"))

        assertFalse(isPlayBooksUrl("https://books.google.com/books?id=abc"))
        assertFalse(isPlayBooksUrl("https://play.google.com/store/apps/details?id=evil"))
        assertFalse(isPlayBooksUrl(""))
    }

    @Test
    fun `isAllowedGoogleBooksUrl accepts books_google_com and play store books https URLs`() {
        assertTrue(isAllowedGoogleBooksUrl("https://books.google.com/"))
        assertTrue(isAllowedGoogleBooksUrl("https://books.google.com/books?id=abc"))
        assertTrue(isAllowedGoogleBooksUrl("https://play.google.com/store/books/details?id=abc"))

        assertFalse(isAllowedGoogleBooksUrl("http://books.google.com/"))
        assertFalse(isAllowedGoogleBooksUrl("https://play.google.com/"))
        assertFalse(isAllowedGoogleBooksUrl("https://play.google.com/store/apps/details?id=evil"))
        assertFalse(isAllowedGoogleBooksUrl("https://play.google.com/store/movies/details?id=abc"))
        assertFalse(isAllowedGoogleBooksUrl("https://evil.com/books.google.com/"))
        assertFalse(isAllowedGoogleBooksUrl(""))
    }
}
