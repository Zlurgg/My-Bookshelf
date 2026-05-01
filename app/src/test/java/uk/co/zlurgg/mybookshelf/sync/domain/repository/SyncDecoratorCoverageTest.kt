package uk.co.zlurgg.mybookshelf.sync.domain.repository

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Architecture test ensuring sync decorator coverage.
 *
 * Each decorator must override all user-facing write methods that should trigger sync.
 * If a write method is removed from a decorator, this test fails.
 * If a new write method is added to a repository interface, the developer must
 * add it to the decorator AND this test's inclusion list.
 *
 * Uses inclusion lists per decorator. Every listed method MUST be declared
 * (overridden) in the decorator class.
 */
class SyncDecoratorCoverageTest {

    // Methods that MUST be overridden (trigger sync)
    private val bookcaseOverrides = setOf("addShelf", "removeShelf", "updateShelf")
    private val bookOverrides = setOf("upsertBook", "deleteBook")
    private val bookshelfOverrides = setOf("addBookToShelf", "removeBookFromShelf")

    @Test
    fun `SyncingBookcaseRepository overrides all write methods`() {
        assertAllOverridesPresent(SyncingBookcaseRepository::class.java, bookcaseOverrides)
    }

    @Test
    fun `SyncingBookRepository overrides all write methods`() {
        assertAllOverridesPresent(SyncingBookRepository::class.java, bookOverrides)
    }

    @Test
    fun `SyncingBookshelfRepository overrides all write methods`() {
        assertAllOverridesPresent(SyncingBookshelfRepository::class.java, bookshelfOverrides)
    }

    private fun assertAllOverridesPresent(decoratorClass: Class<*>, expectedOverrides: Set<String>) {
        val declaredMethodNames = decoratorClass.declaredMethods.map { it.name }.toSet()

        for (method in expectedOverrides) {
            assertTrue(
                "${decoratorClass.simpleName} must override $method but only declares: $declaredMethodNames",
                declaredMethodNames.contains(method),
            )
        }
    }
}
