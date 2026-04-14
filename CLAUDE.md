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
- **IMPORTANT**: Research before writing tests — search for actual implementations, check exact data class definitions, verify methods exist

## Anti-Patterns (YOU MUST avoid these)

- `!!` operator — use safe calls or `require()`
- ViewModel calling repository directly — use UseCases
- Business logic in Composables — move to ViewModel/UseCase
- Manual dependency instantiation — use Koin
- Context in ViewModels — inject what you need
- LiveData in new code — use StateFlow/Flow
- String interpolation in logs — use varargs formatting

## Build Variants

- Debug: `app/src/debug/` (Firebase emulators, dev auth)
- Release: `app/src/release/` (production stubs)
- No `BuildConfig.DEBUG` runtime checks for dev-only code

## Key Locations

| Component | Location |
|-----------|----------|
| DI Modules | `*/di/*Module.kt` |
| UseCases | `*/domain/usecase/` |
| Repositories | `*/data/repository/` |
| ViewModels | `*/presentation/*/ViewModel.kt` |
| Test Utilities | `app/src/test/.../testutil/` |
| Detekt Config | `app/detekt.yml` |
