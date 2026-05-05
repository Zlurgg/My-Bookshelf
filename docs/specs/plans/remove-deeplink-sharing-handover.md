# Handover: Remove Deeplink Sharing & Improve Book Club Sharing

## What to implement

Read and follow `docs/specs/plans/remove-deeplink-sharing.md` — it is the approved, reviewed plan. This handover provides context and key file locations to get you started quickly.

## Context

The app has two sharing mechanisms. We're removing the legacy one (deeplink/URL-encoded bookshelf sharing via GitHub Pages) and improving the remaining one (book club invite codes via Firestore). Additionally, we're adding a QR code display for in-person club invites.

This is a single PR. The plan has 5 phases — execute in order.

## Branch

Create a new branch from `main`: `feat/remove-deeplink-sharing`

## Key Files by Phase

### Phase 1: Code length + shared constant

| File | Action |
|------|--------|
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/domain/model/BookClubCode.kt` | **Create** — `VALID_CHARS` and `CODE_LENGTH = 12` |
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/data/service/BookClubCodeGeneratorImpl.kt` | Edit — use `BookClubCode.*` |
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/domain/usecase/ParseClubCodeUseCaseImpl.kt` | Edit — use `BookClubCode.*`, remove URL parsing, add `.uppercase()` before validation |

### Phase 2: Simplify book club sharing

| File | Action |
|------|--------|
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/domain/usecase/GenerateInviteLinkUseCase.kt` | **Delete** |
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/domain/usecase/GenerateInviteLinkUseCaseImpl.kt` | **Delete** |
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/presentation/components/InviteLinkDialog.kt` | **Rename** to `ClubInviteDialog.kt`, remove `inviteLink` param, rename buttons/toasts |
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/di/BookClubModule.kt` | Edit — remove `GenerateInviteLinkUseCase` DI binding |
| `app/src/main/java/uk/co/zlurgg/mybookshelf/bookclub/presentation/components/JoinBookClubDialog.kt` | Edit — change hint text |
| `app/src/main/res/values/strings.xml` | Edit — update `join_book_club_hint`, add "Copy Code" / "Code copied!" strings |

Also check: whatever ViewModel calls `GenerateInviteLinkUseCase` and stores `inviteLink` in state — remove that logic and the state field. Check `BookClubOperationUseCases.kt` for aggregation.

### Phase 3: QR code

| File | Action |
|------|--------|
| `app/build.gradle.kts` (or `build.gradle`) | Edit — add `io.github.alexzhirkevich:qrose` dependency |
| `ClubInviteDialog.kt` (renamed in phase 2) | Edit — add `QrCode(data = clubCode)` composable at top of dialog |

That's it for QR. No UseCase, no service, no FileProvider. Just the library + composable.

### Phase 4: Remove deeplink sharing (big deletion)

**Delete entirely:**
- `app/src/main/java/uk/co/zlurgg/mybookshelf/sharing/` (entire package tree)
- `app/src/test/.../sharing/` (entire test tree)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/book/domain/model/ShareData.kt`
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/domain/usecase/ShareBookshelfUseCase.kt` (interface)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/domain/usecase/ShareBookshelfUseCaseImpl.kt`
- `docs/share/bookshelf/index.html`

**Edit:**
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/domain/usecase/BookshelfUseCases.kt` — remove `shareBookshelf` field
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/presentation/BookshelfState.kt` — remove `isShareLoading` field (keep `errorMessage`)
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/presentation/BookshelfAction.kt` — remove `OnShareShelf` from sealed interface
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookshelf/presentation/BookshelfViewModel.kt` — remove `shareShelf()` handler + `when` branch
- Bookshelf UI composable — remove share menu item/button
- Koin module for bookshelf — remove `ShareBookshelfUseCase` from `BookshelfUseCases` construction
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/domain/error/DataError.kt` — remove `SHARE_LINK_TOO_LARGE`, `SHARE_FAILED`
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/domain/error/ErrorFormatter.kt` — remove share error cases
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/network/ApiConfig.kt` — remove `shareBaseUrl` (check build.gradle for the property too)
- `app/src/main/AndroidManifest.xml` — remove both `mybookshelf://share` and `mybookshelf://club` intent filters
- `app/src/main/java/uk/co/zlurgg/mybookshelf/app/presentation/MyBookShelfApp.kt` — remove `handleDeepLink()` entirely (both `share` and `club` branches at lines ~312-331). Ensure no crash on unrecognized intents.
- `MainActivity.kt` — remove intent forwarding for deeplinks
- `app/src/main/res/values/strings.xml` — remove: `menu_share_shelf`, `error_share_failed`, `error_share_link_too_large`, `import_success_title`, `import_success_message`, `import_error_title`, `import_name_conflict_title`, `import_name_conflict_message`, `import_new_name_label`, `action_import`, `importing_shelf`
- App-level Koin config — remove sharing module registration + `DeepLinkViewModel` binding

### Phase 5: Documentation

| File | Action |
|------|--------|
| `docs/index.html` | Replace deeplink feature text with book club invite codes |
| `docs/share/index.html` | Remove bookshelf import, simplify to club code instructions |
| `docs/share/club/index.html` | Remove GitHub links, point to Play Store |
| `README.md` | Replace sharing feature description |
| `docs/specs/patterns/usecase.md` | Remove `ShareBookshelfUseCase` reference |
| `docs/specs/style/code-style.md` | Remove `sharing/` from test directory listing |
| `docs/privacy.html` | Review — should still be accurate |

## Verification

After implementation, run:
1. `./gradlew detekt` — lint must pass
2. `./gradlew testDebugUnitTest` — all tests pass
3. Build and run on device:
   - Create a book club → verify 12-char code generated
   - Tap invite → verify QR displayed, "Copy Code" copies raw code, "Share" sends text
   - Join a club → verify "Enter invite code" hint, accepts 12-char code, rejects 8-char
   - Verify no crash on app launch (deeplinks removed cleanly)

## Important Notes

- Read `CLAUDE.md` for project conventions (commit style, error handling, anti-patterns)
- Read `docs/specs/constitution.md` before making architectural decisions
- No `!!` operator — use safe calls or `require()`
- All fallible operations return `Result<T, DataError>`, never throw
- Conventional commits: `feat(bookclub): ...`, `refactor(sharing): ...`, `docs: ...`
- No "Co-Authored-By" footers, no "Generated with Claude Code" signatures
