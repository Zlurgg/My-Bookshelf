package uk.co.zlurgg.mybookshelf.bookshelf.domain.util

/**
 * Domain-level constants for book-related business logic.
 */
object BookDetailConstants {
    /**
     * Tutorial book ID - should remain constant for consistency
     */
    const val TUTORIAL_BOOK_ID = "tutorial-book-welcome"

    /**
     * Tutorial book title
     */
    const val TUTORIAL_BOOK_TITLE = "My Bookshelf Guide"

    /**
     * Tutorial book author
     */
    const val TUTORIAL_BOOK_AUTHOR = "The My Bookshelf Team"

    /**
     * Tutorial book description with app usage tips
     */
    val TUTORIAL_BOOK_DESCRIPTION = """
Welcome to My Bookshelf - your personal book collection organizer!

📚 GETTING STARTED
Your books live on shelves, and shelves live in your bookcase. Think of it like a digital version of your home library!

🔍 SEARCH FOR BOOKS
Tap the search icon (magnifying glass) in any shelf to find books from the Open Library. You can search by title, author, or keywords. Try searching for your favorite book!

➕ ADDING BOOKS
Once you find a book in search results, tap on it to see details, then tap "Add to Shelf" at the bottom. You can add the same book to multiple shelves if you like!

📖 ORGANIZE YOUR COLLECTION
Create multiple shelves to organize your books however you want - by genre, reading status, favorites, or any system that works for you. Long-press a shelf card in the bookcase to rename or delete it.

🎨 CUSTOMIZE SHELF STYLES
Each shelf can have its own visual style! Tap the three-dot menu on any shelf card and choose "Change Style" to pick from Dark Wood, Silver Metal, White Metal, Grey Metal, or Dark Grey Metal.

🔀 REORDER SHELVES
Want to rearrange your bookcase? Tap the unlock icon (🔓) at the top to enter reorder mode. Then drag shelves up and down to organize them. Tap the lock icon (🔒) when you're done.

👀 TIDY vs MESSY VIEW
Each shelf has two viewing modes! Tap the grid/list icon in the shelf to toggle between:
- Tidy view: Clean rows of book spines
- Messy view: Books at different heights (more realistic!)

🗑️ REMOVING BOOKS
Open a book's details and tap "Remove from Shelf" at the bottom. Don't worry - you can always add it back later!

🔗 SHARE YOUR SHELVES
Want to share your reading list with friends? In the bookshelf view, tap the share icon to create a link. Friends can import your shelf with one tap!

💡 HELPFUL TIPS
- The Tutorial Bookshelf (this shelf) can be accessed anytime via the help icon (?) at the top of your bookcase
- Books remember your personal notes and reading status - only visible to you!
- You can delete this tutorial shelf once you're comfortable with the app

Enjoy building your personal library! 📚
    """.trimIndent()

    /**
     * Tutorial book publisher
     */
    const val TUTORIAL_BOOK_PUBLISHER = "My Bookshelf"
}
