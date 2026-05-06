# Plan: Extract UseCases for 9 Direct Repository Calls in ClubOperationsImpl

## Context

Tech debt item 7 from `docs/specs/plans/tech-debt-code-review-fixes.md`. `ClubOperationsImpl` has 9 methods that call `BookClubRepository` directly, bypassing the UseCase layer. This violates the constitution's layered dependency rule: `Presentation → UseCases → Repositories`.

**Goal:** Route all 9 methods through dedicated UseCases. Remove the `bookClubRepository` dependency from `ClubOperationsImpl` entirely.

## Analysis

### All 9 methods are pure delegation — zero business logic

| Method | Repository Call | Return Type | Callers (via ClubOperations interface) |
|--------|----------------|-------------|----------------------------------------|
| `deleteBookClub(code)` | `repository.deleteBookClub(code)` | `Result<Unit, Sync>` | DeleteAccountUseCase, DeleteShelfUseCase |
| `syncBookToClub(code, book)` | `repository.syncBookToClub(code, book)` | `Result<Unit, Sync>` | AddBookToShelfUseCase |
| `removeBookFromClub(code, bookId)` | `repository.removeBookFromClub(code, bookId)` | `Result<Unit, Sync>` | RemoveBookFromShelfUseCase |
| `updateClubStyle(code, style)` | `repository.updateClubStyle(code, style)` | `Result<Unit, Sync>` | UpdateShelfStyleUseCase |
| `clearAllMemberships()` | `repository.clearAllMemberships()` | **`Result<Unit, Local>`** | ClearUserDataUseCase |
| `renameBookClub(code, name)` | `repository.renameBookClub(code, name)` | `Result<Unit, Sync>` | ShelfManagementHandler |
| `getClubsCreatedByUser(userId)` | `repository.getClubsCreatedByUser(userId)` | `Result<List<String>, Sync>` | DeleteAccountUseCase |
| `getClubMembershipsForUser(userId)` | `repository.getRemoteClubMemberships(userId)` | `Result<List<String>, Sync>` | DeleteAccountUseCase |
| `removeUserFromClub(code, userId)` | `repository.removeUserFromClub(code, userId)` | `Result<Unit, Sync>` | DeleteAccountUseCase |

**Note:** `clearAllMemberships()` is the only method returning `DataError.Local` (not `DataError.Sync`).

**Note:** `getClubMembershipsForUser` calls `repository.getRemoteClubMemberships()` — the UseCase name diverges from the repository method name intentionally (UseCase names the domain concept; repository names the data source).

### Precedent: Pure delegation UseCases exist

`GetBookClubPreviewUseCaseImpl` and `SyncBookClubUseCaseImpl` are both single-line delegation UseCases already in the codebase. This is the pattern to follow.

### No caller changes needed

All callers go through the `ClubOperations` interface (cross-feature bridge in `book/domain/service/`). The refactor is internal to `ClubOperationsImpl` — it swaps `bookClubRepository.method()` for `bookClubUseCases.method()`.

## Concerns Addressed

### Alternative considered: Move ClubOperationsImpl to domain layer?

If ClubOperationsImpl were reclassified as a domain-level orchestrator (e.g. `bookclub/domain/service/`), direct repository access would be architecturally legal. This avoids 18 new files.

**Rejected because:**
- `ClubOperationsImpl` holds `@Volatile lastLookedUpCode` — this is UI-flow state (the lookup→join sequence), not domain state. This anchors it in the presentation layer.
- It already depends on `BookClubOperationUseCases`. A domain service calling UseCases that call the same repository it has direct access to creates a confusing dual-path: some operations go through UseCases, others bypass them. Consistent routing through UseCases is cleaner.
- The constitution says "UseCases encapsulate all business logic" — moving business logic out of UseCases and into a domain service would violate this, and leaving it split would be worse.

### Simpler alternative considered: Do nothing for pure delegation?

The constitution is unambiguous: `Presentation → UseCases → Repositories`. `ClubOperationsImpl` lives in `presentation/handlers/` — it's presentation layer. Even for pure delegation, the UseCase layer provides the seam for future business logic and maintains architectural consistency.

### DRY: 9 nearly-identical UseCases

Each UseCase is 1 interface file + 1 impl file = 18 new files. This feels repetitive, but:
- The UseCase spec mandates "One UseCase per business operation"
- Each method operates on a different domain concept (delete vs rename vs style vs membership)
- Grouping would violate SRP
- The existing codebase already has pure delegation UseCases, so this is consistent

### Aggregator bloat: 8 → 17 properties

`BookClubOperationUseCases` would grow from 8 to 17 properties. The caller table reveals natural groupings (account-deletion cluster, shelf-book operations, lifecycle), and splitting into two aggregators would better reflect usage patterns.

However, both aggregators would be consumed by the same class (`ClubOperationsImpl`), so splitting just moves the property count from one constructor to two — same total complexity, more indirection. Keeping one aggregator is simpler. If the aggregator grows further beyond 17, splitting should be reconsidered.

### Edge case: `renameBookClub` return type widening

`ClubOperations.renameBookClub()` returns `Result<Unit, DataError>` (broad). The repository returns `Result<Unit, DataError.Sync>`. The UseCase uses `Result<Unit, DataError.Sync>` (matching the repository). This works because `Result` is declared `sealed interface Result<out D, out E : Error>` — covariant in both parameters. `Result<Unit, DataError.Sync>` is a subtype of `Result<Unit, DataError>`. Verified in `core/domain/result/Result.kt`.

### Performance

Extra function call indirection per method — negligible for suspend functions doing I/O.

### Security

No change — Firestore security rules enforce authorization. The UseCase layer doesn't add or remove any security checks. If validation is needed later (e.g. "only creator can delete"), the UseCase is the right place for it.

### Unused `shelfName` parameter in `createBookClub`

Noted as separate tech debt (from item 5 review). Not addressed here.

## Implementation

### Ordering: Items 4-5 tests already committed

The existing `ClubOperationsImplTest` has a representative pass-through test that uses `MockBookClubRepository` for `deleteBookClub`. This refactor removes the repository dependency, so that test must be updated to use a stub UseCase instead. This is a small modification within the test file.

### UseCase tests: Not needed for pure delegation

The 9 new UseCases are single-line repository delegations. Testing that Kotlin function delegation works adds maintenance cost with no safety value. The behavior is already covered by `ClubOperationsImplTest` (which tests the full chain). This matches the "test one representative, skip boilerplate" principle applied in items 4-5. The existing pure delegation UseCases (`GetBookClubPreviewUseCaseImpl`, `SyncBookClubUseCaseImpl`) do have tests, but those are trivially simple — not a pattern worth extending to 9 more files.

### Per UseCase (repeat 9 times):

**1. Interface** — `bookclub/domain/usecase/{VerbNoun}UseCase.kt`
```kotlin
interface DeleteBookClubUseCase {
    suspend operator fun invoke(code: String): Result<Unit, DataError.Sync>
}
```

**2. Implementation** — `bookclub/domain/usecase/{VerbNoun}UseCaseImpl.kt`
```kotlin
class DeleteBookClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : DeleteBookClubUseCase {
    override suspend operator fun invoke(code: String): Result<Unit, DataError.Sync> {
        return bookClubRepository.deleteBookClub(code)
    }
}
```

### UseCase names (VerbNoun pattern):

1. `DeleteBookClubUseCase` — `invoke(code)`
2. `SyncBookToClubUseCase` — `invoke(code, book)`
3. `RemoveBookFromClubUseCase` — `invoke(code, bookId)`
4. `UpdateClubStyleUseCase` — `invoke(code, style)`
5. `ClearClubMembershipsUseCase` — `invoke()` — **returns `Result<Unit, DataError.Local>`**
6. `RenameBookClubUseCase` — `invoke(code, name)`
7. `GetClubsCreatedByUserUseCase` — `invoke(userId)`
8. `GetClubMembershipsForUserUseCase` — `invoke(userId)` — **internally calls `repository.getRemoteClubMemberships()`**
9. `RemoveUserFromClubUseCase` — `invoke(code, userId)`

### Files to create (18 new files):

All in `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/domain/usecase/`:
- `DeleteBookClubUseCase.kt` + `DeleteBookClubUseCaseImpl.kt`
- `SyncBookToClubUseCase.kt` + `SyncBookToClubUseCaseImpl.kt`
- `RemoveBookFromClubUseCase.kt` + `RemoveBookFromClubUseCaseImpl.kt`
- `UpdateClubStyleUseCase.kt` + `UpdateClubStyleUseCaseImpl.kt`
- `ClearClubMembershipsUseCase.kt` + `ClearClubMembershipsUseCaseImpl.kt`
- `RenameBookClubUseCase.kt` + `RenameBookClubUseCaseImpl.kt`
- `GetClubsCreatedByUserUseCase.kt` + `GetClubsCreatedByUserUseCaseImpl.kt`
- `GetClubMembershipsForUserUseCase.kt` + `GetClubMembershipsForUserUseCaseImpl.kt`
- `RemoveUserFromClubUseCase.kt` + `RemoveUserFromClubUseCaseImpl.kt`

### Files to modify:

**`BookClubOperationUseCases.kt`** — Add 9 new properties:
```kotlin
val deleteBookClub: DeleteBookClubUseCase,
val syncBookToClub: SyncBookToClubUseCase,
val removeBookFromClub: RemoveBookFromClubUseCase,
val updateClubStyle: UpdateClubStyleUseCase,
val clearClubMemberships: ClearClubMembershipsUseCase,
val renameBookClub: RenameBookClubUseCase,
val getClubsCreatedByUser: GetClubsCreatedByUserUseCase,
val getClubMembershipsForUser: GetClubMembershipsForUserUseCase,
val removeUserFromClub: RemoveUserFromClubUseCase
```

**`BookClubModule.kt`** — Register 9 new UseCases in a labeled block after existing UseCase registrations, and update the aggregator `single {}` block with 9 new `get()` entries:
```kotlin
// Club management UseCases (pure delegation)
singleOf(::DeleteBookClubUseCaseImpl).bind<DeleteBookClubUseCase>()
singleOf(::SyncBookToClubUseCaseImpl).bind<SyncBookToClubUseCase>()
// ... 7 more
```

**`ClubOperationsImpl.kt`** — Replace all `bookClubRepository.*` calls with `bookClubUseCases.*` calls. Remove `bookClubRepository` constructor parameter entirely.

**`ClubOperationsImplTest.kt`** — Update the representative pass-through test to stub via UseCase instead of `MockBookClubRepository`. Remove `MockBookClubRepository` dependency if no longer needed. Add 9 stub use cases to the test setup's `createClubOperations()` factory.

## Verification

1. `./gradlew :app:testDebugUnitTest` — all existing tests pass
2. `./gradlew :app:detekt` — no lint violations
3. Verify `ClubOperationsImpl` no longer imports or depends on `BookClubRepository`
4. Build succeeds: `./gradlew assembleDebug`
