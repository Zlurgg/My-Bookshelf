package uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.presenation.util.ShelfMaterial

class BookshelfMappersTest {

    @Test
    fun `toDomain maps correctly`() {
        val entity = BookshelfEntity(
            id = "s1",
            name = "Sci-Fi",
            shelfMaterial = ShelfMaterial.GreyMetal.name
        )

        val domain = entity.toDomain()

        assertEquals("s1", domain.id)
        assertEquals("Sci-Fi", domain.name)
        assertEquals(ShelfMaterial.GreyMetal, domain.shelfMaterial)
        assertEquals(emptyList<Any>(), domain.books)
    }

    @Test
    fun `toEntity maps correctly`() {
        val domain = Bookshelf(
            id = "s2",
            name = "Fantasy",
            books = emptyList(),
            shelfMaterial = ShelfMaterial.DarkWood
        )

        val entity = domain.toEntity()

        assertEquals("s2", entity.id)
        assertEquals("Fantasy", entity.name)
        assertEquals(ShelfMaterial.DarkWood.name, entity.shelfMaterial)
    }
}
