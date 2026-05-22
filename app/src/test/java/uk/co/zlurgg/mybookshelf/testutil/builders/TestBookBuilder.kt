package uk.co.zlurgg.mybookshelf.testutil.builders

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.MaturityRating
import uk.co.zlurgg.mybookshelf.book.domain.model.PrintType
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus

/**
 * Builder pattern for creating test Book instances.
 * Provides sensible defaults and fluent API for customization.
 */
class TestBookBuilder {
    private var id = "test-book-1"
    private var title = "Test Book"
    private var subtitle: String? = null
    private var imageUrl = "http://example.com/image.jpg"
    private var authors = listOf("Test Author")
    private var description: String? = "Test description"
    private var languages = listOf("eng")
    private var firstPublishYear = "2020"
    private var numPages = 200
    private var purchased = false
    private var spineColor = 0xFF112233.toInt()

    // Provider tracking
    private var provider = BookProvider.GOOGLE_BOOKS

    // Personal metadata (NOT exported for privacy)
    private var readingStatus = ReadingStatus.NOT_READ
    private var personalRating: Float = 0f
    private var personalNotes: String = ""
    private var dateAdded: Long? = null
    private var purchaseDate: Long? = null

    // Enhanced metadata from API (shareable)
    private var isbn: String? = null
    private var publisher: String? = null
    private var publishDate: String? = null
    private var subjects: List<String> = emptyList()

    // Google Books metadata
    private var previewLink: String? = null
    private var infoLink: String? = null
    private var maturityRating = MaturityRating.UNKNOWN
    private var printType = PrintType.UNKNOWN

    fun withId(id: String) = apply { this.id = id }
    fun withTitle(title: String) = apply { this.title = title }
    fun withSubtitle(subtitle: String?) = apply { this.subtitle = subtitle }
    fun withImageUrl(imageUrl: String) = apply { this.imageUrl = imageUrl }
    fun withAuthors(authors: List<String>) = apply { this.authors = authors }
    fun withDescription(description: String?) = apply { this.description = description }
    fun withLanguages(languages: List<String>) = apply { this.languages = languages }
    fun withFirstPublishYear(year: String) = apply { this.firstPublishYear = year }
    fun withNumPages(pages: Int) = apply { this.numPages = pages }
    fun withPurchased(purchased: Boolean) = apply { this.purchased = purchased }
    fun withSpineColor(color: Int) = apply { this.spineColor = color }
    fun withProvider(provider: BookProvider) = apply { this.provider = provider }

    // Personal metadata
    fun withReadingStatus(status: ReadingStatus) = apply { this.readingStatus = status }
    fun withPersonalRating(rating: Float) = apply { this.personalRating = rating }
    fun withPersonalNotes(notes: String) = apply { this.personalNotes = notes }
    fun withDateAdded(date: Long?) = apply { this.dateAdded = date }
    fun withPurchaseDate(date: Long?) = apply { this.purchaseDate = date }

    // Enhanced metadata
    fun withIsbn(isbn: String?) = apply { this.isbn = isbn }
    fun withPublisher(publisher: String?) = apply { this.publisher = publisher }
    fun withPublishDate(date: String?) = apply { this.publishDate = date }
    fun withSubjects(subjects: List<String>) = apply { this.subjects = subjects }

    // Google Books metadata
    fun withPreviewLink(link: String?) = apply { this.previewLink = link }
    fun withInfoLink(link: String?) = apply { this.infoLink = link }
    fun withMaturityRating(rating: MaturityRating) = apply { this.maturityRating = rating }
    fun withPrintType(type: PrintType) = apply { this.printType = type }

    fun build() = Book(
        id = id,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        authors = authors,
        description = description,
        languages = languages,
        firstPublishYear = firstPublishYear,
        numPages = numPages,
        purchased = purchased,
        spineColor = spineColor,
        provider = provider,
        // Personal metadata
        readingStatus = readingStatus,
        personalRating = personalRating,
        personalNotes = personalNotes,
        dateAdded = dateAdded,
        purchaseDate = purchaseDate,
        // Enhanced metadata
        isbn = isbn,
        publisher = publisher,
        publishDate = publishDate,
        subjects = subjects,
        // Google Books metadata
        previewLink = previewLink,
        infoLink = infoLink,
        maturityRating = maturityRating,
        printType = printType,
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
            .withNumPages(250)
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
