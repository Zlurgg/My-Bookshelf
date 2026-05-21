package uk.co.zlurgg.mybookshelf.book.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder

class SafeSearchFilterTest {

    @Test
    fun `book with no subjects is safe`() {
        val book = TestBookBuilder().withSubjects(emptyList()).build()
        assertTrue(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `book with erotic fiction subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Erotic fiction")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `book with pornography subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Amateur pornography")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `book with science fiction subject is safe`() {
        val book = TestBookBuilder().withSubjects(listOf("Science fiction")).build()
        assertTrue(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `case insensitivity - EROTICA is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("EROTICA")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `case insensitivity - eRoTiC is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("eRoTiC literature")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `mixed safe and unsafe subjects - one bad subject blocks the book`() {
        val book = TestBookBuilder()
            .withSubjects(listOf("Fiction", "Romance", "Erotica", "Literature"))
            .build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `adult education is safe - not in blocklist`() {
        val book = TestBookBuilder().withSubjects(listOf("Adult education")).build()
        assertTrue(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `bdsm subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("BDSM literature")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `smut subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Smut")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `sexual content subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Sexual content")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `all safe subjects pass filter`() {
        val book = TestBookBuilder()
            .withSubjects(listOf("Fiction", "Science", "History", "Biography"))
            .build()
        assertTrue(SafeSearchFilter.isBookSafe(book))
    }

    // New keyword coverage

    @Test
    fun `sadomasochism subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Sadomasochism")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `spanish sadomasoquismo is blocked via sadomaso prefix`() {
        val book = TestBookBuilder().withSubjects(listOf("Sadomasoquismo")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `masochism subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Masochism")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `sadism subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Sadism")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `hentai subject is blocked`() {
        val book = TestBookBuilder()
            .withSubjects(listOf("Comics & graphic novels, east asian style, manga, erotica & hentai"))
            .build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `bestiality subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("bestiality")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `pedophilia subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Pedophilia")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `child molesters subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Child molesters")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `flagellation subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Flagellation")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `immoral literature subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Immoral Literature")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `prostitution subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Prostitution")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `dominatrix subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("Dominatrix")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `orgasm subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("orgasm")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }

    @Test
    fun `orgy subject is blocked`() {
        val book = TestBookBuilder().withSubjects(listOf("orgy")).build()
        assertFalse(SafeSearchFilter.isBookSafe(book))
    }
}
