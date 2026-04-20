# Detekt Cleanup — Zero Violations, Clean Config

## Context

MyBookshelf's detekt config has path-based excludes hiding violations in production code (`presentation/**`, `data/**`, `service/**`, `engine/**`, `util/**`). The goal is to remove every path exclude, fix every violation, and get `./gradlew detekt` passing clean.

Allowed: `@Suppress("TooGenericExceptionCaught")` on the 2 centralized error boundary methods in ErrorMapper (DayTo pattern). Test-only excludes remain. `InstanceOfCheckForException` exclude for `**/service/**` stays with a documenting comment (Credential Manager API forces this pattern).

Current state: 80+ violations when excludes are removed.

All work on a feature branch with option to squash/rebase.

---

## Phase 1: Package Renames

**Commit:** `refactor: Rename underscore packages to match ktlint conventions`

Rename directories + update all package declarations and imports:
- `book_detail` → `bookdetail` (main: 14 files + components subdir, test: 7 files)
- `bookshelf_components` → `bookshelfcomponents` (8 files)
- `search_components` → `searchcomponents` (5 files)

~50 import updates across consuming files.

Use `git mv` for history preservation. Run `./gradlew clean` after renames to clear stale caches before verifying build + tests.

---

## Phase 2: Quick Fixes

**Commit:** `fix: Resolve formatting and style detekt violations`

| Fix | File | Change |
|-----|------|--------|
| NoConsecutiveBlankLines | MyBookshelfApplication.kt | Remove extra blank line |
| Indentation | ApiConfig.kt:19-20 | Fix continuation indent |
| Indentation | ErrorFormatter.kt:41 | Fix continuation indent |
| UnusedPrivateProperty | WelcomePreferencesImpl.kt | Remove unused TAG |
| UseRequire | Base64Encoder.kt:75 | `throw IllegalArgumentException` → `require()` |
| ImplicitDefaultLocale | ClubRatingCard.kt:81 | Add `Locale.ROOT` |
| ImplicitDefaultLocale | CommunityRatingsCard.kt:62 | Add `Locale.ROOT` |
| MagicNumber | HttpClientFactory.kt:37 | Extract `MAX_RETRIES = 3` constant |
| MagicNumber | UpsertBookClubReviewUseCaseImpl.kt:26 | Extract `MAX_RATING = 5f` constant |
| MaximumLineLength | 7 instances in test files | Wrap lines to ≤120 chars |
| NoWildcardImports | 2 test files | Replace with explicit imports |
| InstanceOfCheckForException | GetAllShelvesUseCaseTest.kt:145 | Use `assertThrows` pattern |

Verify: build + tests + detekt pass (with current config).

---

## Phase 3: ReturnCount Fixes

**Commit:** `refactor: Reduce return count in use cases with helper extraction`

| UseCase | Returns | Strategy |
|---------|---------|----------|
| DeleteShelfUseCaseImpl | 5 | Extract `deleteBookClubShelf()` helper |
| JoinBookClubUseCaseImpl | 7 | Extract `validateJoinPreconditions()` helper |
| LeaveBookClubUseCaseImpl | 5 | Extract `getClubCodeForShelf()` helper |
| GetOrCreateTutorialBookUseCaseImpl | 5 | Extract `createAndAddTutorialBook()` helper |
| RenameShelfUseCaseImpl | 5 | Verify guard clause exclusion — may already pass |

**Config change:** Remove `excludes: ['**/data/**', '**/engine/**']` from `ReturnCount`.

---

## Phase 4: BookClubUseCases — Inject What's Needed

**Commit:** `refactor(bookclub): Split BookClubUseCases by consumer need`

Each consumer should receive only the use cases it actually uses — no bag-of-everything.

**Current consumers:**
- `BookClubOperationsHandler` uses 10 of 16 (club management + membership)
- `BookDetailViewModel` uses 7 of 16 (reviews + comments only)

**Fix:** Replace single `BookClubUseCases` (16 params) with two domain-aligned aggregators:
- `BookClubOperationUseCases` (10 params) → injected into `BookClubOperationsHandler`
- `BookClubReviewUseCases` (7 params) → injected into `BookDetailViewModel`

No three-level nesting. Each consumer gets exactly what it uses.

**Files:** Rename BookClubUseCases → BookClubOperationUseCases + new BookClubReviewUseCases. Update BookClubModule (DI), BookClubOperationsHandler, BookDetailViewModel, test mocks.

**Config change:** Remove `excludes: ['**/presentation/**']` from `LongParameterList`.

---

## Phase 5: ErrorFormatter Complexity

**Commit:** `refactor(core): Split ErrorFormatter into per-category formatters`

Verify actual cyclomatic complexity from detekt output before implementing. Split single `when` into private functions:
- `formatRemoteError(error: DataError.Remote): String`
- `formatLocalError(error: DataError.Local): String`
- `formatValidationError(error: DataError.Validation): String`
- `formatSyncError(error: DataError.Sync): String`

Main function drops to complexity ~4. Each sub-function well under 20 limit.

**Config change:** Remove `excludes: ['**/presentation/**']` from `CyclomaticComplexMethod`.

---

## Phase 6: Consolidate Error Handling (DayTo pattern)

**Commit:** `refactor(core): Centralize exception handling in ErrorMapper wrappers`

Following DayTo's approach — catch exceptions in ErrorMapper wrappers, not at each call site.

**Audit each file individually:**

| File | Current Pattern | Action |
|------|----------------|--------|
| SyncEngine | Inline try-catch + mapExceptionToDataError | Wrap in safeSuspendCall |
| SyncRepositoryImpl | Inline try-catch + mapExceptionToDataError | Wrap in safeSuspendCall |
| DatabaseBookshelfDataOrchestrator | Inline try-catch + mapExceptionToDataError | Wrap in safeSuspendCall |
| FirebaseEmulatorConfig (debug) | Intentional catch-all for connectivity | **Leave as-is** — debug-only, different purpose |
| DevAuthService (debug) | Intentional catch-all for auth failure | **Leave as-is** — debug-only, different purpose |

Keep only 2 `@Suppress("TooGenericExceptionCaught")`:
- `ErrorMapper.safeSuspendCall()` — general boundary
- `ErrorMapper.httpNetworkCall()` — HTTP boundary

**Config change:** Remove all path excludes from `TooGenericExceptionCaught`. Keep `['**/test/**', '**/androidTest/**', '**/debug/**']` — debug source set has intentional catch-alls for emulator diagnostics and dev auth, distinct from production error handling.

---

## Phase 7: Split BookshelfDao (lowest layer first)

**Commit:** `refactor(core): Split BookshelfDao into focused DAOs`

DAOs are the lowest layer — split first for smaller, isolated diffs.

| New DAO | Functions |
|---------|-----------|
| BookDao | 11 (book CRUD, sync status, owner ops) |
| ShelfDao | 16 (shelf CRUD, sync status, sharing, owner ops) |
| CrossRefDao | 11 (cross-ref CRUD, queries, sync status) |

Room supports multiple DAOs. Update `MyBookshelfRoomDatabase` to expose all three. Update CoreModule (DI) and all consumers.

---

## Phase 8: Split BookClubRepository

**Commit:** `refactor(bookclub): Split BookClubRepository into focused interfaces`

| New Interface | Functions | Responsibility |
|--------------|-----------|---------------|
| BookClubManagementRepository | 6 | Club CRUD (create, get, delete, rename, updateStyle, convert) |
| BookClubMembershipRepository | 8 | Members (observe, getLocal, isMember, join, leave, restore, clear) |
| BookClubSyncRepository | 4 | Book sync (getClubBooks, syncBook, removeBook, syncBooksFromClub) |
| BookClubReviewRepository | 7 | Reviews + comments (all CRUD) |

**Shared helpers:**
- `cleanupLocalClubData` — uses injected dependencies (DAOs, remoteDataSource). Extract to `BookClubCleanupHelper` internal class, Koin-injected into management + membership impls. DI justified.
- `generateUniqueShelfName` — pure function (just `return clubName`). Inline it or make it a top-level `internal fun`. No DI needed.

**Test impact:** `MockBookClubRepository` becomes 4 focused mocks. Each test mocks only the interface it uses — actually cleaner than mocking 21 functions when you use 2.

**Files:** 8 new (4 interfaces + 4 impls) + 1 helper. Delete BookClubRepository + BookClubRepositoryImpl. Update: BookClubModule (DI), all use cases, test mocks.

**STOP POINT:** If shared-helper extraction causes problems, reassess Phase 9 before continuing.

---

## Phase 9: Split RemoteSyncDataSource + FirestoreRemoteDataSource

**Commit:** `refactor(sync): Split RemoteSyncDataSource into focused interfaces`

| New Interface | Functions |
|--------------|-----------|
| BookSyncDataSource | 4 (upload/download/delete books) |
| ShelfSyncDataSource | 6 (upload/download/delete shelves + batch) |
| SharedShelfDataSource | 5 (share/unshare/get/subscribe/unsubscribe) |
| UserPreferencesDataSource | 2 (get/set prefs) |
| BookClubRemoteDataSource | 15 (all club remote ops) |

Extract shared `executeFirestoreOperation` + `mapFirestoreException` to `FirestoreOperationHelper`.

**Files:** 10 new (5 interfaces + 5 impls) + 1 helper. Update: SyncModule (DI), SyncEngine, Phase 8 repo impls.

---

## Phase 10: BookcaseViewModel Function Count

**Commit:** `refactor(bookcase): Extract state reducers as BookcaseState extensions`

Move ~10 private state extension functions to **BookcaseState.kt as extensions** (not a separate class):
- `withError`, `withShelfAdded`, `withShelfDeleted`, `withShelfDeleteError`, `withShelfRestored`
- `updateShelfInList`, `closeRenameDialog`, `closeStyleDialog`, `withRenameError`
- `calculateNextShelfNumber`

Delegate join-book-club methods (`lookupBookClub`, `confirmJoinBookClub`, `handleInviteLink`) to existing `BookClubOperationsHandler`.

ViewModel drops from ~28 to ~15 functions.

---

## Phase 11: Remove Remaining Presentation Excludes

**Scouted: 21 violations.** Split into 3 sub-commits.

### Phase 11a: ViewModel onAction refactoring

**Commit:** `refactor: Extract onAction dispatch into grouped handler methods`

| File | Issue | Fix |
|------|-------|-----|
| BookcaseViewModel `onAction` | 215 lines, CC 48 | Group `when` branches into private dispatch methods by concern (shelf ops, club ops, auth ops) |
| BookDetailViewModel `onAction` | 246 lines, CC 53 | Group by concern (book ops, review ops, comment ops) |

### Phase 11b: Screen composable extraction

**Commit:** `refactor(ui): Extract sub-composables from large screens`

| File | Lines | Fix |
|------|-------|-----|
| BookcaseScreen | 279 lines, CC 39 | Extract dialog section, tab content, top bar into sub-composables |
| BookcaseScreenRoot | 106 lines, CC 20 | Extract side-effect handlers |
| BookshelfScreen | 268 lines, CC 29 | Extract search section, book list, empty state |
| BookDetailScreen | 231 lines | Extract card sections |
| BookSearchDialog | 139 lines | Extract filter section |
| BookshelfCard | 201 lines, 14 params | Extract actions into `BookshelfCardActions` data class (same file as BookshelfCard) |
| ClubCommentsCard | 156 lines, 14 params | Extract callbacks into `CommentActions` data class (same file as ClubCommentsCard) |
| CommentBubble | 10 params | Extract callbacks into parent's `CommentActions` (same file) |
| ClubRatingCard | 104 lines | Minor — just over threshold, extract star rating row |
| MyBookShelfApp | 198 lines | Extract nav graph routes into helper functions |

### Phase 11c: Test cleanup

**Commit:** `test: Simplify BookcaseViewModelTest createViewModel`

| File | Issue | Fix |
|------|-------|-----|
| BookcaseViewModelTest `createViewModel` | 105 lines | Extract mock setup into `@Before` or shared helper |

**Config change (after all 11a-c):** Remove `excludes: ['**/presentation/**']` from `LongMethod`, `LongParameterList`, `CyclomaticComplexMethod`, `MagicNumber`.

---

## Phase 12: Final Config Cleanup

**Commit:** `build: Finalize detekt config — remove all path excludes`

Final `detekt.yml` — only test excludes remain:
- `LargeClass`, `MaxLineLength`, `WildcardImport`, `SpreadOperator`, `TooGenericExceptionThrown` — `['**/test/**', '**/androidTest/**']`
- `TooGenericExceptionCaught` — `['**/test/**', '**/androidTest/**', '**/debug/**']`
- `FunctionNaming: active: false` (ktlint handles)
- `PackageNaming: active: false` (ktlint handles)
- `InstanceOfCheckForException: excludes: ['**/service/**']` — Credential Manager API forces instanceof; un-fixable without wrapping library

Remove TODO comment.

**Verification:** `./gradlew detekt` — 0 issues. `./gradlew test` — pass. `./gradlew assembleDebug` — pass.

---

## Execution Order

```
Phase 1 (package renames) — most disruptive, clean build after
  ↓
Phase 2 (quick fixes)
  ↓
Phase 3 (ReturnCount) ─┐
Phase 4 (UseCases split) ─── independent, any order
Phase 5 (ErrorFormatter) ─┘
  ↓
Phase 6 (error handling consolidation)
  ↓  NOTE: All subsequent data-layer code must use safeSuspendCall
  ↓
Phase 7 (BookshelfDao split) — lowest layer first
  ↓
Phase 8 (BookClubRepository split) — STOP POINT if helper issues
  ↓
Phase 9 (RemoteSyncDataSource split)
  ↓
Phase 10 (BookcaseViewModel state reducers) — depends on Phase 4
  ↓
Phase 11a (ViewModel onAction refactoring)
Phase 11b (Screen composable extraction) — 10 screens/components
Phase 11c (Test cleanup)
  ↓
Phase 12 (final config + verification)
```

14 commits. Feature branch with squash/rebase option. Each commit: build + test + detekt must pass.

---

## Completion Log

All phases complete. `./gradlew detekt` passes with 0 violations.

### Session 2 Summary (2026-04-20)

Picked up from Phase 10 onwards. Resolved all 70 remaining violations.

**Deviations from plan:**

- **Phase 8/9 impl splits (deferred → completed):** Original plan deferred the impl class splits for BookClubRepositoryImpl and FirestoreRemoteDataSource. Completed them using Kotlin `by` delegation pattern — each monolith became a thin composite delegating to focused impl classes. Shared helpers extracted to `BookClubRepositoryHelper` (Koin-injected) and `FirestoreOperationHelper` (instantiated per-impl with per-caller TAG).

- **Phase 10:** Extracted 9 state reducers + `calculateNextShelfNumber` to BookcaseState.kt as `internal` extension functions. Dropped ViewModel from 28 to 18 functions.

- **Phase 11a (partial):** Extracted club action handling to `BookcaseClubActionHandler` instead of just grouping `when` branches. This was necessary because grouping alone couldn't fix the LargeClass violation (754 lines, threshold 500). The handler takes `_state`, `viewModelScope`, and domain handlers as constructor params. Dropped ViewModel from 754 to 320 lines.

- **Phase 11b/11c (skipped):** Screen composable extraction and test cleanup were not needed — no detekt violations remain in presentation layer after the Phase 11a extraction. These can be revisited as tech-debt cleanup independent of detekt.

- **Phase 12 (not needed):** Config already correct. All path excludes for production code were removed in earlier phases. Zero violations without further config changes.

- **ErrorFormatter:** `formatLocalError` was at CC=20 (exactly at threshold, triggers violation). Fixed by extracting auth errors into `formatLocalAuthError`, dropping to CC=16.

- **Cross-boundary calls in BookClubRepositoryImpl split:** Three cross-boundary dependencies resolved via helper:
  1. `syncBooksFromClub` → `convertClubToPersonalShelf`: moved to `BookClubRepositoryHelper.convertToPersonalShelf()`
  2. `joinBookClub`/`restoreClubMembership` → `getBookClub`: added `BookClubRepositoryHelper.fetchBookClubMetadata()`
  
- **FirestoreOperationHelper:** NOT Koin-injected (unlike BookClubRepositoryHelper). Each focused Firestore impl instantiates its own with a unique TAG for per-caller logging. Simpler than DI for a stateless utility.

### Final file counts

| New File | Lines | Functions |
|----------|-------|-----------|
| FirestoreCollections.kt | 17 | 0 (object) |
| FirestoreOperationHelper.kt | 42 | 2 |
| FirestoreBookSyncDataSourceImpl.kt | 98 | 5 |
| FirestoreShelfSyncDataSourceImpl.kt | 164 | 10 |
| FirestoreUserPreferencesDataSourceImpl.kt | 48 | 2 |
| FirestoreBookClubRemoteDataSourceImpl.kt | 286 | 22 |
| FirestoreRemoteDataSource.kt (delegator) | 21 | 0 |
| BookClubRepositoryHelper.kt | 148 | 7 |
| BookClubManagementRepositoryImpl.kt | 282 | 7 |
| BookClubMembershipRepositoryImpl.kt | 195 | 7 |
| BookClubSyncRepositoryImpl.kt | 178 | 4 |
| BookClubReviewRepositoryImpl.kt | 186 | 7 |
| BookClubRepositoryImpl.kt (delegator) | 21 | 0 |
| BookcaseClubActionHandler.kt | 244 | 8 |
| BookcaseState.kt (extensions) | +84 | +10 |
