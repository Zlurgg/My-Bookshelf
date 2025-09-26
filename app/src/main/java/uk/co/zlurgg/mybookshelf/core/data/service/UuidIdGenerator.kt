package uk.co.zlurgg.mybookshelf.core.data.service

import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import java.util.UUID

class UuidIdGenerator : IdGenerator {
    override fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}