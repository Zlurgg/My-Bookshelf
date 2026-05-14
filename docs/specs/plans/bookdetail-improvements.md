# Book Detail Improvements Plan

Unify and polish the BookDetail screen, improving visual consistency and ensuring each code path displays the correct content.

## Current State

The BookDetail screen has **3 code paths** in `BookDetailScreen.kt`, determined by two flags:

1. **Tutorial** — `isTutorialBook == true` (book ID is `"tutorial-book-welcome"`)
2. **Book club** — `isBookClub == true` (shelf has `isBookClub` flag). Within this path, `isSignedIn` controls whether club-interactive cards or a sign-in hint appear.
3. **Regular book** — everything else. Within this path, `onShelf` controls whether personal-interactive cards (recommendation, notes, purchased) appear. When `onShelf == false` (e.g. navigated from library with no shelfId), those cards are hidden.

There is no separate "library code path" — library is just the regular-book path with `onShelf = false` and `hasShelfContext = false`.

### Current card layout per code path

| Card | Tutorial | Regular (onShelf) | Regular (!onShelf) | Club (signed in) | Club (guest) |
|------|:---:|:---:|:---:|:---:|:---:|
| BookOverviewCard | Y | Y | Y | Y | Y |
| BookDetailImage | Y | Y | Y | Y | Y |
| RecommendationStatusCard | - | Y | - | - | - |
| PersonalNotesCard | - | Y | - | - | - |
| ClubRatingCard | - | - | - | Y | - |
| ClubCommentsCard | - | - | - | Y | - |
| Sign-in hint (bare Text) | - | - | - | - | Y |
| CommunityRatingsCard | - | Y | Y | Y | Y |
| DescriptionCard | Y (expanded) | Y | Y | Y | Y |
| PublicationDetailsCard | - | Y | Y | Y | Y |
| LanguagesCard | - | Y | Y | Y | Y |
| **PurchasedToggleCard** | - | **Y** | **Y (bug)** | - | - |
| ShelfActionsCard (bottom bar) | - | Y | - | Y | - |

### Issues identified

1. **PurchasedToggleCard not gated by `onShelf`** — it renders unconditionally in the regular-book branch (line 342), so it shows even when navigated from library with no shelf context. It should be gated behind `onShelf`, same as RecommendationStatusCard and PersonalNotesCard.
2. **Guest club sign-in hint is a bare `Text`** — not wrapped in a Card, visually inconsistent with every other element on the screen.
3. **No visual hierarchy** — all cards use identical 2.dp elevation and default shape. No distinction between the hero content (overview, image) and reference metadata.
4. **BookDetailImage is not in a Card** — standalone Box with clipped corners, breaks visual rhythm.
5. **Overview + image block duplicated 3 times** — copy-pasted across tutorial, book-club, and regular-book branches.

---

## Part 1: Variant Content Corrections

### 1a. Gate PurchasedToggleCard behind `onShelf`

Add `if (state.onShelf)` around PurchasedToggleCard (line 341), matching the existing gate pattern used for RecommendationStatusCard (line 282) and PersonalNotesCard (line 298).

### 1b. Create SignInHintCard component

Replace the bare `Text` (lines 227-233) with a `SignInHintCard` composable.

- Wrapped in a Card for visual consistency.
- Uses `secondaryContainer` background color with a lock icon (`Icons.Outlined.Lock`) and the existing `club_detail_sign_in_hint` string.
- **Display-only** — no tap action, no navigation to sign-in. The user can sign in via the account tab. No new `BookDetailAction` or ViewModel changes needed.

---

## Part 2: Visual Polish

### 2a. Hero section — overview + image

Combine `BookOverviewCard` and `BookDetailImage` into a single `BookHeroSection` composable. This replaces two separate items with one cohesive header and eliminates the 3x duplication.

**Layout**: `Column` within a single `Surface` (no Card elevation) — image stacked above overview text, both using full width. This preserves the current vertical flow which works well on narrow phone screens. The visual cohesion comes from removing the Card wrapper and grouping them on a shared surface, not from changing to a Row.

- Surface uses `Color.Transparent` — the hero section is intentionally unboxed, blending with the screen background. The image and text stand on their own without a container.
- Cover image at full width with rounded top corners.
- Title, authors, year, pages, editions below the image.
- For books with no image, overview text renders alone.
- `onImageLoadResult` callback still supported for hiding the image slot on load failure.

### 2b. Section grouping with visual hierarchy

Distinguish content types through lightweight visual treatment:

- **Hero section** (BookHeroSection): No Card elevation — sits as an unboxed header. Title uses `headlineSmall`. Authors/metadata use `bodyMedium` with `onSurfaceVariant`.
- **Interactive section** (recommendation, notes, purchased, club rating, club comments, sign-in hint): Keep current Card styling — 2.dp elevation, default shape. These are the user's primary interaction points.
- **Info section** (community ratings, description, publication, languages): Use `OutlinedCard` with `0.dp` elevation and `outlineVariant` border color at 1.dp width. This is Material 3's standard `OutlinedCard` — no custom border modifiers needed. Signals "reference info" vs "your stuff."
- **Spacing**: Add `Modifier.padding(top = 4.dp)` to the first OutlinedCard in the info section. Combined with the existing `Arrangement.spacedBy(12.dp)`, this produces 16.dp total gap — a clean visual break without an empty Spacer item that would stack arrangement gaps (12 + 16 + 12 = 40.dp unintended).

### 2c. Top app bar refinement

Switch title from `titleLarge` to `titleMedium` with `maxLines = 1` and `overflow = TextOverflow.Ellipsis`.

---

## Part 3: Reduce Code Duplication

Restructure `BookDetailsScreen` from 3 branching blocks into a single linear flow.

### Target structure

```
LazyColumn {
    // 1. Common hero (all paths)
    item { BookHeroSection(...) }

    // 2. Variant-specific interactive section
    when {
        isTutorialBook -> {
            // Nothing — tutorial has no interactive cards
            // (description moves to info section, shown expanded via flag)
        }
        isBookClub && isSignedIn -> {
            item { ClubRatingCard(...) }
            item { ClubCommentsCard(...) }
        }
        isBookClub && !isSignedIn -> {
            item { SignInHintCard() }
        }
        else -> {
            // Regular book — personal cards gated by onShelf
            if (onShelf) {
                item { RecommendationStatusCard(...) }
                item { PersonalNotesCard(...) }
                item { PurchasedToggleCard(...) }
            }
        }
    }

    // 3. Common info section (all paths except tutorial-only subset)
    // First info card gets Modifier.padding(top = 4.dp) for 16.dp total gap
    if (!isTutorialBook) {
        item { CommunityRatingsCard(..., modifier = Modifier.padding(top = 4.dp)) }  // OutlinedCard
    }
    // Tutorial keeps elevated Card for prominence; non-tutorial uses OutlinedCard
    item { DescriptionCard(..., initiallyExpanded = isTutorialBook, outlined = !isTutorialBook) }
    if (!isTutorialBook) {
        item { PublicationDetailsCard(...) }  // OutlinedCard
        item { LanguagesCard(...) }          // OutlinedCard
    }
}
```

### Tutorial path handling

The tutorial book currently shows only overview + image + description (expanded). After restructuring:
- It gets the `BookHeroSection` (overview + image) — same content, better presentation.
- No interactive section (the `when` block has an empty tutorial branch).
- Info section: only `DescriptionCard` with `initiallyExpanded = true`. Uses a regular elevated Card (not OutlinedCard) to keep it visually prominent as the tutorial's primary content. The `!isTutorialBook` gates exclude community ratings, publication details, and languages — preserving the current minimal tutorial view.

### PurchasedToggleCard placement

Lives inside the regular-book branch of the `when` block, alongside RecommendationStatusCard and PersonalNotesCard, gated by `onShelf`. This keeps it with the other personal-interactive cards rather than orphaned after the info section.

---

## Part 4: Extract Magic Numbers to Constants

The bookdetail components have ~90 hardcoded values across 16 files. Many are repeated (e.g. `2.dp` elevation appears 11 times, `16.dp` padding 14 times). Split into two categories:

### Shared constants (used by 2+ components)

New file: `bookdetail/presentation/BookDetailUiConstants.kt`

```kotlin
object BookDetailUiConstants {
    // Card styling — used by all 11 Card-based components
    val CardElevation = 2.dp
    val CardContentPadding = 16.dp

    // Layout spacing — used by BookDetailScreen + most cards
    val SectionSpacing = 12.dp
    val InfoSectionExtraTopPadding = 4.dp
    val SmallSpacing = 8.dp
    val TinySpacing = 4.dp

    // Image — used by BookDetailImage + BookHeroSection
    val BookImageHeight = 200.dp
    val ImageCornerRadius = 8.dp

    // Ratings — used by ClubRatingCard, RecommendationStatusCard, CommunityRatingsCard
    const val MaxStars = 5

    // Timing — used by ViewModel (notes debounce + review debounce)
    const val DebounceDelayMs = 2000L
}
```

Use `val` for Dp values, `const val` for primitives.

### Component-local constants (used by 1 component only)

These stay as `private val`/`private const val` at file scope in the component that uses them. Following standard Compose convention (Material 3 components define their own defaults locally).

Examples:
- `ClubCommentsCard.kt`: `private val CommentBubbleCornerRadius = 12.dp`, `private val CommentBubbleMaxWidth = 280.dp`, `private const val CommentInputMaxLines = 3`
- `PersonalNotesCard.kt`: `private val NotesMaxHeight = 240.dp`, `private const val NotesMaxChars = 5000`, `private const val NotesMinLines = 4`, `private const val NotesMaxLines = 10`
- `DescriptionCard.kt`: `private const val CollapseThreshold = 150`, `private const val MaxLinesCollapsed = 3`
- `ClubReviewsCard.kt`: `private const val ReviewMinLines = 2`, `private const val ReviewMaxLines = 5`
- `BookPlaceholder.kt`: `private val PlaceholderIconSize = 64.dp`

Each component owns its own single-use values — no shared import, no coupling.

---

## Part 5: Move Business Logic Out of Composables

Audit found 5 places where composables contain logic that belongs in the ViewModel or state layer.

### 5a. Club average rating calculation — `ClubRatingCard.kt` (lines 37-43)

The composable filters reviews (`rating > 0`) and computes an average. This is data aggregation based on a business rule.

**Fix**: Add `clubAverageRating: Float` to `BookDetailState`. Compute it in the ViewModel when `clubReviews` are loaded/updated. `ClubRatingCard` receives the pre-computed value instead of the raw review list.

### 5b. Review filtering + user check — `ClubReviewsCard.kt` (lines 46-48)

The composable filters reviews to those with text and checks if the current user has an existing review. Both are business-rule-based filtering.

**Fix**: Add `clubReviewsWithText: List<BookReview>` and `userHasExistingReview: Boolean` to `BookDetailState`. Compute in ViewModel when reviews load. `ClubReviewsCard` receives pre-filtered data.

### 5c. `isTutorialBook` computed in composable — `BookDetailScreen.kt` (line 73)

The screen computes `val isTutorialBook = state.book.id == BookDetailConstants.TUTORIAL_BOOK_ID`. This is a domain-level identity check.

**Fix**: Add `val isTutorialBook: Boolean get() = book?.id == BookDetailConstants.TUTORIAL_BOOK_ID` as a computed property on `BookDetailState` (same pattern as the existing `isSignedIn`). The screen reads `state.isTutorialBook` directly.

### 5d. `formatRelativeTime()` calls in composables — KEEP AS-IS

`ClubCommentsCard.kt` (line 260) and `ClubReviewsCard.kt` (line 183) call `formatRelativeTime()` directly. While this looks like a formatting violation, relative time is inherently a **render-time computation** — it depends on `System.currentTimeMillis()` at display time. Pre-computing in the ViewModel would freeze timestamps at the moment state was emitted ("2 min ago" stays "2 min ago" after 30 minutes unless another state change triggers re-emission). Recomposition gives free staleness correction today.

**Decision**: Leave `formatRelativeTime()` in composables. This is presentation-layer display logic, not business logic.

### 5e. "Edited" check in composable — `ClubCommentsCard.kt` (line 266)

The composable checks `comment.updatedAt > comment.createdAt` to decide whether to show "(edited)". This is a domain concept ("has this comment been edited?").

**Fix**: Add an extension property in `bookdetail/presentation/util/` (alongside `TimestampFormatter.kt`). The composable reads `comment.isEdited` directly. Keeps the domain model clean — `BookComment` in `book/domain/model/` retains only the raw timestamps.

### Summary of state changes

```kotlin
// New computed properties on BookDetailState
val isTutorialBook: Boolean get() = book?.id == BookDetailConstants.TUTORIAL_BOOK_ID

// New fields on BookDetailState
val clubAverageRating: Float = 0f
val clubReviewsWithText: List<BookReview> = emptyList()
val userHasExistingReview: Boolean = false

// isEdited on BookComment (extension property or added to model)
val BookComment.isEdited: Boolean get() = updatedAt > createdAt
```

---

## Implementation Order

| Step | Part | Description | Risk |
|------|------|-------------|------|
| 1 | 1a | Gate PurchasedToggleCard behind `onShelf` | Low — single condition |
| 2 | 1b | Create SignInHintCard component | Low — new file, display-only |
| 3 | 5a-c | Move isTutorialBook, averageRating, reviewsWithText to state | Low — state + ViewModel changes |
| 4 | 5d | Add isEdited extension to BookComment, use in composable | Low — single extension property |
| 5 | 2a | Create BookHeroSection composable | Medium — new component replacing two |
| 6 | 2c | Top app bar title refinement | Low — single line change |
| 7 | 3 | Restructure screen using BookHeroSection + linear flow | Medium — main refactor |
| 8 | 2b | Apply OutlinedCard to info section, add spacing | Low — styling pass |
| 9 | 4 | Create BookDetailUiConstants + replace magic numbers | Low — mechanical, no logic changes |

Steps 1-2 are standalone fixes. Steps 3-4 clean up architecture violations — done before the structural refactor so composables are already simplified when we restructure. Step 5 creates the hero component. Step 6 is a quick independent fix. Step 7 is the main restructure. Step 8 is styling. Step 9 is the mechanical constants cleanup — done last since earlier steps change line numbers and delete duplicated code.

---

## Out of Scope

These items from `post-library-improvements.md` are tracked separately:

- **Delete book from library** — separate feature with dialog + repository changes
- **Add book to shelf from library** — new shelf picker flow
- **Replace FAB with inline add-book slot** — bookshelf UI, not bookdetail
- **Spine color bug** — bookshelf rendering issue
- **Anchor delete account at bottom** — account screen
