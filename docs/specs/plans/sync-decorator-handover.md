# Handover: Repository Decorator Sync Triggers

## What This Is

Eliminate 13 manual `triggerImmediateSync()` calls from 10 use cases by moving sync triggering to repository decorators using Kotlin's `by` delegation. The full rationale, alternatives analysis, and edge cases are in `docs/specs/plans/sync-decorator-fix.md` — read it first. This document tells you how to execute it.

## Branch

Continue on `profile-page`.

## Before You Start

Read these specs — the code must comply:

| Spec | Why |
|------|-----|
| `docs/specs/constitution.md` | Domain: no Android imports. DRY, SRP. Dependency direction. |
| `docs/specs/patterns/usecase.md` | Current "Sync After Mutation" section (you'll replace it) |
| `docs/specs/plans/sync-decorator-fix.md` | The reviewed plan — all design decisions are made |

Read these existing files for reference patterns:

| File | Why |
|------|-----|
| `core/domain/result/Result.kt:30-37` | `onSuccess` extension — decorators use this |
| `sync/domain/SyncConstants.kt` | `TAG_SYNC_TRIGGER` constant |
| `sync/domain/service/SyncSchedulerService.kt` | Interface the decorators depend on |
| `sync/data/worker/SyncScheduler.kt:93-97` | `ExistingWorkPolicy.REPLACE` — confirms de-duplication |
| `sync/data/engine/SyncEngine.kt:199` | `!it.isBookClub` — confirms club exclusion |
| `testutil/mocks/MockSyncSchedulerService.kt` | Reusable test mock with call counting |
| `book/di/BookModule.kt` | Current repo registration (will change) |
| `sync/di/SyncModule.kt` | Where decorator wiring goes |

## Key Decisions (Already Reviewed — Don't Revisit)

- **No CoroutineContext marker needed** — sync engine writes directly to DAOs, bypasses user-facing repos entirely. Verified.
- **Decorators in `sync/domain/repository/`** — sync-aware wrappers of domain contracts. `sync` depends on `book` (correct direction).
- **DI wiring in `sync/di/SyncModule.kt`** — keeps `book` package unaware of sync. BookModule registers concrete types only.
- **`hardDeleteShelf` NOT overridden** — club-only, entity gone from Room, sync engine excludes clubs at `SyncEngine.kt:199`.
- **`UpdateShelfStyle` conditional removed** — decorator triggers sync unconditionally on `updateShelf`. Sync engine handles club vs personal distinction. Harmless redundancy.
- **Architecture test with inclusion list** — catches both "forgot to override" and "forgot to update test".
- **Eventually consistent, not transactional** — DuplicateShelf may sync intermediate state. Harmless.

## Implementation Order

Work in this order. Each step should compile. Commit after each phase.

---

### Phase 1: Create 3 Repository Decorators (3 new files)

All in `app/src/main/java/uk/co/zlurgg/mybookshelf/sync/domain/repository/`.

**Step 1 — SyncingBookcaseRepository.kt**

```kotlin
class SyncingBookcaseRepository(
    private val delegate: BookcaseRepository,
    private val syncScheduler: SyncSchedulerService,
) : BookcaseRepository by delegate
```

Override `addShelf`, `removeShelf`, `updateShelf` — each calls `delegate.method().onSuccess { log + triggerImmediateSync() }`.

Do NOT override: `hardDeleteShelf`, `addSystemShelf`, `clearUserData`, any read/Flow methods.

Add `companion object { private const val TAG = "SyncBookcase" }`.

**Step 2 — SyncingBookRepository.kt**

Same pattern. Override `upsertBook`, `deleteBook`. Do NOT override `upsertSystemBook`.

**Step 3 — SyncingBookshelfRepository.kt**

Same pattern. Override `addBookToShelf`, `removeBookFromShelf`.

**Compile check:** `./gradlew assembleDebug`

---

### Phase 2: Wire Decorators in DI (2 files)

**Step 4 — BookModule.kt**

Change repository registrations from:
```kotlin
singleOf(::BookshelfRepositoryImpl).bind<BookshelfRepository>()
singleOf(::BookcaseRepositoryImpl).bind<BookcaseRepository>()
singleOf(::BookRepositoryImpl).bind<BookRepository>()
```
To:
```kotlin
single { BookshelfRepositoryImpl(get(), get()) }
single { BookcaseRepositoryImpl(get(), get(), get()) }
single { BookRepositoryImpl(get(), get(), get(), get()) }
```

Register concrete types only — no interface binding.

**Step 5 — SyncModule.kt**

Add decorator wiring:
```kotlin
single<BookshelfRepository> { SyncingBookshelfRepository(get<BookshelfRepositoryImpl>(), get()) }
single<BookcaseRepository> { SyncingBookcaseRepository(get<BookcaseRepositoryImpl>(), get()) }
single<BookRepository> { SyncingBookRepository(get<BookRepositoryImpl>(), get()) }
```

Add imports for decorators and `*RepositoryImpl` classes.

**Compile check:** `./gradlew assembleDebug`

---

### Phase 3: Remove Manual Sync from Use Cases (11 files)

For each file: remove `SyncSchedulerService` from constructor, remove `triggerImmediateSync()` calls + Timber log lines, remove unused imports (`SyncSchedulerService`, `SyncConstants`, maybe `Timber`).

| # | File | Calls Removed | Notes |
|---|------|--------------|-------|
| 1 | `bookcase/domain/usecase/CreateShelfUseCaseImpl.kt` | 1 | Check if `Timber` used elsewhere before removing import |
| 2 | `bookcase/domain/usecase/DeleteShelfUseCaseImpl.kt` | 2 | Both `invoke()` and `restore()`. Keep `Timber` for other logs |
| 3 | `bookcase/domain/usecase/DuplicateShelfUseCaseImpl.kt` | 1 | |
| 4 | `bookcase/domain/usecase/RenameShelfUseCaseImpl.kt` | 1 | Simplify return if possible |
| 5 | `bookcase/domain/usecase/ReorderShelvesUseCaseImpl.kt` | 1 | |
| 6 | `bookcase/domain/usecase/UpdateShelfStyleUseCaseImpl.kt` | 1 | Remove `!isBookClub` conditional, simplify to `return bookcaseRepository.updateShelf(updatedShelf)` |
| 7 | `book/domain/usecase/AddBookToShelfUseCaseImpl.kt` | 1 | Keep `Timber` for other logs |
| 8 | `book/domain/usecase/RemoveBookFromShelfUseCaseImpl.kt` | 1 | Keep `Timber` for other logs |
| 9 | `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseImpl.kt` | 1 | Simplify return |
| 10 | `bookdetail/domain/usecase/UpdateBookMetadataUseCaseImpl.kt` | 1 | Simplify return |
| 11 | `book/domain/usecase/UpsertBookUseCaseImpl.kt` | 0 | Remove KDoc warning only |

**Do NOT modify (keep manual sync):**
- `auth/domain/usecase/ResumeSessionUseCaseImpl.kt`
- `sync/domain/usecase/MigrateLocalDataUseCaseImpl.kt`
- `bookclub/domain/usecase/ValidateBookClubMembershipsUseCaseImpl.kt`

**Compile check:** `./gradlew assembleDebug`

---

### Phase 4: Tests (9 modified + 4 new)

**Step 6 — Update 9 use case test files**

Remove `MockSyncSchedulerService` from constructor and `tearDown()` for each modified use case's test. Remove any sync-related assertions. List:
- `CreateShelfUseCaseTest`, `DeleteShelfUseCaseTest`, `ReorderShelvesUseCaseTest`, `UpdateShelfStyleUseCaseTest`, `RenameShelfUseCaseTest`, `AddBookToShelfUseCaseTest`, `RemoveBookFromShelfUseCaseTest`, `ToggleBookPurchaseUseCaseTest`, `UpdateBookMetadataUseCaseTest`

Check each test file exists first — search for `*UseCaseTest.kt` in the relevant package.

**Step 7 — Create 3 decorator test files**

In `app/src/test/java/uk/co/zlurgg/mybookshelf/sync/domain/repository/`:

For each decorator test, create a simple mock delegate (anonymous object implementing the repo interface), pass `MockSyncSchedulerService`, and verify:
- Write method success → `triggerImmediateSyncCallCount == 1`
- Write method error → `triggerImmediateSyncCallCount == 0`
- Non-overridden method → `triggerImmediateSyncCallCount == 0`
- Flow method → delegates correctly

**Step 8 — Create architecture test**

`SyncDecoratorCoverageTest.kt` in same package. Uses reflection with inclusion lists per decorator.

**Test check:** `./gradlew test`

---

### Phase 5: Update Spec (1 file)

**File:** `docs/specs/patterns/usecase.md`

Replace the "Sync After Mutation" section with documentation of the decorator pattern. Include:
- Which methods are overridden per decorator
- The 3 exceptions and why
- Why no infinite loop
- Eventually consistent note for multi-write use cases
- Architecture test enforcement

---

## Files Quick Reference

### New files (3 production + 4 test)
| File | Purpose |
|------|---------|
| `sync/domain/repository/SyncingBookcaseRepository.kt` | Decorator |
| `sync/domain/repository/SyncingBookRepository.kt` | Decorator |
| `sync/domain/repository/SyncingBookshelfRepository.kt` | Decorator |
| Tests: `sync/domain/repository/SyncingBookcaseRepositoryTest.kt` | |
| Tests: `sync/domain/repository/SyncingBookRepositoryTest.kt` | |
| Tests: `sync/domain/repository/SyncingBookshelfRepositoryTest.kt` | |
| Tests: `sync/domain/repository/SyncDecoratorCoverageTest.kt` | Architecture test |

### Modified files (13 production + 9 tests + 1 doc)
| File | Change |
|------|--------|
| `book/di/BookModule.kt` | Register concrete types only (no interface binding) |
| `sync/di/SyncModule.kt` | Add decorator wiring for 3 repo interfaces |
| 10 use case files | Remove `SyncSchedulerService` + sync calls |
| `book/domain/usecase/UpsertBookUseCaseImpl.kt` | Remove KDoc warning |
| 9 use case test files | Remove sync mock from constructors |
| `docs/specs/patterns/usecase.md` | Replace "Sync After Mutation" section |

## Gotchas

1. **Koin circular resolution** — `single<BookcaseRepository> { SyncingBookcaseRepository(get(), get()) }` would resolve `get()` as the interface (itself). Must use `get<BookcaseRepositoryImpl>()`.
2. **BookModule must NOT bind interfaces** — Change to `single { Impl(...) }` without `.bind<Interface>()`. SyncModule handles the interface binding.
3. **Check `Timber` import before removing** — Some use cases use `Timber` for non-sync logs. Only remove the import if no other Timber calls remain.
4. **`DuplicateShelfUseCaseImpl` calls both `BookcaseRepository.addShelf` and `BookshelfRepository.addBookToShelf`** — Both decorators fire. REPLACE de-duplicates. Harmless.
5. **Architecture test uses reflection** — Use `declaredMethods` (not `methods`) to check overrides in the decorator class itself, not inherited methods.

## Verification Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew test` passes
- [ ] No `SyncSchedulerService` import in the 10 modified use cases
- [ ] `triggerImmediateSync()` still in ResumeSession, MigrateLocalData, ValidateBookClubMemberships
- [ ] Decorators do NOT override `addSystemShelf`, `clearUserData`, `upsertSystemBook`, `hardDeleteShelf`
- [ ] Architecture test catches a hypothetical new write method
- [ ] On-device: create shelf → logcat shows `SyncTrigger` from decorator
- [ ] On-device: duplicate shelf with 3 books → multiple `SyncTrigger` logs, only 1 SyncWorker run
