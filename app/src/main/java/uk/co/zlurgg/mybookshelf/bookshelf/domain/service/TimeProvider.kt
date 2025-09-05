package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

interface TimeProvider {
    fun currentTimeMillis(): Long
}