package uk.co.zlurgg.mybookshelf.book.presentation.util

import uk.co.zlurgg.mybookshelf.book.domain.model.Book

data class BookRowData(
    val books: List<Book>,
    val styles: List<BookDisplayStyle>
)

fun calculateBookRows(
    books: List<Book>,
    availableWidthDp: Float,
    isTidyMode: Boolean,
    reservedLeadingWidthDp: Float = 0f,
): List<BookRowData> {
    val rows = mutableListOf<BookRowData>()
    var bookIndex = 0

    while (bookIndex < books.size) {
        // First pass: determine how many books fit
        // Reserve leading width on the first row only (for add slot)
        val rowAvailableWidth = if (rows.isEmpty()) {
            availableWidthDp - reservedLeadingWidthDp
        } else {
            availableWidthDp
        }
        var currentRowWidth = 0f
        var booksInRow = 0

        while (bookIndex + booksInRow < books.size) {
            val book = books[bookIndex + booksInRow]
            val bookStyle = if (isTidyMode) {
                BookDisplayStyle.VERTICAL
            } else {
                getBookDisplayStyle(book)
            }
            val bookWidth = getBookWidth(book, bookStyle) + 6f
            val potentialRowWidth = currentRowWidth + bookWidth

            if (potentialRowWidth <= rowAvailableWidth) {
                currentRowWidth = potentialRowWidth
                booksInRow++
            } else {
                break
            }
        }

        if (booksInRow == 0) booksInRow = 1

        // Second pass: apply position-aware styling
        val endIndex = minOf(bookIndex + booksInRow, books.size)
        val rowBooks = books.subList(bookIndex, endIndex)
        val rowStyles = mutableListOf<BookDisplayStyle>()

        rowBooks.forEachIndexed { index, book ->
            val bookStyle = if (isTidyMode) {
                BookDisplayStyle.VERTICAL
            } else {
                val baseStyle = getBookDisplayStyle(book)
                when {
                    index == 0 && baseStyle == BookDisplayStyle.LEANING_LEFT ->
                        BookDisplayStyle.VERTICAL

                    index == rowBooks.size - 1 && baseStyle == BookDisplayStyle.LEANING_RIGHT -> {
                        val widthSoFar = rowStyles.mapIndexed { styleIndex, style ->
                            getBookWidth(rowBooks[styleIndex], style) + 6f
                        }.sum()
                        val remainingSpace = rowAvailableWidth - widthSoFar
                        if (remainingSpace > 30f) BookDisplayStyle.VERTICAL else baseStyle
                    }

                    else -> baseStyle
                }
            }
            rowStyles.add(bookStyle)
        }

        rows.add(BookRowData(books = rowBooks.toList(), styles = rowStyles))
        bookIndex = endIndex
    }

    return rows
}

/** Width reserved for the add-book slot: bookend (60dp) + row spacing (6dp). */
const val ADD_SLOT_RESERVED_WIDTH = 66f
