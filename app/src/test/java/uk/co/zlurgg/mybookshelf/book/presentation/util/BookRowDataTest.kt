package uk.co.zlurgg.mybookshelf.book.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.testutil.builders.TestBookBuilder

class BookRowDataTest {

    @Test
    fun `empty list returns no rows`() {
        val rows = calculateBookRows(
            books = emptyList(),
            availableWidthDp = 400f,
            isTidyMode = false
        )
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `single book produces one row`() {
        val books = listOf(TestBookBuilder().withId("book-1").build())
        val rows = calculateBookRows(
            books = books,
            availableWidthDp = 400f,
            isTidyMode = false
        )
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].books.size)
        assertEquals(1, rows[0].styles.size)
    }

    @Test
    fun `tidy mode uses VERTICAL style for all books`() {
        val books = List(5) { TestBookBuilder().withId("book-$it").build() }
        val rows = calculateBookRows(
            books = books,
            availableWidthDp = 1000f,
            isTidyMode = true
        )
        val allStyles = rows.flatMap { it.styles }
        assertTrue(allStyles.all { it == BookDisplayStyle.VERTICAL })
    }

    @Test
    fun `narrow width forces at least one book per row`() {
        val books = listOf(
            TestBookBuilder().withId("book-1").withNumPages(1000).build()
        )
        val rows = calculateBookRows(
            books = books,
            availableWidthDp = 10f,
            isTidyMode = false
        )
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].books.size)
    }

    @Test
    fun `all books accounted for across rows`() {
        val books = List(20) {
            TestBookBuilder().withId("book-$it").withNumPages(200 + it * 50).build()
        }
        val rows = calculateBookRows(
            books = books,
            availableWidthDp = 300f,
            isTidyMode = false
        )
        val totalBooks = rows.sumOf { it.books.size }
        assertEquals(20, totalBooks)
    }

    @Test
    fun `each row has matching books and styles count`() {
        val books = List(10) { TestBookBuilder().withId("book-$it").build() }
        val rows = calculateBookRows(
            books = books,
            availableWidthDp = 300f,
            isTidyMode = false
        )
        rows.forEach { row ->
            assertEquals(row.books.size, row.styles.size)
        }
    }

    @Test
    fun `reserved leading width reduces first row capacity`() {
        val books = List(10) { TestBookBuilder().withId("book-$it").withNumPages(200).build() }

        val rowsWithout = calculateBookRows(
            books = books,
            availableWidthDp = 400f,
            isTidyMode = true
        )
        val rowsWith = calculateBookRows(
            books = books,
            availableWidthDp = 400f,
            isTidyMode = true,
            reservedLeadingWidthDp = ADD_SLOT_RESERVED_WIDTH
        )

        assertTrue(
            "First row should have fewer books with reserved width",
            rowsWith[0].books.size <= rowsWithout[0].books.size
        )
        // All books still accounted for
        assertEquals(10, rowsWith.sumOf { it.books.size })
    }

    @Test
    fun `reserved leading width only affects first row`() {
        val books = List(20) { TestBookBuilder().withId("book-$it").withNumPages(200).build() }

        val rowsWithout = calculateBookRows(
            books = books,
            availableWidthDp = 300f,
            isTidyMode = true
        )
        val rowsWith = calculateBookRows(
            books = books,
            availableWidthDp = 300f,
            isTidyMode = true,
            reservedLeadingWidthDp = ADD_SLOT_RESERVED_WIDTH
        )

        // Second row onward should have same capacity (if enough books to fill multiple rows)
        if (rowsWith.size > 2 && rowsWithout.size > 2) {
            assertEquals(
                "Second row capacity should be unchanged",
                rowsWithout[1].books.size,
                rowsWith[1].books.size
            )
        }
    }

    @Test
    fun `reserved leading width with single book still produces one row`() {
        val books = listOf(TestBookBuilder().withId("book-1").withNumPages(200).build())
        val rows = calculateBookRows(
            books = books,
            availableWidthDp = 400f,
            isTidyMode = true,
            reservedLeadingWidthDp = ADD_SLOT_RESERVED_WIDTH
        )
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].books.size)
    }

    @Test
    fun `zero reserved width behaves same as no parameter`() {
        val books = List(5) { TestBookBuilder().withId("book-$it").build() }
        val rowsDefault = calculateBookRows(
            books = books,
            availableWidthDp = 400f,
            isTidyMode = false
        )
        val rowsExplicitZero = calculateBookRows(
            books = books,
            availableWidthDp = 400f,
            isTidyMode = false,
            reservedLeadingWidthDp = 0f
        )
        assertEquals(rowsDefault.size, rowsExplicitZero.size)
        rowsDefault.zip(rowsExplicitZero).forEach { (a, b) ->
            assertEquals(a.books.size, b.books.size)
        }
    }
}
