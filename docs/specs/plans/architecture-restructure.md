# Architecture Restructure — Phase 1

## Problem

`bookshelf/` contains 241 Kotlin files across 6 screens and 2 features in a single package. This happened because bookclub grew out of bookshelf, but they're separate concerns sharing some domain models. Adding new features (manual book entry, barcode scanner) would make this worse.

**Current file counts:**
- `domain/usecase/bookcase/`: 38 files (screen-specific)
- `domain/usecase/bookclub/`: 40 files (separate feature)
- `domain/usecase/bookdetail/`: 31 files (screen-specific)
- `presentation/bookshelf/`: search + shelf display (screen-specific)
- `presentation/deeplink/`: 10 files (separate feature)
- `presentation/welcome/`: 10 files (separate feature)
- `data/book/`: 18 files (shared data access)
- `data/service/`: 8 files (sharing services)
- `di/`: 2 modules (BookshelfModule, BookClubModule)

## Target Structure

```
uk.co.zlurgg.mybookshelf/
├── app/                    # Navigation, Application class (unchanged)
├── di/                     # Root AppModule (unchanged)
├── core/                   # Infrastructure (unchanged)
├── auth/                   # Authentication (unchanged)
├── sync/                   # Cloud sync (unchanged)
│
├── book/                   # EXTRACTED — shared book domain + data
│   ├── domain/
│   │   ├── model/         # Book, Bookshelf, Bookcase, ReadingStatus, ShareData
│   │   ├── repository/    # BookRepository, BookcaseRepository, BookshelfRepository
│   │   ├── service/       # BookColorGenerator
│   │   └── util/          # ShelfStyle, BookshelfConstants, BookDetailConstants
│   ├── data/
│   │   ├── dto/           # SearchResponseDto, BookWorkDto, etc.
│   │   ├── network/       # OpenLibraryApiService, KtorRemoteBookDataSource
│   │   ├── repository/    # BookRepositoryImpl, BookcaseRepositoryImpl, BookshelfRepositoryImpl
│   │   └── mappers/       # Book/Shelf entity mappers
│   └── di/                # BookModule
│
├── bookcase/               # EXTRACTED — bookcase screen
│   ├── domain/usecase/    # GetAllShelves, CreateShelf, DeleteShelf, Reorder, Rename, etc.
│   ├── presentation/      # BookcaseViewModel, BookcaseScreen, components/, handlers/
│   └── di/                # BookcaseModule
│
├── shelf/                  # EXTRACTED — shelf screen (books on shelf + search)
│   ├── domain/usecase/    # SearchBooks, GetShelfBooks, AddBookToShelf, etc.
│   ├── presentation/      # BookshelfViewModel, BookshelfScreen, searchcomponents/, etc.
│   └── di/                # ShelfModule
│
├── bookdetail/             # EXTRACTED — book detail screen
│   ├── domain/usecase/    # GetBookDetails, UpsertBook, TogglePurchase, UpdateMetadata, etc.
│   ├── presentation/      # BookDetailViewModel, BookDetailScreen, components/
│   └── di/                # BookDetailModule
│
├── bookclub/               # EXTRACTED — book club feature
│   ├── domain/
│   │   ├── model/         # BookClub, BookClubReview, BookClubComment, Membership
│   │   ├── repository/    # BookClubManagement/Membership/Sync/ReviewRepository
│   │   └── usecase/       # All club use cases (create, join, leave, reviews, comments)
│   ├── data/
│   │   ├── repository/    # All BookClub*RepositoryImpl classes
│   │   └── service/       # BookClubCodeGeneratorImpl
│   ├── presentation/      # Club components, handlers
│   └── di/                # BookClubModule (already exists)
│
├── sharing/                # EXTRACTED — export/import/deeplink
│   ├── domain/
│   │   ├── usecase/       # Export/import use cases from deeplink/ + export/
│   │   └── service/       # BookshelfSerializer, BookshelfImportValidator, etc.
│   ├── data/              # JsonBookshelfSerializer, UrlEncodedShareTokenService, etc.
│   ├── presentation/      # DeepLinkScreen, DeepLinkViewModel
│   └── di/                # SharingModule
│
└── welcome/                # EXTRACTED — tutorial/onboarding
    ├── domain/usecase/    # Tutorial shelf/book creation
    ├── presentation/      # WelcomeScreen, WelcomeService
    └── di/                # WelcomeModule
```

## Execution — 8 Steps

Each step is one commit. Build + test + detekt must pass after each. Use `git mv` for history.

### Step 1: Extract `book/` shared domain

Move files that are referenced by 3+ features:

| Source | Destination |
|--------|------------|
| `bookshelf/domain/model/Book.kt` | `book/domain/model/` |
| `bookshelf/domain/model/Bookshelf.kt` | `book/domain/model/` |
| `bookshelf/domain/model/Bookcase.kt` | `book/domain/model/` |
| `bookshelf/domain/model/ReadingStatus.kt` | `book/domain/model/` |
| `bookshelf/domain/model/ShareData.kt` | `book/domain/model/` |
| `bookshelf/domain/model/BookDetailsWithShelfStatus.kt` | `book/domain/model/` |
| `bookshelf/domain/repository/BookRepository.kt` | `book/domain/repository/` |
| `bookshelf/domain/repository/BookcaseRepository.kt` | `book/domain/repository/` |
| `bookshelf/domain/repository/BookshelfRepository.kt` | `book/domain/repository/` |
| `bookshelf/domain/service/BookColorGenerator.kt` | `book/domain/service/` |
| `bookshelf/domain/util/ShelfStyle.kt` | `book/domain/util/` |
| `bookshelf/domain/util/BookshelfConstants.kt` | `book/domain/util/` |
| `bookshelf/domain/util/BookDetailConstants.kt` | `book/domain/util/` |

**~13 files moved. ~100+ import updates across the codebase.**

This is the highest-impact step. Everything depends on these models.

### Step 2: Extract `book/data/`

Move shared data layer:

| Source | Destination |
|--------|------------|
| `bookshelf/data/book/dto/` | `book/data/dto/` |
| `bookshelf/data/book/network/` | `book/data/network/` |
| `bookshelf/data/book/repository/BookRepositoryImpl.kt` | `book/data/repository/` |
| `bookshelf/data/book/repository/BookcaseRepositoryImpl.kt` | `book/data/repository/` |
| `bookshelf/data/book/repository/BookshelfRepositoryImpl.kt` | `book/data/repository/` |
| `bookshelf/data/mappers/` | `book/data/mappers/` |
| `bookshelf/data/export/` | `book/data/export/` |

**~18 files. Extract BookModule from BookshelfModule.**

### Step 3: Extract `bookclub/`

Move all book club domain + data + presentation:

| Source | Destination |
|--------|------------|
| `bookshelf/domain/model/BookClub*.kt` (4 files) | `bookclub/domain/model/` |
| `bookshelf/domain/repository/BookClub*.kt` (5 files) | `bookclub/domain/repository/` |
| `bookshelf/domain/usecase/bookclub/` (all files) | `bookclub/domain/usecase/` |
| `bookshelf/domain/service/BookClubCodeGenerator.kt` | `bookclub/domain/service/` |
| `bookshelf/data/book/repository/BookClub*Impl.kt` (5 files) | `bookclub/data/repository/` |
| `bookshelf/data/service/BookClubCodeGeneratorImpl.kt` | `bookclub/data/service/` |
| `bookshelf/presentation/bookclub/` (all files) | `bookclub/presentation/` |
| `bookshelf/di/BookClubModule.kt` | `bookclub/di/` |

**~40+ files. BookClubModule already exists, just moves location.**

### Step 4: Extract `bookcase/`

Move bookcase screen:

| Source | Destination |
|--------|------------|
| `bookshelf/domain/usecase/bookcase/` | `bookcase/domain/usecase/` |
| `bookshelf/presentation/bookcase/` | `bookcase/presentation/` |

**~20 files. Create BookcaseModule extracted from BookshelfModule.**

### Step 5: Extract `shelf/`

Move shelf screen (the actual bookshelf view with books + search):

| Source | Destination |
|--------|------------|
| `bookshelf/domain/usecase/bookshelf/` | `shelf/domain/usecase/` |
| `bookshelf/presentation/bookshelf/` | `shelf/presentation/` |

**~15 files. Create ShelfModule.**

### Step 6: Extract `bookdetail/`

Move book detail screen:

| Source | Destination |
|--------|------------|
| `bookshelf/domain/usecase/bookdetail/` | `bookdetail/domain/usecase/` |
| `bookshelf/presentation/bookdetail/` | `bookdetail/presentation/` |

**~20 files. Create BookDetailModule.**

### Step 7: Extract `sharing/` and `welcome/`

Move remaining features:

| Source | Destination |
|--------|------------|
| `bookshelf/domain/usecase/deeplink/` | `sharing/domain/usecase/` |
| `bookshelf/domain/usecase/export/` | `sharing/domain/usecase/` |
| `bookshelf/domain/service/Bookshelf*Service.kt` | `sharing/domain/service/` |
| `bookshelf/domain/service/BookshelfImportValidator.kt` | `sharing/domain/service/` |
| `bookshelf/domain/service/BookshelfSerializer.kt` | `sharing/domain/service/` |
| `bookshelf/domain/service/ShareTokenService.kt` | `sharing/domain/service/` |
| `bookshelf/data/service/` (remaining) | `sharing/data/` |
| `bookshelf/presentation/deeplink/` | `sharing/presentation/` |
| `bookshelf/domain/usecase/tutorial/` | `welcome/domain/usecase/` |
| `bookshelf/domain/usecase/welcome/` | `welcome/domain/usecase/` |
| `bookshelf/presentation/welcome/` | `welcome/presentation/` |

**~25 files. Create SharingModule and WelcomeModule.**

### Step 8: Delete empty `bookshelf/` and update AppModule

- Remove the now-empty `bookshelf/` package
- Move `bookshelf/presentation/MyBookShelfApp.kt` to `app/`
- Move `bookshelf/presentation/preview/` to `app/` or `core/`
- Move `bookshelf/presentation/components/` to `core/presentation/`
- Move `bookshelf/presentation/util/` to `core/presentation/`
- Update `AppModule` to include all new feature modules
- Final verification: `./gradlew clean assembleDebug test detekt`

**~10 files. Clean up stragglers.**

## DI Module Changes

**Current:**
```
AppModule includes: core, auth, sync, bookClub, bookshelf
```

**Target:**
```
AppModule includes: core, auth, sync, book, bookcase, shelf, bookdetail, bookclub, sharing, welcome
```

Each new module owns its use cases, ViewModels, and any feature-specific repositories. The `book` module owns shared repos and their impls.

## Test Impact

Tests mirror the main source set structure. Each step that moves main files needs corresponding test moves:
- `test/.../bookshelf/domain/usecase/bookcase/` → `test/.../bookcase/domain/usecase/`
- `test/.../bookshelf/presentation/bookcase/` → `test/.../bookcase/presentation/`
- etc.

Mock files in `testutil/mocks/` may reference moved types — imports update automatically with package renames.

## Risk Mitigation

- Feature branch with squash option
- Each step is independently buildable and testable
- Steps 1-2 (shared domain) are the riskiest — most import changes
- Steps 3-7 are increasingly safe as the dependency graph narrows
- `./gradlew clean` after each step to clear stale caches
- If any step breaks, revert just that step without losing prior work

## Estimated Scope

| Step | Files moved | Import updates (est.) | Risk |
|------|------------|----------------------|------|
| 1 | ~13 | ~100+ | High — everything depends on models |
| 2 | ~18 | ~50 | Medium — data layer consumers |
| 3 | ~40 | ~30 | Medium — self-contained feature |
| 4 | ~20 | ~20 | Low — screen-specific |
| 5 | ~15 | ~15 | Low — screen-specific |
| 6 | ~20 | ~15 | Low — screen-specific |
| 7 | ~25 | ~20 | Low — mostly self-contained |
| 8 | ~10 | ~10 | Low — cleanup |

Total: ~161 files, 8 commits.
