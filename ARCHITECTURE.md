MyBookshelf — Architecture, Roadmap, and Implementation Guide

Overview
- Purpose: Curate, store, and share reading lists as “bookshelves” inside a user’s “bookcase”. Users can search for books from a remote catalog, add them to shelves, view details, and optionally purchase via affiliate links. Shelves persist locally (Room). Medium-term goal: sharing shelves among users; long-term goal: publish on Google Play.
- Tech Stack: 
  - Language/UI: Kotlin, Jetpack Compose
  - DI: Koin
  - Concurrency: Coroutines + Flow
  - Networking: Ktor client (Android engine)
  - Persistence: Room (BookshelfDatabase, DAO)
  - Tooling: Gradle (KTS)

High-Level Architecture
- Presentation (Compose + ViewModel)
  - Screens: Bookcase (list of shelves), Bookshelf (books in a shelf), Book Detail.
  - ViewModels expose immutable StateFlow state + onAction() reducers.
  - Navigation integrates screens; Koin injects ViewModels (BookshelfViewModel gets shelfId).
- Domain
  - Entities: Book, Bookshelf, Bookcase.
  - Repositories: BookshelfRepository, BookcaseRepository.
- Data
  - Room: BookshelfDatabase with DAO (bookshelves). Books-on-shelf persistence is not complete yet.
  - Networking: RemoteBookDataSource (Ktor) performs book search; DTOs mapped to domain models.

Current Data Flow (as of repository state)
1) User opens a shelf → BookshelfViewModel loads books for shelfId via BookshelfRepository.getBooksForShelf().
2) User taps FAB → BookSearchDialog opens.
3) User types query → BookshelfViewModel.searchBooks() calls BookshelfRepository.searchBooks() → RemoteBookDataSource.searchBooks(query) → DTOs mapped to domain Books.
4) User adds a book → BookshelfViewModel.addBookToShelf() → BookshelfRepository.addBookToShelf(shelfId, book) → currently stored in-memory map.
5) Bookcase screen: BookcaseViewModel loads shelves from BookcaseRepository (Room-backed).

Gaps and Risks
- Books persistence: Books for shelves are stored in-memory (lost on app restart). Must be persisted in Room.
- Affiliate purchase: Domain Book lacks a dedicated affiliateUrl field; UI does not expose a purchase action yet.
- Sharing: No backend or export/import flow. Need a plan for shareable shelves.
- Error/UI states: Some state flags overlap (isLoading vs isSearchLoading); ensure consistent usage and dialog dismissal.
- Analytics/attribution: Needed for affiliate conversion tracking.
- Privacy/consent and store readiness: No policy assets or settings panel yet.

Networking Details and Plan
- Current: Ktor client via HttpClientFactory(Android engine), RemoteBookDataSource.fetch/search, DTOs -> domain using mappers.
- Additions:
  - Introduce AffiliateLinkResolver abstraction to generate or attach affiliate links to Book results. Options:
    - If API returns buy links: map to affiliateUrl when present.
    - If not, map using an ISBN/OLID → retailer deep-link template (e.g., https://www.amazon.com/dp/{ASIN}?tag=AFFILIATE_ID). Store affiliate partner IDs in BuildConfig or remote config.
  - Error handling: Standardize Result<Data, DataError.Remote>; map HTTP/network errors to user-friendly messages. Add retry/backoff for transient errors.
  - Caching: Optional—cache last search results in memory (ViewModel) or simple in-db cache for offline resilience.

Room Persistence Plan for Books on Shelves
- Current: BookEntity exists; BookshelfDao/Database exist. Verify entity relations:
  - If not present, add:
    - BookEntity (id, title, author, coverUrl, rating, description, isbn, affiliateUrl?, createdAt)
    - ShelfEntity (id, name, shelfMaterial)
    - Cross-ref table ShelfBookCrossRef(shelfId, bookId, addedAt)
  - DAO methods:
    - getBooksForShelf(shelfId): Flow<List<BookWithMeta>> (JOIN via cross-ref)
    - insertBooks(vararg books)
    - upsertShelf(shelf)
    - addBookToShelf(shelfId, book): insertBook if missing, insert cross-ref
    - removeBookFromShelf(shelfId, bookId): delete cross-ref
- Repository changes (BookshelfRepositoryImpl):
  - Replace in-memory map with DAO-backed implementation.
  - Ensure Flow emits updates on add/remove.
- Migrations:
  - If adding new tables/columns (e.g., affiliateUrl), bump DB version and supply Migration. For dev phase, you can fallback to destructive migration while iterating.

Compose UI Patterns/Improvements
- State hoisting: continue using ViewModel StateFlow + onAction.
- Dialogs: Use a single source of truth (isSearchDialogVisible). Ensure onDismiss sets it to false and clears search query if appropriate.
- Loading flags: unify isLoading and isSearchLoading; e.g., use isSearchLoading exclusively for search and isLoading for shelf loading.
- Snackbars/toasts: present add/remove confirmations and error messages.
- Navigation: Pass shelfId through routes safely; preview states for consistent design-time behavior.
- Testing: 
  - Unit tests for ViewModel reducers.
  - Instrumented tests with Compose testing for search and add flows.

Sharing Strategy
- Short-term (no backend):
  - Export/import: Serialize a shelf (JSON) with book metadata; share via intent. Import merges into recipient’s local Room DB.
  - Share link + public JSON: Optional—upload JSON to a pastebin-like service or Firebase Storage and share link.
- Long-term (backend):
  - Backend with auth (Firebase Auth or custom), a simple REST for shelves and books, ownership + membership. Room remains offline cache; implement sync layer.
  - Add share/invite by link (deep link) to subscribe to someone’s shelf.

Affiliate Purchase Integration
- Domain changes:
  - Add affiliateUrl: String? to Book.
- Data mapping:
  - Populate affiliateUrl during mapping if source provides id/isbn; otherwise resolve via AffiliateLinkResolver.
- UI changes:
  - BookDetailScreen: Add “Buy” button visible when affiliateUrl != null; launch CustomTabs Intent with ref parameter.
  - Track click events for attribution.
- Compliance:
  - Disclose affiliate relationships in About/Settings.

Roadmap to MVP (Play Store)
1) Persistence ✓
  - Add cross-ref table; implement DAO and repository methods for books on shelves.
  - Replace in-memory storage in BookshelfRepositoryImpl with Room-backed implementation.
2) Search & Add ✓
  - Ensure search dialog UX is smooth, loading/error handling consistent. Debounce queries (300–500ms).
3) Book Detail & Purchase ✓
  - Show detailed info, rating, description, cover.
  - Add Buy button (CustomTabs) using affiliateUrl.
4) Share Shelves (Phase 1) ✓
  - Implement export/import (JSON) via ShareSheet and system file picker.
  - Optional: Generate shareable deep link that opens import flow.
5) Polish & Stability ✓
  - Error handling, snackbars, undo remove, empty states, offline behavior.
  - App icon, splash, themes, dark mode.
6) Privacy & Compliance ✓
  - Add Privacy Policy URL, affiliate disclosure, in-app section explaining data usage.
  - Consider consent if using analytics/ads.
7) QA & Release ✓
  - Unit and UI tests.
  - Internal test track releases; capture crash reports (Firebase Crashlytics optional).
  - Store listing: screenshots, description, keywords.

Actionable Implementation Steps (Code)
- Data layer
  - Create ShelfBookCrossRef entity and relations if missing.
  - Extend DAO: getBooksForShelf(shelfId): Flow<List<Book>>, insertBook(s), insertCrossRef, deleteCrossRef.
  - Update BookshelfRepositoryImpl to use DAO; remove in-memory map.
  - Add affiliateUrl to Book and BookEntity; bump DB version + Migration.
- Domain/Presentation
  - Update mappers for affiliateUrl.
  - BookshelfViewModel: no behavioral change except remove manual state append; rely on Flow from DAO after add/remove.
  - BookDetailScreen: add Buy button; open Custom Tabs.
  - BookSearchDialog: ensure onDismiss sets isSearchDialogVisible=false; unify loading flags.
- UX
  - Debounce search input (use coroutine debounce)
  - Add snackbar for add/remove/undo
- Sharing (Phase 1)
  - Add JSON export/import utilities
  - Intent-based share and import entrypoint

Testing
- Unit tests for DAO and Repositories
- ViewModel reducer tests
- Compose UI tests for search/add flow and buy button visibility

Open Questions / Decisions
- Which remote catalog (Open Library/Google Books/etc.)? Current DTOs suggest Open Library; confirm.
- Affiliate partners to support (Amazon, Bookshop.org, Kobo, etc.) and tracking requirements.
- Sharing medium-term target: backend or stay local for initial release?

Appendix: File Pointers
- DI: app/src/main/java/.../di/AppModule.kt
- ViewModels: .../presentation/bookcase/* and .../presentation/bookshelf/*
- Data: .../data/book/* (network, dto, mappers, repository impl)
- Room: .../data/book/database/*
- UI Components: .../presentation/bookshelf/bookshelf_components/* and search_components/*

This document is a living guide. As we implement the steps above, update sections on schema, API contracts, and navigation.
