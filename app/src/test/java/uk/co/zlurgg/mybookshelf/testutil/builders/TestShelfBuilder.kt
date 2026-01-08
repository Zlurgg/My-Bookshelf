package uk.co.zlurgg.mybookshelf.testutil.builders

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle

/**
 * Builder pattern for creating test Bookshelf instances.
 * Provides sensible defaults and fluent API for customization.
 */
class TestShelfBuilder {
    private var id = "test-shelf-1"
    private var name = "Test Shelf"
    private var books = emptyList<Book>()
    private var shelfStyle = ShelfStyle.DarkWood
    private var position = 0
    private var isTidyMode = false
    private var isBookClub = false
    private var clubCode: String? = null
    private var clubCreatorId: String? = null

    fun withId(id: String) = apply { this.id = id }

    fun withName(name: String) = apply { this.name = name }

    fun withBooks(books: List<Book>) = apply { this.books = books }

    fun withStyle(style: ShelfStyle) = apply { this.shelfStyle = style }

    fun withPosition(position: Int) = apply { this.position = position }

    fun withTidyMode(isTidyMode: Boolean) = apply { this.isTidyMode = isTidyMode }

    fun withIsBookClub(isBookClub: Boolean) = apply { this.isBookClub = isBookClub }

    fun withClubCode(clubCode: String?) = apply { this.clubCode = clubCode }

    fun withClubCreatorId(clubCreatorId: String?) = apply { this.clubCreatorId = clubCreatorId }

    fun build() =
        Bookshelf(
            id = id,
            name = name,
            books = books,
            shelfStyle = shelfStyle,
            position = position,
            isTidyMode = isTidyMode,
            isBookClub = isBookClub,
            clubCode = clubCode,
            clubCreatorId = clubCreatorId,
        )

    companion object {
        /**
         * Creates a list of test shelves with different properties for testing scenarios.
         */
        fun createTestShelves(count: Int = 3): List<Bookshelf> {
            return (1..count).map { i ->
                TestShelfBuilder()
                    .withId("test-shelf-$i")
                    .withName("Test Shelf $i")
                    .withPosition(i - 1)
                    .withStyle(ShelfStyle.values()[i % ShelfStyle.values().size])
                    .build()
            }
        }

        /**
         * Creates a shelf with common test scenarios.
         */
        fun fiction() =
            TestShelfBuilder()
                .withId("fiction-shelf")
                .withName("Fiction")
                .withStyle(ShelfStyle.DarkWood)
                .build()

        fun nonFiction() =
            TestShelfBuilder()
                .withId("non-fiction-shelf")
                .withName("Non-Fiction")
                .withStyle(ShelfStyle.SilverMetal)
                .withPosition(1)
                .build()

        fun emptyShelf() =
            TestShelfBuilder()
                .withId("empty-shelf")
                .withName("Empty Shelf")
                .withBooks(emptyList())
                .build()
    }
}
