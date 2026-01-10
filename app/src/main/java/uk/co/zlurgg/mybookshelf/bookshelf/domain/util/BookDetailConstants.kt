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
Tap the + button in any shelf to find books from the Open Library. You can search by title, author, or keywords. Try searching for your favorite book!

➕ ADDING BOOKS
Once you find a book in search results, tap the + icon to add it to your shelf. You can add the same book to multiple shelves if you like!

📖 ORGANIZE YOUR COLLECTION
Create multiple shelves to organize your books however you want - by genre, reading status, favorites, or any system that works for you. Long-press a shelf card in the bookcase to rename or delete it.

👥 BOOK CLUBS
Share your reading journey with friends! Swipe to the "Book Clubs" tab to:
- Create a club and share the invite code with friends
- Join a friend's club by entering their code
- Add books that everyone in the club can see
- Rate books and compare your ratings with others
- Leave comments and discuss what you're reading
Book Clubs sync automatically across all members - perfect for reading groups, book discussions, or just sharing recommendations with friends!

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

    /**
     * Tutorial book cover image - uses app icon via Android resource URI
     */
    const val TUTORIAL_BOOK_IMAGE_URL = "android.resource://uk.co.zlurgg.mybookshelf/mipmap/ic_launcher"

    /**
     * Tutorial book default rating values
     */
    const val TUTORIAL_BOOK_RATING = 5.0
    const val TUTORIAL_BOOK_PERSONAL_RATING = 5f
    const val TUTORIAL_BOOK_RATING_COUNT = 1
    const val TUTORIAL_BOOK_EDITION_COUNT = 1
}
