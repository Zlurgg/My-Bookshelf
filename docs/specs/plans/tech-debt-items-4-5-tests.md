# Plan: Add Tests for BookClubCodeGeneratorImpl and ClubOperationsImpl

## Context

Tech debt items 4 and 5 from `docs/specs/plans/tech-debt-code-review-fixes.md`. Both classes have non-trivial logic with no test coverage.

## New Files

### 1. `app/src/test/.../bookclub/data/service/BookClubCodeGeneratorImplTest.kt`

**Stub approach:** Create a `StubBookClubRemoteDataSource` implementing `BookClubRemoteDataSource` (22 methods) with only `getBookClubMetadata()` implemented — all others throw `NotImplementedError`. Then wrap it in a minimal `RemoteSyncDataSource` adapter that delegates `BookClubRemoteDataSource` and stubs the other 3 sub-interfaces (`BookSyncDataSource`, `ShelfSyncDataSource`, `UserPreferencesDataSource`) the same way. This keeps the stub focused and only the relevant sub-interface needs updating if methods change.

**SecureRandom non-determinism:** `SecureRandom` is a private companion `val` — not injectable. We accept the non-determinism and test code format by running generation in a loop (~100 iterations) to get statistical confidence that length = 12 and all characters are within `BookClubCode.VALID_CHARS`. Note as follow-up: injecting `Random` would improve testability.

**Test scenarios:**
- Successful generation on first attempt (metadata returns null)
- Code format validation: loop ~100 generations, assert length = 12 and chars ⊆ VALID_CHARS
- Retry when code already exists (metadata returns non-null), then succeeds on attempt 2
- Retry succeeds on last attempt (attempt 5) — boundary test
- Failure after MAX_RETRIES (5) exhausted — all return existing, returns `GENERATION_FAILED`
- Network error on first attempt — propagates error immediately, no retry
- Network error on later attempt — retries 1-2 collide, attempt 3 returns network error, propagates

### 2. `app/src/test/.../bookclub/presentation/handlers/ClubOperationsImplTest.kt`

**Stubs:** Inline `object : Interface` stubs for each use case interface in `BookClubOperationUseCases` (none are `fun interface`, so SAM lambdas aren't available). Each is a single-method interface so the stubs are small. Reuses existing `MockBookClubRepository` from `testutil/mocks/`.

**Note:** `restoreBookClubMemberships` is in `BookClubOperationUseCases` but not called by `ClubOperationsImpl` — the stub can return a no-op default.

**Note:** `createBookClub(shelfId, shelfName)` — `shelfName` parameter is unused in the implementation. Flag as separate tech debt, don't test for it.

**Test scenarios — lookupBookClub:**
- Invalid code → `LookupResult.InvalidCode`
- Valid code, club found → `LookupResult.Found` with correct name/code/memberCount
- Valid code, club not found (null) → `LookupResult.NotFound(CLUB_NOT_FOUND)`
- Valid code, preview error → `LookupResult.NotFound` with propagated error

**Test scenarios — lastLookedUpCode state management:**
- Successful lookup sets lastLookedUpCode → no-arg `joinBookClub()` uses it
- No prior lookup → no-arg `joinBookClub()` returns error `CLUB_NOT_FOUND`
- `clearLookupState` → no-arg `joinBookClub()` returns error
- Lookup A succeeds, then lookup B succeeds → no-arg join uses B (last-wins)
- Lookup A succeeds, then lookup B fails → lastLookedUpCode still holds A (stale state — document this behavior)
- `joinBookClub(code)` sets lastLookedUpCode as side effect → subsequent no-arg join uses that code

**Test scenarios — joinBookClub(code):**
- Success → maps domain `JoinResult.Success` to service `JoinResult.Success` (with shelfName)
- Already member → maps domain `JoinResult.AlreadyMember` to service `JoinResult.AlreadyMember`
- Error → propagates

**Test scenarios — createBookClub:**
- Success → wraps code in `BookClubCreationResult`
- Error → propagates

**Test scenarios — other use case delegations:**
- `syncBooksFromClub` maps repository `SyncResult` to service `SyncResult`
- `leaveBookClub` delegates to use case, returns result
- `validateMemberships` returns names on success, empty list on error

**Test scenarios — repository pass-through:**
- Test one representative pass-through (e.g. `deleteBookClub`) to document the delegation pattern. Skip the remaining 8 — they are pure `return bookClubRepository.method(args)` with zero logic. Adding nine near-identical tests is boilerplate, not safety.

## Patterns to Follow

- JUnit assertions (`Assert.assertEquals`, `Assert.assertTrue`)
- `runTest { }` for coroutines
- `@After fun tearDown()` with mock reset
- Given/When/Then comment structure
- Test grouping by category with `// ========== Section ==========` comments

## Verification

Run: `./gradlew test --tests "*BookClubCodeGeneratorImplTest" --tests "*ClubOperationsImplTest"`
