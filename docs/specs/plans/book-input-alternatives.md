# Play Store Improvements Plan

## Problem

The `bookshelf/` package contains 241 files treating 4+ distinct screens as one feature. This happened because bookclub grew out of bookshelf, but they're separate concerns that share some domain models and data access.

Current structure:
```
bookshelf/                          # 241 files — too large, mixed concerns
├── data/book/repository/           # Repos for books, shelves, AND clubs
├── domain/usecase/bookcase/        # Bookcase screen logic
├── domain/usecase/bookclub/        # Club logic (different feature)
├── domain/usecase/bookdetail/      # Detail screen logic
├── domain/usecase/bookshelf/       # Shelf screen logic
├── presentation/bookcase/          # Screen 1
├── presentation/bookshelf/         # Screen 2
├── presentation/bookdetail/        # Screen 3
├── presentation/bookclub/          # Screen 4 (handlers, not a full screen)
├── presentation/deeplink/          # Screen 5
└── presentation/welcome/           # Screen 6
```

Adding manual book entry means another screen — and stuffing it into this package makes the problem worse.

## Target Architecture

Extract into separate feature packages with shared domain models lifted to a higher level:

```
uk.co.zlurgg.mybookshelf/
├── app/                    # Application, navigation
├── di/                     # Root DI aggregator
├── core/                   # Infrastructure (database, network, error handling)
│   ├── data/              # Room, Ktor, preferences
│   └── domain/            # Result, DataError, shared services
├── auth/                   # Authentication (existing, clean)
├── sync/                   # Cloud sync (existing, clean)
│
├── book/                   # NEW — shared book domain (models, repos, mappers)
│   ├── domain/model/      # Book, Bookshelf, ReadingStatus, ShelfStyle
│   ├── domain/repository/ # BookRepository, BookcaseRepository, BookshelfRepository
│   ├── data/              # Repo impls, Room mappers, DTOs
│   └── di/                # BookModule
│
├── bookcase/               # Screen: shelf list (the "home" screen)
│   ├── domain/usecase/    # GetAllShelves, CreateShelf, DeleteShelf, etc.
│   ├── presentation/      # BookcaseViewModel, BookcaseScreen, components
│   └── di/                # BookcaseModule
│
├── shelf/                  # Screen: books on a shelf + search
│   ├── domain/usecase/    # SearchBooks, GetShelfBooks, AddBookToShelf
│   ├── presentation/      # BookshelfViewModel, BookshelfScreen, components
│   └── di/                # ShelfModule
│
├── bookdetail/             # Screen: single book details
│   ├── domain/usecase/    # GetBookDetails, UpsertBook, TogglePurchase
│   ├── presentation/      # BookDetailViewModel, BookDetailScreen, components
│   └── di/                # BookDetailModule
│
├── bookclub/               # Feature: book clubs (create, join, reviews, comments)
│   ├── domain/model/      # BookClub, BookClubReview, BookClubComment, Membership
│   ├── domain/repository/ # BookClubManagement/Membership/Sync/ReviewRepository
│   ├── domain/usecase/    # All club use cases
│   ├── data/              # Club repo impls
│   ├── presentation/      # Club components, handlers
│   └── di/                # BookClubModule
│
├── addbook/                # NEW Screen: manual book entry
│   ├── domain/usecase/    # AddManualBook
│   ├── presentation/      # ManualBookEntryScreen, form components
│   └── di/                # AddBookModule
│
├── sharing/                # Feature: export/import/deeplink
│   ├── domain/            # Export/import use cases
│   ├── data/              # Serializers, token services
│   ├── presentation/      # DeepLinkScreen
│   └── di/                # SharingModule
│
└── welcome/                # Screen: onboarding/tutorial
    ├── domain/usecase/    # Tutorial shelf creation
    ├── presentation/      # WelcomeScreen
    └── di/                # WelcomeModule
```

## Key Principles

- **`book/` is shared domain** — models and repositories used by multiple features, similar to `core/`
- **Each screen owns its use cases** — BookcaseUseCases stays in `bookcase/`, not shared
- **Club is its own feature** — it has its own models, repos, and use cases
- **Manual entry is a new feature** — clean from the start, not bolted onto shelf
- **Sharing/export is its own feature** — currently buried in bookshelf data/service

## What Moves Where

| Current Location | Target | Reason |
|-----------------|--------|--------|
| `bookshelf/domain/model/Book.kt` | `book/domain/model/` | Shared across all features |
| `bookshelf/domain/model/Bookshelf.kt` | `book/domain/model/` | Shared |
| `bookshelf/domain/model/BookClub*.kt` | `bookclub/domain/model/` | Club-specific |
| `bookshelf/domain/repository/Book*Repository.kt` | `book/domain/repository/` | Shared data access |
| `bookshelf/domain/repository/BookClub*Repository.kt` | `bookclub/domain/repository/` | Club-specific |
| `bookshelf/domain/usecase/bookcase/` | `bookcase/domain/usecase/` | Screen-specific |
| `bookshelf/domain/usecase/bookclub/` | `bookclub/domain/usecase/` | Feature-specific |
| `bookshelf/domain/usecase/bookdetail/` | `bookdetail/domain/usecase/` | Screen-specific |
| `bookshelf/domain/usecase/bookshelf/` | `shelf/domain/usecase/` | Screen-specific |
| `bookshelf/domain/usecase/deeplink/` | `sharing/domain/usecase/` | Feature-specific |
| `bookshelf/domain/usecase/export/` | `sharing/domain/usecase/` | Feature-specific |
| `bookshelf/domain/usecase/tutorial/` | `welcome/domain/usecase/` | Screen-specific |
| `bookshelf/data/book/repository/` | `book/data/` + `bookclub/data/` | Split by concern |
| `bookshelf/data/service/` | `sharing/data/` | Serializers, token services |
| `bookshelf/presentation/bookcase/` | `bookcase/presentation/` | Screen-specific |
| `bookshelf/presentation/bookshelf/` | `shelf/presentation/` | Screen-specific |
| `bookshelf/presentation/bookdetail/` | `bookdetail/presentation/` | Screen-specific |
| `bookshelf/presentation/bookclub/` | `bookclub/presentation/` | Feature-specific |
| `bookshelf/presentation/deeplink/` | `sharing/presentation/` | Feature-specific |
| `bookshelf/presentation/welcome/` | `welcome/presentation/` | Screen-specific |

## Manual Book Entry Feature

**What:** Form screen for adding books without API search.

**Fields:**
- Required: title, author
- Optional: cover image (camera/gallery), description, page count, publish year, ISBN, publisher

**Entry points:**
- "Add manually" button in search dialog (when no results or user prefers)
- Direct option from shelf FAB menu

**Implementation:**
- New `addbook/` feature package (clean from the start)
- `ManualBookEntryScreen` with form
- Cover image: pick from gallery or capture with camera, stored locally
- Creates a `Book` domain model and saves via `BookRepository`
- No API dependency — works fully offline

## Phased Approach

**Phase 1: Architecture restructure** — move files into proper packages. Mechanical but large. Do before adding features to avoid making the current mess worse.

**Phase 2: Manual book entry** — add `addbook/` feature. Clean implementation in new structure.

**Phase 3: ISBN barcode scanner** — add ML Kit barcode scanning as another input method. Camera scan → OpenLibrary ISBN lookup → fallback to manual entry.

**Phase 4 (if needed): Google Books API** — alternative search source. Only if users report OpenLibrary gaps.

## Risk

The restructure is the biggest refactor this project has seen — 241 files moving across packages. Every import changes. Tests need updating. DI modules need rewiring.

Mitigation:
- Do on a feature branch
- Phase 1 is purely mechanical (rename packages, update imports) — no logic changes
- Use `git mv` for history preservation
- Run `./gradlew clean` after moves
- Build + test + detekt after each batch of moves
- Consider doing it in sub-phases: shared domain first, then one screen at a time
