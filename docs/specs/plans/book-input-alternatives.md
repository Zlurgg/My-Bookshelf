# Play Store Improvements Plan

## Problem

The `bookshelf/` package contains 241 files treating 4+ distinct screens as one feature. This happened because bookclub grew out of bookshelf, but they're separate concerns that share some domain models and data access.

**Status: Phase 1 restructure is COMPLETE.** The monolithic `bookshelf/` package has been split into separate feature packages. See below for the actual architecture.

Adding manual book entry means another screen — now clean to add as `addbook/`.

## Target Architecture

Current architecture (restructure complete):

```
uk.co.zlurgg.mybookshelf/
├── app/                    # Application, navigation
├── di/                     # Root DI aggregator (AppModule)
├── core/                   # Infrastructure (database, network, error handling)
│   ├── data/              # Room, Ktor, preferences
│   └── domain/            # Result, DataError, shared services
├── auth/                   # Authentication (Google Sign-In, auth state)
├── sync/                   # Cloud sync (Firestore sync engine)
│
├── book/                   # Shared book domain (models, repos, shared use cases)
│   ├── domain/model/      # Book, Bookshelf, ReadingStatus, ShelfStyle
│   ├── domain/repository/ # BookRepository, BookcaseRepository, BookshelfRepository
│   ├── domain/usecase/    # AddBookToShelf, RemoveBookFromShelf, UpsertBook (shared)
│   ├── data/              # Repo impls, Room mappers, DTOs, network
│   └── di/                # BookModule
│
├── bookcase/               # Screen: shelf list (the "home" screen)
│   ├── domain/usecase/    # GetAllShelves, CreateShelf, DeleteShelf, etc.
│   ├── presentation/      # BookcaseViewModel, BookcaseScreen, components, handlers
│   └── di/                # BookcaseModule
│
├── bookshelf/              # Screen: books on a shelf + search
│   ├── domain/usecase/    # SearchBooks, GetShelfBooks, ShareBookshelf, etc.
│   ├── presentation/      # BookshelfViewModel, BookshelfScreen, components
│   └── di/                # BookshelfModule
│
├── bookdetail/             # Screen: single book details
│   ├── domain/usecase/    # GetBookDetails, TogglePurchase, UpdateBookMetadata
│   ├── presentation/      # BookDetailViewModel, BookDetailScreen, components
│   └── di/                # BookDetailModule
│
├── bookclub/               # Feature: book clubs (create, join, reviews, comments)
│   ├── domain/            # Models, repos, use cases
│   ├── data/              # Club repo impls
│   ├── presentation/      # Club components, handlers
│   └── di/                # BookClubModule
│
├── addbook/                # PLANNED: manual book entry
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

## Restructure Status

All moves from the original monolithic `bookshelf/` package are **complete**. The package split was done across multiple prior refactors. See `docs/specs/plans/bookshelf-package-split.md` for the detailed execution log.

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

**Phase 1: Architecture restructure** — COMPLETE. Monolithic `bookshelf/` split into `book/`, `bookcase/`, `bookdetail/`, `bookshelf/` (alongside existing `bookclub/`, `sharing/`, `welcome/`).

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
