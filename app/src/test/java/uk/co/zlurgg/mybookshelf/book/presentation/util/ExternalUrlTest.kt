package uk.co.zlurgg.mybookshelf.book.presentation.util

import androidx.compose.ui.platform.UriHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalUrlTest {

    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun `allowlisted https Google Books URL is opened`() {
        val handler = RecordingUriHandler()

        openExternalUrl(handler, "https://books.google.com/books?id=abc")

        assertEquals(listOf("https://books.google.com/books?id=abc"), handler.opened)
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

            openExternalUrl(handler, url)

            assertTrue("URL '$url' should not be opened", handler.opened.isEmpty())
        }
    }

    @Test
    fun `isAllowedExternalUrl accepts only books_google_com https URLs`() {
        assertTrue(isAllowedExternalUrl("https://books.google.com/"))
        assertTrue(isAllowedExternalUrl("https://books.google.com/books?id=abc"))

        assertFalse(isAllowedExternalUrl("http://books.google.com/"))
        assertFalse(isAllowedExternalUrl("https://play.google.com/"))
        assertFalse(isAllowedExternalUrl("https://evil.com/books.google.com/"))
        assertFalse(isAllowedExternalUrl(""))
    }
}
