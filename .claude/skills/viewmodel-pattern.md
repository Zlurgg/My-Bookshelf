---
name: ViewModel State Management
description: How to implement ViewModels with single StateFlow, immutable state, sealed Action interface, and handler pattern
---

# ViewModel State/Action Pattern

Load the full spec before implementing:
@docs/specs/patterns/state-management.md

Key points:
- Single `StateFlow<ScreenState>` with immutable data class
- Sealed `Action` interface for all user interactions
- `onAction(action: Action)` as single entry point
- State fields: `errorMessage: String?`, `isLoading: Boolean`, `showXxxDialog: Boolean`
- Update via `_state.update { it.copy(...) }`
- Extract handler classes for large ViewModels (group related operations)
- Always collect state in tests to trigger initialization
- Use `advanceUntilIdle()` after dispatching actions in tests
