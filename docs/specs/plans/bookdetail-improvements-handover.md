# Book Detail Improvements — Implementation Handover

## Context

Branch: `bookdetails-improvements` (off `main`)
Plan: `docs/specs/plans/bookdetail-improvements.md` — fully reviewed and approved.
Goal: Unify the BookDetail screen across its 3 code paths, fix content bugs, improve visual hierarchy, clean up architecture violations, and extract magic numbers.

## What to read before starting

1. **The plan** — `docs/specs/plans/bookdetail-improvements.md` is the source of truth. Every decision has been reviewed. Follow it as written.
2. **Constitution** — `docs/specs/constitution.md` — non-negotiable architecture rules.
3. **Compose screen pattern** — `docs/specs/patterns/compose-screens.md` — Root/Screen composable conventions.
4. **State management pattern** — `docs/specs/patterns/state-management.md` — ViewModel state/action pattern.

## Implementation steps (9 total, in order)

### Step 1 — Gate PurchasedToggleCard behind `onShelf` [Low risk]
- File: `bookdetail/presentation/BookDetailScreen.kt` line 341
- Wrap PurchasedToggleCard in `if (state.onShelf)` — same pattern as RecommendationStatusCard (line 282) and PersonalNotesCard (line 298)

### Step 2 — Create SignInHintCard [Low risk]
- New file: `bookdetail/presentation/components/SignInHintCard.kt`
- Card wrapper, `secondaryContainer` background, `Icons.Outlined.Lock` icon, existing string `R.string.club_detail_sign_in_hint`
- Display-only — no tap action, no new BookDetailAction
- Replace bare `Text` at BookDetailScreen.kt lines 227-233

### Step 3 — Move business logic to state [Low risk]
- **`BookDetailState.kt`**: Add computed property `val isTutorialBook: Boolean get() = book?.id == BookDetailConstants.TUTORIAL_BOOK_ID` (follows existing `isSignedIn` pattern). Add fields: `clubAverageRating: Float = 0f`, `clubReviewsWithText: List<BookReview> = emptyList()`, `userHasExistingReview: Boolean = false`
- **`BookDetailViewModel.kt`**: Compute `clubAverageRating` when `clubReviews` load (filter `rating > 0`, average). Compute `clubReviewsWithText` (filter `reviewText.isNotBlank()`). Compute `userHasExistingReview` (`reviews.any { it.userId == currentUserId && it.reviewText.isNotBlank() }`)
- **`BookDetailScreen.kt`**: Replace `val isTutorialBook = state.book.id == BookDetailConstants.TUTORIAL_BOOK_ID` with `state.isTutorialBook`
- **`ClubRatingCard.kt`**: Change param from `reviews: List<BookReview>` to `averageRating: Float`. Remove lines 37-43 (the filter+average computation)
- **`ClubReviewsCard.kt`**: Change param to receive pre-filtered list + boolean. Remove lines 46-48

### Step 4 — Add isEdited extension [Low risk]
- New file: `bookdetail/presentation/util/BookCommentExtensions.kt` (alongside `TimestampFormatter.kt`)
- `val BookComment.isEdited: Boolean get() = updatedAt > createdAt`
- Update `ClubCommentsCard.kt` line 266: replace `comment.updatedAt > comment.createdAt` with `comment.isEdited`
- **Keep `formatRelativeTime()` calls in composables** — render-time computation, moving it would cause staleness

### Step 5 — Create BookHeroSection [Medium risk]
- New file: `bookdetail/presentation/components/BookHeroSection.kt`
- Column layout on a `Surface` with `Color.Transparent` — intentionally unboxed
- Image at full width with rounded top corners, overview text below (title `headlineSmall`, authors/metadata `bodyMedium` with `onSurfaceVariant`)
- Params: title, authors, firstPublishYear, numPages, numEditions, imageUrl, onImageLoadResult
- For no image: overview text renders alone
- Replaces separate `BookOverviewCard` + `BookDetailImage` items

### Step 6 — Top app bar title [Low risk]
- `BookDetailScreen.kt` line 78: Change `titleLarge` to `titleMedium`, add `maxLines = 1, overflow = TextOverflow.Ellipsis`

### Step 7 — Restructure screen [Medium risk, core refactor]
- Replace the 3 branching blocks (tutorial/club/regular) with a single linear flow:
  1. Common hero: `BookHeroSection` (all paths)
  2. Variant-specific middle: `when` block — tutorial=empty, club+signed=rating+comments, club+guest=SignInHintCard, regular=personal cards gated by `onShelf` (recommendation + notes + purchased)
  3. Common info section: `!isTutorialBook` gates on CommunityRatingsCard, PublicationDetailsCard, LanguagesCard. DescriptionCard always shown with `initiallyExpanded = isTutorialBook`
- **Tutorial DescriptionCard keeps elevated Card** (not OutlinedCard) — `outlined = !isTutorialBook` param
- **First info card** gets `Modifier.padding(top = 4.dp)` for 16.dp total gap (12dp arrangement + 4dp padding)
- PurchasedToggleCard lives inside the regular-book branch, gated by `onShelf`
- See pseudo-code in the plan for exact structure

### Step 8 — OutlinedCard for info section [Low risk]
- Switch CommunityRatingsCard, PublicationDetailsCard, LanguagesCard to `OutlinedCard` with `0.dp` elevation and `outlineVariant` border at 1.dp
- DescriptionCard: add `outlined` param — uses OutlinedCard when true, regular Card when false
- First info card gets `Modifier.padding(top = 4.dp)`

### Step 9 — Extract magic numbers to constants [Low risk]
- **Shared** — New file `bookdetail/presentation/BookDetailUiConstants.kt`: `CardElevation` (2.dp), `CardContentPadding` (16.dp), `SectionSpacing` (12.dp), `InfoSectionExtraTopPadding` (4.dp), `SmallSpacing` (8.dp), `TinySpacing` (4.dp), `BookImageHeight` (200.dp), `ImageCornerRadius` (8.dp), `MaxStars` (5), `DebounceDelayMs` (2000L). Use `val` for Dp, `const val` for primitives.
- **Component-local** — `private val`/`private const val` at file scope in each component for single-use values. Examples: `CommentBubbleCornerRadius` in ClubCommentsCard, `NotesMaxHeight` in PersonalNotesCard, `CollapseThreshold` in DescriptionCard. See plan for full list.

## Key decisions made during planning (do not revisit)

- **No Row layout for hero** — Column on transparent Surface. Row cramps content on narrow phones.
- **`formatRelativeTime()` stays in composables** — render-time computation, moving to ViewModel causes staleness.
- **Tutorial DescriptionCard keeps elevated Card** — OutlinedCard would look less prominent for onboarding.
- **SignInHintCard is display-only** — no navigation to sign-in, no new actions.
- **Constants split: shared object + component-local** — no god-object. Only values used by 2+ components go in the shared object.
- **PurchasedToggleCard in variant section** — not after info cards. Sits with other personal-interactive cards.

## Files you will touch

| File | Steps |
|------|-------|
| `bookdetail/presentation/BookDetailScreen.kt` | 1, 2, 3, 6, 7 |
| `bookdetail/presentation/BookDetailState.kt` | 3 |
| `bookdetail/presentation/BookDetailViewModel.kt` | 3, 9 |
| `bookdetail/presentation/components/ClubRatingCard.kt` | 3, 9 |
| `bookdetail/presentation/components/ClubReviewsCard.kt` | 3, 9 |
| `bookdetail/presentation/components/ClubCommentsCard.kt` | 4, 9 |
| `bookdetail/presentation/components/BookOverviewCard.kt` | 7 (replaced by hero), 9 |
| `bookdetail/presentation/components/BookDetailImage.kt` | 7 (replaced by hero), 9 |
| `bookdetail/presentation/components/CommunityRatingsCard.kt` | 8, 9 |
| `bookdetail/presentation/components/DescriptionCard.kt` | 8, 9 |
| `bookdetail/presentation/components/PublicationDetailsCard.kt` | 8, 9 |
| `bookdetail/presentation/components/LanguagesCard.kt` | 8, 9 |
| `bookdetail/presentation/components/PersonalNotesCard.kt` | 9 |
| `bookdetail/presentation/components/RecommendationStatusCard.kt` | 9 |
| `bookdetail/presentation/components/PurchasedToggleCard.kt` | 9 |
| `bookdetail/presentation/components/ShelfActionsCard.kt` | 9 |
| `bookdetail/presentation/components/BookPlaceholder.kt` | 9 |

**New files:**
- `bookdetail/presentation/components/SignInHintCard.kt` (Step 2)
- `bookdetail/presentation/components/BookHeroSection.kt` (Step 5)
- `bookdetail/presentation/util/BookCommentExtensions.kt` (Step 4)
- `bookdetail/presentation/BookDetailUiConstants.kt` (Step 9)

## Commit strategy

One commit per step. Conventional commits format per CLAUDE.md:
- `fix(bookdetail): gate PurchasedToggleCard behind onShelf`
- `feat(bookdetail): add SignInHintCard component`
- `refactor(bookdetail): move business logic from composables to state`
- `refactor(bookdetail): add isEdited extension for BookComment`
- `feat(bookdetail): add BookHeroSection composable`
- `fix(bookdetail): truncate long titles in top app bar`
- `refactor(bookdetail): restructure screen to eliminate duplication`
- `style(bookdetail): apply OutlinedCard to info section`
- `refactor(bookdetail): extract magic numbers to constants`

## What not to do

- Don't add features beyond the plan (no delete book, no shelf picker)
- Don't add Co-Authored-By footers or "Generated with Claude Code" signatures
- Don't create UI model wrappers (CommentUiModel/ReviewUiModel) — that approach was explicitly rejected
- Don't move `formatRelativeTime()` out of composables
- Don't use `!!` operator, LiveData, or Context in ViewModels
