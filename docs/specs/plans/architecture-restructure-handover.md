# Architecture Restructure — Handover

## What to do

Execute the 3-step restructure plan in `docs/specs/plans/architecture-restructure.md`. Read it fully before starting.

## Current state

- Branch: `restructure-extract-screens-to-seperate-packages` (off `main`)
- Build: passing (`./gradlew assembleDebug`)
- Tests: passing (`./gradlew test`)
- Detekt: passing (`./gradlew detekt`)
- The plan doc and book-input-alternatives doc are committed but no code changes yet

## What the plan does (summary)

Splits the 241-file `bookshelf/` package into focused packages:

1. **Step 1:** Extract `book/` — shared domain models (Book, Bookshelf, etc.), repositories, data layer, preview data, shared UI utils. Fix `BookshelfScreen` → `AddBookToShelfUseCaseImpl` violation by moving `MAX_BOOKS_PER_SHELF` to `BookshelfConstants`.

2. **Step 2:** Extract `bookclub/` — club models, repos, use cases, handlers. Create `ClubOperations` and `BookReviewProvider` interfaces in `book/domain/service/`. Bookclub implements them. Refactor bookshelf ViewModels to depend on interfaces, not concrete club classes. Zero bookshelf→bookclub imports after this step.

3. **Step 3:** Extract `sharing/` (deeplinks, export/import) and `welcome/` (tutorial, onboarding). Move `MyBookShelfApp.kt` to `app/`. Clean up empty directories.

## Key decisions already made

- **Interfaces created in Step 2, not Step 1** — no dead code. ViewModels keep concrete imports through Step 1.
- **Slim DTOs** (`BookReview`, `BookComment`) in `book/domain/model/` — bookclub maps `BookClubReview → BookReview` at the boundary. Consistent with entity→domain mapping pattern used elsewhere.
- **`BookcaseClubActionHandler`** stays in `bookshelf/`, consumes `ClubOperations` interface. It's bookcase's bridge to club functionality.
- **`BookClubRepositoryHelper`** moves to `bookclub/data/repository/` in Step 2.
- **`data/export/`** goes to `sharing/data/` in Step 3, NOT `book/data/`.
- **Room entities/DAOs** are in `core/` — no impact, don't move them.
- **Navigation** is string-based (`app/NavigationRoute.kt`) — package moves don't break routes.
- **Dependencies flow downward.** If you find violations during the restructure, fix them at the time.

## Consumers of BookClubOperationsHandler (the main coupling point)

| Consumer | Methods used | Post-restructure |
|----------|-------------|-----------------|
| BookcaseViewModel | validateMemberships (1) | Uses `ClubOperations` interface |
| BookcaseClubActionHandler | create, join, leave, lookup, invite, clearLookupState (6) | Uses `ClubOperations` interface |
| BookshelfViewModel | syncBooksFromClub (1) | Uses `ClubOperations` interface |
| BookDetailViewModel | 7 review/comment methods | Uses `BookReviewProvider` interface |

## How to execute each step

1. `git mv` directories for history preservation
2. `sed -i` to bulk-update package declarations and imports
3. Verify no old package references remain: `grep -rn "bookshelf.domain.model" app/src --include="*.kt"`
4. `./gradlew clean assembleDebug test detekt`
5. Commit

## Import counts (verified)

- `Book.kt` alone: 48 imports across codebase
- All domain models: 153 imports
- These all change in Step 1 (package path update)

## Files NOT to move

- `core/data/database/entity/` — Room entities stay in core
- `core/data/database/dao/` — DAOs stay in core
- `sync/` — unchanged
- `auth/` — unchanged

## After completion

- `bookshelf/` should be ~80 files (screens + use cases only)
- `book/` should have shared models, repos, preview data, shared UI
- `bookclub/` should be fully self-contained
- `sharing/` and `welcome/` should be self-contained
- Zero imports from `bookclub/` in `bookshelf/`
- AppModule includes: core, auth, sync, book, bookshelf, bookclub, sharing, welcome

## Gotchas

- Windows + `git mv` can be tricky with case sensitivity — verify renames took effect
- Run `./gradlew clean` after each step to clear stale compiled classes
- `AndroidBookshelfExportService` in sharing/ has Android Context dependency — verify it's Koin-injected
- Test files in `testutil/mocks/` import moved types — imports update, files stay in testutil
- Compose `@Preview` functions import from `preview/` package — update those imports
