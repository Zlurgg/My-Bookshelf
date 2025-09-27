package uk.co.zlurgg.mybookshelf.testutil.builders

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

/**
 * Builder pattern for creating test Book instances.
 * Provides sensible defaults and fluent API for customization.
 */
class TestBookBuilder {
    private var id = "test-book-1"
    private var title = "Test Book"
    private var imageUrl = "http://example.com/image.jpg"
    private var authors = listOf("Test Author")
    private var description = "Test description"
    private var languages = listOf("eng")
    private var firstPublishYear = "2020"
    private var averageRating: Double? = 4.0
    private var ratingCount = 10
    private var numPages = 200
    private var numEditions = 1
    private var purchased = false
    private var spineColor = 0xFF112233.toInt()

    fun withId(id: String) = apply { this.id = id }
    fun withTitle(title: String) = apply { this.title = title }
    fun withImageUrl(imageUrl: String) = apply { this.imageUrl = imageUrl }
    fun withAuthors(authors: List<String>) = apply { this.authors = authors }
    fun withDescription(description: String) = apply { this.description = description }
    fun withLanguages(languages: List<String>) = apply { this.languages = languages }
    fun withFirstPublishYear(year: String) = apply { this.firstPublishYear = year }
    fun withAverageRating(rating: Double?) = apply { this.averageRating = rating }
    fun withRatingCount(count: Int) = apply { this.ratingCount = count }
    fun withNumPages(pages: Int) = apply { this.numPages = pages }
    fun withNumEditions(editions: Int) = apply { this.numEditions = editions }
    fun withPurchased(purchased: Boolean) = apply { this.purchased = purchased }
    fun withSpineColor(color: Int) = apply { this.spineColor = color }

    fun build() = Book(
        id = id,
        title = title,
        imageUrl = imageUrl,
        authors = authors,
        description = description,
        languages = languages,
        firstPublishYear = firstPublishYear,
        averageRating = averageRating,
        ratingCount = ratingCount,
        numPages = numPages,
        numEditions = numEditions,
        purchased = purchased,
        spineColor = spineColor
    )

    companion object {
        /**
         * Creates a book with complete, realistic test data.
         */
        fun completeBook() = TestBookBuilder()
            .withId("complete-book-1")
            .withTitle("The Complete Guide to Testing")
            .withAuthors(listOf("Test Author", "Another Author"))
            .withDescription("A comprehensive guide to software testing")
            .withFirstPublishYear("2020")
            .withAverageRating(4.5)
            .withRatingCount(42)
            .withNumPages(250)
            .withNumEditions(3)
            .withPurchased(true)
            .build()

        /**
         * Creates a book with minimal data (for testing null/empty scenarios).
         */
        fun minimalBook() = TestBookBuilder()
            .withId("minimal-book")
            .withTitle("Minimal Book")
            .withAuthors(emptyList())
            .withDescription("")
            .withAverageRating(null)
            .withRatingCount(0)
            .withNumEditions(0)
            .build()

        /**
         * Creates a book for testing purchased scenarios.
         */
        fun purchasedBook() = TestBookBuilder()
            .withId("purchased-book")
            .withTitle("Purchased Book")
            .withPurchased(true)
            .build()
    }
}