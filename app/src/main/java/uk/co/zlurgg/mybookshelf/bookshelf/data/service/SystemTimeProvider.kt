package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}