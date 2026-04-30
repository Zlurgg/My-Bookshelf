# Fix Sync Gaps: Trigger Sync on All Mutating User Actions

## Context

After the profile→account refactor, on-device testing revealed that data doesn't appear in the Firestore emulator. Investigation found that while most shelf/book CRUD operations correctly trigger `SyncSchedulerService.triggerImmediateSync()`, several code paths are missing sync triggers. The most impactful gap is that auto-sign-in (app relaunch when already signed in) never triggers sync at all — so local changes sit unsynced and remote changes aren't pulled until the 15-minute periodic sync fires.

## Gaps to Fix

| # | Gap | File | Impact |
|---|-----|------|--------|
| 1 | Auto-sign-in doesn't sync | `auth/presentation/SignInViewModel.kt` | Local changes never push on app relaunch |
| 2 | Dev sign-in doesn't sync | `auth/data/usecase/DevSignInUseCaseImpl.kt` | Dev testing always stale |
| 3 | ToggleBookPurchase doesn't sync | `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseImpl.kt` | Purchase status stays local |
| 4 | UpdateShelfStyle doesn't sync | `bookcase/domain/usecase/UpdateShelfStyleUseCaseImpl.kt` | Personal shelf style changes stay local |
| 5 | ReorderShelves doesn't sync | `bookcase/domain/usecase/ReorderShelvesUseCaseImpl.kt` | Shelf positions stay local |

**Skipping:** `UpsertBookUseCaseImpl` — only called as a cache-for-navigation from `BookshelfViewModel.OnBookClick`, not a user mutation. Add KDoc warning so future callers know it doesn't sync.

## Implementation

### Change 1: ResumeSessionUseCase + clean up sign-in sync ownership (Gaps 1 & 2)

**Problem:** Three sign-in paths (`signIn`, `devSignIn`, `checkSignInStatus`) each independently call `syncUserPreferencesUseCase()` + `restoreBookClubMemberships()` in the ViewModel, and handle sync inconsistently — `SignInUseCaseImpl` triggers sync in the use case, `DevSignInUseCaseImpl` doesn't, and `checkSignInStatus` doesn't.

**Solution:** Single owner for all post-auth setup. Sign-in use cases authenticate + persist state. `ResumeSessionUseCase` handles everything after: prefs, clubs, sync. No duplication, no double-triggering, consistent across all paths.

**Responsibility split:**
- `SignInUseCaseImpl` / `DevSignInUseCaseImpl` — authenticate + set auth state (SRP: "sign the user in")
- `ResumeSessionUseCase` — sync prefs + restore clubs + schedule/trigger sync (SRP: "set up the authenticated session")
- `SignInViewModel` — orchestrate: authenticate, then resume session

**New file: `auth/domain/usecase/ResumeSessionUseCase.kt`**
```kotlin
interface ResumeSessionUseCase {
    suspend operator fun invoke()
}
```

**New file: `auth/domain/usecase/ResumeSessionUseCaseImpl.kt`**
```kotlin
class ResumeSessionUseCaseImpl(
    private val syncUserPreferences: SyncUserPreferencesUseCase,
    private val restoreBookClubMemberships: RestoreBookClubMembershipsUseCase,
    private val syncScheduler: SyncSchedulerService,
) : ResumeSessionUseCase {
    override suspend operator fun invoke() {
        syncUserPreferences()

        when (val result = restoreBookClubMemberships()) {
            is Result.Success -> {
                Timber.tag(TAG).d(
                    "Book club memberships restored: %d restored, %d failed",
                    result.data.restoredCount,
                    result.data.failedCount
                )
            }
            is Result.Error -> {
                // Log but don't fail the session setup
                Timber.tag(TAG).w("Failed to restore book club memberships: %s", result.error)
            }
        }

        // Defensive: re-establish periodic sync in case WorkManager cleared it
        // (e.g., app data cleared, OS killed the worker). Normally a no-op
        // since schedulePeriodicSync uses ExistingPeriodicWorkPolicy.KEEP.
        syncScheduler.schedulePeriodicSync()
        syncScheduler.triggerImmediateSync()
    }

    companion object {
        private const val TAG = "ResumeSession"
    }
}
```

**`auth/domain/usecase/SignInUseCaseImpl.kt`:**
- Remove `syncScheduler: SyncSchedulerService` from constructor
- Remove `syncScheduler.schedulePeriodicSync()` and `syncScheduler.triggerImmediateSync()` calls (lines 39-40)
- Sign-in now only authenticates + persists auth state — sync is handled by `ResumeSessionUseCase`

**`auth/presentation/SignInViewModel.kt`:**
- Add constructor param: `private val resumeSession: ResumeSessionUseCase`
- Remove `syncUserPreferencesUseCase` and `restoreBookClubMembershipsUseCase` constructor params
- Replace the duplicated post-auth sequences in all three paths:
  - `checkSignInStatus()`: replace `syncUserPreferencesUseCase()` + `restoreBookClubMemberships()` with `resumeSession()`
  - `signIn()`: replace `syncUserPreferencesUseCase()` + `restoreBookClubMemberships()` with `resumeSession()`
  - `devSignIn()`: replace `syncUserPreferencesUseCase()` + `restoreBookClubMemberships()` with `resumeSession()`
- Delete the private `restoreBookClubMemberships()` helper method (logic moves into use case)

**`auth/di/AuthModule.kt`:**
- Register `ResumeSessionUseCase` binding
- Update `SignInUseCaseImpl` constructor: remove `SyncSchedulerService` (now 2 params instead of 3)
- Update `SignInViewModel` constructor: replace `syncUserPreferencesUseCase`, `restoreBookClubMembershipsUseCase` with `resumeSession`

**New test file: `auth/domain/usecase/ResumeSessionUseCaseTest.kt`**
- Test: invokes syncUserPreferences, restoreBookClubMemberships, schedulePeriodicSync, triggerImmediateSync

**`auth/domain/usecase/SignInUseCaseTest.kt`** (if exists):
- Remove `SyncSchedulerService` mock from `SignInUseCaseImpl` construction
- Remove any assertions on sync calls (sync is no longer this use case's responsibility)

**`auth/presentation/SignInViewModelTest.kt`:**
- Replace `syncUserPreferencesUseCase` + `restoreBookClubMembershipsUseCase` mocks with single `resumeSession` mock
- Update `SignInUseCaseImpl` construction (remove `SyncSchedulerService`)
- Add test: auto-sign-in calls resumeSession
- Add test: not-signed-in does not call resumeSession

**No changes to `DevSignInUseCaseImpl` or `DebugModule`** — dev sign-in already doesn't trigger sync, and now it correctly doesn't need to because `ResumeSessionUseCase` handles it from the ViewModel.

### Change 2: ToggleBookPurchaseUseCase (Gap 3)

**`bookdetail/domain/usecase/ToggleBookPurchaseUseCaseImpl.kt`:**
- Add constructor param: `private val syncSchedulerService: SyncSchedulerService`
- After successful upsert, before return, add sync trigger with `SyncConstants.TAG_SYNC_TRIGGER` log
- Follows exact same pattern as sibling `UpdateBookMetadataUseCaseImpl`

**`bookdetail/domain/usecase/ToggleBookPurchaseUseCaseTest.kt`:**
- Add `MockSyncSchedulerService`, pass to use case constructor
- No DI changes needed — `singleOf(::ToggleBookPurchaseUseCaseImpl)` auto-resolves

### Change 3: UpdateShelfStyleUseCase (Gap 4)

**`bookcase/domain/usecase/UpdateShelfStyleUseCaseImpl.kt`:**
- Add constructor param: `private val syncSchedulerService: SyncSchedulerService`
- Only trigger sync for personal shelves (`!shelfToUpdate.isBookClub`) — club shelves already push to Firestore directly via `clubOperations.updateClubStyle()` and are excluded from the sync engine
- No DI changes needed

**New test file: `bookcase/domain/usecase/UpdateShelfStyleUseCaseTest.kt`:**
- Test: personal shelf update triggers sync
- Test: book club shelf update does NOT trigger sync (conditional branch coverage)

### Change 4: ReorderShelvesUseCase (Gap 5)

**`bookcase/domain/usecase/ReorderShelvesUseCaseImpl.kt`:**
- Add constructor param: `private val syncSchedulerService: SyncSchedulerService`
- Trigger sync after successful reorder, before return
- No DI changes needed

**`bookcase/domain/usecase/ReorderShelvesUseCaseTest.kt`:**
- Add `MockSyncSchedulerService`, pass to use case constructor
- Add to `tearDown()` reset

### Change 5: KDoc warning on UpsertBookUseCase

**`book/domain/usecase/UpsertBookUseCaseImpl.kt`:**
- Add KDoc:
  ```kotlin
  /**
   * WARNING: Does not trigger sync. This is intentional — UpsertBook is a building-block
   * used by parent use cases (AddBookToShelf, etc.) that handle sync themselves.
   * If you call this directly for a user-facing mutation, you must trigger sync separately.
   */
  ```

### Change 6: Document sync requirement in UseCase spec

**`docs/specs/patterns/usecase.md`:**
- Add a "Sync After Mutation" section documenting:
  - All mutating use cases must inject `SyncSchedulerService` and call `triggerImmediateSync()` after successful local mutations
  - Use `SyncConstants.TAG_SYNC_TRIGGER` for the log tag
  - Building-block use cases (called only by parents that sync) may skip this — but must document it with a KDoc warning
  - This is a manual convention with no compile-time enforcement — tech debt to address later (e.g., repository write observer, decorator pattern)

## Edge Cases Considered

- **Offline:** `triggerImmediateSync()` enqueues WorkManager with `NetworkType.CONNECTED` constraint — queues until online. No special handling needed.
- **Guest users:** `checkSignInStatus()` only enters the sync path when `isSignedIn == true`. `SyncWorker` also no-ops when no user is signed in. Safe.
- **Rapid reorders:** `ExistingWorkPolicy.REPLACE` means only the last sync fires. Room writes are synchronous within each use case call, so positions are consistent when sync reads them.
- **Club shelf style:** Guarded with `!isBookClub` check to avoid wasteful sync enqueue for data the sync engine ignores.
- **Concurrent sync on guest data migration:** After `signIn()`, `ResumeSessionUseCase` triggers immediate sync. If the user then imports guest data, `MigrateLocalDataUseCaseImpl` triggers another immediate sync. `ExistingWorkPolicy.REPLACE` means the migration sync replaces the session sync — safe because WorkManager cancels the in-flight worker and starts fresh. The migration sync will push all local data including newly migrated guest data.

## Tech Debt: Sync as Cross-Cutting Concern

The codebase now has ~15 call sites doing `syncSchedulerService.triggerImmediateSync()`. Every new mutating use case must remember to add this call. This is a manual convention with no compile-time enforcement.

**Future options to reduce risk:**
- Repository write observer that auto-triggers sync on Room mutations
- UseCase decorator/wrapper that adds sync after any successful mutation
- At minimum: the UseCase spec doc (Change 6) makes the requirement discoverable

## Files Modified

| File | Change |
|------|--------|
| `auth/domain/usecase/ResumeSessionUseCase.kt` | **New** — interface |
| `auth/domain/usecase/ResumeSessionUseCaseImpl.kt` | **New** — aggregates post-auth setup: prefs + clubs + sync |
| `auth/domain/usecase/ResumeSessionUseCaseTest.kt` | **New** — verifies all delegate calls |
| `auth/domain/usecase/SignInUseCaseImpl.kt` | Remove `SyncSchedulerService`, remove sync calls |
| `auth/presentation/SignInViewModel.kt` | Replace 3 post-auth params with `resumeSession`, call in all paths |
| `auth/di/AuthModule.kt` | Register ResumeSessionUseCase, update SignInUseCaseImpl + SignInViewModel params |
| `auth/domain/usecase/SignInUseCaseTest.kt` | Remove SyncSchedulerService mock and sync assertions |
| `auth/presentation/SignInViewModelTest.kt` | Replace mocks, update SignInUseCaseImpl construction, add tests |
| `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseImpl.kt` | Add sync trigger |
| `bookdetail/domain/usecase/ToggleBookPurchaseUseCaseTest.kt` | Add mock to constructor |
| `bookcase/domain/usecase/UpdateShelfStyleUseCaseImpl.kt` | Add sync trigger (personal shelves only) |
| `bookcase/domain/usecase/UpdateShelfStyleUseCaseTest.kt` | **New** — branch coverage tests |
| `bookcase/domain/usecase/ReorderShelvesUseCaseImpl.kt` | Add sync trigger |
| `bookcase/domain/usecase/ReorderShelvesUseCaseTest.kt` | Add mock to constructor |
| `book/domain/usecase/UpsertBookUseCaseImpl.kt` | Add KDoc warning |
| `docs/specs/patterns/usecase.md` | Add "Sync After Mutation" section |

## Verification

1. `./gradlew assembleDebug` — compiles
2. `./gradlew test` — all tests pass
3. `./gradlew detekt` — no lint violations
4. On-device: sign in → create shelf → check Firestore emulator at localhost:4000 → data appears
5. On-device: kill app → relaunch → data syncs on auto-sign-in (check logcat for `SyncWorker`)
