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

fun getBookWidth(style: BookDisplayStyle): Float {
    return when (style) {
        BookDisplayStyle.VERTICAL -> 60f
        BookDisplayStyle.LEANING_LEFT, BookDisplayStyle.LEANING_RIGHT -> 70f
        BookDisplayStyle.HORIZONTAL_STACK -> 150f // only used for stacks
    }
}