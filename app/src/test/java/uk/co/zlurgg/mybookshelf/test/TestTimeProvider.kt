package uk.co.zlurgg.mybookshelf.test

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.TimeProvider

class TestTimeProvider(private var currentTime: Long = 0L) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTime
    
    fun setTime(time: Long) {
        currentTime = time
    }
    
    fun advanceBy(millis: Long) {
        currentTime += millis
    }
}