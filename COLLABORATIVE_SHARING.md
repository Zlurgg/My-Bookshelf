# Book Club Feature

## Overview

Add collaborative "Book Club" shelves where multiple users can build and manage a shared collection together. This is a **new feature** that complements the existing deep link sharing (which gives recipients a copy).

## Feature Comparison

| Feature | Deep Link Share (Existing) | Book Club (New) |
|---------|---------------------------|-----------------|
| Purpose | Give someone a copy of your shelf | Collaborate on a collection together |
| Data Storage | Embedded in URL | Firestore real-time sync |
| Account Required | No (import only) | Yes (Google Sign-In) |
| Permissions | Recipient owns their copy | All members equal |
| Updates | One-time transfer | Real-time sync |
| Reviews | Private to each user | Shared among members |

## User Flow

### Share Menu (Updated)
When user taps "Share" on a shelf:
```
┌─────────────────────────────┐
│ Share "{Shelf Name}"        │
├─────────────────────────────┤
│ 📤 Share Copy               │  ← Existing: URL-encoded deep link
│ 👥 Create Book Club         │  ← New: Creates collaborative space
└─────────────────────────────┘
```

### Create Book Club Flow
1. User selects "Create Book Club" from share menu
2. App creates Book Club in Firestore with shelf's books
3. User becomes first member
4. App shows share code / invite link
5. User shares invite with friends

### Join Book Club Flow
1. User receives invite link: `https://zlurgg.github.io/My-Bookshelf/club/{code}`
2. Web page shows club preview + "Open in App" button
3. App opens with join confirmation
4. User joins → local "{Name} (Book Club)" shelf created
5. Books sync from Firestore

**Alternative: Manual Entry**
1. User selects "Join Book Club" from bookcase menu
2. Dialog accepts code OR pasted URL
3. App extracts code, validates, fetches preview
4. User confirms → joins club

### Bookcase Menu (New Option)
```
┌───────────────────────────────────┐
│ ⋮ Menu                            │
├───────────────────────────────────┤
│ 👥 Join Book Club                 │
│ ...existing options...            │
└───────────────────────────────────┘
```

### Join Dialog
```
┌───────────────────────────────────┐
│ Join Book Club                    │
├───────────────────────────────────┤
│ Enter code or paste invite link   │
│ ┌───────────────────────────────┐ │
│ │ ABC12XYZ                      │ │
│ └───────────────────────────────┘ │
│                                   │
│ [Cancel]                [Join]    │
└───────────────────────────────────┘
```

Input parsing:
- `ABC12XYZ` → use directly as code
- `https://.../club/ABC12XYZ` → extract `ABC12XYZ`

---

## Key Decisions

| Aspect | Decision |
|--------|----------|
| Data Location | `/bookClubs/{code}/` in Firestore |
| Local Copy | Each member gets "{Name} (Book Club)" shelf locally |
| Ownership | No special owner - all members equal |
| Leaving | User choice: keep local copy as regular shelf or delete |
| Last Member Leaves | Auto-delete club from Firestore (cleanup) |
| Reviews | Shared - visible to all members |
| Permissions | Anyone can add/remove books, only own reviews editable |
| Conflicts | Last-write-wins for metadata, additive merge for books |
| Name conflicts | Auto-rename: "{Name} (Book Club)", "{Name} (Book Club) 2", etc. |
| Limits | Soft limits: 50 members, 500 books per club |
| Club Code | 8-char alphanumeric (generated), supports manual entry |
| Invite URL | `https://zlurgg.github.io/My-Bookshelf/club/{code}` |
| Deep Link | `mybookshelf://club/{code}` |

---

## Firestore Structure

```
/bookClubs/{code}/
  metadata: { name, style, createdAt, createdBy, lastModifiedAt }
  /members/{userId}: { joinedAt, displayName }
  /books/{bookId}: { title, authors, coverUrl, isbn, addedBy, addedAt, ... }
  /reviews/{bookId}_{userId}: { rating, reviewText, createdAt, updatedAt }
```

**Security Rules:**
- `metadata`: Public read (for web preview), authenticated write
- `members`: Members can read all, write own entry
- `books`: Members can read/write
- `reviews`: Members can read all, write own reviews

---

## Implementation Phases

The phases are structured as **vertical slices** - each phase delivers working functionality you can stop at and ship.

| Phase | Delivers | Can Stop? |
|-------|----------|-----------|
| 1 | Create book club + view locally | ✅ Creator can make clubs |
| 2 | Join via code/link | ✅ Others can join |
| 3 | Add/remove books syncs | ✅ Fully collaborative |
| 4 | Reviews feature | ✅ Social features |
| 5 | Leave club + cleanup | ✅ Complete feature |

---

### Phase 1: Create Book Club (MVP)

**What users can do after this phase:**
- Create a book club from any shelf via share menu
- See invite link/code to share with friends
- View their shelf marked as "(Book Club)"
- Books from original shelf are uploaded to Firestore

**Data Layer:**
```kotlin
// BookClubMembershipEntity.kt
@Entity(tableName = "book_club_memberships")
data class BookClubMembershipEntity(
    @PrimaryKey val id: String,
    val clubCode: String,
    val localShelfId: String,
    val joinedAt: Long,
    val lastSyncedAt: Long,
    val syncStatus: SyncStatus
)

// Modify BookshelfEntity - add fields:
// isBookClub: Boolean = false
// clubCode: String? = null
```

**DTOs:**
- `BookClubMetadataDto` - Firestore metadata document
- `BookClubMemberDto` - member subdocument
- `BookClubBookDto` - book in club

**Remote Data Source (subset):**
```kotlin
suspend fun createBookClub(code: String, metadata: BookClubMetadataDto): Result<Unit, DataError.Sync>
suspend fun getBookClubMetadata(code: String): Result<BookClubMetadataDto?, DataError.Sync>
suspend fun addMember(code: String, member: BookClubMemberDto): Result<Unit, DataError.Sync>
suspend fun addBookToClub(code: String, book: BookClubBookDto): Result<Unit, DataError.Sync>
suspend fun getClubBooks(code: String): Result<List<BookClubBookDto>, DataError.Sync>
```

**Services:**
- `BookClubCodeGenerator` - generates unique 8-char codes with collision checking

**Repository (subset):**
```kotlin
interface BookClubRepository {
    suspend fun createBookClub(shelfId: String): Result<String, DataError.Sync>
    fun observeMyBookClubs(): Flow<List<BookClubMembership>>
}
```

**UseCases:**
- `CreateBookClubUseCase` - creates club from shelf, uploads books, returns code
- `GenerateInviteLinkUseCase` - creates shareable URL from code

**Domain Models:**
```kotlin
data class BookClub(
    val code: String,
    val name: String,
    val style: ShelfStyle,
    val createdAt: Long,
    val createdBy: String,
    val bookCount: Int,
    val memberCount: Int
)

data class BookClubMembership(
    val clubCode: String,
    val localShelfId: String,
    val joinedAt: Long,
    val lastSyncedAt: Long
)
```

**Presentation:**
- `CreateBookClubDialog` - confirmation when creating
- `InviteLinkSheet` - shows code + share button
- Modify share menu - add "Create Book Club" option
- Modify shelf list item - show "(Book Club)" badge

**Files to create:**
- `bookshelf/data/database/entity/BookClubMembershipEntity.kt`
- `bookshelf/data/database/dao/BookClubDao.kt`
- `bookshelf/data/service/BookClubCodeGenerator.kt`
- `sync/data/dto/BookClubMetadataDto.kt`
- `sync/data/dto/BookClubMemberDto.kt`
- `sync/data/dto/BookClubBookDto.kt`
- `bookshelf/domain/repository/BookClubRepository.kt`
- `bookshelf/data/repository/BookClubRepositoryImpl.kt`
- `bookshelf/domain/model/BookClub.kt`
- `bookshelf/domain/model/BookClubMembership.kt`
- `bookshelf/domain/usecase/bookclub/CreateBookClubUseCase.kt`
- `bookshelf/domain/usecase/bookclub/GenerateInviteLinkUseCase.kt`
- `bookshelf/presentation/bookclub/components/CreateBookClubDialog.kt`
- `bookshelf/presentation/bookclub/components/InviteLinkSheet.kt`

**Files to modify:**
- `bookshelf/data/database/entity/BookshelfEntity.kt` (add fields)
- `bookshelf/data/database/MyBookshelfDatabase.kt` (migration 8→9)
- `sync/data/repository/RemoteSyncDataSource.kt` (add methods)
- `sync/data/service/FirestoreRemoteDataSource.kt` (implement methods)
- `di/AppModule.kt` (register new components)
- Share menu component (add option)
- Shelf list item component (add badge)

**Tests:** ~15 unit + ~5 integration

---

### Phase 2: Join Book Club

**What users can do after this phase:**
- Join a book club via invite link
- Join by manually entering/pasting code
- See club's books in their local shelf
- Web page shows club preview for users without app

**Remote Data Source (additions):**
```kotlin
suspend fun isMember(code: String, userId: String): Result<Boolean, DataError.Sync>
suspend fun getMembers(code: String): Result<List<BookClubMemberDto>, DataError.Sync>
```

**Repository (additions):**
```kotlin
suspend fun joinBookClub(code: String): Result<BookClub, DataError.Sync>
suspend fun getBookClub(code: String): Result<BookClub?, DataError.Sync>
```

**UseCases:**
- `JoinBookClubUseCase` - validates code, adds member, creates local shelf
- `ParseClubCodeUseCase` - extracts code from URL or validates raw code
- `GetBookClubUseCase` - gets club details for preview

**Presentation:**
- `JoinBookClubViewModel` - handles join flow state
- `JoinBookClubDialog` - enter code / paste link
- Modify bookcase menu - add "Join Book Club" option

**Navigation:**
- Add deep link: `mybookshelf://club/{code}`
- Add web deep link handling

**Web Page:**
- `docs/club/index.html` - shows club preview + "Open in App" button

**Files to create:**
- `bookshelf/domain/usecase/bookclub/JoinBookClubUseCase.kt`
- `bookshelf/domain/usecase/bookclub/ParseClubCodeUseCase.kt`
- `bookshelf/domain/usecase/bookclub/GetBookClubUseCase.kt`
- `bookshelf/presentation/bookclub/JoinBookClubViewModel.kt`
- `bookshelf/presentation/bookclub/JoinBookClubState.kt`
- `bookshelf/presentation/bookclub/components/JoinBookClubDialog.kt`
- `docs/club/index.html`

**Files to modify:**
- `BookClubRepository.kt` (add methods)
- `BookClubRepositoryImpl.kt` (implement)
- `RemoteSyncDataSource.kt` (add methods)
- `FirestoreRemoteDataSource.kt` (implement)
- `app/navigation/Routes.kt`
- `app/navigation/NavGraph.kt`
- `AndroidManifest.xml` (deep link intent filter)
- `di/AppModule.kt`
- Bookcase menu component

**Tests:** ~18 unit + ~4 integration

---

### Phase 3: Collaborative Books (Sync)

**What users can do after this phase:**
- Add books to club → syncs to all members
- Remove books from club → syncs to all members
- Pull to refresh for manual sync
- See real-time updates from other members

**Remote Data Source (additions):**
```kotlin
suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync>
suspend fun getClubBooksSince(code: String, sinceTimestamp: Long): Result<List<BookClubBookDto>, DataError.Sync>
```

**Repository (additions):**
```kotlin
suspend fun addBookToClub(code: String, book: Book): Result<Unit, DataError.Sync>
suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync>
fun observeClubBooks(code: String): Flow<List<Book>>
suspend fun syncBookClub(code: String): Result<Unit, DataError.Sync>
```

**Sync Engine:**
- `BookClubSyncEngine` - handles push/pull of book changes

**UseCases:**
- `AddBookToClubUseCase` - adds book locally + pushes to Firestore
- `RemoveBookFromClubUseCase` - removes book locally + pushes to Firestore
- `SyncBookClubUseCase` - manual sync trigger

**Presentation:**
- Modify `BookshelfViewModel` to detect book club and use sync
- Add pull-to-refresh for club shelves
- Show sync status indicator

**Files to create:**
- `sync/data/engine/BookClubSyncEngine.kt`
- `bookshelf/domain/usecase/bookclub/AddBookToClubUseCase.kt`
- `bookshelf/domain/usecase/bookclub/RemoveBookFromClubUseCase.kt`
- `bookshelf/domain/usecase/bookclub/SyncBookClubUseCase.kt`

**Files to modify:**
- `BookClubRepository.kt` (add methods)
- `BookClubRepositoryImpl.kt` (implement)
- `RemoteSyncDataSource.kt` (add methods)
- `FirestoreRemoteDataSource.kt` (implement)
- `BookshelfViewModel.kt` (club-aware actions)
- `BookshelfScreen.kt` (sync UI)
- `di/AppModule.kt`

**Tests:** ~20 unit + ~5 integration

---

### Phase 4: Reviews

**What users can do after this phase:**
- Add rating/review to any book in club
- See other members' reviews
- Edit/delete own reviews

**Data Layer:**
```kotlin
// BookReviewEntity.kt
@Entity(tableName = "book_reviews")
data class BookReviewEntity(
    @PrimaryKey val id: String,
    val clubCode: String,
    val bookId: String,
    val userId: String,
    val displayName: String,
    val rating: Float?,
    val reviewText: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)
```

**DTOs:**
- `BookReviewDto` - Firestore review document

**Remote Data Source (additions):**
```kotlin
suspend fun upsertReview(code: String, review: BookReviewDto): Result<Unit, DataError.Sync>
suspend fun deleteReview(code: String, reviewId: String): Result<Unit, DataError.Sync>
suspend fun getReviewsForBook(code: String, bookId: String): Result<List<BookReviewDto>, DataError.Sync>
```

**Domain Model:**
```kotlin
data class BookReview(
    val bookId: String,
    val userId: String,
    val displayName: String,
    val rating: Float?,
    val reviewText: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

**UseCases:**
- `AddOrUpdateReviewUseCase`
- `DeleteReviewUseCase`
- `GetBookReviewsUseCase`

**Presentation:**
- `BookReviewsSheet` - shows all reviews for a book
- `AddReviewDialog` - add/edit review
- Modify book detail to show reviews for club books

**Files to create:**
- `bookshelf/data/database/entity/BookReviewEntity.kt`
- `sync/data/dto/BookReviewDto.kt`
- `bookshelf/domain/model/BookReview.kt`
- `bookshelf/domain/usecase/review/AddOrUpdateReviewUseCase.kt`
- `bookshelf/domain/usecase/review/DeleteReviewUseCase.kt`
- `bookshelf/domain/usecase/review/GetBookReviewsUseCase.kt`
- `bookshelf/presentation/bookclub/components/BookReviewsSheet.kt`
- `bookshelf/presentation/bookclub/components/AddReviewDialog.kt`

**Files to modify:**
- `BookClubDao.kt` (review queries)
- `MyBookshelfDatabase.kt` (add entity)
- `RemoteSyncDataSource.kt` (add methods)
- `FirestoreRemoteDataSource.kt` (implement)
- `BookDetailScreen.kt` (show reviews for club books)
- `di/AppModule.kt`

**Tests:** ~12 unit + ~3 integration

---

### Phase 5: Leave & Members Management

**What users can do after this phase:**
- View list of club members
- Leave a book club (keep or delete local copy)
- Last member leaving auto-deletes club from Firestore

**Remote Data Source (additions):**
```kotlin
suspend fun removeMember(code: String, userId: String): Result<Unit, DataError.Sync>
suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync>
```

**Repository (additions):**
```kotlin
suspend fun leaveBookClub(code: String, keepLocalCopy: Boolean): Result<Unit, DataError.Sync>
fun observeMembers(code: String): Flow<List<BookClubMember>>
```

**Domain Model:**
```kotlin
data class BookClubMember(
    val userId: String,
    val displayName: String,
    val joinedAt: Long
)
```

**UseCases:**
- `LeaveBookClubUseCase` - leaves club, auto-deletes if last member
- `GetClubMembersUseCase` - observes member list

**Presentation:**
- `MembersSheet` - bottom sheet showing club members
- `LeaveBookClubDialog` - confirm with keep/delete option
- Add "Members" and "Leave" options to club shelf menu

**Files to create:**
- `bookshelf/domain/model/BookClubMember.kt`
- `bookshelf/domain/usecase/bookclub/LeaveBookClubUseCase.kt`
- `bookshelf/domain/usecase/bookclub/GetClubMembersUseCase.kt`
- `bookshelf/presentation/bookclub/components/MembersSheet.kt`
- `bookshelf/presentation/bookclub/components/LeaveBookClubDialog.kt`

**Files to modify:**
- `BookClubRepository.kt` (add methods)
- `BookClubRepositoryImpl.kt` (implement)
- `RemoteSyncDataSource.kt` (add methods)
- `FirestoreRemoteDataSource.kt` (implement)
- `BookshelfScreen.kt` (club menu options)
- `di/AppModule.kt`

**Tests:** ~10 unit + ~3 integration

---

## Cleanup (Optional)

**Can be removed (old unused code):**
- `sync/data/dto/SharedShelfDto.kt` - old owner/subscriber model, never fully implemented

**Keep unchanged:**
- All deep link sharing code (`ExportBookshelfUseCase`, `ImportBookshelfUseCase`, etc.)
- `docs/share/index.html`
- `UrlEncodedShareTokenService`, `Base64Encoder`

---

## Testing Strategy

**Unit Tests:**

*UseCases (13 total):*
- All book club UseCases with mock `BookClubRepository`
- All review UseCases with mock repositories
- `ParseClubCodeUseCase`:
  - Valid 8-char code → returns code
  - Full URL → extracts code
  - URL with trailing slash → extracts code
  - Invalid format → returns `DataError.Validation.INVALID_CLUB_CODE`
  - Empty input → returns `DataError.Validation.INVALID_CLUB_CODE`
- `LeaveBookClubUseCase`:
  - Leave with other members remaining → removes user only
  - Leave as last member → deletes entire club from Firestore
  - Keep local copy option → shelf becomes regular (non-club) shelf
  - Delete local copy option → removes local shelf

*Services:*
- `BookClubCodeGenerator`:
  - Generates 8-char codes
  - Uses only allowed character set (no 0/O/1/I/L)
  - Retries on collision (mock Firestore returns exists=true, then false)
  - Fails after max retries with `DataError.Sync.GENERATION_FAILED`

*Repository & Sync:*
- `BookClubRepositoryImpl` with fake `RemoteSyncDataSource`
- `BookClubSyncEngine` sync logic
- DTO/Entity mappers
- Domain model validation

**Integration Tests:**
- `BookClubDao` with real Room database
- `BookClubMembershipEntity` CRUD
- `BookReviewEntity` CRUD
- Sync status transitions
- `BookshelfEntity` with `isBookClub` and `clubCode` fields

**Test Utilities:**
- `FakeBookClubRepository` - implements interface with in-memory data
- `FakeRemoteSyncDataSource` - for testing code collision scenarios
- `TestBookClubFactory` - creates test BookClub/Member/Review objects
- `TestBookClubCodeGenerator` - deterministic code generation for tests

**Estimated by Phase:**
| Phase | Unit | Integration | Total |
|-------|------|-------------|-------|
| 1: Create | ~15 | ~5 | ~20 |
| 2: Join | ~18 | ~4 | ~22 |
| 3: Sync | ~20 | ~5 | ~25 |
| 4: Reviews | ~12 | ~3 | ~15 |
| 5: Leave | ~10 | ~3 | ~13 |
| **Total** | **~75** | **~20** | **~95** |

---

## Logging Strategy

**Tags:**
- `BookClubSync` - sync operations
- `BookClubRepo` - repository operations
- `BookClubCode` - code generation and parsing
- `BookReviews` - review operations

**Log levels:**
- `Timber.d()` - sync start/complete, code generation success, join/leave, URL parsing
- `Timber.w()` - conflict resolution, retry attempts, limit warnings, code collision retries
- `Timber.e()` - sync failures, Firestore errors, validation failures, code generation exhausted

**Key logging points:**
- `BookClubCodeGenerator`: log generation attempts, collisions, final code
- `ParseClubCodeUseCase`: log input type (code vs URL), extraction result
- `LeaveBookClubUseCase`: log member count check, club deletion if last member

---

## Error Types

**New `DataError.Sync` cases to add:**
```kotlin
GENERATION_FAILED    // Code generation exhausted retries
CLUB_NOT_FOUND       // Invalid club code
ALREADY_MEMBER       // User already in this club
NOT_MEMBER           // User not in club (for operations requiring membership)
```

**New `DataError.Validation` cases to add:**
```kotlin
INVALID_CLUB_CODE    // Code format invalid (wrong length, invalid chars)
```

**ErrorFormatter mappings to add:**
```kotlin
DataError.Sync.GENERATION_FAILED -> "Unable to generate club code. Please try again."
DataError.Sync.CLUB_NOT_FOUND -> "Book club not found. Check the code and try again."
DataError.Sync.ALREADY_MEMBER -> "You're already a member of this book club."
DataError.Sync.NOT_MEMBER -> "You're not a member of this book club."
DataError.Validation.INVALID_CLUB_CODE -> "Invalid club code format."
```

---

## Coding Standards

Follow existing patterns:

1. **Result Pattern**: `Result<T, DataError.Sync>` for all fallible operations
2. **Named variables**: `createResult`, `joinResult`, etc. in `when` expressions
3. **ErrorFormatter**: Use `ErrorFormatter.formatDataErrorMessage()`
4. **Pure Domain**: Domain layer has zero Android dependencies
5. **UseCase Pattern**: ViewModels → UseCases → Repositories
6. **DTO/Entity Separation**: DTOs for Firestore, Entities for Room
7. **Extension Functions**: `toEntity()`, `toDto()`, `toDomain()`
8. **StateFlow**: ViewModels expose `StateFlow<State>`
9. **Sealed Interfaces**: For Actions/Events
10. **Koin**: `viewModel { (code: String) -> BookClubViewModel(code, get(), ...) }`

---

## Open Questions

- [ ] Should book clubs have an "owner" who can delete the entire club? (Currently: no owner, all equal)
- [ ] Notification strategy for new books/members? (Future enhancement)

## Resolved Decisions

- [x] **Club code format**: 8-char alphanumeric (excludes confusing chars), supports manual entry
- [x] **Last member leaves**: Auto-delete club from Firestore (cleanup orphaned data)

---

## Implementation Progress

### Phase 1: Create Book Club - COMPLETE ✅ (2025-12-17)

**Implemented:**
- ✅ `BookClubMembershipEntity` with Room database (migration 9→10)
- ✅ `BookClubDao` for local membership queries
- ✅ `BookClubCodeGenerator` + `BookClubCodeGeneratorImpl` with collision checking
- ✅ `BookClubMetadataDto`, `BookClubMemberDto`, `BookClubBookDto` for Firestore
- ✅ `BookClub` and `BookClubMembership` domain models
- ✅ `BookClubRepository` interface + `BookClubRepositoryImpl`
- ✅ `CreateBookClubUseCase` + `GenerateInviteLinkUseCase`
- ✅ `BookClubUseCases` facade for ViewModel injection
- ✅ `ShareOptionsDialog` and `InviteLinkDialog` UI components
- ✅ Share menu updated in both BookshelfScreen and BookcaseScreen
- ✅ `BookshelfEntity` extended with `isBookClub` and `clubCode` fields
- ✅ `Bookshelf` domain model extended with same fields
- ✅ Firestore security rules for `/bookClubs/` collection
- ✅ ProGuard rules for Firestore DTO serialization

**Bug Fixes Applied:**
- ✅ Duplicate book club creation fixed (checks existing clubCode first)
- ✅ Orphaned book clubs fixed (deleteBookClub on shelf deletion)
- ✅ ProGuard obfuscation fixed (keep rules for DTOs)
- ✅ Firestore permission denied fixed (security rules)

**Code Quality:**
- ✅ `BookClubOperationsHandler` extracted to eliminate duplicate createBookClub logic
- ✅ All 448 unit tests passing
- ✅ DRY principle enforced
- ✅ Clean Architecture maintained

**Files Created:**
```
bookshelf/presentation/bookcase/handlers/BookClubOperationsHandler.kt
bookshelf/presentation/bookshelf/bookclub_components/ShareOptionsDialog.kt
bookshelf/presentation/bookshelf/bookclub_components/InviteLinkDialog.kt
bookshelf/domain/usecase/bookclub/CreateBookClubUseCase.kt
bookshelf/domain/usecase/bookclub/CreateBookClubUseCaseImpl.kt
bookshelf/domain/usecase/bookclub/GenerateInviteLinkUseCase.kt
bookshelf/domain/usecase/bookclub/GenerateInviteLinkUseCaseImpl.kt
bookshelf/domain/usecase/bookclub/BookClubUseCases.kt
bookshelf/domain/repository/BookClubRepository.kt
bookshelf/data/book/repository/BookClubRepositoryImpl.kt
bookshelf/domain/service/BookClubCodeGenerator.kt
bookshelf/data/service/BookClubCodeGeneratorImpl.kt
bookshelf/domain/model/BookClub.kt
bookshelf/domain/model/BookClubMembership.kt
bookshelf/data/mappers/BookClubMappers.kt
core/data/database/entity/BookClubMembershipEntity.kt
core/data/database/dao/BookClubDao.kt
sync/data/dto/BookClubMetadataDto.kt
sync/data/dto/BookClubMemberDto.kt
sync/data/dto/BookClubBookDto.kt
```

---

### Phase 2: Join Book Club - COMPLETE ✅ (2025-12-18)

**Implemented:**
- ✅ `JoinBookClubUseCase` + `JoinBookClubUseCaseImpl`
- ✅ `GetBookClubUseCase` + `GetBookClubUseCaseImpl`
- ✅ `ParseClubCodeUseCase` + `ParseClubCodeUseCaseImpl`
- ✅ `JoinBookClubDialog` UI component
- ✅ `BookClubPreviewDialog` UI component
- ✅ Bookcase menu "Join Book Club" option
- ✅ `BookClubRepository.joinBookClub()` implementation
- ✅ `BookClubRepository.isMemberOfClub()` implementation
- ✅ `BookClubRepository.getClubBooks()` implementation
- ✅ Book club membership restore on sign-in (denormalization approach)
- ✅ `RestoreBookClubMembershipsUseCase` + implementation
- ✅ `UserPreferencesFirestoreDto.clubMemberships` field added
- ✅ `RemoteSyncDataSource.addClubMembership()` / `removeClubMembership()` methods
- ✅ `ClearUserDataUseCaseImpl` now clears book club memberships on sign-out
- ✅ Firestore rules for members subcollection (read other members)
- ✅ Firestore rules for user settings subcollection

**Bug Fixes Applied:**
- ✅ Collection group query replaced with denormalization (best practice)
- ✅ `addClubMembership` uses `set()` with merge (creates doc if not exists)
- ✅ Members can read other members (for member count)
- ✅ Book club memberships cleared on sign-out (fixes stale local data)
- ✅ Timber logging fixed (varargs `%s` format, throwable first parameter)

**Files Created:**
```
bookshelf/domain/usecase/bookclub/JoinBookClubUseCase.kt
bookshelf/domain/usecase/bookclub/JoinBookClubUseCaseImpl.kt
bookshelf/domain/usecase/bookclub/GetBookClubUseCase.kt
bookshelf/domain/usecase/bookclub/GetBookClubUseCaseImpl.kt
bookshelf/domain/usecase/bookclub/ParseClubCodeUseCase.kt
bookshelf/domain/usecase/bookclub/ParseClubCodeUseCaseImpl.kt
bookshelf/domain/usecase/bookclub/RestoreBookClubMembershipsUseCase.kt
bookshelf/domain/usecase/bookclub/RestoreBookClubMembershipsUseCaseImpl.kt
bookshelf/presentation/bookclub/components/JoinBookClubDialog.kt
bookshelf/presentation/bookclub/components/BookClubPreviewDialog.kt
```

**Files Modified:**
```
sync/data/dto/UserPreferencesFirestoreDto.kt (added clubMemberships)
sync/data/repository/RemoteSyncDataSource.kt (added addClubMembership/removeClubMembership)
sync/data/service/FirestoreRemoteDataSource.kt (implemented new methods + debug logging)
bookshelf/data/book/repository/BookClubRepositoryImpl.kt (join, restore, getRemoteClubMemberships)
bookshelf/data/usecase/ClearUserDataUseCaseImpl.kt (clears book club memberships)
core/data/database/dao/BookClubDao.kt (added deleteAllMemberships)
firestore.rules (settings subcollection + members read rule)
di/AppModule.kt (new use cases registered)
```

**Deferred to Future:**
- Web page for club preview (docs/club/index.html)
- Deep link handling (mybookshelf://club/{code})

---

### Phase 3: Collaborative Books (Sync) - COMPLETE ✅ (2025-12-19)

**Architecture Decision:**
Instead of creating separate `AddBookToClubUseCase` / `RemoveBookFromClubUseCase`, we modified the existing `AddBookToShelfUseCaseImpl` and `RemoveBookFromShelfUseCaseImpl` to detect book club shelves and sync automatically. This follows DRY and keeps book operations unified.

**Implemented:**
- ✅ `BookClubRepository.syncBookToClub()` - syncs a book to Firestore club collection
- ✅ `BookClubRepository.removeBookFromClub()` - removes a book from Firestore club collection
- ✅ `RemoteSyncDataSource.removeBookFromClub()` - Firestore delete operation
- ✅ `AddBookToShelfUseCaseImpl` - now syncs to club if shelf is a book club
- ✅ `RemoveBookFromShelfUseCaseImpl` - now removes from club if shelf is a book club
- ✅ Presentation layer refactored - book club components consolidated into `bookclub/` folder

**How It Works:**
1. When a book is added to a shelf, `AddBookToShelfUseCaseImpl`:
   - Checks if shelf is a book club (`isBookClub == true && clubCode != null`)
   - If yes, calls `bookClubRepository.syncBookToClub(clubCode, book)`
   - Failure to sync doesn't fail the local operation (graceful degradation)

2. When a book is removed from a shelf, `RemoveBookFromShelfUseCaseImpl`:
   - Checks if shelf is a book club BEFORE removing locally
   - If yes, calls `bookClubRepository.removeBookFromClub(clubCode, bookId)`
   - Failure to sync doesn't fail the local operation (graceful degradation)

**Files Created:**
```
(No new files - functionality added to existing use cases)
```

**Files Modified:**
```
bookshelf/domain/repository/BookClubRepository.kt (added syncBookToClub, removeBookFromClub)
bookshelf/data/book/repository/BookClubRepositoryImpl.kt (implemented new methods)
sync/data/repository/RemoteSyncDataSource.kt (added removeBookFromClub)
sync/data/service/FirestoreRemoteDataSource.kt (implemented removeBookFromClub)
bookshelf/domain/usecase/book_detail/AddBookToShelfUseCaseImpl.kt (added club sync)
bookshelf/domain/usecase/book_detail/RemoveBookFromShelfUseCaseImpl.kt (added club sync)
```

**Presentation Refactoring:**
Book club UI components consolidated from scattered locations into dedicated folder:
```
OLD:
  bookshelf/presentation/bookshelf/bookclub_components/*.kt
  bookshelf/presentation/bookcase/handlers/BookClubOperationsHandler.kt

NEW:
  bookshelf/presentation/bookclub/handlers/BookClubOperationsHandler.kt
  bookshelf/presentation/bookclub/components/ShareOptionsDialog.kt
  bookshelf/presentation/bookclub/components/InviteLinkDialog.kt
  bookshelf/presentation/bookclub/components/JoinBookClubDialog.kt
  bookshelf/presentation/bookclub/components/BookClubPreviewDialog.kt
```

**Test Updates:**
- ✅ `AddBookToShelfUseCaseTest.kt` - added mock dependencies
- ✅ `RemoveBookFromShelfUseCaseTest.kt` - added mock dependencies
- ✅ `MockBookClubRepository.kt` - added syncBookToClub/removeBookFromClub mocks
- ✅ `BookcaseViewModelTest.kt` - updated import path
- ✅ `BookshelfViewModelTest.kt` - updated import path
- ✅ `SyncEngineTest.kt` - added removeBookFromClub to FakeRemoteSyncDataSource
- ✅ All tests passing

**Next Phase:** Phase 4 - Reviews (Optional) or Phase 5 - Leave & Members Management
