# Architecture Restructure Plan

## Problem

The `bookshelf/` package contains 241 files treating everything as one flat feature. The real issue isn't that there are too many screens — it's that:

1. **Book club** is a separate domain that grew inside bookshelf
2. **Shared book domain** (models, repos) is trapped inside a feature package
3. **Unrelated features** (welcome, sharing/deeplinks) are stuffed in
4. **Everything is flat** — bookcase, shelf, club, details are treated as peers when they have a natural hierarchy

## The App's Actual Structure

```
Bookcase (root screen)
├── Tab: My Shelves
│   └── Shelf (books on one shelf)
│       └── Book Detail (one book — also reachable from clubs)
├── Tab: Book Clubs
│   └── Club Shelf (books in club — functionally same as shelf + social)
│       └── Book Detail (same screen, with reviews/comments)
└── Settings menu (sign in/out, help)

Welcome (onboarding — completely separate)
Sign In (auth — completely separate)
Deep Link Handler (entry point, not a screen in the hierarchy)
```

Book detail belongs to **book**, not to shelf or club — a book can exist on multiple shelves and in clubs.

## Target Structure (3 steps, not 8)

```
uk.co.zlurgg.mybookshelf/
├── app/                        # Application, navigation, MyBookShelfApp
├── di/                         # Root AppModule
├── core/                       # Infrastructure (unchanged)
├── auth/                       # Authentication (unchanged)
├── sync/                       # Cloud sync (unchanged)
│
├── book/                       # NEW — shared book domain + data
│   ├── domain/
│   │   ├── model/             # Book, Bookshelf, Bookcase, ReadingStatus, ShelfStyle, etc.
│   │   ├── repository/        # BookRepository, BookcaseRepository, BookshelfRepository
│   │   ├── service/           # BookColorGenerator
│   │   └── util/              # ShelfStyle, BookshelfConstants, BookDetailConstants
│   ├── data/
│   │   ├── dto/               # SearchResponseDto, BookWorkDto, etc.
│   │   ├── network/           # OpenLibraryApiService, KtorRemoteBookDataSource
│   │   ├── repository/        # BookRepositoryImpl, BookcaseRepositoryImpl, BookshelfRepositoryImpl
│   │   └── mappers/           # Book/Shelf entity mappers
│   ├── presentation/
│   │   ├── preview/           # Sample data for Compose previews
│   │   ├── components/        # MessageDialog, shared book UI components
│   │   └── util/              # BookDisplayUtils, BookImageUtils
│   └── di/                    # BookModule
│
├── bookclub/                   # NEW — extracted from bookshelf
│   ├── domain/
│   │   ├── model/             # BookClub, BookClubReview, BookClubComment, Membership
│   │   ├── repository/        # BookClubManagement/Membership/Sync/ReviewRepository + impls
│   │   ├── usecase/           # All club use cases
│   │   └── service/           # BookClubCodeGenerator
│   ├── data/
│   │   ├── repository/        # All BookClub*RepositoryImpl classes
│   │   └── service/           # BookClubCodeGeneratorImpl
│   ├── presentation/          # Club components, handlers
│   └── di/                    # BookClubModule (already exists)
│
├── bookshelf/                  # STAYS — but cleaner
│   ├── domain/usecase/
│   │   ├── bookcase/          # GetAllShelves, CreateShelf, DeleteShelf, etc.
│   │   ├── bookshelf/         # SearchBooks, GetShelfBooks, AddBookToShelf, etc.
│   │   └── bookdetail/        # GetBookDetails, UpsertBook, TogglePurchase, etc.
│   ├── presentation/
│   │   ├── bookcase/          # BookcaseViewModel, BookcaseScreen (root — hosts shelves + clubs tabs)
│   │   ├── bookshelf/         # BookshelfViewModel, BookshelfScreen (shelf view + search)
│   │   └── bookdetail/        # BookDetailViewModel, BookDetailScreen (book view + club reviews)
│   └── di/                    # BookshelfModule (slimmed — no club use cases)
│
├── sharing/                    # NEW — extracted from bookshelf
│   ├── domain/
│   │   ├── usecase/           # Export + import use cases (from deeplink/ + export/)
│   │   └── service/           # BookshelfSerializer, ImportValidator, ShareTokenService, etc.
│   ├── data/                  # JsonBookshelfSerializer, UrlEncodedShareTokenService, export DTOs
│   ├── presentation/          # DeepLinkScreen, DeepLinkViewModel
│   └── di/                    # SharingModule
│
└── welcome/                    # NEW — extracted from bookshelf
    ├── domain/usecase/        # Tutorial shelf/book creation, welcome check
    ├── presentation/          # WelcomeScreen, WelcomeService
    └── di/                    # WelcomeModule
```

## Why This Layout

- **`book/`** is shared infrastructure (like `core/` but for the book domain). Models, repos, preview data, shared UI utils all live here. Multiple features depend on it, none own it.
- **`bookclub/`** is genuinely separate — it has its own models (BookClub, Review, Comment, Membership), its own repos, its own use cases. The only coupling to bookshelf is that BookcaseViewModel shows clubs in a tab, and BookDetailViewModel shows reviews. Those are import dependencies, not ownership.
- **`bookshelf/`** keeps its screens together because they ARE the same feature — managing books on shelves. Bookcase → Shelf → Detail is one navigation flow. The screens share domain knowledge (shelves, books, search) and splitting them would create artificial boundaries with high coupling.
- **`sharing/`** and **`welcome/`** are self-contained features that don't belong in bookshelf.

## Dependency Flow Principle

**Dependencies flow downward.** Parent screens should not import from child/sibling features. When we find dependency inversion or other violations of our architectural principles during the restructure, we fix them at the time — not defer.

```
book/ (shared domain — no feature dependencies)
  ↑
bookshelf/ → book/          (uses shared models + repos)
bookclub/  → book/          (uses shared models + repos)
sharing/   → book/          (uses shared models for export)
welcome/   → book/          (uses shared models for tutorial)
```

**Bookshelf should NOT depend on bookclub.** Bookcase is the parent that hosts club tabs — the dependency must be inverted.

## Cross-Feature Coupling — Violations to Fix

### 1. `BookcaseViewModel` → `BookClubOperationsHandler` (WRONG DIRECTION)

Bookcase is the parent screen. It should not import from bookclub. Currently BookcaseViewModel directly calls `BookClubOperationsHandler` for create, join, leave, invite operations.

**Fix:** Define `ClubOperations` interface in `book/domain/service/` with the 7 methods actually used by `BookcaseClubActionHandler` + `BookcaseViewModel`. `BookClubOperationsHandler` in `bookclub/` implements it. Bookshelf depends on the interface via Koin. Dependency flows downward: bookclub → book (implements), bookshelf → book (consumes).

Created in Step 2 (not Step 1) — the concrete class still exists and works until Step 2 moves it.

### 2. `BookDetailViewModel` → `BookClubReviewUseCases` (WRONG DIRECTION)

Book detail shows club reviews/comments but shouldn't know about bookclub's use case aggregator.

**Fix:** Define `BookReviewProvider` interface in `book/domain/service/` with 7 review/comment methods. Returns slim `BookReview`/`BookComment` DTOs from `book/domain/model/` (not club-specific models). `bookclub/` implements the interface and maps its rich `BookClubReview` → `BookReview` at the boundary. BookDetailViewModel depends on the interface.

Created in Step 2 alongside the implementation.

### 3. `BookshelfScreen` → `AddBookToShelfUseCaseImpl.MAX_BOOKS_PER_SHELF` (VIOLATION)

Presentation layer importing a concrete UseCase implementation to access a constant.

**Fix:** Move `MAX_BOOKS_PER_SHELF = 20` to `BookshelfConstants` in `book/domain/util/`. Both the use case impl and the screen import from the shared constant. No concrete impl dependency.

## Execution — 3 Steps

### Step 1: Extract `book/` shared domain + data

Move models, repositories, and shared presentation utils that are referenced by 3+ features.

**Domain moves:**
- `bookshelf/domain/model/Book.kt` → `book/domain/model/`
- `bookshelf/domain/model/Bookshelf.kt` → `book/domain/model/`
- `bookshelf/domain/model/Bookcase.kt` → `book/domain/model/`
- `bookshelf/domain/model/ReadingStatus.kt` → `book/domain/model/`
- `bookshelf/domain/model/ShareData.kt` → `book/domain/model/`
- `bookshelf/domain/model/BookDetailsWithShelfStatus.kt` → `book/domain/model/`
- `bookshelf/domain/repository/BookRepository.kt` → `book/domain/repository/`
- `bookshelf/domain/repository/BookcaseRepository.kt` → `book/domain/repository/`
- `bookshelf/domain/repository/BookshelfRepository.kt` → `book/domain/repository/`
- `bookshelf/domain/service/BookColorGenerator.kt` → `book/domain/service/`
- `bookshelf/domain/util/*` → `book/domain/util/`

**Data moves:**
- `bookshelf/data/book/dto/` → `book/data/dto/`
- `bookshelf/data/book/network/` → `book/data/network/`
- `bookshelf/data/book/repository/BookRepositoryImpl.kt` → `book/data/repository/`
- `bookshelf/data/book/repository/BookcaseRepositoryImpl.kt` → `book/data/repository/`
- `bookshelf/data/book/repository/BookshelfRepositoryImpl.kt` → `book/data/repository/`
- `bookshelf/data/mappers/` → `book/data/mappers/`

**Presentation moves:**
- `bookshelf/presentation/preview/` → `book/presentation/preview/`
- `bookshelf/presentation/components/` → `book/presentation/components/`
- `bookshelf/presentation/util/` → `book/presentation/util/`

**DI:** Extract BookModule from BookshelfModule for shared repos.

**DO NOT move:** `bookshelf/data/export/` — that belongs in `sharing/` (Step 3).

**Fix during this step:**
- Move `MAX_BOOKS_PER_SHELF` from `AddBookToShelfUseCaseImpl` to `BookshelfConstants` — removes presentation→impl violation

**Do NOT create interfaces yet** — defer to Step 2 where implementations land. ViewModels keep concrete imports until then.

**Risk:** Highest — every feature imports Book/Bookshelf models. ~100+ import updates.

### Step 2: Extract `bookclub/`

Move all book club domain, data, and presentation.

**Domain moves:**
- `bookshelf/domain/model/BookClub*.kt` (4 files) → `bookclub/domain/model/`
- `bookshelf/domain/repository/BookClub*.kt` (5 files) → `bookclub/domain/repository/`
- `bookshelf/domain/usecase/bookclub/` (all) → `bookclub/domain/usecase/`
- `bookshelf/domain/service/BookClubCodeGenerator.kt` → `bookclub/domain/service/`

**Data moves:**
- `bookshelf/data/book/repository/BookClub*Impl.kt` (5 files) → `bookclub/data/repository/`
- `bookshelf/data/book/repository/BookClubRepositoryHelper.kt` → `bookclub/data/repository/`
- `bookshelf/data/service/BookClubCodeGeneratorImpl.kt` → `bookclub/data/service/`

**Presentation moves:**
- `bookshelf/presentation/bookclub/` (all) → `bookclub/presentation/`

**Dependency inversion (created during this step, not Step 1):**

Actual usage analysis:
- `BookcaseViewModel` calls `bookClubOperations.validateMemberships()` — 1 method
- `BookcaseClubActionHandler` calls create, join, leave, lookup, invite, clearLookupState — 6 methods
- `BookshelfViewModel` calls `bookClubOperations.syncBooksFromClub()` — 1 method
- `BookDetailViewModel` calls reviews + comments CRUD — 7 methods

Interfaces to create in `book/domain/service/`:
- `ClubOperations` — 8 methods across all 3 bookshelf consumers (validateMemberships, create, join, leave, lookup, invite, clearLookupState, syncBooksFromClub). Single interface is appropriate — these are all "things bookshelf screens need from clubs", not a God interface.
- `BookReviewProvider` — the 7 review/comment methods `BookDetailViewModel` uses. Returns `BookReview`/`BookComment` (slim DTOs in `book/domain/model/`, no "Club" prefix). Bookclub maps its rich models at the boundary.

The coupling between bookshelf and bookclub is inherent — a club shelf IS a shelf with social features. The interface formalises this relationship without eliminating it. Bookshelf knows the contract, not the implementation.

Review/comment model placement:
- `BookClubReview`, `BookClubComment` stay in `bookclub/domain/model/` — they're club-specific with club metadata
- `BookReview`, `BookComment` (slim DTOs) created in `book/domain/model/` — what `BookReviewProvider` returns. Just rating/text/author, no club context
- `bookclub/` maps `BookClubReview → BookReview` at the provider boundary

Wiring:
- `BookClubOperationsHandler` implements `ClubOperations`
- New `BookClubReviewProviderImpl` implements `BookReviewProvider`
- `BookcaseViewModel`, `BookcaseClubActionHandler`, and `BookshelfViewModel` depend on `ClubOperations` (interface)
- `BookDetailViewModel` depends on `BookReviewProvider` (interface)

**`BookcaseClubActionHandler` disposition:** Stays in `bookshelf/presentation/bookcase/handlers/`. It's bookcase's bridge to club functionality. Gets refactored to depend on `ClubOperations` interface instead of `BookClubOperationsHandler` concrete.

**DI:** `BookClubModule.kt` moves to `bookclub/di/`. Register `ClubOperations` and `BookReviewProvider` bindings.

**BookcaseUseCases → ShareBookshelfUseCase:** This is internal to `bookshelf/` (bookcase subdirectory importing from bookshelf subdirectory). Intentional — same feature package.

**Post-step verification:** Zero imports from `bookclub/` in `bookshelf/`. All cross-feature communication flows through interfaces in `book/domain/service/`.

**Rollback:** Each step is independently shippable. If Step 2 is problematic, Step 1 alone is a valid state (shared domain extracted, everything else still in bookshelf). Consider squashing all 3 before merging to main.

### Step 3: Extract `sharing/` and `welcome/`

**Sharing moves:**
- `bookshelf/domain/usecase/deeplink/` → `sharing/domain/usecase/`
- `bookshelf/domain/usecase/export/` → `sharing/domain/usecase/`
- `bookshelf/domain/service/BookshelfExportService.kt` → `sharing/domain/service/`
- `bookshelf/domain/service/BookshelfImportValidator.kt` → `sharing/domain/service/`
- `bookshelf/domain/service/BookshelfSerializer.kt` → `sharing/domain/service/`
- `bookshelf/domain/service/ShareTokenService.kt` → `sharing/domain/service/`
- `bookshelf/domain/service/BookshelfDataOrchestrator.kt` → `sharing/domain/service/`
- `bookshelf/data/service/` (remaining: serializers, token services) → `sharing/data/`
- `bookshelf/data/export/` → `sharing/data/export/`
- `bookshelf/presentation/deeplink/` → `sharing/presentation/`

**Welcome moves:**
- `bookshelf/domain/usecase/tutorial/` → `welcome/domain/usecase/`
- `bookshelf/domain/usecase/welcome/` → `welcome/domain/usecase/`
- `bookshelf/presentation/welcome/` → `welcome/presentation/`

**DI:** Create SharingModule and WelcomeModule. Remove these from BookshelfModule.

**Post-step cleanup:**
- Move `bookshelf/presentation/MyBookShelfApp.kt` to `app/`
- Update AppModule: add book, sharing, welcome modules
- Delete any empty directories in bookshelf/
- `./gradlew clean assembleDebug test detekt`

## What Stays in `bookshelf/`

After all 3 steps, bookshelf/ contains only:
```
bookshelf/
├── domain/usecase/
│   ├── bookcase/          # Shelf CRUD use cases
│   ├── bookshelf/         # Search + shelf book management use cases
│   └── bookdetail/        # Book detail use cases
├── presentation/
│   ├── bookcase/          # Root screen with tabs
│   ├── bookshelf/         # Shelf view + search
│   └── bookdetail/        # Book detail view
└── di/
    └── BookshelfModule.kt # Slimmed — use cases + ViewModels only
```

This is the right size — ~80 files of tightly related screen logic.

## Navigation Impact

Routes are string-based in `app/NavigationRoute.kt` — package moves don't affect them. The nav graph in `MyBookShelfApp.kt` imports Screen composables by full path — those imports update with the package moves. No structural changes needed.

## Test Impact

Test directories mirror main. Each step moves tests alongside source:
- `test/.../bookshelf/domain/model/` tests → `test/.../book/domain/model/`
- `test/.../bookshelf/domain/usecase/bookclub/` tests → `test/.../bookclub/domain/usecase/`
- `testutil/mocks/MockBookClubRepository.kt` — imports update, file stays in testutil

## Pre-execution Verification (completed)

| Item | Status | Finding |
|------|--------|---------|
| Room entities/DAOs | No impact | All in `core/data/database/` — not moved |
| BookClubRepositoryHelper | Moves in Step 2 | Lives in `bookshelf/data/book/repository/` → `bookclub/data/repository/` |
| FirestoreOperationHelper | No impact | Lives in `sync/data/service/` — not moved |
| Import count for `Book.kt` | 48 imports | Manageable |
| All domain model imports | 153 imports | Confirms ~100+ estimate for Step 1 |
| ShelfNameInputDialog | Cross-feature | Used by bookcase + deeplink → moves to `book/presentation/` |
| AndroidBookshelfExportService | Verify DI | Moves to `sharing/data/` — confirm Koin injection, not manual |

## Risk Mitigation

- Feature branch with squash option
- 3 steps, each independently buildable and shippable
- `./gradlew clean` after each step
- Step 1 is the riskiest (153 import changes) — if it works, Steps 2-3 are straightforward
- Fix the `BookshelfScreen` UseCase impl import as part of Step 1

## Estimated Scope

| Step | Files moved | Import updates | Risk |
|------|-----------|----------------|------|
| 1 | ~40 | ~153 | High — shared models everywhere |
| 2 | ~50 | ~30 | Medium — mostly self-contained |
| 3 | ~30 | ~20 | Low — self-contained features |

Total: ~120 files, 3 commits.
