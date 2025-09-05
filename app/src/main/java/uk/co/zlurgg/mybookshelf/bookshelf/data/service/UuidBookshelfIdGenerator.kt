package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfIdGenerator
import java.util.UUID

class UuidBookshelfIdGenerator : BookshelfIdGenerator {
    override fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}