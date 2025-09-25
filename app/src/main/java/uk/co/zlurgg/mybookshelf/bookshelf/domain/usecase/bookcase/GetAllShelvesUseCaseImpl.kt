package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository

@OptIn(ExperimentalCoroutinesApi::class)
class GetAllShelvesUseCaseImpl(
    private val repository: BookcaseRepository
) : GetAllShelvesUseCase {

    override suspend fun execute(): Flow<List<Bookshelf>> {
        return repository.getAllShelves()
            .flatMapLatest { shelves ->
                // Create a flow of book counts for all shelves
                if (shelves.isEmpty()) {
                    // If no shelves, just emit the shelves with empty counts
                    flowOf(shelves)
                } else {
                    // Create individual flows for each shelf's book count
                    val countFlows = shelves.map { shelf ->
                        repository.getBookCountForShelf(shelf.id)
                            .map { count -> shelf.id to count }
                    }

                    // Combine all count flows together and update shelves
                    combine(countFlows) { counts ->
                        val countMap = counts.toMap()
                        shelves.map { shelf ->
                            // Note: This preserves the original shelf but could be enhanced
                            // to include book count if needed in the Bookshelf model
                            shelf
                        }
                    }
                }
            }
    }
}