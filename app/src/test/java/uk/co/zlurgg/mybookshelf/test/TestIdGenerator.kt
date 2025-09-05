package uk.co.zlurgg.mybookshelf.test

import java.util.concurrent.atomic.AtomicInteger

object TestIdGenerator {
    private val counter = AtomicInteger(0)
    
    fun generateBookId(prefix: String = "test-book"): String {
        return "$prefix-${counter.incrementAndGet()}"
    }
    
    fun generateShelfId(prefix: String = "test-shelf"): String {
        return "$prefix-${counter.incrementAndGet()}"
    }
}