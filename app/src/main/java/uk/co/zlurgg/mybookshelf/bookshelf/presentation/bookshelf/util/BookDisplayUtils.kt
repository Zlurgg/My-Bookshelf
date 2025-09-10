package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

fun getBookDisplayStyle(book: Book): BookDisplayStyle {
    // Use book ID hash to consistently assign display style to each book
    val bookHash = book.id.hashCode()
    return when (bookHash % 4) {
        0 -> BookDisplayStyle.VERTICAL
        1 -> BookDisplayStyle.LEANING_LEFT
        2 -> BookDisplayStyle.LEANING_RIGHT
        3 -> BookDisplayStyle.HORIZONTAL_STACK
        else -> BookDisplayStyle.VERTICAL
    }
}

fun getBookThickness(numPages: Int?): Float {
    // Calculate realistic book thickness based on page count
    val pages = numPages ?: 250 // Default to average book size if unknown
    return when {
        pages < 100 -> 30f      // Thin pamphlet/novella
        pages < 150 -> 35f
        pages < 200 -> 40f      // Slim book
        pages < 250 -> 45f
        pages < 300 -> 50f      // Standard paperback
        pages < 350 -> 55f
        pages < 400 -> 60f      // Normal book
        pages < 450 -> 65f
        pages < 500 -> 70f
        pages < 600 -> 80f      // Thick book
        pages < 800 -> 90f      // Very thick book
        else -> 100f            // Massive tome
    }
}

fun getBookWidth(book: Book, style: BookDisplayStyle): Float {
    val baseThickness = getBookThickness(book.numPages)
    
    return when (style) {
        BookDisplayStyle.VERTICAL -> 
            baseThickness + VERTICAL_BOOK_PADDING
        BookDisplayStyle.LEANING_LEFT, BookDisplayStyle.LEANING_RIGHT -> 
            (baseThickness * LEANING_SPACE_MULTIPLIER) + LEANING_BOOK_PADDING
        BookDisplayStyle.HORIZONTAL_STACK -> 
            HORIZONTAL_BOOK_WIDTH
    }
}

// Constants for book width calculations
private const val VERTICAL_BOOK_PADDING = 2f // 1dp padding each side
private const val LEANING_BOOK_PADDING = 4f // 2dp padding each side  
private const val LEANING_SPACE_MULTIPLIER = 1.4f // 40% more space for lean
private const val HORIZONTAL_BOOK_WIDTH = 150f