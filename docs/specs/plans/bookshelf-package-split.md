# Refactor: Split bookshelf/ into book/, bookcase/, bookshelf/

## Context

The `bookshelf/` package contains 3 distinct features (bookdetail, bookcase, bookshelf screen) in one package with a single DI module. This makes the package too large and violates SRP. Before adding the manual book entry feature (`addbook/`), we need clean package boundaries.

**Goal:** After refactoring:
- `book/` — shared domain (models, repos, data) + bookdetail feature (use cases + presentation)
- `bookcase/` — new top-level package: home screen (shelf list, create/delete/rename shelves)
- `bookshelf/` — just the shelf screen (viewing books on a shelf, searching, adding)

**Dependency graph:**
```
bookshelf/ → bookcase/ → book/ → core/
                  ↓
              welcome/  (tutorial shelf creation — see Known Coupling below)

bookclub/ → book/ → core/
```

`bookshelf/ → bookcase/` is a single interface import (`GetShelfByIdUseCase`). This is a conscious trade-off — see decision rationale in Phase 2 Step 2.2.

### Known Coupling: bookcase/ → welcome/

Three touch points:
- `CreateShelfUseCaseImpl` → `GetOrCreateTutorialBookUseCase` (tutorial book on shelf creation)
- `ShelfManagementHandler` → `HandleTutorialAccessUseCase` (help icon → tutorial restore)
- `BookcaseViewModel` → `TutorialAccessResult` (result type)

This is the same pattern as `ClubOperations` in `book/domain/service/` for cross-feature communication. **TODO**: Consider extracting a `TutorialOperations` interface in `book/domain/service/` to decouple bookcase/ from welcome/ — same inversion pattern used for book clubs. Not blocking for this refactor, but should be addressed before adding more cross-feature coupling.

---

## Phase 1: Move bookdetail → book/

Lowest risk. BookDetailViewModel depends only on BookDetailUseCases, BookReviewProvider, AuthUseCases.

### Step 1.1: Move use case files (13 files)

`bookshelf/domain/usecase/bookdetail/*.kt` → `book/domain/usecase/bookdetail/*.kt`

Files: AddBookToShelfUseCase + Impl, RemoveBookFromShelfUseCase + Impl, GetBookDetailsUseCase + Impl, UpsertBookUseCase + Impl, ToggleBookPurchaseUseCase + Impl, UpdateBookMetadataUseCase + Impl, BookDetailUseCases (facade)

Update package declarations: `bookshelf.domain.usecase.bookdetail` → `book.domain.usecase.bookdetail`

### Step 1.2: Move presentation files (~18 files)

`bookshelf/presentation/bookdetail/**/*.kt` → `book/presentation/bookdetail/**/*.kt`

Files: BookDetailViewModel, BookDetailScreen, BookDetailState, BookDetailAction + ~14 component files in components/

### Step 1.3: Update BookshelfUseCases facade imports

`bookshelf/domain/usecase/bookshelf/BookshelfUseCases.kt` — 3 imports change from `bookshelf.domain.usecase.bookdetail.*` to `book.domain.usecase.bookdetail.*` (AddBookToShelf, RemoveBookFromShelf, UpsertBook)

### Step 1.4: Rewire DI

**BookshelfModule.kt** — remove:
- 6 bookdetail use case bindings (lines 54-59)
- BookDetailUseCases facade (lines 78-87)
- BookDetailViewModel binding (lines 137-145)

**BookModule.kt** — add:
- 6 bookdetail use case bindings
- BookDetailUseCases facade
- BookDetailViewModel binding (with bookId + shelfId params)

### Step 1.5: Update navigation

**MyBookShelfApp.kt** — change imports:
- `bookshelf.presentation.bookdetail.BookDetailViewModel` → `book.presentation.bookdetail.BookDetailViewModel`
- `bookshelf.presentation.bookdetail.BookDetailsScreenRoot` → `book.presentation.bookdetail.BookDetailsScreenRoot`

### Step 1.6: Move tests (7 files)

- 6 use case tests: `test/.../bookshelf/domain/usecase/bookdetail/` → `test/.../book/domain/usecase/bookdetail/`
- 1 VM test: `test/.../bookshelf/presentation/bookdetail/BookDetailViewModelTest.kt` → `test/.../book/presentation/bookdetail/`

### Step 1.7: Verify

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest
```

**Commit:** `refactor(book): Move bookdetail feature from bookshelf/ to book/ package`

---

## Phase 2: Extract bookcase → bookcase/

Higher complexity: more files, handlers, cross-feature deps, dead code removal.

### Step 2.1: Remove dead code

Audited all BookshelfModule.kt bindings against actual usage. Only dead code found:

- **BookcaseUseCases.kt**: Remove `shareShelf: ShareBookshelfUseCase` field + its import. Never called from BookcaseViewModel — sharing only happens from BookshelfViewModel via BookshelfUseCases.
- **ShelfOperationsHandler.kt**: Remove `shareShelf()` method (line 46-48). Zero callers confirmed via grep.
- **BookshelfModule.kt**: Remove `shareShelf = get()` from BookcaseUseCases factory.
- **BookcaseViewModelTest.kt**: Remove `shareShelf = MockShareBookshelfUseCase()` from facade construction. Note: edit this in its old location before Step 2.9 moves the file.

All other BookshelfModule bindings have live callers — no additional dead code.

### Step 2.2: Keep GetShelfByIdUseCase in bookcase/ (decision)

GetShelfByIdUseCase is a pure passthrough (`return bookcaseRepository.getShelfById(shelfId)`). Two options were considered:

**Option A (rejected): Move to book/.** Eliminates bookshelf/ → bookcase/ dep, but book/ becomes a dumping ground for "things multiple features need." GetShelfByIdUseCase is semantically a shelf-management concern, not shared book domain.

**Option B (chosen): Keep in bookcase/.** BookshelfViewModel imports `GetShelfByIdUseCase` from bookcase/ — a clean one-way dependency for a single interface. Shelf lookup belongs with shelf management. The project convention forbids VMs calling repositories directly, so eliminating the use case entirely is not an option.

This creates `bookshelf/ → bookcase/` (single interface import). Acceptable because:
- It's one-way, not circular
- It's a single interface, not a module-wide dependency
- The alternative (book/ dumping ground) trades honesty for a prettier graph

### Step 2.3: Update BookshelfViewModel

Replace `BookcaseUseCases` facade injection with direct `GetShelfByIdUseCase` (ISP — don't inject 9 use cases for 1 call):

```kotlin
class BookshelfViewModel(
    private val bookshelfUseCases: BookshelfUseCases,
    private val getShelfById: GetShelfByIdUseCase,  // was: bookcaseUseCases (9 use cases)
    private val bookClubOperations: ClubOperations,
    private val shelfId: String
)
```

Update `loadShelfDetails()`: `getShelfById(shelfId)` instead of `bookcaseUseCases.getShelfById(shelfId)`

Update BookshelfModule.kt VM binding: replace `bookcaseUseCases = get()` with `getShelfById = get()`

### Step 2.4: Move bookcase use case files (17 files)

`bookshelf/domain/usecase/bookcase/*.kt` → `bookcase/domain/usecase/*.kt`

Files (17): BookcaseUseCases (facade), ClearUserData + Impl, CreateShelf + Impl, DeleteShelf + Impl, DuplicateShelf + Impl, GetAllShelves + Impl, GetShelfById + Impl, RenameShelf + Impl, ReorderShelves + Impl, UpdateShelfStyle + Impl

Update package: `bookshelf.domain.usecase.bookcase` → `bookcase.domain.usecase`

### Step 2.5: Move bookcase presentation files (~20 files)

`bookshelf/presentation/bookcase/**/*.kt` → `bookcase/presentation/**/*.kt`

Files: BookcaseViewModel, BookcaseScreen, BookcaseState, BookcaseAction, BookcaseTab + handlers/ (ShelfOperationsHandler, ShelfManagementHandler, BookcaseClubActionHandler) + components/ (~10 files)

**Handler lifecycle note:** ShelfOperationsHandler and ShelfManagementHandler are **stateless** — they hold only use case refs (also singletons) and do validation/delegation. Singleton lifetime is correct. BookcaseClubActionHandler takes `MutableStateFlow<BookcaseState>` + `CoroutineScope` — VM-scoped, so manual instantiation inside BookcaseViewModel is correct. No DI change needed for it.

### Step 2.6: Create BookcaseModule.kt

New file: `bookcase/di/BookcaseModule.kt`

Contains:
- 8 bookcase use case bindings (NOT ClearUserData — stays in AuthModule)
- BookcaseUseCases facade (without shareShelf, 8 fields)
- ShelfOperationsHandler + ShelfManagementHandler singletons
- BookcaseViewModel binding

### Step 2.7: Clean up BookshelfModule.kt

After removing all bookdetail (Phase 1) and bookcase (Phase 2) bindings, BookshelfModule.kt should contain only:
- 4 use case bindings: SearchBooks, GetShelfBooks, ShareBookshelf, UpdateShelfTidyMode
- BookshelfUseCases facade
- BookshelfViewModel binding

### Step 2.8: Update external imports

**AppModule.kt**: Add `bookcaseModule` to module list

**AuthModule.kt**: Change ClearUserDataUseCase imports:
- `bookshelf.domain.usecase.bookcase.ClearUserDataUseCase` → `bookcase.domain.usecase.ClearUserDataUseCase`
- Same for Impl

**MyBookShelfApp.kt**: Change bookcase imports:
- `bookshelf.presentation.bookcase.*` → `bookcase.presentation.*`

**MockUseCases.kt** (8 import changes):
- `bookshelf.domain.usecase.bookcase.*` → `bookcase.domain.usecase.*` for: MockGetAllShelves, MockCreateShelf, MockDeleteShelf, MockDuplicateShelf, MockGetShelfById, MockRenameShelf, MockReorderShelves, MockUpdateShelfStyle
- MockShareBookshelfUseCase uses FQN `bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase` — unchanged

**BookshelfModule.kt**: Add `import bookcase.domain.usecase.GetShelfByIdUseCase` for the VM binding updated in Step 2.3

**Auth tests** (SignOutUseCaseTest, SignInViewModelTest): Update ClearUserDataUseCase import paths

### Step 2.9: Move bookcase tests (7 files)

- 6 use case tests: `test/.../bookshelf/domain/usecase/bookcase/` → `test/.../bookcase/domain/usecase/`
  - GetAllShelvesUseCaseTest, CreateShelfUseCaseTest, DeleteShelfUseCaseTest, GetShelfByIdUseCaseTest, RenameShelfUseCaseTest, ReorderShelvesUseCaseTest
- 1 VM test: `test/.../bookshelf/presentation/bookcase/BookcaseViewModelTest.kt` → `test/.../bookcase/presentation/`

### Step 2.10: Verify

```bash
./gradlew assembleDebug && ./gradlew testDebugUnitTest
```

**Commit:** `refactor(bookcase): Extract bookcase feature into top-level package`

---

## Phase 3: Fix stale test packages + cleanup

### Step 3.1: Fix integration test packages

Move 3 files from `androidTest/.../bookshelf/data/repository/` → `androidTest/.../book/data/repository/`
- BookRepositoryIntegrationTest.kt
- BookcaseRepositoryIntegrationTest.kt
- BookshelfRepositoryIntegrationTest.kt

These test repository implementations that already live in `book/data/repository/`. The test package declarations are stale from the Phase 1 restructure. BookcaseRepositoryIntegrationTest stays in book/ (not bookcase/) because BookcaseRepositoryImpl lives in book/data/.

### Step 3.2: Fix DAO test packages

Move 2 files from `test/.../bookshelf/data/database/` → `test/.../core/data/database/`
- BookshelfDaoTest.kt
- BookshelfDaoSyncTest.kt

These test DAOs in core/data/database/. Package declarations are stale.

### Step 3.3: Remove empty directories

Delete empty dirs left behind by moves under `bookshelf/domain/usecase/bookdetail/`, `bookshelf/domain/usecase/bookcase/`, `bookshelf/presentation/bookdetail/`, `bookshelf/presentation/bookcase/`

### Step 3.4: Update documentation

- AppModule.kt KDoc comment
- Update `docs/specs/plans/book-input-alternatives.md` to reflect actual architecture
- Update CLAUDE.md package table

### Step 3.5: Full verification

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew detekt
./gradlew connectedDebugAndroidTest  # if device available
```

**Commit:** `chore: Fix stale test packages and update documentation`

---

## Risks

### Koin runtime verification gap

No `checkModules` tests exist in this project. Koin DI errors are runtime failures — a missing or misconfigured binding compiles fine but crashes at app launch. This is an existing gap, not introduced by this refactor, but the DI rewiring increases the chance of hitting it.

**Mitigation options (in order of confidence):**
1. Manual smoke test: launch app, navigate to each screen (bookcase → bookshelf → bookdetail)
2. Add `koinTest { checkModules() }` test as part of this refactor
3. Accept the risk and rely on existing test coverage

**Recommendation:** Option 1 at minimum. Option 2 would be valuable but is a separate concern — could be a follow-up.

---

## Analysis

### Clean Architecture / SOLID
- **SRP**: Each package owns one feature. BookshelfModule.kt goes from 146 lines (3 features) to ~30 lines (1 feature).
- **DIP**: Use case interfaces stay in domain layer. DI wiring in dedicated modules. No concrete dependencies crossing package boundaries.
- **ISP**: BookshelfViewModel no longer injects BookcaseUseCases (9 use cases) for 1 method call. Gets exactly the interface it needs.

### DRY
- AddBookToShelf, RemoveBookFromShelf, UpsertBook are defined once in book/ and referenced by both BookDetailUseCases and BookshelfUseCases facades. No duplication.
- GetShelfByIdUseCase defined once in bookcase/, used by both BookshelfViewModel and BookcaseUseCases facade.

### Edge Cases Addressed
- **BookcaseClubActionHandler** is manually instantiated in BookcaseViewModel (not Koin-injected). Moves with the VM — no DI change needed.
- **ClearUserDataUseCase** is bound in AuthModule.kt, not BookshelfModule. Only the import path changes.
- **Handler singleton lifetime**: ShelfOperationsHandler and ShelfManagementHandler are stateless (hold only singleton use case refs). Singleton lifetime is correct. BookcaseClubActionHandler holds VM-scoped state — correctly instantiated manually.
- **BookcaseViewModelTest** constructs BookcaseUseCases with `shareShelf` param — must be updated when removing the field.

### Assumptions Validated
- Navigation is lambda-based (no hardcoded cross-feature routes)
- All 3 ViewModels are independently constructible via Koin
- BookshelfViewModel only uses getShelfById from BookcaseUseCases (confirmed line 153)
- shareShelf in BookcaseUseCases has zero callers (confirmed via grep)
- All other BookshelfModule bindings have live callers (no additional dead code)
- GetShelfByIdUseCase is a pure passthrough (confirmed — single line: `return bookcaseRepository.getShelfById(shelfId)`)

### Performance
- No runtime impact. Adding one more Koin module has negligible startup cost.

### Security
- Pure package restructuring. No new API surfaces, permissions, or data exposure.

## Critical Files

| File | Change |
|------|--------|
| `bookshelf/di/BookshelfModule.kt` | Strip to bookshelf-only bindings |
| `book/di/BookModule.kt` | Add bookdetail use case bindings |
| `bookcase/di/BookcaseModule.kt` | **New** — all bookcase bindings |
| `di/AppModule.kt` | Add bookcaseModule |
| `auth/di/AuthModule.kt` | Update ClearUserDataUseCase import |
| `app/presentation/MyBookShelfApp.kt` | Update bookdetail + bookcase imports |
| `bookshelf/domain/usecase/bookshelf/BookshelfUseCases.kt` | Update bookdetail imports to book/ |
| `bookshelf/presentation/bookshelf/BookshelfViewModel.kt` | Replace BookcaseUseCases with GetShelfByIdUseCase |
| `testutil/mocks/MockUseCases.kt` | Update 8 bookcase mock imports |
| `bookshelf/presentation/bookcase/BookcaseViewModelTest.kt` | Remove shareShelf from facade construction |
