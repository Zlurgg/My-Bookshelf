package uk.co.zlurgg.mybookshelf.core.data.service

import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}