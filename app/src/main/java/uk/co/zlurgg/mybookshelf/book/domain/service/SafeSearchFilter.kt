package uk.co.zlurgg.mybookshelf.book.domain.service

import uk.co.zlurgg.mybookshelf.book.domain.model.Book

object SafeSearchFilter {

    private val blockedKeywords = listOf(
        // Core explicit content
        "erotica", "erotic", "pornograph", "sexual",
        "bdsm", "bondage", "smut", "fetish", "kink",
        // BDSM / violence-adjacent
        "sadomaso", "masochism", "sadism", "flagellation",
        // Illegal / extreme
        "pedophil", "molest", "bestiality", "hentai",
        // Broader adult content
        "immoral", "prostitut", "dominatrix", "orgasm", "orgy"
    )

    fun isBookSafe(book: Book): Boolean {
        if (containsBlockedKeyword(book.title)) return false
        return book.subjects.none { containsBlockedKeyword(it) }
    }

    private fun containsBlockedKeyword(text: String): Boolean {
        val lower = text.lowercase()
        return blockedKeywords.any { keyword -> lower.contains(keyword) }
    }
}
