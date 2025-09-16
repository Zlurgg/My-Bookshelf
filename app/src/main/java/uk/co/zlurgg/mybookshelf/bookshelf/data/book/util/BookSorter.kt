package uk.co.zlurgg.mybookshelf.bookshelf.data.book.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import kotlin.math.ln
import kotlin.math.max

class BookSorter {

    fun sortBooks(
        books: List<Book>,
        sortBy: BookSearchSort,
        originalQuery: String = ""
    ): List<Book> {
        return when (sortBy) {
            BookSearchSort.BEST_MATCH -> sortByBestMatch(books, originalQuery)
            BookSearchSort.HIGHEST_RATED -> sortByHighestRated(books)
            BookSearchSort.MOST_POPULAR -> sortByMostPopular(books)
            BookSearchSort.NEWEST -> sortByNewest(books)
            BookSearchSort.OLDEST -> sortByOldest(books)
            BookSearchSort.TITLE_A_Z -> sortByTitleAscending(books)
            BookSearchSort.TITLE_Z_A -> sortByTitleDescending(books)
            BookSearchSort.AUTHOR_A_Z -> sortByAuthorAscending(books)
            BookSearchSort.AUTHOR_Z_A -> sortByAuthorDescending(books)
        }
    }

    private fun sortByBestMatch(books: List<Book>, query: String): List<Book> {
        if (query.isBlank()) return books

        val queryLower = query.lowercase()

        return books.sortedByDescending { book ->
            val titleSimilarity = calculateTitleSimilarity(book.title, queryLower)
            val authorMatch = calculateAuthorMatch(book.authors, queryLower)
            val ratingsScore = calculateRatingsScore(book.averageRating, book.ratingCount)
            val popularityScore = calculatePopularityScore(book.ratingCount)

            // Weighted scoring: Title 40%, Author 20%, Ratings 25%, Popularity 15%
            (titleSimilarity * 0.4) + (authorMatch * 0.2) + (ratingsScore * 0.25) + (popularityScore * 0.15)
        }
    }

    private fun calculateTitleSimilarity(title: String, query: String): Double {
        val titleLower = title.lowercase()

        return when {
            titleLower == query -> 100.0 // Exact match
            titleLower.startsWith(query) -> 90.0 // Starts with query
            titleLower.contains(query) -> 70.0 // Contains query
            else -> {
                // Calculate Levenshtein distance for fuzzy matching
                val distance = levenshteinDistance(titleLower, query)
                val maxLength = max(titleLower.length, query.length)
                val similarity = (maxLength - distance).toDouble() / maxLength
                similarity * 50.0 // Scale to 0-50 range
            }
        }
    }

    private fun calculateAuthorMatch(authors: List<String>, query: String): Double {
        if (authors.isEmpty()) return 0.0

        val bestMatch = authors.maxOfOrNull { author ->
            val authorLower = author.lowercase()
            when {
                authorLower == query -> 100.0
                authorLower.startsWith(query) -> 80.0
                authorLower.contains(query) -> 60.0
                else -> 0.0
            }
        } ?: 0.0

        return bestMatch
    }

    private fun calculateRatingsScore(averageRating: Double?, ratingCount: Int?): Double {
        if (averageRating == null || ratingCount == null || ratingCount == 0) return 0.0

        // Weighted rating considering both average and count
        // Uses logarithmic scaling for rating count to prevent dominance
        val countWeight = ln(ratingCount.toDouble() + 1) / ln(1000.0) // Normalize to 0-1 range
        return (averageRating / 5.0) * 100.0 * (0.7 + 0.3 * countWeight)
    }

    private fun calculatePopularityScore(ratingCount: Int?): Double {
        if (ratingCount == null || ratingCount == 0) return 0.0

        // Logarithmic scaling for popularity
        return ln(ratingCount.toDouble() + 1) / ln(10000.0) * 100.0
    }

    private fun sortByHighestRated(books: List<Book>): List<Book> {
        return books.sortedWith { a, b ->
            val ratingA = a.averageRating ?: 0.0
            val ratingB = b.averageRating ?: 0.0

            when {
                ratingB != ratingA -> ratingB.compareTo(ratingA)
                else -> (b.ratingCount ?: 0).compareTo(a.ratingCount ?: 0) // Tie-breaker: more ratings
            }
        }
    }

    private fun sortByMostPopular(books: List<Book>): List<Book> {
        return books.sortedByDescending { it.ratingCount ?: 0 }
    }

    private fun sortByNewest(books: List<Book>): List<Book> {
        return books.sortedWith { a, b ->
            val yearA = a.firstPublishYear?.toIntOrNull() ?: Int.MIN_VALUE
            val yearB = b.firstPublishYear?.toIntOrNull() ?: Int.MIN_VALUE
            yearB.compareTo(yearA) // Descending order (newest first)
        }
    }

    private fun sortByOldest(books: List<Book>): List<Book> {
        return books.sortedWith { a, b ->
            val yearA = a.firstPublishYear?.toIntOrNull() ?: Int.MAX_VALUE
            val yearB = b.firstPublishYear?.toIntOrNull() ?: Int.MAX_VALUE
            yearA.compareTo(yearB) // Ascending order (oldest first)
        }
    }

    private fun sortByTitleAscending(books: List<Book>): List<Book> {
        return books.sortedBy { it.title.lowercase() }
    }

    private fun sortByTitleDescending(books: List<Book>): List<Book> {
        return books.sortedByDescending { it.title.lowercase() }
    }

    private fun sortByAuthorAscending(books: List<Book>): List<Book> {
        return books.sortedBy { book ->
            book.authors.firstOrNull()?.lowercase() ?: "zzz" // Unknown authors go to end
        }
    }

    private fun sortByAuthorDescending(books: List<Book>): List<Book> {
        return books.sortedByDescending { book ->
            book.authors.firstOrNull()?.lowercase() ?: ""
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}