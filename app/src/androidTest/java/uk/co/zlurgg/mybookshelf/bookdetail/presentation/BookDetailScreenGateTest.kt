package uk.co.zlurgg.mybookshelf.bookdetail.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.MaturityRating
import uk.co.zlurgg.mybookshelf.book.domain.model.PrintType
import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus

/**
 * Screen-gate matrix for [BookDetailsScreen] non-club, non-tutorial branch.
 *
 * The personal cards (ReadingStatusCard / PersonalNotesCard / PurchasedToggleCard)
 * must be visible iff `state.isInLibrary`. The v2 plan gated on `isInLibrary ||
 * hasShelfContext`, which still rendered the cards for shelf-search previews
 * (`!isInLibrary && hasShelfContext`) — under v3's column-scoped writes those
 * edits would be silent no-ops, so the cards must be hidden, not rendered.
 *
 * Five rows, exhaustive over the (isInLibrary, hasShelfContext, onShelf) tuple.
 * Per-row tests so a future loosening regresses to the exact v2 hole as a single
 * failing test, not buried in a parameterised case.
 *
 * Runs as instrumented because Robolectric + ui-test-junit4 cannot resolve the
 * ComponentActivity host without project-wide manifest changes. The project's
 * androidTest sourceSet already wires ui-test-junit4 + manifest.
 */
@RunWith(AndroidJUnit4::class)
class BookDetailScreenGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleBook = Book(
        id = "book-1",
        title = "Gate Test Book",
        subtitle = null,
        imageUrl = "",
        authors = listOf("Author"),
        description = "Description",
        languages = listOf("eng"),
        firstPublishYear = "2020",
        numPages = 200,
        purchased = false,
        spineColor = 0xFF112233.toInt(),
        provider = BookProvider.GOOGLE_BOOKS,
        readingStatus = ReadingStatus.NOT_READ,
        personalRating = 0f,
        personalNotes = "",
        dateAdded = null,
        purchaseDate = null,
        isbn = null,
        publisher = null,
        publishDate = null,
        subjects = emptyList(),
        previewLink = null,
        infoLink = null,
        maturityRating = MaturityRating.UNKNOWN,
        printType = PrintType.UNKNOWN,
        searchSnippet = null,
    )

    private fun stateWith(
        isInLibrary: Boolean,
        hasShelfContext: Boolean,
        onShelf: Boolean = false,
    ) = BookDetailState(
        isLoading = false,
        book = sampleBook,
        onShelf = onShelf,
        hasShelfContext = hasShelfContext,
        isInLibrary = isInLibrary,
    )

    private fun assertPersonalCardsVisible() {
        // "Not Read" is the dropdown value for ReadingStatus.NOT_READ — unique to
        // ReadingStatusCard. "Reading Status" is ambiguous because the string is
        // used as both the card title and the dropdown label.
        composeRule.onNodeWithText("Not Read").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Rating and Review").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Unowned").performScrollTo().assertIsDisplayed()
    }

    private fun assertPersonalCardsHidden() {
        // assertDoesNotExist is robust to N matching nodes (passes iff N == 0), so
        // the title/label duplication is harmless here.
        composeRule.onNodeWithText("Reading Status").assertDoesNotExist()
        composeRule.onNodeWithText("Rating and Review").assertDoesNotExist()
        composeRule.onNodeWithText("Unowned").assertDoesNotExist()
        composeRule.onNodeWithText("Owned").assertDoesNotExist()
    }

    @Test
    fun row1_LibraryOwnedBook_isInLibraryTrue_hasShelfContextFalse_showsCards() {
        composeRule.setContent {
            BookDetailsScreen(
                state = stateWith(isInLibrary = true, hasShelfContext = false),
                onAction = {},
            )
        }
        assertPersonalCardsVisible()
    }

    @Test
    fun row2_ShelfWithOwnedBookOnShelf_showsCards() {
        composeRule.setContent {
            BookDetailsScreen(
                state = stateWith(isInLibrary = true, hasShelfContext = true, onShelf = true),
                onAction = {},
            )
        }
        assertPersonalCardsVisible()
    }

    @Test
    fun row3_ShelfSearchOwnedBookNotOnThisShelf_showsCards() {
        composeRule.setContent {
            BookDetailsScreen(
                state = stateWith(isInLibrary = true, hasShelfContext = true, onShelf = false),
                onAction = {},
            )
        }
        assertPersonalCardsVisible()
    }

    @Test
    fun row4_ShelfSearchPreview_isInLibraryFalse_hasShelfContextTrue_hidesCards() {
        // The v2 hole: v2's `isInLibrary || hasShelfContext` would render cards
        // here. This test fails if that gate ever comes back.
        composeRule.setContent {
            BookDetailsScreen(
                state = stateWith(isInLibrary = false, hasShelfContext = true, onShelf = false),
                onAction = {},
            )
        }
        assertPersonalCardsHidden()
    }

    @Test
    fun row5_LibrarySearchPreview_isInLibraryFalse_hasShelfContextFalse_hidesCards() {
        composeRule.setContent {
            BookDetailsScreen(
                state = stateWith(isInLibrary = false, hasShelfContext = false),
                onAction = {},
            )
        }
        assertPersonalCardsHidden()
    }
}
