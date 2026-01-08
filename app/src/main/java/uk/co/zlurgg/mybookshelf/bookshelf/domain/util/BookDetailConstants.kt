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
    val TUTORIAL_BOOK_DESCRIPTION =
        """
        Welcome to My Bookshelf - your personal book collection organizer!

        📚 GETTING STARTED
        Your books live on shelves, and shelves live in your bookcase. Think of it like a digital version of your home library!

        🔍 SEARCH FOR BOOKS
        Tap the + button in any shelf to find books from the Open Library. You can search by title, author, or keywords. Try searching for your favorite book!

        ➕ ADDING BOOKS
        Once you find a book in search results, tap the + icon to add it to your shelf. You can add the same book to multiple shelves if you like!

        📖 ORGANIZE YOUR COLLECTION
        Create multiple shelves to organize your books however you want - by genre, reading status, favorites, or any system that works for you. Long-press a shelf card in the bookcase to rename or delete it.

        🎨 CUSTOMIZE SHELF STYLES
        Each shelf can have its own visual style! Tap the three-dot menu on any shelf card and choose "Change Style" to pick from different wood and metal finishes.

        🔀 REORDER SHELVES
        Want to rearrange your bookcase? Tap the unlock icon at the top to enter reorder mode. Then drag shelves up and down to organize them. Tap the lock icon when you're done.

        👀 TIDY vs MESSY VIEW
        Each shelf has two viewing modes! Tap the toggle icon in the shelf to switch between:
        - Tidy view: Clean rows of book spines
        - Messy view: Books at different angles (more realistic!)

        🗑️ REMOVING BOOKS
        Open a book's details and tap "Remove from Shelf" at the bottom. Don't worry - you can always add it back later!

        🔗 SHARE YOUR SHELVES
        Want to share your reading list with friends? In the bookshelf view, tap the share icon to create a link. Friends can import your shelf with one tap!

        👥 BOOK CLUBS
        Share your reading journey with friends! Swipe to the "Book Clubs" tab to:
        - Create a Book Club and invite friends with a code
        - Join a friend's club by entering their invite code
        - Rate books and see what others think
        - Comment on books and discuss with club members
        Book Clubs sync automatically - everyone sees the same books and discussions!

        ☁️ CLOUD SYNC
        Sign in with Google to sync your collection across devices! Tap the menu icon and choose "Sign In" to enable cloud sync. Your personal shelves and Book Club memberships will stay in sync.

        💡 HELPFUL TIPS
        - The Tutorial Bookshelf (this shelf) can be accessed anytime via the Help menu
        - Books remember your personal notes and reading status
        - Book Clubs require sign-in to sync with other members
        - You can delete this tutorial shelf once you're comfortable with the app

        Enjoy building your personal library! 📚
        """.trimIndent()

    /**
     * Tutorial book publisher
     */
    const val TUTORIAL_BOOK_PUBLISHER = "My Bookshelf"
}
