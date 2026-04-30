# Handover: Fix Sync Gaps

## What This Is

Add missing `triggerImmediateSync()` calls so data syncs to Firestore after all user actions. Create `ResumeSessionUseCase` to own all post-auth setup (prefs, clubs, sync) and remove sync from `SignInUseCaseImpl`. The full rationale and review history is in `docs/specs/plans/sync-gaps-fix.md` — read it first. This document tells you how to execute it.

## Branch

Continue on `profile-page`.

## Before You Start

Read these specs — the code must comply:

| Spec | Why |
|------|-----|
| `docs/specs/constitution.md` | Domain: no Android imports. ViewModels depend on UseCases, not services. |
| `docs/specs/patterns/usecase.md` | Interface + Impl pattern, sync-after-mutation convention (you'll add this) |
| `docs/specs/plans/sync-gaps-fix.md` | The reviewed plan — all design decisions are made |

Read these existing files for reference patterns:

| File | Why |
|------|-----|
| `auth/domain/usecase/SignInUseCaseImpl.kt` | Current sync trigger pattern (lines 39-40 — you'll remove these) |
| `bookcase/domain/usecase/CreateShelfUseCaseImpl.kt:48-50` | Established sync-after-mutation pattern to follow |
| `sync/domain/SyncConstants.kt` | `TAG_SYNC_TRIGGER` log tag constant |
| `testutil/mocks/MockSyncSchedulerService.kt` | Reusable test mock with call counting |

## Key Decisions (Already Reviewed — Don't Revisit)

- **ResumeSessionUseCase aggregates all post-auth setup** — prefs, club restoration, sync scheduling. ViewModel calls it once after any auth check. Eliminates duplication across three sign-in paths.
- **Sync removed from SignInUseCaseImpl** — sign-in use cases authenticate only. ResumeSessionUseCase owns sync. No double-triggering.
- **DevSignInUseCaseImpl unchanged** — it doesn't trigger sync, and doesn't need to — ResumeSessionUseCase handles it from the ViewModel.
- **UpsertBookUseCase skipped** — only called as a cache-for-navigation, not a user mutation. KDoc warning added instead.
- **UpdateShelfStyle conditional** — only trigger sync for personal shelves, not club shelves (clubs push to Firestore directly and are excluded from the sync engine).
- **schedulePeriodicSync() is defensive** — normally a no-op on resume since WorkManager persists jobs. Called in case OS cleared it.

## Implementation Order

Work in this order. Each step should compile. Commit after each phase.

---

### Phase 1: ResumeSessionUseCase + SignIn cleanup (Changes 1)

**Step 1 — Create ResumeSessionUseCase interface**

**New file:** `auth/domain/usecase/ResumeSessionUseCase.kt`
```kotlin
package uk.co.zlurgg.mybookshelf.auth.domain.usecase

interface ResumeSessionUseCase {
    suspend operator fun invoke()
}
```

**Step 2 — Create ResumeSessionUseCaseImpl**

**New file:** `auth/domain/usecase/ResumeSessionUseCaseImpl.kt`

Dependencies:
- `SyncUserPreferencesUseCase` — from `sync/domain/usecase/`
- `RestoreBookClubMembershipsUseCase` — from `bookclub/domain/usecase/`
- `SyncSchedulerService` — from `sync/domain/service/`

Implementation:
1. Call `syncUserPreferences()`
2. Call `restoreBookClubMemberships()` — handle `Result.Error` with `Timber.tag(TAG).w(...)` (log but don't fail)
3. Call `syncScheduler.schedulePeriodicSync()`
4. Call `syncScheduler.triggerImmediateSync()`

Add `companion object { private const val TAG = "ResumeSession" }`.

Check `RestoreBookClubMembershipsUseCase` return type — it returns `Result<RestoreResult, DataError.Sync>` where `RestoreResult` has `restoredCount` and `failedCount`. Log both on success.

**Step 3 — Remove sync from SignInUseCaseImpl**

**File:** `auth/domain/usecase/SignInUseCaseImpl.kt`
- Remove `private val syncScheduler: SyncSchedulerService` from constructor (goes from 3 params to 2)
- Remove lines 38-40 (the sync scheduling block):
  ```kotlin
  // Remove these lines:
  Timber.tag(TAG).d("Scheduling sync after sign-in")
  syncScheduler.schedulePeriodicSync()
  syncScheduler.triggerImmediateSync()
  ```
- Remove `import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService`

**Step 4 — Update SignInViewModel**

**File:** `auth/presentation/SignInViewModel.kt`
- Replace constructor params:
  ```kotlin
  // Remove these two:
  private val syncUserPreferencesUseCase: SyncUserPreferencesUseCase,
  private val restoreBookClubMembershipsUseCase: RestoreBookClubMembershipsUseCase,
  // Add this one:
  private val resumeSession: ResumeSessionUseCase,
  ```
- In `checkSignInStatus()` (~line 116-119): replace `syncUserPreferencesUseCase()` + `restoreBookClubMemberships()` with `resumeSession()`
- In `signIn()` (~line 155-159): replace `syncUserPreferencesUseCase()` + `restoreBookClubMemberships()` with `resumeSession()`
- In `devSignIn()` (~line 75-76): replace `syncUserPreferencesUseCase()` + `restoreBookClubMemberships()` with `resumeSession()`
- Delete the private `restoreBookClubMemberships()` method (~lines 269-284)
- Update imports: remove `SyncUserPreferencesUseCase` + `RestoreBookClubMembershipsUseCase`, add `ResumeSessionUseCase`

**Step 5 — Update AuthModule DI**

**File:** `auth/di/AuthModule.kt`
- Add registration: `single<ResumeSessionUseCase> { ResumeSessionUseCaseImpl(get(), get(), get()) }`
- Update `SignInUseCaseImpl` construction: remove one `get()` (goes from 3 to 2 args)
- Update `SignInViewModel` construction: replace `syncUserPreferencesUseCase = get()` + `restoreBookClubMembershipsUseCase = get()` with `resumeSession = get()`
- Add imports for `ResumeSessionUseCase` and `ResumeSessionUseCaseImpl`
- Remove imports for `SyncUserPreferencesUseCase` and `RestoreBookClubMembershipsUseCase` if no longer used in this file

**Compile check:** `./gradlew assembleDebug`

---

### Phase 2: Fix use case sync gaps (Changes 2-4)

These are independent — can be done in any order.

**Step 6 — ToggleBookPurchaseUseCaseImpl**

**File:** `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseImpl.kt`
- Add constructor param: `private val syncSchedulerService: SyncSchedulerService`
- Add imports: `SyncSchedulerService`, `SyncConstants`, `Timber`
- After the successful upsert path (before `return Result.Success(updatedBook)`), add:
  ```kotlin
  Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: ToggleBookPurchase")
  syncSchedulerService.triggerImmediateSync()
  ```

Reference `bookdetail/domain/usecase/UpdateBookMetadataUseCaseImpl.kt:69-70` for the exact pattern.

**Step 7 — UpdateShelfStyleUseCaseImpl**

**File:** `bookcase/domain/usecase/UpdateShelfStyleUseCaseImpl.kt`
- Add constructor param: `private val syncSchedulerService: SyncSchedulerService`
- Add import: `SyncSchedulerService`, `SyncConstants`
- Change the `return bookcaseRepository.updateShelf(updatedShelf)` (line 53) to:
  ```kotlin
  return when (val result = bookcaseRepository.updateShelf(updatedShelf)) {
      is Result.Success -> {
          if (!shelfToUpdate.isBookClub) {
              Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: UpdateShelfStyle")
              syncSchedulerService.triggerImmediateSync()
          }
          result
      }
      is Result.Error -> result
  }
  ```

**Step 8 — ReorderShelvesUseCaseImpl**

**File:** `bookcase/domain/usecase/ReorderShelvesUseCaseImpl.kt`
- Add constructor param: `private val syncSchedulerService: SyncSchedulerService`
- Add imports: `SyncSchedulerService`, `SyncConstants`, `Timber`
- Before `return Result.Success(updatedShelves)` (line 47), add:
  ```kotlin
  Timber.tag(SyncConstants.TAG_SYNC_TRIGGER).d("Sync triggered by: ReorderShelves")
  syncSchedulerService.triggerImmediateSync()
  ```

**Compile check:** `./gradlew assembleDebug`

---

### Phase 3: KDoc + spec updates (Changes 5-6)

**Step 9 — UpsertBookUseCaseImpl KDoc**

**File:** `book/domain/usecase/UpsertBookUseCaseImpl.kt`
- Add KDoc above the class:
  ```kotlin
  /**
   * WARNING: Does not trigger sync. This is intentional — UpsertBook is a building-block
   * used by parent use cases (AddBookToShelf, etc.) that handle sync themselves.
   * If you call this directly for a user-facing mutation, you must trigger sync separately.
   */
  ```

**Step 10 — UseCase spec update**

**File:** `docs/specs/patterns/usecase.md`
- Add a "Sync After Mutation" section after "Testing UseCases", documenting:
  - All mutating use cases must inject `SyncSchedulerService` and call `triggerImmediateSync()` after successful local mutations
  - Use `SyncConstants.TAG_SYNC_TRIGGER` for the log tag
  - Building-block use cases may skip this but must add a KDoc warning
  - This is a manual convention — tech debt for future enforcement

---

### Phase 4: Tests (Step 11)

**Step 11a — ResumeSessionUseCaseTest**

**New file:** `app/src/test/java/uk/co/zlurgg/mybookshelf/auth/domain/usecase/ResumeSessionUseCaseTest.kt`

Check `RestoreBookClubMembershipsUseCase` interface for exact return type before writing mocks.

Test cases:
1. `invoke - calls syncUserPreferences`
2. `invoke - calls restoreBookClubMemberships`
3. `invoke - calls schedulePeriodicSync and triggerImmediateSync`
4. `invoke - club restoration failure does not prevent sync`

Use `MockSyncSchedulerService` from `testutil/mocks/`. Create inline mocks for the other two use cases.

**Step 11b — Update SignInViewModelTest**

**File:** `app/src/test/java/uk/co/zlurgg/mybookshelf/auth/presentation/SignInViewModelTest.kt`
- Remove `syncUserPreferencesUseCase` and `restoreBookClubMembershipsUseCase` mocks
- Add mock `ResumeSessionUseCase` (simple: `override suspend fun invoke() {}` with call tracking)
- Update `createViewModel()` to pass `resumeSession` instead of the two removed params
- Update `SignInUseCaseImpl` construction (remove `SyncSchedulerService` — it may use `mockSyncScheduler` currently)
- Add test: `auto sign-in - triggers resumeSession`
- Add test: `not signed in - does not trigger resumeSession`

**Step 11c — Update SignInUseCaseTest (if exists)**

Search for `auth/domain/usecase/SignInUseCaseTest.kt` or similar. If it exists:
- Remove `SyncSchedulerService` mock from `SignInUseCaseImpl` construction
- Remove assertions on `schedulePeriodicSync` / `triggerImmediateSync` calls

**Step 11d — Update ToggleBookPurchaseUseCaseTest**

**File:** `app/src/test/java/uk/co/zlurgg/mybookshelf/bookdetail/domain/usecase/ToggleBookPurchaseUseCaseTest.kt`
- Add `private val mockSyncSchedulerService = MockSyncSchedulerService()`
- Pass to use case constructor
- Add `mockSyncSchedulerService.reset()` to tearDown

**Step 11e — Create UpdateShelfStyleUseCaseTest**

**New file:** `app/src/test/java/uk/co/zlurgg/mybookshelf/bookcase/domain/usecase/UpdateShelfStyleUseCaseTest.kt`

Check what mocks are needed: `BookcaseRepository`, `ClubOperations`, `AuthService`, `SyncSchedulerService`.

Test cases:
1. `personal shelf - triggers sync after update`
2. `book club shelf - does NOT trigger sync`

**Step 11f — Update ReorderShelvesUseCaseTest**

**File:** `app/src/test/java/uk/co/zlurgg/mybookshelf/bookcase/domain/usecase/ReorderShelvesUseCaseTest.kt`
- Add `private val mockSyncSchedulerService = MockSyncSchedulerService()`
- Pass to use case constructor
- Add `mockSyncSchedulerService.reset()` to tearDown

**Test check:** `./gradlew test`

---

## Files Quick Reference

**All paths relative to `app/src/main/java/uk/co/zlurgg/mybookshelf/`** unless noted.

### New files (4 + 1 test)
| File | Purpose |
|------|---------|
| `auth/domain/usecase/ResumeSessionUseCase.kt` | Interface |
| `auth/domain/usecase/ResumeSessionUseCaseImpl.kt` | Aggregates post-auth setup: prefs + clubs + sync |
| Tests: `auth/domain/usecase/ResumeSessionUseCaseTest.kt` | |
| Tests: `bookcase/domain/usecase/UpdateShelfStyleUseCaseTest.kt` | Branch coverage |

### Modified files (11 + 4 tests)
| File | Change |
|------|--------|
| `auth/domain/usecase/SignInUseCaseImpl.kt` | Remove `SyncSchedulerService`, remove sync calls |
| `auth/presentation/SignInViewModel.kt` | Replace 3 post-auth params with `resumeSession` |
| `auth/di/AuthModule.kt` | Register ResumeSessionUseCase, update SignInUseCaseImpl + SignInViewModel |
| `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseImpl.kt` | Add sync trigger |
| `bookcase/domain/usecase/UpdateShelfStyleUseCaseImpl.kt` | Add sync trigger (personal only) |
| `bookcase/domain/usecase/ReorderShelvesUseCaseImpl.kt` | Add sync trigger |
| `book/domain/usecase/UpsertBookUseCaseImpl.kt` | Add KDoc warning |
| `docs/specs/patterns/usecase.md` | Add "Sync After Mutation" section |
| Tests: `auth/presentation/SignInViewModelTest.kt` | Replace mocks, add tests |
| Tests: `auth/domain/usecase/SignInUseCaseTest.kt` | Remove sync mock/assertions (if exists) |
| Tests: `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseTest.kt` | Add sync mock |
| Tests: `bookcase/domain/usecase/ReorderShelvesUseCaseTest.kt` | Add sync mock |

## Gotchas

1. **`SignInUseCaseImpl` constructor arg count changes** — from 3 to 2. Update both the DI registration (`AuthModule.kt`) and any test that constructs it directly.
2. **`SignInViewModel` constructor params change** — two removed, one added. Update `AuthModule.kt` and `SignInViewModelTest.kt`.
3. **`RestoreBookClubMembershipsUseCase` return type** — check the exact return type before writing mocks. It returns `Result<RestoreResult, DataError.Sync>` — `RestoreResult` has `restoredCount` and `failedCount`.
4. **`UpdateShelfStyleUseCaseImpl` has no `singleOf` registration** — check if it's registered via `BookcaseUseCases` aggregator or directly. The `SyncSchedulerService` dependency must be resolvable by Koin.
5. **Don't add sync to `DevSignInUseCaseImpl`** — `ResumeSessionUseCase` handles it from the ViewModel. No changes to debug source set needed.
6. **`SyncConstants.TAG_SYNC_TRIGGER`** — use this tag for all sync trigger logs, not a custom tag.

## Verification Checklist

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew detekt` passes
- [ ] `./gradlew test` passes
- [ ] No `SyncSchedulerService` import in `SignInUseCaseImpl`
- [ ] No `syncUserPreferencesUseCase` or `restoreBookClubMembershipsUseCase` in `SignInViewModel` constructor
- [ ] `ResumeSessionUseCase` called in all three ViewModel paths (checkSignInStatus, signIn, devSignIn)
- [ ] On-device: sign in → create shelf → Firestore emulator shows data
- [ ] On-device: kill app → relaunch → logcat shows `SyncWorker` running on auto-sign-in
- [ ] On-device: toggle book purchase → Firestore updates
