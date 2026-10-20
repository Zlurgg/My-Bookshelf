# MyBookshelf - Claude Code Instructions

Architecture specs live in `docs/specs/`. IMPORTANT: Read the relevant spec before implementing or modifying any pattern.

| Spec | Purpose |
|------|---------|
| `docs/specs/constitution.md` | Non-negotiable architectural principles |
| `docs/specs/style/code-style.md` | Naming conventions, detekt rules, testing |
| `docs/specs/patterns/usecase.md` | UseCase implementation pattern |
| `docs/specs/patterns/state-management.md` | ViewModel state/action pattern |
| `docs/specs/patterns/repository.md` | Repository implementation pattern |
| `docs/specs/patterns/compose-screens.md` | Root/Screen composable pattern |
| `docs/specs/operations/` | Google Books API key restrictions and security, dependency updates |
| `docs/specs/plans/known-issues.md` | Open issues and deferred fixes. Check before starting new work |

## Commit Style

- Conventional commits: `type(scope): description`
- No "Generated with Claude Code" signatures
- No "Co-Authored-By" footers
- Examples: `feat(bookclub): Add rating system`, `fix(ui): Fix hardcoded strings`

## Error Handling

All fallible operations return `Result<T, DataError>`, never throw. Use `ErrorMapper.safeSuspendCall()` in repositories. Handle both `Result.Success` and `Result.Error` in ViewModels.

## Testing

- **StateFlow ViewModels**: Always collect state to trigger initialization
- **Coroutine Testing**: Use `advanceUntilIdle()` after actions
- **Integration Tests**: Real Room database, stub only network/external services
- **DataStore in tests**: Create it with `scope = CoroutineScope(testDispatcher + Job())` and cancel that scope before `Dispatchers.resetMain()`. The default IO scope causes flaky "Dispatchers.Main is used concurrently" teardown failures
- **IMPORTANT**: Research before writing tests — search for actual implementations, check exact data class definitions, verify methods exist

## Anti-Patterns (YOU MUST avoid these)

- `!!` operator — use safe calls or `require()`
- ViewModel calling repository directly — use UseCases
- Business logic in Composables — move to ViewModel/UseCase
- Manual dependency instantiation — use Koin
- Context in ViewModels — inject what you need
- LiveData in new code — use StateFlow/Flow
- String interpolation in logs — use varargs formatting

## Book Search

- Google Books is the primary provider. `FallbackRemoteBookDataSource` falls back to Open Library **on page 1 only**, on 429/403/`PROVIDER_UNAVAILABLE`/5xx or an empty Google page. Page 2+ never switches provider
- `BookSearchResponse` / `SearchResult` carry `provider`. "Load more" only appears for Google results, and the search dialog shows an Open Library notice for OL results
- `HttpClientFactory` retries 5xx once and network exceptions 3 times (20s HTTP timeout)
- Debug Google outages by replaying the request with curl (see `docs/specs/plans/search-google-outage.md` §Verification) before suspecting the key or the build

## Releases

1. Bump `versionCode` (it must exceed every code ever uploaded to any track) and `versionName` in `app/build.gradle.kts`
2. Add an entry to `RELEASE_NOTES.md` and update the version badge in `docs/index.html`
3. Merge to `main`, then run `gh workflow run release.yml --ref main -f track=internal`. The workflow runs tests and detekt, then uploads the AAB to Play as a **draft**
4. The owner adds release notes, rolls out in Play Console and promotes to production
- Play re-signs with the Play App Signing key, so a locally signed build can't be installed over a Play install (or the reverse); uninstall first

## Build Variants

- Debug: `app/src/debug/` (Firebase emulators, dev auth)
- Release: `app/src/release/` (production stubs)
- No `BuildConfig.DEBUG` runtime checks for dev-only code

## Package Structure

| Package | Purpose |
|---------|---------|
| `app/` | Application entry, navigation, `MyBookShelfApp` |
| `core/` | Infrastructure (database, network, preferences) |
| `account/` | Account management (profile display, account deletion) |
| `auth/` | Authentication (sign-in, sign-out, user provider, credential fetching) |
| `sync/` | Cloud sync (Firestore sync engine) |
| `book/` | Shared book domain (models, repos, network, preview, shared use cases) |
| `bookcase/` | Home screen (shelf list, create/delete/rename shelves) |
| `bookdetail/` | Book detail screen (view/edit book metadata, purchase toggle) |
| `bookshelf/` | Shelf screen (viewing books on a shelf, searching, adding) |
| `bookclub/` | Book club feature (club models, repos, use cases, UI) |
| `welcome/` | Tutorial, onboarding |

Dependencies flow downward: `bookshelf/` → `bookcase/` (GetShelfByIdUseCase) → `book/` → `core/`. `bookdetail/` → `book/`. `bookclub/` → `book/`. `account/` → `auth/` + `sync/` + `book/` (via `ClubOperations`). Cross-feature communication uses interfaces in `book/domain/service/` (`ClubOperations`, `BookReviewProvider`) and `auth/presentation/service/` (`CredentialFetcher`).

## Key Locations

| Component | Location |
|-----------|----------|
| DI Modules | `*/di/*Module.kt` |
| UseCases | `*/domain/usecase/` |
| Repositories | `*/data/repository/` |
| ViewModels | `*/presentation/*/ViewModel.kt` |
| Cross-feature interfaces | `book/domain/service/` |
| Shared domain models | `book/domain/model/` |
| Test Utilities | `app/src/test/.../testutil/` |
| Detekt Config | `app/detekt.yml` |
