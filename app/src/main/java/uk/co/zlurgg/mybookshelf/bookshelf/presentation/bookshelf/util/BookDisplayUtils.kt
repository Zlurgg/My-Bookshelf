package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.util

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

fun getBookDisplayStyle(book: Book): BookDisplayStyle {
    // Use book ID hash to consistently assign display style to each book
    val bookHash = book.id.hashCode()
    return when (bookHash % 4) {
        0 -> BookDisplayStyle.VERTICAL
        1 -> BookDisplayStyle.LEANING_LEFT
        2 -> BookDisplayStyle.LEANING_RIGHT
        3 -> BookDisplayStyle.HORIZONTAL_STACK // Now allow horizontal style for individual books
        else -> BookDisplayStyle.VERTICAL
    }
}

fun getBookThickness(numPages: Int?): Float {
    // Calculate realistic book thickness based on page count
    val pages = numPages ?: 250 // Default to average book size if unknown
    return when {
        pages < 100 -> 25f      // Thin pamphlet/novella
        pages < 200 -> 35f      // Slim book
        pages < 300 -> 45f      // Standard paperback
        pages < 400 -> 55f      // Normal book
        pages < 600 -> 70f      // Thick book
        pages < 800 -> 85f      // Very thick book
        else -> 100f            // Massive tome
    }
}

fun getBookWidth(book: Book, style: BookDisplayStyle): Float {
    val baseThickness = getBookThickness(book.numPages)
    
    return when (style) {
        BookDisplayStyle.VERTICAL -> baseThickness
        BookDisplayStyle.LEANING_LEFT, 
        BookDisplayStyle.LEANING_RIGHT -> baseThickness * 1.4f // 40% more space needed for lean
        BookDisplayStyle.HORIZONTAL_STACK -> 150f // Horizontal uses height, not thickness
    }
}