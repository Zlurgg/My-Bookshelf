package uk.co.zlurgg.mybookshelf.book.domain.service

import uk.co.zlurgg.mybookshelf.core.domain.util.MaterialColorGenerator

/**
 * Generates appropriate colors for book spines.
 * This is a wrapper around the generic MaterialColorGenerator for book-specific use cases.
 */
object BookColorGenerator {

    /**
     * Generates a matte, realistic color suitable for book spines.
     * Returns an ARGB color as Int that mimics real book materials.
     */
    fun generateSpineColor(): Int {
        return MaterialColorGenerator.generateMatteColor()
    }
}
