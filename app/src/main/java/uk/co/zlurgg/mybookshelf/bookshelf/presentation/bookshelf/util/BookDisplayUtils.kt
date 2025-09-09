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

fun getDynamicBookDisplayStyle(
    book: Book,
    positionInRow: Int,
    totalBooksInRow: Int,
    remainingSpace: Float
): BookDisplayStyle {
    // Create a deterministic but position-dependent seed
    // This ensures books change orientation based on their current position context
    val positionSeed = (book.id.hashCode() + positionInRow * 7 + totalBooksInRow * 13) % 4
    
    val baseStyle = when (positionSeed) {
        0 -> BookDisplayStyle.VERTICAL
        1 -> BookDisplayStyle.LEANING_LEFT
        2 -> BookDisplayStyle.LEANING_RIGHT
        3 -> BookDisplayStyle.HORIZONTAL_STACK
        else -> BookDisplayStyle.VERTICAL
    }
    
    // Apply position rules
    return when {
        // First book in row: can't lean left (no support)
        positionInRow == 0 && baseStyle == BookDisplayStyle.LEANING_LEFT -> 
            BookDisplayStyle.VERTICAL
        
        // Last book in row: can't lean right into big space (no support)
        positionInRow == totalBooksInRow - 1 && 
        baseStyle == BookDisplayStyle.LEANING_RIGHT && 
        remainingSpace > 30f -> 
            BookDisplayStyle.VERTICAL
        
        // All other cases: use base style
        else -> baseStyle
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
        BookDisplayStyle.VERTICAL -> baseThickness
        BookDisplayStyle.LEANING_LEFT, 
        BookDisplayStyle.LEANING_RIGHT -> baseThickness * 1.4f // 40% more space needed for lean
        BookDisplayStyle.HORIZONTAL_STACK -> 150f // Horizontal uses height, not thickness
    }
}