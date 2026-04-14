---
name: UseCase Pattern
description: How to implement UseCases following Clean Architecture - interface, invoke operator, DI registration, testing
---

# UseCase Pattern

Load the full spec before implementing:
@docs/specs/patterns/usecase.md

Key points:
- Interface + `operator fun invoke()` for callable syntax
- VerbNoun naming (e.g., `GetBookUseCase`, `DeleteShelfUseCase`)
- One UseCase per business operation
- Must be main-safe (use appropriate dispatchers)
- Register in Koin with `factoryOf(::UseCaseImpl) { bind<UseCaseInterface>() }`
- Aggregate multiple UseCases via a data class (e.g., `BookUseCases`)
- Use `ErrorMapper.safeSuspendCall()` for error handling
- Test naming: `action - condition - expected result`
