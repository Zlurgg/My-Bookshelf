package uk.co.zlurgg.mybookshelf.book.domain.util

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
Look for the add slot (+) on the first row of any shelf to search for books. Type your query (title, author, or keywords) and tap the search icon - or press enter - to fetch results from Google Books (with Open Library as a backup).

🔎 SEARCH ONLY YOUR LIBRARY
While searching to add a book, flip the "My library" toggle on to search only books you already own - perfect for re-shelving a book you've read without going back out to the web. Toggle it off to search Google Books again.

➕ ADDING BOOKS
Once you find a book in search results, tap the + icon to add it to your shelf. You can add the same book to multiple shelves if you like!

📖 ORGANIZE YOUR COLLECTION
Create multiple shelves to organize your books however you want - by genre, reading status, favorites, or any system that works for you. Tap the three-dot menu on any shelf card to rename, delete, or change its style.

👥 BOOK CLUBS
Share your reading journey with friends! Swipe to the "Book Clubs" tab to:
- Create a club and share the invite code with friends
- Join a friend's club by entering their code
- Add books that everyone in the club can see
- Rate books and compare your ratings with others
- Leave comments and discuss what you're reading
Book Clubs sync automatically across all members - perfect for reading groups, book discussions, or just sharing recommendations with friends!

📖 YOUR LIBRARY
The Library tab shows every book you own across all your shelves in one place. Use it to:
- Search your entire collection by title or author
- Sort by recently added, title (A-Z), or author (A-Z)
- Filter by reading status: Not Read, Reading, Finished, or Abandoned
- Tap any book to view its details, add notes, or update your reading status
Your Library updates automatically as you add books to shelves - no extra steps needed!

🎨 CUSTOMIZE SHELF STYLES
Each shelf can have its own visual style! In the three-dot menu on any shelf card, choose "Change Style" to pick from different wood and metal finishes.

🔀 REORDER SHELVES
Want to rearrange your bookcase? Tap the unlock icon at the top to enter reorder mode. Then drag shelves up and down to organize them. Tap the lock icon when you're done.

👀 TIDY vs MESSY VIEW
Each shelf has two viewing modes! Tap the toggle icon in the shelf to switch between:
- Tidy view: Clean rows of book spines
- Messy view: Books at different angles (more realistic!)

🗑️ REMOVING BOOKS
Open a book's details and tap "Remove from Shelf" at the bottom. Don't worry - you can always add it back later!

🔗 INVITE FRIENDS
Want to read together? Tap the three-dot menu on a shelf card and choose "Create Book Club" to turn it into a shared club. Already in a club? Use "Invite to Club" from the same menu to share the invite code with friends.

☁️ CLOUD SYNC
Sign in with Google to sync your collection across devices! Tap the profile icon in the top-right to access your account and sign in. Your personal shelves and Book Club memberships will stay in sync.

💡 HELPFUL TIPS
- The Tutorial Bookshelf (this shelf) can be accessed anytime via the Help menu
- Books remember your personal notes and reading status
- Book Clubs require sign-in to sync with other members

Enjoy building your personal library! 📚
    """.trimIndent()

    /**
     * Tutorial book publisher
     */
    const val TUTORIAL_BOOK_PUBLISHER = "My Bookshelf"

    /**
     * Tutorial book cover image - marker resolved to R.drawable in LoadImage
     */
    const val TUTORIAL_BOOK_IMAGE_URL = "local:tutorial_book_cover"

    /**
     * Tutorial book default rating values
     */
    const val TUTORIAL_BOOK_PERSONAL_RATING = 5f
}
