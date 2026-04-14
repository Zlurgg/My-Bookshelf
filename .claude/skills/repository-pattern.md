---
name: Repository Pattern
description: How to implement repositories - interface in domain, implementation in data, Result returns, ErrorMapper usage
---

# Repository Pattern

Load the full spec before implementing:
@docs/specs/patterns/repository.md

Key points:
- Interface in domain layer, implementation in data layer
- Return `Result<T, DataError>` for fallible operations
- Return `Flow<T>` for reactive/observable data
- Use `ErrorMapper.safeSuspendCall(TAG) { ... }` for all database operations
- Entity-to-domain mapping as extension functions (e.g., `BookEntity.toDomain()`)
- Register in Koin: `singleOf(::RepositoryImpl) { bind<RepositoryInterface>() }`
- Domain layer has ZERO dependencies on data layer types
