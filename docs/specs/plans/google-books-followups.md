# Google Books — Future Follow-ups

**Status:** Captured during `spike-test-google-books` delivery; none yet scheduled.
**Origin:** Consolidates open items from three plans archived after delivery in 2026-05-26: `google-books-api-integration.md` Section 10 (pagination, PrintType filter), `google-books-search-quality.md` items 3 + 5 (no-author junk, navigation-state preservation). Items 4 + 5 (`saleInfo` capture, affiliate monetisation) were added 2026-05-27 from closed-testing smoke-test findings. The original plans are recoverable from git history if rationale is needed.
**Scope:** Items deferred or surfaced after the spike branch closed. Each is small enough to land independently; they don't form a single feature.

## Items

### 1. Pagination

Neither the OL integration before the spike nor the Google Books integration after it implements pagination. Both fetch a fixed cap (40 for Google, 15 for the legacy OL path). `BookSearchResponse.totalResults` was dropped during spike delivery (no consumer), so adding pagination needs:

- Re-introduce a `totalResults` (or equivalent) at the `BookSearchResponse` / `SearchResult` layer.
- Thread `startIndex` (Google) / `offset` (OL) through `RemoteBookDataSource.searchBooks(...)`.
- `BookApiService` will likely need provider-specific interfaces at this point — OL uses `offset`, Google uses `startIndex`, and a unified parameter name has to map to both.
- UI surface for "load more" or infinite scroll in `BookSearchDialog`.

Not blocking. Users who can't find a book in the first 40 results can refine their query.

### 2. PrintType filtering

`PrintType` enum (`BOOK` / `MAGAZINE` / `UNKNOWN`) was added to `Book` during the spike but is not consumed for filtering. Add a user-toggleable filter (alongside the existing safe-search switch) to exclude magazines, or extend `SafeSearchFilter` to filter on `printType` when the user opts in.

Trivial change once a UX placement is chosen.

### 3. Optional: no-author junk filter

Google occasionally returns books with no authors — typically PediaPress Wikipedia compilations. If they become a visible pattern, filter at the data source in `GoogleBooksRemoteBookDataSource` alongside the existing language and blank-title filters:

```kotlin
?.filter { !it.volumeInfo?.authors.isNullOrEmpty() }
```

Currently rare; not worth blanket-applying without a real user-visible problem. Keep this as a "fix when noticed" item.

### 4. Capture `saleInfo` and label the buy/read button accurately

`GoogleBooksSearchResponseDto` currently deserialises `volumeInfo` and `searchInfo` only. Google also returns a `saleInfo` block per volume:

```json
"saleInfo": {
  "saleability": "FOR_SALE" | "FREE" | "NOT_FOR_SALE" | "FOR_SALE_AND_RENTAL" | "FOR_RENTAL_ONLY",
  "buyLink": "https://play.google.com/store/books/details?id=…",
  "listPrice":   { "amount": 9.99, "currencyCode": "GBP" },
  "retailPrice": { "amount": 7.99, "currencyCode": "GBP" }
}
```

Today the "View on Google Books" button is wired to `infoLink`, which redirects to Play Books for purchasable titles. Surfaced on 2026-05-27: the button copy was misleading (now mitigated by labelling it "View on Google Play" when the URL matches `https://play.google.com/store/books/` — see `PublicationDetailsCard.kt` + `ExternalUrl.isPlayBooksUrl`). That's still a workaround; the proper fix is to know the *intent* of the link instead of inferring from the URL host.

**What this would unlock:**
- Distinct "Buy on Play Books — £7.99" button only when `saleability` is `FOR_SALE` or `FOR_SALE_AND_RENTAL`.
- "Read free on Play Books" CTA for `FREE` titles (public-domain works often have this).
- Hide the buy/read button entirely for `NOT_FOR_SALE` and fall back to the existing "View on Google Books" → `infoLink` behaviour.
- Show price inline on the detail screen (`retailPrice` preferred over `listPrice` when both are present).

**Scope:**
- Add `GoogleSaleInfoDto` + `GooglePriceDto` to `GoogleBooksSearchResponseDto.kt`.
- Add `saleability`, `buyLink`, `price` fields to `Book` (domain) and persist on `BookEntity` (Room migration to v2).
- HTTPS coerce `buyLink` in `GoogleBookMappers` exactly like `infoLink` / `previewLink`. Add `https://play.google.com/store/books/` to the launch allowlist for `buyLink` too (already there for `infoLink` — verify reuse).
- The OpenLibrary fallback path has no equivalent sale info; for OL-sourced books these fields stay null and the UI continues to show only the existing OL-side "View online" link. That's fine — `OpenLibraryRemoteBookDataSource` doesn't need to change, and the detail screen already handles null gracefully.

**Pairs naturally with item 5 below** if we ever pursue affiliate monetisation, since both depend on knowing whether a book is purchasable.

### 5. Affiliate monetisation — investigate UK book affiliate programs

Google Books API attribution (`source=gbs_api`) is not a paid referral, and the **Google Play Affiliate Program was discontinued in 2019** — so the existing "View on Google Books"/"View on Google Play" buttons earn nothing. If we want to monetise outbound traffic without compromising UX, the live options as of 2026:

| Program | UK availability | Notes |
|---------|----------------|-------|
| **Bookshop.org UK** | Yes | Pays a percentage to the affiliate, funds independent bookshops. Strong fit for a reader-focused indie app, both ethically and audience-wise. |
| **Amazon Associates UK** | Yes | Largest catalogue and highest conversion, but lowest per-sale cut for books and least aligned with an indie-app brand. |
| **Hive.co.uk** | Yes | Affiliate API exists; smaller catalogue than Amazon, supports indie bookshops. |
| **Apple Books Affiliate** | Discontinued 2019 | — |
| **Google Play Affiliate** | Discontinued 2019 | — |

**Implementation sketch (when prioritised):**
- ISBN-based affiliate URL builder, gated behind a feature flag so we can ship dark before turning on.
- Surface as an additional "Buy from [program]" button on the detail card, alongside (not replacing) the existing Google/Play link. Users keep the choice.
- Depends on item 4 above for the `saleability` signal — only show the affiliate CTA on books that are actually purchasable, so the button doesn't 404 on out-of-print titles.
- Disclosure: required by all three programs (a "Some links may earn us a commission" line in About or near the button).

Not a near-term ask. Captured here so we don't lose the thread when item 4 lands.

### 6. Preserve search-dialog state across preview-and-back navigation

Surfaced during on-device testing. When a user searches, taps a result to preview it, then navigates back from the detail screen, the search dialog comes up empty — query and result list are gone. The user has to retype and re-search, which is friction when they're browsing several books in the same search.

**Likely causes (investigate before fixing):**
- `OnSearchResultBookClick` in the relevant ViewModel may close the dialog as a side-effect of triggering navigation.
- The `navigateToBook` flag is cleared on return — that clear may also reset `bookSearchState.query` and `.results`.
- The dialog visibility flag (`isSearchDialogVisible`) is being toggled off as part of the navigation flow.

**Possible fixes, by increasing scope:**
- **Cheapest:** keep `bookSearchState` intact through the navigate-out + navigate-back round trip; reopen the dialog automatically on return so the user lands back on their results.
- **Moderate:** keep the dialog visible while the detail screen sits in front of it (so back drops the user straight into the still-open dialog, no flicker).
- **Largest:** convert the preview pane from a separate navigation destination to a bottom-sheet overlay above the search dialog — the user never leaves the search context.

Affects both `BookshelfScreen` and `LibraryScreen` (both invoke `BookSearchDialog` and both call `OnSearchResultBookClick`).

### 7. Preview-cache library leak — separate plan

Out of scope for this doc but adjacent: the `OnSearchResultBookClick` handler in both `BookshelfViewModel` and `LibraryViewModel` upserts the tapped book into the local DB before navigating to the detail screen. The library query at `BookDao.getAllPersonalBooks()` then treats those rows as library entries because the storage layer has no signal distinguishing "previewed" from "explicitly added." Tapping a search result silently adds it to the user's library.

Latent since 2025-09-03 (commit `cb611f02`); made user-visible by the Library screen (commits `d5ec0f76` → `9b839e03`); confirmed during the 2026-05-27 closed-testing smoke test.

Full design + DoD in **[preview-cache-library-leak.md](preview-cache-library-leak.md)**. Closed testing ships with the bug known and noted to testers; the fix (Option A, nav-route seed) is the targeted next-session work.

### 8. Cover-image quality on the detail screen — documented, not planned

Surfaced 2026-05-27 during closed-testing smoke test (Frank Herbert's *Dune*, 1965 Chilton Books edition, id `ZnwFAQAAIAAJ`). Search shows a usable thumbnail; spine and detail screen show a white bar where the cover should be. Investigation produced a clear root-cause and an equally clear "won't fix" decision.

**Root cause is two-layered.**

First layer: we only capture one image URL per book. `GoogleImageLinksDto` deserialises `smallThumbnail` and `thumbnail` (the latter is what we store on `Book.imageUrl`); Google also returns `small`, `medium`, `large`, `extraLarge`, all ignored. So every surface in the app starts from the same single URL (`zoom=1`, ~128px). To get something bigger for the detail screen and spines, `BookImageUtils.withMediumImage()` / `withSpineImage()` rewrite the URL to `zoom=3`.

Second layer: `zoom=1` and `zoom=3` are not the same image at different resolutions — they are different scan tiers per Google's API. Most books have a coherent set across tiers; some records (often academic-library scans like the Chilton *Dune*) have a real low-res thumbnail at `zoom=1` and a placeholder white-bar at `zoom=3`. The transform we apply for spine/detail puts those records into the white-bar tier specifically.

**Resolution sufficiency by surface** (xxhdpi 3x device, typical modern phone):

| Surface | Render dp | Pixels needed | `zoom=1` (~128px) | `zoom=3` (~300px) |
|---|---|---|---|---|
| Search row | 56 × 84 | 168 × 252 | Borderline (soft) | Fine |
| Spine (paperback) | ~45 × 45 | 135 × 135 | Fine | Fine |
| Detail screen | ~133 × 200 | ~400 × 600 | Insufficient | Soft |

So `zoom=3` is the right call for spine and detail on most records. The Dune-1965 case is a Google data limitation, not a sizing bug — Google has no good high-res scan for that edition.

**What a real fix would require:**

1. Capture `imageLinks.{small,medium,large,extraLarge}` in `GoogleImageLinksDto`. Add fields to `Book` or compute the "best available URL per surface" at render time.
2. For records like Dune-1965 where every tier above the thumbnail is the white-bar placeholder, detect the placeholder and fall back to `BookIconPlaceholder`. Detection requires image-content analysis — load the bytes, compute a low-entropy / solid-colour signal, threshold it. That's an image parser, and the false-positive risk (real white covers misclassified as placeholders) is non-trivial.

**Decision: not planned.** Accept the limitation. The cohort of books affected is small enough (mostly older / academic-library scans), the fix surface is large (DTO change + per-surface picker + heuristic placeholder detection), and the user impact is cosmetic. If this becomes a recurring complaint after wider rollout, revisit — start with the DTO change alone (which is cheap and helps every other record), and only consider the placeholder-detection heuristic if the white-bar cases remain prominent.
