package uk.co.zlurgg.mybookshelf.testutil

import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test implementation of IdGenerator for deterministic testing.
 */
class TestIdGenerator : IdGenerator {
    private val counter = AtomicInteger(0)

    override fun generateId(): String = "test-id-${counter.incrementAndGet()}"

    fun reset() {
        counter.set(0)
    }

    fun generateBookId(prefix: String = "test-book"): String {
        return "$prefix-${counter.incrementAndGet()}"
    }

    fun generateShelfId(prefix: String = "test-shelf"): String {
        return "$prefix-${counter.incrementAndGet()}"
    }
}