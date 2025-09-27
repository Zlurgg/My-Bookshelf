package uk.co.zlurgg.mybookshelf.testutil.builders

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookcase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

/**
 * Builder pattern for creating test Bookcase instances.
 * Provides sensible defaults and fluent API for customization.
 */
class TestBookcaseBuilder {
    private var id = "test-bookcase"
    private var bookshelves = emptyList<Bookshelf>()
    private var bookCounts = emptyMap<String, Int>()

    fun withId(id: String) = apply { this.id = id }
    fun withShelves(shelves: List<Bookshelf>) = apply { this.bookshelves = shelves }
    fun withBookCounts(counts: Map<String, Int>) = apply { this.bookCounts = counts }

    fun build() = Bookcase(
        id = id,
        bookshelves = bookshelves,
        bookCounts = bookCounts
    )

    companion object {
        /**
         * Creates a bookcase with common test shelves.
         */
        fun withCommonShelves() = TestBookcaseBuilder()
            .withShelves(TestShelfBuilder.createTestShelves(3))
            .withBookCounts(mapOf(
                "test-shelf-1" to 5,
                "test-shelf-2" to 12,
                "test-shelf-3" to 0
            ))

        /**
         * Creates an empty bookcase for testing.
         */
        fun empty() = TestBookcaseBuilder()
            .withShelves(emptyList())
            .withBookCounts(emptyMap())
    }
}