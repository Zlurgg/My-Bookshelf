package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder

/**
 * Test for BookSorter - Focused on sorting algorithm logic.
 * Tests business logic: Complex sorting with weighted scoring.
 * Mocks: None (pure function with test data)
 */
class BookSorterTest {

    private val sorter = BookSorter()

    @Test
    fun `sortBooks with BEST_MATCH prioritizes exact title match`() {
        // Given
        val exactMatch = TestBookBuilder().withTitle("Lord of the Rings").build()
        val partialMatch = TestBookBuilder().withTitle("The Rings of Power").build()
        val books = listOf(partialMatch, exactMatch)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.BEST_MATCH, "Lord of the Rings")

        // Then
        assertEquals("Exact match should be first", exactMatch, sorted[0])
    }

    @Test
    fun `sortBooks with BEST_MATCH prioritizes starts with over contains`() {
        // Given
        val startsWithMatch = TestBookBuilder().withTitle("Harry Potter and the Stone").build()
        val containsMatch = TestBookBuilder().withTitle("The Story of Harry Potter").build()
        val books = listOf(containsMatch, startsWithMatch)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.BEST_MATCH, "Harry Potter")

        // Then
        assertEquals("Starts-with should be first", startsWithMatch, sorted[0])
    }

    @Test
    fun `sortBooks with NEWEST puts latest published books first`() {
        // Given
        val old = TestBookBuilder().withFirstPublishYear("1950").build()
        val recent = TestBookBuilder().withFirstPublishYear("2020").build()
        val books = listOf(old, recent)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.NEWEST)

        // Then
        assertEquals("2020 book should be first", recent, sorted[0])
        assertEquals("1950 book should be second", old, sorted[1])
    }

    @Test
    fun `sortBooks with OLDEST puts earliest published books first`() {
        // Given
        val old = TestBookBuilder().withFirstPublishYear("1950").build()
        val recent = TestBookBuilder().withFirstPublishYear("2020").build()
        val books = listOf(recent, old)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.OLDEST)

        // Then
        assertEquals("1950 book should be first", old, sorted[0])
        assertEquals("2020 book should be second", recent, sorted[1])
    }

    @Test
    fun `sortBooks with HIGHEST_RATED puts best rated books first`() {
        // Given
        val lowRated = TestBookBuilder().withAverageRating(3.0).build()
        val highRated = TestBookBuilder().withAverageRating(4.5).build()
        val books = listOf(lowRated, highRated)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.HIGHEST_RATED)

        // Then
        assertEquals("4.5 rated book should be first", highRated, sorted[0])
        assertEquals("3.0 rated book should be second", lowRated, sorted[1])
    }

    @Test
    fun `sortBooks with HIGHEST_RATED uses rating count as tie-breaker`() {
        // Given
        val sameRatingFewReviews = TestBookBuilder()
            .withAverageRating(4.0)
            .withRatingCount(10)
            .build()
        val sameRatingManyReviews = TestBookBuilder()
            .withAverageRating(4.0)
            .withRatingCount(1000)
            .build()
        val books = listOf(sameRatingFewReviews, sameRatingManyReviews)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.HIGHEST_RATED)

        // Then
        assertEquals("More reviews should win tie", sameRatingManyReviews, sorted[0])
    }

    @Test
    fun `sortBooks with MOST_POPULAR puts books with most ratings first`() {
        // Given
        val lessPopular = TestBookBuilder().withRatingCount(100).build()
        val morePopular = TestBookBuilder().withRatingCount(10000).build()
        val books = listOf(lessPopular, morePopular)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.MOST_POPULAR)

        // Then
        assertEquals("10000 ratings should be first", morePopular, sorted[0])
        assertEquals("100 ratings should be second", lessPopular, sorted[1])
    }

    @Test
    fun `sortBooks handles null publish years in NEWEST sort`() {
        // Given
        val nullYear = TestBookBuilder().build() // No year set = null
        val validYear = TestBookBuilder().withFirstPublishYear("2020").build()
        val books = listOf(nullYear, validYear)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.NEWEST)

        // Then
        assertEquals("Valid year should be first", validYear, sorted[0])
        assertEquals("Null year should be last", nullYear, sorted[1])
    }

    @Test
    fun `sortBooks handles null ratings in HIGHEST_RATED sort`() {
        // Given
        val nullRating = TestBookBuilder().build() // No rating set = null
        val validRating = TestBookBuilder().withAverageRating(4.0).build()
        val books = listOf(nullRating, validRating)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.HIGHEST_RATED)

        // Then
        assertEquals("Valid rating should be first", validRating, sorted[0])
        assertEquals("Null rating should be last", nullRating, sorted[1])
    }

    @Test
    fun `sortBooks returns empty list for empty input`() {
        // Given
        val books = emptyList<uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book>()

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.BEST_MATCH, "query")

        // Then
        assertTrue("Should return empty list", sorted.isEmpty())
    }

    @Test
    fun `sortBooks with BEST_MATCH matches author names`() {
        // Given
        val matchingAuthor = TestBookBuilder()
            .withTitle("Some Book")
            .withAuthors(listOf("J.K. Rowling"))
            .build()
        val nonMatchingAuthor = TestBookBuilder()
            .withTitle("Another Book")
            .withAuthors(listOf("Stephen King"))
            .build()
        val books = listOf(nonMatchingAuthor, matchingAuthor)

        // When
        val sorted = sorter.sortBooks(books, BookSearchSort.BEST_MATCH, "rowling")

        // Then
        assertEquals("Matching author should be first", matchingAuthor, sorted[0])
    }
}
