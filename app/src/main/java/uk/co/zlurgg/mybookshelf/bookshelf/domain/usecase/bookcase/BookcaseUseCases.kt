package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase

/**
 * Facade aggregating all bookcase related UseCases.
 * Simplifies ViewModel constructor dependencies and provides clean separation of concerns.
 */
class BookcaseUseCases(
    val getAllShelves: GetAllShelvesUseCase,
    val createShelf: CreateShelfUseCase,
    val deleteShelf: DeleteShelfUseCase,
    val reorderShelves: ReorderShelvesUseCase,
    val getShelfById: GetShelfByIdUseCase,
    val renameShelf: RenameShelfUseCase,
    val updateShelfStyle: UpdateShelfStyleUseCase,
    val duplicateShelf: DuplicateShelfUseCase,
    val shareShelf: ShareBookshelfUseCase,
)
