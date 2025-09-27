package uk.co.zlurgg.mybookshelf.testutil.helpers

import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

class TestTimeProvider(private var currentTime: Long = 0L) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTime
    
    fun setTime(time: Long) {
        currentTime = time
    }
    
    fun advanceBy(millis: Long) {
        currentTime += millis
    }
}