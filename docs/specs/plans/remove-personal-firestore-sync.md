# Plan: Remove Personal Firestore Sync

## Context

MyBookshelf currently syncs personal books/shelves to Firestore for cross-device access. This adds significant complexity (~50 files in `sync/`) with conflict resolution, WorkManager scheduling, guest data migration, and decorator repositories -- all for a feature that isn't core to the product vision. Removing it makes the app truly offline-first (constitution principle), simplifies auth flows, and scopes Firestore to book clubs only. Pre-release, so no migration concerns.

## Key Design Decisions

1. **ownerId on BookshelfEntity**: Keep. Three real purposes post-sync: system tutorial identification (`__system_tutorial__`), club shelf user association (set to userId by book club code), and query filtering (`getShelvesForUser` uses it to show/hide club shelves based on sign-in state). Remove from `BookEntity` and `BookshelfBookCrossRef` (not used post-sync).
2. **sync/ package**: Delete entirely. Move club Firestore code to `bookclub/data/`.
3. **Sign-out**: No longer deletes personal books/shelves. Does delete club shelves and membership tracking (club data is identity-bound). `RestoreBookClubMembershipsUseCase` recreates them on re-sign-in.
4. **Account deletion**: Cleans up Firestore clubs + deletes Firebase Auth. Deletes local club shelves. Personal data stays.
5. **Room migration**: Destructive (`fallbackToDestructiveMigration`). Pre-release.
6. **removeShelf/hardDeleteShelf**: Consolidate into one method. Post-sync, both are hard deletes.
7. **ownerId write path**: Personal shelves always get `ownerId = null` (via `toEntity()` no-arg mapper). Club shelves get `ownerId = userId` (via `toEntity(userId)` in book club code). No sync engine involvement needed.
8. **Sign-out order**: Firebase sign-out first, then local cleanup. If crash occurs during local cleanup, next sign-in restores everything via `RestoreBookClubMembershipsUseCase`.

---

## Phase 1: Decouple Book Club Firestore from sync/

**Goal**: Make book club code independent of sync/ so it can be deleted. Does NOT move files yet (avoids broken intermediate state where sync/ code loses its dependencies).

### 1a. Switch bookclub repos from `RemoteSyncDataSource` to `BookClubRemoteDataSource`

6 files currently depend on `RemoteSyncDataSource` -- change constructor param type:
- `bookclub/data/repository/BookClubManagementRepositoryImpl.kt`
- `bookclub/data/repository/BookClubMembershipRepositoryImpl.kt`
- `bookclub/data/repository/BookClubSyncRepositoryImpl.kt`
- `bookclub/data/repository/BookClubReviewRepositoryImpl.kt`
- `bookclub/data/repository/BookClubRepositoryHelper.kt`
- `bookclub/data/service/BookClubCodeGeneratorImpl.kt`

Also update `bookclub/data/mappers/BookClubMappers.kt` -- imports sync DTOs directly (`BookClubBookDto`, `BookClubMemberDto`, etc.). These imports will break when sync/ is deleted in Phase 2. For now, keep them pointing at sync/ paths; Phase 2 will update them when the DTOs move to `bookclub/data/dto/`.

### 1b. Fix `getRemoteClubMemberships` to use existing method

`BookClubMembershipRepositoryImpl.getRemoteClubMemberships()` (line 138) calls `remoteDataSource.getUserPreferences(userId)` which is on `UserPreferencesDataSource`, not `BookClubRemoteDataSource`. `BookClubRemoteDataSource` already has `getClubMembershipsForUser(userId)` (line 52). Switch the call -- no new method needed:

```kotlin
// Before (uses UserPreferencesDataSource via composite)
val prefsResult = remoteDataSource.getUserPreferences(userId)
val memberships = prefsResult.data?.clubMemberships ?: emptyList()

// After (uses BookClubRemoteDataSource directly)
val membershipsResult = remoteDataSource.getClubMembershipsForUser(userId)
```

### 1c. Remove sync dependencies from ValidateBookClubMembershipsUseCaseImpl

File: `bookclub/domain/usecase/ValidateBookClubMembershipsUseCaseImpl.kt`
- Remove `SyncSchedulerService` dependency and `syncSchedulerService.triggerImmediateSync()` call (no personal sync to trigger post-club-conversion)
- Remove `SyncConstants.TAG_SYNC_TRIGGER` log tag -- use local TAG instead
- Relocate the constant if needed (it was just a log tag, so just use the class TAG)

### 1d. Update BookClubModule DI

Add `BookClubRemoteDataSource` binding (currently only in `SyncModule`):
```kotlin
single { FirebaseFirestore.getInstance() }
single<BookClubRemoteDataSource> { FirestoreBookClubRemoteDataSourceImpl(get()) }
```

Update all repo constructor injections from `get<RemoteSyncDataSource>()` to `get<BookClubRemoteDataSource>()`.

### 1e. Verify

All tests pass. Book club code no longer imports anything from `sync/` (except DTO paths which move in Phase 2). Sync code still works unchanged.

**Commit**: `refactor(bookclub): decouple Firestore code from sync package`

---

## Phase 2: Delete sync/ and Move Club Firestore Files

**Goal**: Delete sync/ entirely. Move preserved files to bookclub/ in the same commit to avoid broken state.

### 2a. Move club Firestore files from sync/ to bookclub/

| From (sync/) | To (bookclub/) |
|---|---|
| `data/repository/BookClubRemoteDataSource.kt` | `data/remote/BookClubRemoteDataSource.kt` |
| `data/service/FirestoreBookClubRemoteDataSourceImpl.kt` | `data/remote/FirestoreBookClubRemoteDataSourceImpl.kt` |
| `data/service/FirestoreOperationHelper.kt` | `data/remote/FirestoreOperationHelper.kt` |
| `data/service/FirestoreCollections.kt` | `data/remote/FirestoreCollections.kt` |
| `data/dto/BookClubMetadataDto.kt` | `data/dto/BookClubMetadataDto.kt` |
| `data/dto/BookClubMemberDto.kt` | `data/dto/BookClubMemberDto.kt` |
| `data/dto/BookClubBookDto.kt` | `data/dto/BookClubBookDto.kt` |
| `data/dto/BookClubReviewDto.kt` | `data/dto/BookClubReviewDto.kt` |
| `data/dto/BookClubCommentDto.kt` | `data/dto/BookClubCommentDto.kt` |

`UserPreferencesFirestoreDto` -- internalize into `FirestoreBookClubRemoteDataSourceImpl` as a private class or package-private file in `bookclub/data/remote/`. It's just deserialization glue for reading `club_memberships`, not a public DTO. Remove the `welcomeShown` field since welcome state is local-only (DataStore) post-sync.

Strip `FirestoreCollections.kt` of personal sync constants (`USERS_COLLECTION/books`, `USERS_COLLECTION/bookshelves`, `SHARED_SHELVES_COLLECTION`). Keep club constants and user settings path.

Update all imports in `bookclub/data/mappers/BookClubMappers.kt` and any other bookclub files that referenced sync DTO paths.

### 2b. Simplify MarkWelcomeShownUseCaseImpl

File: `welcome/domain/usecase/MarkWelcomeShownUseCaseImpl.kt`
- Remove `UserPreferencesRepository` dependency (from `sync.domain.repository`)
- Remove the Firestore write (`userPreferencesRepository.setWelcomeShown(uid, true)`)
- Welcome state becomes local-only via `WelcomePreferences` (DataStore). This is the correct offline-first behavior.
- Constructor: `(welcomePreferences, currentUserProvider)`

### 2c. Delete the entire sync/ package

~40+ source files. Full list in appendix.

### 2d. Remove syncModule from app init

File: `di/AppModule.kt` - Remove `syncModule` from modules list.

### 2e. Wire repository interfaces directly in BookModule

File: `book/di/BookModule.kt` -- currently provides concrete types only (SyncModule provided interface bindings via decorators). Add interface bindings:
```kotlin
single<BookshelfRepository> { BookshelfRepositoryImpl(get(), get()) }
single<BookcaseRepository> { BookcaseRepositoryImpl(get(), get(), get()) }
single<BookRepository> { BookRepositoryImpl(get(), get(), get(), get()) }
```

### 2f. Remove WorkManager dependency

File: `app/build.gradle.kts` - Remove `implementation(libs.work.runtime.ktx)` (line 130) and `androidTestImplementation(libs.work.testing)` (line 143). Nothing else uses WorkManager after SyncWorker/SyncScheduler deletion.
File: `gradle/libs.versions.toml` - Remove `work-runtime-ktx`, `work-testing` entries and `workManager` version.

**Commit**: `refactor(sync): remove personal sync infrastructure, move club Firestore to bookclub`

---

## Phase 3: Simplify Auth Flows

### 3a. Simplify SignOutUseCaseImpl

File: `auth/domain/usecase/SignOutUseCaseImpl.kt`
- Remove: `SyncSchedulerService`, `ClearUserDataUseCase`, `SyncRepository`
- Keep: `CurrentUserProvider` (needed to capture userId before sign-out)
- New flow (crash-safe order -- Firebase first, then local cleanup):
  0. `val userId = currentUserProvider.getCurrentUserId()` -- **capture before sign-out** (Firebase clears auth state on sign-out, so currentUserProvider returns null after step 1)
  1. `authService.signOut()` + `authStateRepository.setSignedInState(false)`
  2. `clubOperations.clearAllMemberships()` -- deletes local `BookClubMembershipEntity` rows
  3. `bookcaseRepository.deleteClubShelves(userId)` -- deletes club shelf entities + their cross-refs
- Personal books/shelves are **not** deleted
- **Crash recovery**: If crash between steps 1 and 2, stale membership entities and club shelves remain. On next sign-in, `restoreClubMembership(code)` checks `bookClubDao.getMembershipByClubCode(code)` (line 161) -- if existing membership found, returns early without creating duplicates. However, if membership was deleted but shelf wasn't (crash between steps 2 and 3), an orphan shelf remains invisible (ownerId filter) but leaks DB space. Acceptable for a crash edge case; no user-visible impact.

### 3b. Simplify ResumeSessionUseCaseImpl

File: `auth/domain/usecase/ResumeSessionUseCaseImpl.kt`
- Remove: `SyncUserPreferencesUseCase`, `SyncSchedulerService`
- Keep: `RestoreBookClubMembershipsUseCase`
- New flow: just call `restoreBookClubMemberships()`

### 3c. Simplify CheckSignInStatusUseCaseImpl

File: `auth/domain/usecase/CheckSignInStatusUseCaseImpl.kt`
- Remove: `bookcaseRepository` dependency and `revertOrphanedDataToGuest()` call
- Without sync, orphaned data (ownerId set but user not signed in) can't occur for personal data. Club shelf orphans are harmless (invisible, cleaned up on next sign-in).

### 3d. Simplify SignInViewModel

File: `auth/presentation/SignInViewModel.kt`
- Remove: `HasGuestDataUseCase`, `MigrateLocalDataUseCase`
- Remove: `importGuestData()`, `skipGuestDataImport()` methods
- After sign-in, always proceed directly to destination (no guest data check)

File: `auth/presentation/SignInState.kt` - Remove `showGuestDataImportDialog`, `guestDataInfo`
File: `auth/presentation/SignInAction.kt` - Remove `ImportGuestData`, `SkipGuestDataImport`
File: `auth/presentation/components/ImportGuestDataDialog.kt` - **Delete**

### 3e. Simplify DeleteAccountUseCaseImpl

File: `account/domain/usecase/DeleteAccountUseCaseImpl.kt`
- Remove: `SyncSchedulerService`, `SyncRepository`
- Remove: `syncRepository.deleteAllRemoteData(userId)` -- no personal data in Firestore to clean. Test data from development is orphaned; clean manually via Firebase console.
- Keep: club cleanup (delete created clubs, remove user from joined clubs), Firebase Auth deletion
- `finalizeLocalCleanup()`:
  - Remove `syncRepository.clearSyncData(userId)`
  - Replace `bookcaseRepository.revertUserDataToGuest(userId)` with `bookcaseRepository.deleteClubShelves(userId)`
  - Keep `authStateRepository.setSignedInState(false)`

### 3f. Delete ClearUserDataUseCase

Files to **delete**:
- `bookcase/domain/usecase/ClearUserDataUseCase.kt`
- `bookcase/domain/usecase/ClearUserDataUseCaseImpl.kt`

File: `auth/di/AuthModule.kt` - Remove binding, update constructor params for `SignOutUseCaseImpl`, `SignInViewModel`.

**Commit**: `refactor(auth): simplify sign-in/out/delete flows for offline-first`

---

## Phase 4: Clean Up Entity Schema and DAOs

### 4a. Simplify entities

**BookEntity** - Remove: `ownerId`, `lastModifiedAt`, `syncStatus`, `cloudId`, `version`
**BookshelfEntity** - Remove: `lastModifiedAt`, `syncStatus`, `cloudId`, `version`, `isShared`, `shareCode`. Keep: `ownerId` (for system + club identity)
**BookshelfBookCrossRef** - Remove: `syncStatus`, `lastModifiedAt`
**BookClubMembershipEntity** - Keep `syncStatus` (used by `BookClubDao` for club-specific sync tracking, independent of personal sync)
**SyncMetadataEntity** - **Delete entirely**

### 4b. Clean up DAOs

**BookDao** - Remove: `getPendingSyncBooks`, `updateBookSyncStatus`, `countOrphanBooks`, `assignOwnerToOrphanBooks`, `getBooksByOwner`, `markAllBooksPending`, `deleteAllBooksForOwner`, `revertBooksToGuest`. Simplify `upsertBookWithSyncInit` to plain `upsert` (no timestamp/version).

**ShelfDao** - Remove: `getPendingSyncShelves`, `updateShelfSyncStatus`, `countOrphanShelves`, `assignOwnerToOrphanShelves`, `getShelvesByOwner`, `markAllShelvesPending`, `deleteAllShelvesForOwner`, `revertShelvesToGuest`, `findOrphanedOwnerId`, share-related queries.
- Simplify `getShelvesForUser()`: remove `syncStatus != 'DELETED'` filter. Keep ownerId filter for system + club separation.
- Keep `deleteClubShelvesForOwner(userId)` -- needed by sign-out and account deletion.

**CrossRefDao** - Remove: `getPendingSyncCrossRefs`, `updateCrossRefSyncStatus`, `markAllCrossRefsForShelfAs`, `deleteAllCrossRefsForOwner`, `deleteCrossRefsForClubShelves`, `resetCrossRefSyncStatusForOwner`.
- Remove `syncStatus != 'DELETED'` from `getBooksForShelf`, `getBookCountForShelf`, `isBookInAnyShelf`, `getShelvesForBook`.

**BookClubDao** - Update queries that reference removed fields:
- `observeBookClubShelves()` (line 40): remove `AND syncStatus != 'DELETED'` (syncStatus removed from BookshelfEntity)
- `getBookIdsForShelf()` (line 54): remove `AND syncStatus != 'DELETED'` (syncStatus removed from BookshelfBookCrossRef)
- Keep `observeAllMemberships()` (line 24) `syncStatus != 'DELETED'` filter -- BookClubMembershipEntity retains its own syncStatus
- Keep `updateMembershipSyncStatus()` (line 34) -- same
- Keep `getPendingSyncMemberships()` (line 59) -- same

**SyncDao** - **Delete entirely**

### 4c. Consolidate removeShelf/hardDeleteShelf

**BookcaseRepository interface**: Remove `hardDeleteShelf()`. `removeShelf()` becomes the single hard-delete method.
**BookcaseRepositoryImpl**: `removeShelf()` implementation changes from soft-delete (set syncStatus=DELETED) to hard-delete (delete cross-refs + delete shelf). Remove `hardDeleteShelf()`.
Update all callers of `hardDeleteShelf()` to use `removeShelf()`.

Note: Room does NOT have FK CASCADE on `BookshelfBookCrossRef` (no ForeignKey annotations found). `deleteClubShelvesForOwner` in ShelfDao only deletes the BookshelfEntity rows. Cross-refs must be deleted separately. Ensure `removeShelf()` and `deleteClubShelves()` both delete cross-refs before deleting the shelf entity, via `CrossRefDao.deleteAllCrossRefsForShelf(shelfId)`.

### 4d. Clean up BookcaseRepository interface

Remove: `clearUserData()`, `revertOrphanedDataToGuest()`, `revertUserDataToGuest()`, `hardDeleteShelf()`
Add: `deleteClubShelves(userId: String): Result<Unit, DataError.Local>` -- deletes cross-refs then club shelves for the given user.

### 4e. Update repository implementations

**BookcaseRepositoryImpl**: Remove `.copy(syncStatus = "SYNCED")` calls (line ~80) used for system entity creation -- `syncStatus` no longer exists on the entity.
**BookRepositoryImpl**: Remove `.copy(syncStatus = "SYNCED")` calls (line ~55) -- same.

### 4f. Update mappers

- `BookshelfMappers.kt`: Remove sync fields from `toEntity()` construction. Keep `toEntity(ownerId)` overload (used by system shelf + club shelf creation).
- `BookMappers.kt`: Remove sync fields from entity mapping.

### 4g. Destructive Room migration

File: `core/data/database/MyBookshelfRoomDatabase.kt`
- Increment DB version
- Remove `SyncMetadataEntity` from entities array
- Remove `SyncDao` abstract property
- Add `fallbackToDestructiveMigration()` in builder

**Commit**: `refactor(database): remove sync fields from entities, consolidate shelf deletion`

---

## Phase 5: Firestore Rules and Docs

### 5a. Strip personal data rules from `firestore.rules`

Remove rules for:
- `/users/{userId}/books/{bookId}`
- `/users/{userId}/bookshelves/{shelfId}`
- `/sharedShelves/{shareCode}`

Keep:
- `/users/{userId}` and `/users/{userId}/settings/{document=**}` (club memberships)
- `/bookClubs/{clubCode}` and all subcollections

### 5b. Update constitution.md

- Remove "cloud sync is OPT-IN only" (sync no longer exists)
- Update "Optional auth" to: "Google Sign-In for book clubs, not required for core bookshelf functionality"

**Commit**: `chore: update Firestore rules and constitution for sync removal`

---

## Phase 6: Tests

### 6a. Delete sync test files (~10 files)

All files under `app/src/test/.../sync/` plus:
- `testutil/mocks/MockSyncRepository.kt`
- `testutil/mocks/MockSyncSchedulerService.kt`

### 6b. Update affected test files

- `SignOutUseCaseTest.kt` - Remove sync mocks, add `ClubOperations` mock, test club cleanup + sign-out
- `ResumeSessionUseCaseTest.kt` - Remove sync mocks, test only restores memberships
- `CheckSignInStatusUseCaseTest.kt` - Remove orphan revert expectations
- `SignInViewModelTest.kt` - Remove guest data use case mocks and import dialog tests
- `DeleteAccountUseCaseTest.kt` - Remove sync mocks, test club cleanup + auth deletion
- `BookClubCodeGeneratorImplTest.kt` - **Significant rewrite**: currently constructs a mock `RemoteSyncDataSource` from 4 sub-interfaces (imports 13 sync types including `BookFirestoreDto`, `SharedShelfDto`, etc.). Replace with a direct mock of `BookClubRemoteDataSource` only.
- `ValidateBookClubMembershipsUseCaseImplTest.kt` - Remove `MockSyncSchedulerService`, update constructor
- `BookcaseViewModelTest.kt` - Remove `SyncSchedulerService` mock, `MockSyncRepository`, `ClearUserDataUseCase` mock
- `BookDetailViewModelTest.kt` - Remove `SyncSchedulerService` mock, `MockSyncRepository`
- Any test referencing `ClearUserDataUseCase` - remove mock implementations
- Tests referencing `hardDeleteShelf` - update to `removeShelf`

### 6c. New tests

- Sign-out preserves personal books/shelves, deletes club shelves
- Sign-out then re-sign-in (same user): club memberships restored from Firestore
- Account deletion cleans up clubs, deletes club shelves, preserves personal data
- `removeShelf()` hard-deletes (verify cross-refs and shelf entity both gone)
- `deleteClubShelves()` cascades to cross-refs

**Commit**: `test: update tests for sync removal`

---

## Critical Analysis

### Edge Cases

1. **Orphaned WorkManager entries**: Testers with old builds have stale SyncWorker registrations. Harmless -- WorkManager discards when class is missing.
2. **Orphaned Firestore data**: Personal books/shelves pushed during development remain at `/users/{userId}/books/` and `/bookshelves/`. `deleteAllRemoteData` is removed so account deletion won't clean these. Manually purge via Firebase console.
3. **No FK CASCADE on cross-refs**: Room entities have no ForeignKey annotations. `deleteClubShelves()` and `removeShelf()` must explicitly delete cross-refs before shelf entities to avoid orphaned rows. Verified in Phase 4c.
4. **Sign-out crash window**: Firebase sign-out happens first. If crash during local cleanup, `restoreClubMembership()` has an idempotency check (line 161-164: returns early if `BookClubMembershipEntity` exists for that club code). Gap: if membership entity was deleted but club shelf wasn't (crash between steps 2 and 3), an orphan shelf entity remains. It's invisible via ownerId filter and doesn't affect the user. A future "vacuum orphans" utility could clean these, but not worth building pre-release.

### Assumptions

1. **Single user per device for personal data**: Sound given constitution. Club data remains identity-aware (ownerId on club shelves, Firestore membership tracking).
2. **No background club sync needed**: Club operations are user-initiated. If periodic sync is needed later, build fresh in bookclub/.
3. **ownerId write path is correct post-sync**: Verified -- personal shelves always get `null` (no-arg `toEntity()`), club shelves get `userId` (book club code calls `toEntity(user.userId)`), system shelves get `__system_tutorial__` (via `addSystemShelf`). The `getShelvesForUser(userId)` query handles all three cases correctly.
4. **BookClubMembershipEntity.syncStatus is independent**: This entity's syncStatus is used by BookClubDao for club-specific sync tracking (pending membership syncs, soft-deleted memberships). It has no relation to the personal sync infrastructure being removed.

### Simpler Alternatives Considered

1. **Keep fields, just stop syncing**: Less churn but dead columns confuse future developers. Rejected: pre-release = zero migration cost.
2. **Keep sync/ package, gut it**: "sync" package containing only club Firestore code violates SRP. Rejected.
3. **Replace ownerId with isSystem boolean**: Would require keeping ownerId anyway for club shelf identity. Two columns where one suffices. Rejected.

### Performance

- `removeShelf()` changes from O(1) soft-delete to O(n) hard-delete. Negligible for typical shelf sizes.
- Removing syncStatus DAO filters: no impact (deleted items now hard-deleted).
- Removing SyncWorker: improved battery life.

### Security

- **Critical**: Remove Firestore rules for `/users/{userId}/books/` and `/bookshelves/`. Dead rules = dead attack surface.
- **Local data on sign-out**: Data stays on device. Shared-device isolation is not a goal for a personal bookshelf app.

### Clean Principles

- **SRP**: sync/ mixing personal sync + club Firestore resolved. Each package owns its data layer. `removeShelf`/`hardDeleteShelf` duplication eliminated.
- **DRY**: Syncing*Repository decorators (3 wrapper classes) eliminated. Three cleanup methods (`clearUserData`, `revertUserDataToGuest`, `revertOrphanedDataToGuest`) replaced by one (`deleteClubShelves`).
- **Dependency Rule**: Domain layer has zero sync dependencies after Phase 2.

---

## Verification

1. `./gradlew test` -- all unit tests pass
2. `./gradlew connectedAndroidTest` -- integration tests pass
3. Manual: fresh install, create shelves, add books, sign in/out (personal data persists, club shelves cleared and restored), create/join book clubs, account deletion
4. Firebase emulator: club operations work, personal data paths return permission denied

---

## Appendix: Files to Delete (sync/ package)

### Source (~40 files)
- `sync/di/SyncModule.kt`
- `sync/data/engine/SyncEngine.kt`
- `sync/data/repository/` -- BookSyncDataSource, ShelfSyncDataSource, UserPreferencesDataSource, RemoteSyncDataSource, SyncRepositoryImpl, UserPreferencesRepositoryImpl
- `sync/data/service/` -- FirestoreBookSyncDataSourceImpl, FirestoreShelfSyncDataSourceImpl, FirestoreUserPreferencesDataSourceImpl, FirestoreRemoteDataSource, AndroidConnectivityMonitor, DefaultConflictResolver
- `sync/data/mappers/SyncDomainMapper.kt`
- `sync/data/worker/` -- SyncWorker, SyncScheduler
- `sync/data/dto/` -- BookFirestoreDto, BookshelfFirestoreDto, SharedShelfDto, UserPreferencesFirestoreDto (internalized into Firestore impl)
- `sync/domain/model/` -- all (SyncResult, SyncState, SyncProgress, SyncPhase, MigrationResult, GuestDataInfo, ConflictResolution, SyncStatus)
- `sync/domain/repository/` -- SyncRepository, SyncingBookRepository, SyncingBookcaseRepository, SyncingBookshelfRepository, UserPreferencesRepository
- `sync/domain/service/` -- ConflictResolver, ConnectivityMonitor, SyncSchedulerService
- `sync/domain/usecase/` -- all 6 files (HasGuestData, MigrateLocalData, SyncUserPreferences)
- `sync/domain/SyncConstants.kt`

### Tests (~10 files)
- All under `test/.../sync/`
- `testutil/mocks/MockSyncRepository.kt`, `MockSyncSchedulerService.kt`

### Also delete
- `auth/presentation/components/ImportGuestDataDialog.kt`
- `bookcase/domain/usecase/ClearUserDataUseCase.kt`
- `bookcase/domain/usecase/ClearUserDataUseCaseImpl.kt`

### Gradle cleanup
- `app/build.gradle.kts` -- remove `implementation(libs.work.runtime.ktx)` and `androidTestImplementation(libs.work.testing)`
- `gradle/libs.versions.toml` -- remove `work-runtime-ktx`, `work-testing`, and `workManager` version
