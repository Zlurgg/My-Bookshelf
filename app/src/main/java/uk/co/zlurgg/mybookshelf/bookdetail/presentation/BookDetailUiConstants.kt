package uk.co.zlurgg.mybookshelf.bookdetail.presentation

import androidx.compose.ui.unit.dp

object BookDetailUiConstants {
    // Card styling — used by all Card-based components
    val CardElevation = 2.dp
    val CardContentPadding = 16.dp

    // Layout spacing — used by BookDetailScreen + most cards
    val SectionSpacing = 12.dp
    val SmallSpacing = 8.dp
    val TinySpacing = 4.dp

    // Image — used by BookDetailImage + BookHeroSection
    val BookImageHeight = 200.dp
    val ImageCornerRadius = 8.dp

    // Ratings — used by ClubRatingCard
    const val MaxStars = 5

    // Timing — used by ViewModel for the club-review debounce. Personal-notes
    // writes are no longer debounced: column-scoped UPDATE is cheap enough to
    // run per keystroke. Firestore-bound review writes still batch.
    const val DebounceDelayMs = 2000L
}
