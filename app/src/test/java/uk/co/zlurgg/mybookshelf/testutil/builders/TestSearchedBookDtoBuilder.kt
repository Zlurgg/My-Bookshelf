package uk.co.zlurgg.mybookshelf.testutil.builders

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchedBookDto

/**
 * Builder pattern for creating test SearchedBookDto instances.
 * Provides sensible defaults and fluent API for customization.
 */
class TestSearchedBookDtoBuilder {
    private var id = "/works/OL123W"
    private var title = "Test Book"
    private var coverKey: String? = "OLCOVER1M"
    private var coverAlternativeKey: Int? = 123
    private var authorNames: List<String>? = listOf("Test Author")
    private var languages: List<String>? = listOf("eng")
    private var firstPublishYear: Int? = 2020
    private var ratingsAverage: Double? = 4.0
    private var ratingsCount: Int? = 10
    private var numPagesMedian: Int? = 200
    private var numEditions: Int? = 1
    private var isbns: List<String>? = null
    private var publishers: List<String>? = null
    private var publishDates: List<String>? = null
    private var internetArchiveIds: List<String>? = null

    fun withId(id: String) = apply { this.id = id }

    fun withTitle(title: String) = apply { this.title = title }

    fun withCoverKey(coverKey: String?) = apply { this.coverKey = coverKey }

    fun withCoverAlternativeKey(key: Int?) = apply { this.coverAlternativeKey = key }

    fun withAuthorNames(names: List<String>?) = apply { this.authorNames = names }

    fun withLanguages(languages: List<String>?) = apply { this.languages = languages }

    fun withFirstPublishYear(year: Int?) = apply { this.firstPublishYear = year }

    fun withRatingsAverage(average: Double?) = apply { this.ratingsAverage = average }

    fun withRatingsCount(count: Int?) = apply { this.ratingsCount = count }

    fun withNumPagesMedian(pages: Int?) = apply { this.numPagesMedian = pages }

    fun withNumEditions(editions: Int?) = apply { this.numEditions = editions }

    fun withIsbns(isbns: List<String>?) = apply { this.isbns = isbns }

    fun withPublishers(publishers: List<String>?) = apply { this.publishers = publishers }

    fun withPublishDates(dates: List<String>?) = apply { this.publishDates = dates }

    fun withInternetArchiveIds(ids: List<String>?) = apply { this.internetArchiveIds = ids }

    fun build() =
        SearchedBookDto(
            id = id,
            title = title,
            coverKey = coverKey,
            coverAlternativeKey = coverAlternativeKey,
            authorNames = authorNames,
            languages = languages,
            firstPublishYear = firstPublishYear,
            ratingsAverage = ratingsAverage,
            ratingsCount = ratingsCount,
            numPagesMedian = numPagesMedian,
            numEditions = numEditions,
            isbns = isbns,
            publishers = publishers,
            publishDates = publishDates,
            internetArchiveIds = internetArchiveIds,
        )

    companion object {
        /**
         * Creates a DTO with all fields populated for comprehensive testing.
         */
        fun withAllFields() =
            TestSearchedBookDtoBuilder()
                .withId("/works/OL123W")
                .withTitle("Complete Test Book")
                .withCoverKey("OLCOVER1M")
                .withCoverAlternativeKey(123)
                .withAuthorNames(listOf("Author One", "Author Two"))
                .withLanguages(listOf("eng", "spa"))
                .withFirstPublishYear(1999)
                .withRatingsAverage(4.5)
                .withRatingsCount(42)
                .withNumPagesMedian(321)
                .withNumEditions(2)
                .withIsbns(listOf("978-0123456789", "978-9876543210"))
                .withPublishers(listOf("Test Publisher", "Another Publisher"))
                .withPublishDates(listOf("1999-01-01", "2000-06-15"))
                .withInternetArchiveIds(listOf("test-book-1999"))
                .build()

        /**
         * Creates a DTO with minimal fields for testing null handling.
         */
        fun withMinimalFields() =
            TestSearchedBookDtoBuilder()
                .withId("/works/OL999W")
                .withTitle("Minimal Book")
                .withCoverKey(null)
                .withAuthorNames(null)
                .withLanguages(null)
                .withRatingsAverage(null)
                .withRatingsCount(0)
                .withNumEditions(null)
                .withIsbns(null)
                .withPublishers(null)
                .withPublishDates(null)
                .withInternetArchiveIds(null)
                .build()

        /**
         * Creates a DTO without cover key to test alternative key usage.
         */
        fun withNoCoverKey() =
            TestSearchedBookDtoBuilder()
                .withId("/works/OL888W")
                .withTitle("No Cover Key Book")
                .withCoverKey(null)
                .withCoverAlternativeKey(555)
                .build()
    }
}
