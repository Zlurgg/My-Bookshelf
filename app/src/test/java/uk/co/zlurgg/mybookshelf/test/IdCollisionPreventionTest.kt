package uk.co.zlurgg.mybookshelf.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.presentation.sampleBook
import uk.co.zlurgg.mybookshelf.core.presentation.sampleBooks

class IdCollisionPreventionTest {

    @Test
    fun sampleBooks_have_unique_IDs() {
        val ids = sampleBooks.map { it.id }
        val uniqueIds = ids.toSet()
        
        // All IDs should be unique
        assertEquals("Sample books should have unique IDs", ids.size, uniqueIds.size)
    }
    
    @Test
    fun sampleBook_does_not_collide_with_sampleBooks() {
        val sampleBookIds = sampleBooks.map { it.id }.toSet()
        val singleBookId = sampleBook.id
        
        // Single sample book ID should not exist in the list
        assertEquals("Single sampleBook should not collide with sampleBooks list", false, singleBookId in sampleBookIds)
    }
    
    @Test
    fun sampleBooks_use_namespaced_IDs() {
        sampleBooks.forEach { book ->
            assertEquals("Sample books should use 'sample-' prefix", true, book.id.startsWith("sample-"))
        }
    }
    
    @Test
    fun testIdGenerator_produces_unique_IDs() {
        val id1 = TestIdGenerator.generateBookId()
        val id2 = TestIdGenerator.generateBookId()
        val id3 = TestIdGenerator.generateBookId()
        
        // All generated IDs should be different
        assertNotEquals("Generated IDs should be unique", id1, id2)
        assertNotEquals("Generated IDs should be unique", id2, id3)
        assertNotEquals("Generated IDs should be unique", id1, id3)
    }
    
    @Test
    fun testIdGenerator_uses_proper_prefixes() {
        val bookId = TestIdGenerator.generateBookId()
        val shelfId = TestIdGenerator.generateShelfId()
        val customId = TestIdGenerator.generateBookId("custom")
        
        assertEquals("Book ID should use default prefix", true, bookId.startsWith("test-book-"))
        assertEquals("Shelf ID should use default prefix", true, shelfId.startsWith("test-shelf-"))
        assertEquals("Custom ID should use custom prefix", true, customId.startsWith("custom-"))
    }
}