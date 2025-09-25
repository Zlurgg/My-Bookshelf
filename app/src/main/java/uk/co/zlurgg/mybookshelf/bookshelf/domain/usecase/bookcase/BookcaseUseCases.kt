package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

/**
 * Facade aggregating all bookcase related UseCases.
 * Simplifies ViewModel constructor dependencies and provides clean separation of concerns.
 */
class BookcaseUseCases(
    val getAllShelves: GetAllShelvesUseCase,
    val createShelf: CreateShelfUseCase,
    val deleteShelf: DeleteShelfUseCase,
    val reorderShelves: ReorderShelvesUseCase
)