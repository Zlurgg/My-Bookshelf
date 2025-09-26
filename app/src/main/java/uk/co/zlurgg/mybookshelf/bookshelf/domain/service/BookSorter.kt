package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.core.util.TextUtils
import kotlin.math.ln
import kotlin.math.max

class BookSorter {

    companion object {
        // Weighted scoring weights for BEST_MATCH algorithm
        private const val TITLE_WEIGHT = 0.4
        private const val AUTHOR_WEIGHT = 0.2
        private const val RATINGS_WEIGHT = 0.25
        private const val POPULARITY_WEIGHT = 0.15

        // Title similarity scores
        private const val EXACT_MATCH_SCORE = 100.0
        private const val STARTS_WITH_SCORE = 90.0
        private const val CONTAINS_SCORE = 70.0
        private const val FUZZY_MATCH_MAX_SCORE = 50.0

        // Author match scores
        private const val AUTHOR_EXACT_MATCH_SCORE = 100.0
        private const val AUTHOR_STARTS_WITH_SCORE = 80.0
        private const val AUTHOR_CONTAINS_SCORE = 60.0
    }

    fun sortBooks(
        books: List<Book>,
        sortBy: BookSearchSort,
        searchQuery: String = ""
    ): List<Book> {
        return when (sortBy) {
            BookSearchSort.BEST_MATCH -> sortByBestMatch(books, searchQuery)
            BookSearchSort.NEWEST -> sortByNewest(books)
            BookSearchSort.OLDEST -> sortByOldest(books)
            BookSearchSort.HIGHEST_RATED -> sortByHighestRated(books)
            BookSearchSort.MOST_POPULAR -> sortByMostPopular(books)
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
            (titleSimilarity * TITLE_WEIGHT) + (authorMatch * AUTHOR_WEIGHT) +
                    (ratingsScore * RATINGS_WEIGHT) + (popularityScore * POPULARITY_WEIGHT)
        }
    }

    private fun calculateTitleSimilarity(title: String, query: String): Double {
        val titleLower = title.lowercase()

        return when {
            titleLower == query -> EXACT_MATCH_SCORE
            titleLower.startsWith(query) -> STARTS_WITH_SCORE
            titleLower.contains(query) -> CONTAINS_SCORE
            else -> {
                // Calculate Levenshtein distance for fuzzy matching
                val similarity = TextUtils.calculateStringSimilarity(titleLower, query)
                similarity * FUZZY_MATCH_MAX_SCORE
            }
        }
    }

    private fun calculateAuthorMatch(authors: List<String>, query: String): Double {
        if (authors.isEmpty()) return 0.0

        val bestMatch = authors.maxOfOrNull { author ->
            val authorLower = author.lowercase()
            when {
                authorLower == query -> AUTHOR_EXACT_MATCH_SCORE
                authorLower.startsWith(query) -> AUTHOR_STARTS_WITH_SCORE
                authorLower.contains(query) -> AUTHOR_CONTAINS_SCORE
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

}