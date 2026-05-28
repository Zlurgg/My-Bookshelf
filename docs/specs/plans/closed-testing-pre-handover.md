# Handover — `closed-testing-pre` branch

**Date:** 2026-05-27
**Branch:** `closed-testing-pre` (intended to be merged into `main` before closed testing actually starts)
**Author note:** Captured at end of a long working session covering release prep + smoke-test findings. Written so the next session can pick up cold.

## What this PR / merge contains

Three independent changes plus documentation:

1. **`fix(bookdetail)` — Play Books URLs + honest button label.**
   The Books API's `infoLink` redirects to Play Books (`https://play.google.com/store/books/details?…`) for purchasable titles. The launch-site allowlist only permitted `https://books.google.com/`, so the "View on Google Books" button silently dropped. Allowlist now permits `https://play.google.com/store/books/` too (with the `/store/books/` path segment enforced — not the bare host). Button label switches to "View on Google Play" when the URL is a Play Books link. File `ExternalUrl.kt` renamed to `GoogleBooksUrl.kt` (the implementation was always Google-specific; the generic name was dishonest about scope). All call sites + tests updated.

2. **`feat(theme)` — disable Material You dynamic colours.**
   Default-flip in `MyBookshelfTheme.kt`: `dynamicColor = false`. The curated palette now wins over system-derived dynamic colours on devices that support them. All call sites use the default; no explicit `dynamicColor = true` overrides exist. Easy to revisit later if you want a user-facing "use system colours" preference.

3. **`docs(plans)` — followups + new deferred plan.**
   - `google-books-followups.md` extended with items 4 (`saleInfo` capture for accurate buy/read buttons + price), 5 (UK book affiliate program investigation — Bookshop.org, Amazon Associates, Hive; Google Play and Apple Books programs are dead since 2019), 7 (pointer to the preview-cache leak plan), 8 (cover-image quality finding — documented as "not planned" with reasoning).
   - **New plan: `preview-cache-library-leak.md`** — see below.

## What was deferred and where to find it

### Preview-cache library leak (fixed in the follow-up session)

Tapping a search-result row in either the Bookshelf or Library search dialog used to write the previewed book to the local DB before navigating to the detail screen. The library query treats all DB rows that aren't on a shelf as "in the library", so previewed books appeared in the user's library even though they only intended to look. Eight-month-old latent bug, surfaced once the Library screen was added.

**Fixed via Option C (repository-level preview cache), not Option A as originally planned.** With pre-release constraints relaxed (no live users, no schema-freeze) and after confirming the navigation layer is string-based rather than type-safe, the repository-cache approach won on smaller surface and cleaner architectural alignment. `BookRepositoryImpl` now holds a process-scoped `ConcurrentHashMap` cache populated by `SearchBooksUseCase` and read by `getBookById` as a DB fallback. The `Book` domain model and `NavigationRoute.BookDetail` are untouched. Full landed design in `docs/specs/plans/preview-cache-library-leak.md` under the "Chosen fix" section.

The trade-off: cache is lost on process death. If Android kills the app between search-tap and detail render, the user lands on an empty detail screen and has to re-tap. Sub-second window on modern devices.

The schema-flag approach (`cachedAt`/`isPreviewOnly` column) was again considered and rejected — orphan-row problem stands regardless of release stage. Do not re-propose.

The race-fix from commit `4016bbd5` is preserved structurally. The race itself is no longer reachable (the click handler is now synchronous) so `bookshelf-navigation-race.md` should be annotated "race no longer reachable" if revisited.

### Cover-image quality (not planned)

Documented as `google-books-followups.md` item 8. Some Google Books records (e.g. Frank Herbert's *Dune* 1965 Chilton edition) have a usable thumbnail at `zoom=1` but a white-bar placeholder at `zoom=3`. Our spine + detail screens use `zoom=3` (via `withSpineImage` / `withMediumImage`), so those records render with a blank cover. Decision: accept the limitation; the fix surface is large (DTO change + per-surface picker + placeholder-detection heuristic) and user impact is cosmetic. Revisit only if it becomes a recurring complaint.

## Closed-testing checklist — still outstanding

These are *not* in this PR. They are release-prep gates from `docs/specs/plans/closed-testing-release-prep.md` that remain after this merge:

- **Phase 2.1** (Crashlytics end-to-end verify): code is in, on-device verification pending. Two tests to run before tester upload — (a) throw a deliberate `RuntimeException` from a release build, confirm it appears in the Firebase Crashlytics console; (b) rebuild with `GOOGLE_BOOKS_API_KEY=` blank, install, trigger a search, confirm a non-fatal log surfaces.
- **Phase 4.1 strict DoD**: smoke test done, but the DoD requires "fresh install of the **exact APK to be uploaded**." Fixes in this merge weren't on the smoke-tested APK. Reinstall + 2-min spot-check needed once the preview-cache fix also lands.
- **Phase 4.2**: quota sanity-check after ~24h of actual tester traffic.
- **Tester comms**: "Uninstall MyBookshelf before installing the next build." Required because the Room schema was reset to v1 with no downgrade migration — existing v5 installs will crash on first launch under the new build.

## Branch hygiene for the next session

The natural sequence:

1. Merge `closed-testing-pre` into `main`.
2. Branch `fix/preview-cache-library-leak` off updated `main`.
3. Execute `docs/specs/plans/preview-cache-library-leak.md` on the new branch.
4. PR back to `main`.
5. *Then* tackle the remaining closed-testing checklist (Phase 2.1 verification, Phase 4.1 reinstall spot-check, tester comms, upload).

The preview-cache fix is architecturally orthogonal to anything else on `closed-testing-pre` — it touches navigation + ViewModels + the domain `Book` type. No conflicts expected.

## Pointers for the next session

| Where | What |
|---|---|
| `docs/specs/plans/preview-cache-library-leak.md` | The deferred bug fix — file-by-file plan, test surgery notes, DoD, history. |
| `docs/specs/plans/google-books-followups.md` items 4, 5, 8 | Other open Google Books work (saleInfo, affiliate monetisation, cover-image quality). |
| `docs/specs/plans/closed-testing-release-prep.md` | Master release-prep plan; the remaining items above live here. |
| `docs/specs/operations/api-key-restrictions.md` | Cloud Console SHA-1 allowlist state (debug + release). |
| Memory: `preview-cache-library-leak.md` | Surfaces in any future conversation about this codebase; carries the "no schema flag" decision forward. |

## What I'd recommend the next session do first

Read this handover. Read `preview-cache-library-leak.md`. Then make the changes. The plan is detailed enough that no fresh investigation should be needed. If anything in the plan surprises future-you, prefer reading the linked commits over re-deriving — the 8-month history is what makes this bug subtle, and the commits are evidence.
