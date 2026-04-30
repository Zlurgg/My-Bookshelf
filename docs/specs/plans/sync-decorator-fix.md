# Plan: Eliminate Manual Sync Triggers via Repository Decorators

## Context

After the sync-gaps fix, there are 13 manual `triggerImmediateSync()` calls across 10 use cases (plus 3 exceptions that keep their calls). Every new mutating use case must remember to add this call — a manual convention with no compile-time enforcement. The `usecase.md` spec itself calls this "tech debt."

This plan replaces the manual pattern with 3 repository decorator classes using Kotlin's `by` delegation. The decorators automatically trigger sync on successful write operations. Use cases no longer need `SyncSchedulerService`.

### Why this works without a CoroutineContext marker

The sync engine (`SyncWorker` → `SyncRepository` → `SyncEngine`) writes **directly to DAOs** (`bookshelfDao`, `syncDao`), completely bypassing user-facing repositories (`BookRepository`, `BookcaseRepository`, `BookshelfRepository`). The decorators only wrap the user-facing interfaces, so sync engine writes never pass through them. No infinite loop possible.

### Alternatives considered

| Alternative | Verdict | Reason |
|---|---|---|
| Keep manual calls + architecture test | Rejected | Doesn't eliminate duplication (13 call sites remain). Test-time check, not structural |
| Room InvalidationTracker | Rejected | Can't distinguish user writes from sync engine writes at DAO level. Data layer concern |
| UseCase base class / decorator | Rejected | Use cases have different invoke signatures. Conditional sync (club shelves) can't be expressed uniformly |
| CoroutineContext marker | Unnecessary | Sync engine bypasses user repos entirely — no marker needed |
| **`by` delegation decorator** | **Accepted** | Minimal code, structural enforcement, DRY, testable, idiomatic Kotlin |

## Implementation

### Phase 1: Create 3 Repository Decorators

New files in `app/src/main/java/uk/co/zlurgg/mybookshelf/sync/domain/repository/`:

**`SyncingBookcaseRepository.kt`** — wraps `BookcaseRepository`
- Override: `addShelf`, `removeShelf`, `updateShelf`
- Delegate (no sync): `hardDeleteShelf` (club-only, entities gone from Room — sync is pointless), `addSystemShelf`, `clearUserData`, all read/Flow methods
- On `Result.Success`: log with `SyncConstants.TAG_SYNC_TRIGGER`, call `syncScheduler.triggerImmediateSync()`

**`SyncingBookRepository.kt`** — wraps `BookRepository`
- Override: `upsertBook`, `deleteBook`
- Delegate (no sync): `upsertSystemBook`, all read methods

**`SyncingBookshelfRepository.kt`** — wraps `BookshelfRepository`
- Override: `addBookToShelf`, `removeBookFromShelf`
- Delegate: all read/Flow methods

Uses existing `Result.onSuccess` extension from `core/domain/result/Result.kt:30` — no new helper needed (DRY).

### Phase 2: Wire Decorators in DI

**File:** `sync/di/SyncModule.kt` (not BookModule — keeps book package unaware of sync)

Add decorator wiring that overrides the book repository bindings:

```kotlin
// Override book repo bindings with sync-aware decorators
single<BookshelfRepository> { SyncingBookshelfRepository(get<BookshelfRepositoryImpl>(), get()) }
single<BookcaseRepository> { SyncingBookcaseRepository(get<BookcaseRepositoryImpl>(), get()) }
single<BookRepository> { SyncingBookRepository(get<BookRepositoryImpl>(), get()) }
```

**File:** `book/di/BookModule.kt`

Change from `singleOf(::Impl).bind<Interface>()` to registering impls by concrete type only (no interface binding — sync module handles that):

```kotlin
single { BookshelfRepositoryImpl(get(), get()) }
single { BookcaseRepositoryImpl(get(), get(), get()) }
single { BookRepositoryImpl(get(), get(), get(), get()) }
```

This keeps the dependency direction correct: `sync` → `book`, not `book` → `sync`.

Uses `get<ConcreteType>()` in SyncModule to resolve the raw impl (avoids circular resolution).

### Phase 3: Remove Manual Sync from 10 Use Cases

Remove `SyncSchedulerService` constructor param + `triggerImmediateSync()` calls + unused imports from:

1. `bookcase/domain/usecase/CreateShelfUseCaseImpl.kt` — 1 call
2. `bookcase/domain/usecase/DeleteShelfUseCaseImpl.kt` — 2 calls (delete + restore)
3. `bookcase/domain/usecase/DuplicateShelfUseCaseImpl.kt` — 1 call
4. `bookcase/domain/usecase/RenameShelfUseCaseImpl.kt` — 1 call
5. `bookcase/domain/usecase/ReorderShelvesUseCaseImpl.kt` — 1 call
6. `bookcase/domain/usecase/UpdateShelfStyleUseCaseImpl.kt` — 1 call + remove conditional `!isBookClub` guard
7. `book/domain/usecase/AddBookToShelfUseCaseImpl.kt` — 1 call
8. `book/domain/usecase/RemoveBookFromShelfUseCaseImpl.kt` — 1 call
9. `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseImpl.kt` — 1 call
10. `bookdetail/domain/usecase/UpdateBookMetadataUseCaseImpl.kt` — 1 call

Also remove KDoc warning from `book/domain/usecase/UpsertBookUseCaseImpl.kt` — no longer needed since all `upsertBook()` calls auto-sync via decorator.

**Keep manual sync in (3 exceptions — write through non-decorated paths):**
- `auth/domain/usecase/ResumeSessionUseCaseImpl.kt` — session setup, not a repo mutation
- `sync/domain/usecase/MigrateLocalDataUseCaseImpl.kt` — writes through `SyncRepository` → DAO directly
- `bookclub/domain/usecase/ValidateBookClubMembershipsUseCaseImpl.kt` — writes through `bookshelfDao.upsertShelf()` directly via `BookClubRepositoryHelper`

No DI changes needed for use cases — `singleOf(::Impl)` auto-resolves fewer constructor params.

### Phase 4: Architecture Test for New-Method Detection

**New file:** `app/src/test/.../sync/domain/repository/SyncDecoratorCoverageTest.kt`

An architecture test that verifies all user-facing write methods on the 3 decorated repositories are overridden in their respective decorators. Prevents the failure mode of "forgot to override new write method in decorator" — the same class of problem the decorators solve for use cases.

Approach: Maintain an **inclusion list** of expected overrides per decorator. Two assertions per decorator:
1. Every method in the inclusion list is declared (overridden) in the decorator class
2. Every `suspend fun` returning `Result<*, DataError.Local>` on the interface that is NOT in the inclusion list is NOT overridden (safely delegated)

This catches both "forgot to override a write" (assertion 1 fails when inclusion list has a method the decorator doesn't override) and "forgot to update the test" (assertion 2 fails when a new write method is added to the interface without being listed). No exclusion list to maintain — the inclusion list IS the source of truth for which methods trigger sync.

Test cases:
- `SyncingBookcaseRepository overrides all write methods on BookcaseRepository`
- `SyncingBookRepository overrides all write methods on BookRepository`
- `SyncingBookshelfRepository overrides all write methods on BookshelfRepository`

If someone adds `suspend fun archiveShelf(id: String): Result<Unit, DataError.Local>` to `BookcaseRepository`, this test fails until the decorator is updated (either to override it or to add it to the exclusion list with justification).

### Phase 5: Update Tests

**Remove sync mock from 9 use case test files** (remove `MockSyncSchedulerService` from constructors, remove sync assertions):
- `CreateShelfUseCaseTest`, `DeleteShelfUseCaseTest`, `ReorderShelvesUseCaseTest`, `UpdateShelfStyleUseCaseTest`, `RenameShelfUseCaseTest`, `AddBookToShelfUseCaseTest`, `RemoveBookFromShelfUseCaseTest`, `ToggleBookPurchaseUseCaseTest`, `UpdateBookMetadataUseCaseTest`

**3 new decorator test files** in `app/src/test/.../sync/domain/repository/`:
- `SyncingBookcaseRepositoryTest.kt`:
  - `addShelf success` → triggers sync
  - `addShelf error` → no sync
  - `removeShelf success` → triggers sync
  - `updateShelf success` → triggers sync
  - `hardDeleteShelf` → no sync (delegated, not overridden)
  - `addSystemShelf` → no sync (delegated)
  - `clearUserData` → no sync (delegated, passes through decorator but delegates without sync)
  - `getAllShelves Flow` → delegates correctly (verify `by` delegation works for Flow return types)
- `SyncingBookRepositoryTest.kt` — same pattern for `upsertBook`, `deleteBook`, `upsertSystemBook`
- `SyncingBookshelfRepositoryTest.kt` — same pattern for `addBookToShelf`, `removeBookFromShelf`

### Phase 6: Update Spec

**File:** `docs/specs/patterns/usecase.md`

Replace the "Sync After Mutation" section to document:
- Decorator pattern and which methods are overridden
- The 3 exceptions and why
- Why no infinite loop (sync engine bypasses user repos)
- `hardDeleteShelf` deliberately excluded (club cleanup, entity gone from Room)
- New write methods: architecture test enforces coverage

## Edge Cases

| Case | Handling |
|---|---|
| `DuplicateShelfUseCaseImpl` makes 1 `addShelf` + N `addBookToShelf` calls | Fires `triggerImmediateSync()` from `SyncingBookcaseRepository.addShelf` and N times from `SyncingBookshelfRepository.addBookToShelf`. `ExistingWorkPolicy.REPLACE` (confirmed `SyncScheduler.kt:95`) de-duplicates — only last enqueue's work runs. Note: eventually consistent, not transactionally consistent — WorkManager could start between `addShelf` and first `addBookToShelf`, syncing a shelf with zero books. Subsequent triggers catch the books. Harmless (server-side intermediate state only, user never sees it). |
| `DeleteShelfUseCaseImpl.restore()` calls `addShelf()` | `SyncingBookcaseRepository` catches it. Manual call removed. |
| `UpdateShelfStyle` on club shelves | Decorator triggers sync unconditionally on `updateShelf`. Sync engine excludes club shelves from push (`SyncEngine.kt:199`: `!it.isBookClub`) and pull (`SyncEngine.kt:328`). Harmless redundancy. |
| `addSystemShelf` / `upsertSystemBook` | NOT overridden in decorators — delegated as-is via `by delegate`. No sync triggered. |
| `clearUserData` (sign-out cleanup) | Passes through `SyncingBookcaseRepository` but delegates to impl without sync (not overridden). Called by `ClearUserDataUseCaseImpl` during sign-out. Correct: no sync after wiping data. |
| `hardDeleteShelf` (club cleanup) | NOT overridden. Club shelves are hard-deleted from Room immediately. Sync is pointless — entity is gone from Room, and sync engine excludes club shelves anyway (`SyncEngine.kt:199`). |
| New write method added to repository interface | Delegated without sync by default. Architecture test (`SyncDecoratorCoverageTest`) fails, forcing developer to either override in decorator or add to exclusion list. |

## Assumptions

1. **`ExistingWorkPolicy.REPLACE` de-duplicates** — Verified: `SyncScheduler.kt:95`
2. **Koin `single { Impl(...) }` resolves by concrete type** — Standard Koin behavior, `get<ConcreteType>()` works
3. **Sync engine excludes club data** — Verified: `SyncEngine.kt:199` (`!it.isBookClub`), `SyncEngine.kt:328` (skips club shelves during pull)
4. **No other repositories write user-syncable data** — Only `BookRepository`, `BookcaseRepository`, `BookshelfRepository` + the 3 exceptions
5. **Koin resolves lazily, module order is irrelevant** — BookModule registers concrete types (`BookRepositoryImpl`), SyncModule registers interface types (`BookRepository`). Different type keys, no override conflict. `get<BookRepositoryImpl>()` resolves at call time, not registration time — both bindings exist by then.

## Performance

- `triggerImmediateSync()` is non-suspending, enqueues a WorkManager request (~microseconds)
- `DuplicateShelf` with N books: N+1 WorkManager enqueues, all de-duplicated via REPLACE. Only the final enqueue results in actual sync work.
- `by delegate` generates bytecode-level delegation — zero overhead for non-overridden methods (including Flow returns)

## Security

No new attack surface. Decorators don't handle auth, credentials, or network. `triggerImmediateSync()` is an internal WorkManager enqueue.

## Clean Architecture

- **SRP**: Each decorator has one job — intercept write results to trigger sync
- **DRY**: Eliminates 13 duplicated trigger+log patterns across 10 use cases
- **Domain purity**: Decorators depend only on domain interfaces (`BookcaseRepository`, `SyncSchedulerService`) + Timber. No Android imports
- **OCP**: New write methods delegate without sync by default. Architecture test enforces conscious decision
- **Dependency direction**: Decorators in `sync/domain/repository/`, wired in `sync/di/SyncModule.kt`. Sync depends on book (correct direction), book doesn't know about sync
- **Location**: `sync/domain/repository/` — sync-aware wrappers of domain contracts

## Verification

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew test` passes
- [ ] No `SyncSchedulerService` import in the 10 modified use cases
- [ ] `triggerImmediateSync()` still in ResumeSession, MigrateLocalData, ValidateBookClubMemberships
- [ ] Decorators do NOT override `addSystemShelf`, `clearUserData`, `upsertSystemBook`, `hardDeleteShelf`
- [ ] Architecture test catches a hypothetical new write method (verify by temporarily adding one)
- [ ] On-device: create shelf → logcat shows `SyncTrigger` from decorator
- [ ] On-device: duplicate shelf with 3 books → logcat shows multiple `SyncTrigger` logs, only 1 actual SyncWorker run
