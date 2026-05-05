# Plan: Remove Deeplink Sharing & Improve Book Club Sharing

## Summary

Remove the legacy deeplink-based bookshelf sharing (GitHub Pages hosted, URL-encoded tokens) and simplify book club sharing to use codes only (no generated URLs). Increase code length from 8 to 12 characters for security. Add QR code generation and sharing for club invites.

## Motivation

- Deeplink sharing produces ugly, fragile URLs with a 2KB limit
- Relies on GitHub Pages hosting which is being removed
- Book clubs already solve sharing in a robust way via Firestore
- The app is moving to offline-first; sharing without an account is no longer a goal
- 8-char codes are guessable by a determined brute-force script; 12-char codes are not

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Backwards compat for 8-char codes | No | Clean break. Existing clubs are un-joinable by new members; owners can recreate. No migration script. |
| `mybookshelf://club` deeplink | Remove | YAGNI. No URL is generated or shared anymore. QR codes encode the raw code, not a deeplink. |
| QR code scanning | Not in-app | Phone cameras natively read QR codes and copy text. User pastes into join dialog. Add in-app scanner later if users request it. |
| QR code storage | None | QR is generated on-the-fly from the club code. Deterministic — same code = same QR. No Firestore storage needed. |
| Atomicity | Single PR (code + manifest + docs) | Phases 1-3 are interdependent; shipping partially leaves inconsistent UI. Docs included since they're trivial. |
| Rate limiting | Follow-up task | Code length is defense-in-depth. Firestore Security Rules rate limiting is the real fix — tracked separately. |

## Changes

### Phase 1: Increase Code Length (8 -> 12) & Extract Shared Constant

**New: `bookclub/domain/model/BookClubCode.kt`**
- Extract `VALID_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"` and `CODE_LENGTH = 12`
- Single source of truth consumed by both generator and parser

**`BookClubCodeGeneratorImpl`:**
- Change `CODE_LENGTH` from 8 to 12 (reference `BookClubCode.CODE_LENGTH`)
- Replace local `ALLOWED_CHARS` with `BookClubCode.VALID_CHARS`
- 30^12 = ~531 quadrillion combinations

**`ParseClubCodeUseCaseImpl`:**
- Replace local `ALLOWED_CHARS` with `BookClubCode.VALID_CHARS`
- Update validation to expect exactly 12 characters (`BookClubCode.CODE_LENGTH`)
- No backwards compat — reject 8-char codes

### Phase 2: Simplify Book Club Sharing

**Delete `GenerateInviteLinkUseCase` + `GenerateInviteLinkUseCaseImpl`:**
- Remove from `BookClubModule.kt` DI registration
- Remove from any ViewModel that calls it (check BookClubViewModel state for `inviteLink` field)

**Simplify `ParseClubCodeUseCaseImpl`:**
- Remove all URL parsing logic (web URLs, app deeplinks, path extraction)
- Only accept raw 12-character alphanumeric codes
- Validation: call `.uppercase()` on input first (some QR readers output lowercase), then check each char is in `BookClubCode.VALID_CHARS`, length == 12

**Update `InviteLinkDialog`:**
- Remove `inviteLink: String` parameter entirely (only `clubCode` needed)
- Rename "Copy Link" button label to "Copy Code"
- Copy action: copy `clubCode` to clipboard (not `inviteLink`)
- Update `ClipData.newPlainText` label: "Book Club Invite Link" -> "Book Club Invite Code"
- Update toast: "Link copied!" -> "Code copied!"
- "Share" button intent text: `"Join my book club \"$clubName\" on MyBookshelf!\n\nCode: $clubCode"`
- No URL anywhere in share text

**Update `JoinBookClubDialog`:**
- Change hint from "Enter code or paste invite link" to "Enter invite code"

**Update string resources:**
- `join_book_club_hint` -> "Enter invite code"
- Add/rename strings for "Copy Code" button label and "Code copied!" toast

**Update callers of `InviteLinkDialog`:**
- Remove `inviteLink` argument from all call sites
- Remove ViewModel logic that calls `GenerateInviteLinkUseCase` and stores result in state

### Phase 3: QR Code Generation & Sharing

**Library choice: qrose (`io.github.alexzhirkevich:qrose`)**
- Compose-native — renders QR as a composable with zero boilerplate
- ~50KB, no heavy dependencies
- Display: `QrCode(data = clubCode, modifier = Modifier.size(200.dp))`
- For sharing as image: capture composable to Bitmap via `GraphicsLayer` / `drawToBitmap()`

**QR payload: raw 12-char club code only**
- Content: `ABCD1234EFGH` (just the code, no wrapping text or URL)
- Rationale: keeps it simple, works directly with the "paste into join dialog" flow. Adding context text would require the user to extract the code manually. The share message text that accompanies the QR image provides the context.

**No UseCase — this is just a composable:**

| Component | Responsibility | Layer |
|-----------|---------------|-------|
| `QrCode` composable (qrose) | Render QR in dialog for in-person scanning | `bookclub/presentation/components/` (inline in `InviteLinkDialog`) |

No domain/service interface, no bitmap generation, no UseCase. The qrose library renders directly as a Compose composable. That's the entire integration for now.

**Future: Share QR as image (deferred)**
When/if users request sharing QR as a PNG in group chats:
- Add `QrBitmapGenerator` — generate Bitmap from QR data matrix directly (not composable capture, which is fragile on some OEM ROMs)
- Add `QrShareHelper` — write bitmap to cache, create FileProvider URI, fire `ACTION_SEND`
- Add FileProvider setup (manifest + `file_paths.xml`)
- Check whether qrose exposes a non-composable API; if not, add `zxing:core` for bitmap path only
- Use single overwritten file (`invite_qr.png`) — no cache cleanup needed

**No FileProvider needed (deferred with image sharing).**

**Rename `InviteLinkDialog` → `ClubInviteDialog`:**
- Rename file and composable function (old name references "Link" which no longer applies)
- Add QR code composable displayed prominently at top of dialog (primary use: in-person scanning)
- Below QR: code text in monospace
- Buttons: "Copy Code" | "Share" | "Done"
- "Share" sends text message with code (the common case — recipients paste into join dialog)
- QR image sharing deferred — the QR's primary value is in-person scanning. Image sharing (post QR to group chat for others to screenshot+scan) is a secondary flow; add later if users request it. YAGNI for now.
- Update all call sites referencing the old composable name

**No in-app scanning:**
- Recipient scans QR with phone camera → gets 12-char code as text → pastes into "Join Book Club" dialog
- The join flow already handles pasted codes — no changes needed on the receiving end

### Phase 4: Remove Deeplink Sharing

**Delete entire `sharing/` package:**
- `sharing/domain/` — UseCases (`ExportBookshelfUseCase`, `DeepLinkImportUseCase`, `ImportBookshelfUseCase`, `CheckImportConflictUseCase`), services (`ShareTokenService`, `BookshelfDataOrchestrator`, `BookshelfSerializer`)
- `sharing/data/` — `UrlEncodedShareTokenService`, `AndroidBookshelfExportService`, `AndroidShareService`, `DatabaseBookshelfDataOrchestrator`, `JsonBookshelfSerializer`, `BookshelfImportValidatorImpl`, `BookshelfExportMapper`, export models (`BookshelfExportData`, `ExportedBookshelf`, `BookIdentifier`)
- `sharing/presentation/` — `DeepLinkViewModel`, `DeepLinkState`, `DeepLinkAction`, dialogs (`ImportLoadingDialog`, `ImportSuccessDialog`, `ImportErrorDialog`, `ImportNameConflictDialog`)
- `sharing/di/` — DI module

**Delete `book/domain/model/ShareData.kt`:**
- Only consumed by `ExportBookshelfUseCase` and `AndroidShareService` (both deleted)

**Remove from `BookshelfViewModel` / bookshelf feature:**
- `BookshelfUseCases.shareBookshelf` — remove field from wrapper class
- `ShareBookshelfUseCase` / `ShareBookshelfUseCaseImpl` in `bookshelf/domain/usecase/` — delete
- `BookshelfState.isShareLoading` — remove field
- `BookshelfState.errorMessage` — keep (shared by sync, search, and other actions)
- `BookshelfAction.OnShareShelf` — remove from sealed interface + any `when` exhaustive match
- `BookshelfViewModel.shareShelf()` — remove action handler (and its `errorMessage` usage)
- Bookshelf UI — remove share menu item / button
- Koin module providing `BookshelfUseCases` — update constructor (remove `shareBookshelf` param)

**Remove from `core/domain/error/`:**
- `DataError.Local.SHARE_LINK_TOO_LARGE` — remove variant
- `DataError.Local.SHARE_FAILED` — remove variant
- `ErrorFormatter` — remove cases for both share errors

**Remove from app entry points:**
- `AndroidManifest.xml` — remove `mybookshelf://share` intent filter AND `mybookshelf://club` intent filter
- `MyBookShelfApp.kt` — remove `handleDeepLink()` function entirely (both `share` and `club` branches gone). If `handleDeepLink` does nothing else, delete it. Ensure unrecognized intents are silently ignored (no crash on else).
- `MainActivity.kt` — remove intent forwarding to `MyBookShelfApp`
- Navigation — remove any deeplink import routes

**Remove `ApiConfig.shareBaseUrl`:**
- Only consumed by `AndroidShareService` (deleted) and `GenerateInviteLinkUseCaseImpl` (deleted)
- Remove from `ApiConfig` class
- Remove from any BuildConfig / build.gradle property that provides it

**Remove string resources:**
- `menu_share_shelf`
- `error_share_failed`
- `error_share_link_too_large`
- `import_success_title` / `import_success_message`
- `import_error_title`
- `import_name_conflict_title` / `import_name_conflict_message`
- `import_new_name_label`
- `action_import`
- `importing_shelf`
- Any menu XML referencing `menu_share_shelf`

**Remove DI registrations:**
- Unbind sharing module from app-level Koin config
- Remove `DeepLinkViewModel` from Koin

**Proguard/R8:** No sharing-specific keep rules exist — nothing to clean up.

**Remove tests:**
- All tests under `sharing/` test directories (unit + integration)
- Any test referencing `ShareBookshelfUseCase` or `DeepLinkViewModel`

### Phase 5: Update Documentation & Web Pages

**`docs/index.html`:**
- Remove "Share collections via deep links" from feature list
- Replace with "Create and join book clubs with invite codes"

**`docs/share/bookshelf/index.html`:**
- Delete entirely

**`docs/share/index.html`:**
- Remove "Import a Bookshelf" option
- Simplify to only show book club join instructions (enter code in app)

**`docs/share/club/index.html`:**
- Remove GitHub release download links
- Point to Play Store for app download
- Simplify to: "Open MyBookshelf, tap Join Book Club, enter your code"

**`README.md`:**
- Replace "Bookshelf Sharing - Export and share your shelves via deep links"
- With "Book Clubs - Create and join book clubs with invite codes"

**`RELEASE_NOTES.md`:**
- Leave historical entries as-is
- New release entry documents the removal

**`docs/specs/patterns/usecase.md`:**
- Remove `ShareBookshelfUseCase` reference from examples

**`docs/specs/style/code-style.md`:**
- Remove `sharing/` from test directory listing

**`docs/privacy.html`:**
- Review wording — should still be accurate (book club data in Firestore remains)

## Existing 8-char Clubs

Old clubs with 8-char codes become un-joinable by new members after this change. This is acceptable because:
- The app is pre-release / early stage
- Existing members remain in their clubs (membership is stored, not re-validated)
- Club owners can delete and recreate with a new 12-char code
- No migration script needed

## Security: Rate Limiting (follow-up)

Code length alone doesn't prevent brute-force. A follow-up task should add:
- Firestore Security Rules limiting read frequency on `book_clubs/{code}` per authenticated user
- Or a Cloud Function wrapper for club lookup with rate limiting
- This is tracked separately from this PR

## Testing

- Verify book club creation generates 12-char codes
- Verify "Copy Code" copies raw code to clipboard
- Verify "Share Code" sends plain text message with code (no URL)
- Verify QR code is displayed in invite dialog (scan with phone camera to confirm it contains the raw 12-char code)
- Verify join dialog accepts 12-char codes only
- Verify 8-char codes are rejected with clear error
- Verify app builds cleanly with no orphaned imports
- Verify removal of `mybookshelf://share` and `mybookshelf://club` intent filters doesn't crash on stale intents (Android won't route them — no code needed)
- Verify all sharing/ tests are removed and remaining tests pass
- Run full detekt lint pass

## Execution

Single PR. All phases are interdependent — shipping partially leaves inconsistent UI/behaviour.

Order within the PR:
1. Phase 1 (code length + shared constant) — foundation
2. Phase 2 (simplify BC sharing) — depends on new code length
3. Phase 3 (QR code generation) — builds on simplified InviteLinkDialog
4. Phase 4 (remove deeplink sharing) — big deletion, depends on phase 2 removing invite link generation
5. Phase 5 (docs) — reflects final state
