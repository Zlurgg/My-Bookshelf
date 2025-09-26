package uk.co.zlurgg.mybookshelf.core.domain.service

interface TimeProvider {
    fun currentTimeMillis(): Long
}