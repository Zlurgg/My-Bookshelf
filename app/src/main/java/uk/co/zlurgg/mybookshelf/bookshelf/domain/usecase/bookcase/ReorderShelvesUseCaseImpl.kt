package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ReorderShelvesUseCaseImpl(
    private val repository: BookcaseRepository,
) : ReorderShelvesUseCase {
    override suspend fun execute(
        shelfToMove: Bookshelf,
        newPosition: Int,
        currentShelves: List<Bookshelf>,
    ): Result<List<Bookshelf>, DataError.Local> {
        return try {
            val currentShelfIndex = currentShelves.indexOfFirst { it.id == shelfToMove.id }

            if (currentShelfIndex == -1) {
                return Result.Error(DataError.Local.NOT_FOUND)
            }

            // Create reordered list with updated positions
            val reorderedList =
                currentShelves.toMutableList().apply {
                    removeAt(currentShelfIndex)
                    add(newPosition.coerceIn(0, size), shelfToMove.copy(position = newPosition))
                }

            // Update positions for all affected shelves
            val updatedShelves =
                reorderedList.mapIndexed { index, bookshelf ->
                    bookshelf.copy(position = index)
                }

            // Persist changes - only update shelves whose positions actually changed
            val originalPositions = currentShelves.associate { it.id to it.position }
            val shelvesToUpdate =
                updatedShelves.filter {
                    originalPositions[it.id] != it.position
                }

            shelvesToUpdate.forEach { updatedShelf ->
                repository.updateShelf(updatedShelf)
            }

            Result.Success(updatedShelves)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}
