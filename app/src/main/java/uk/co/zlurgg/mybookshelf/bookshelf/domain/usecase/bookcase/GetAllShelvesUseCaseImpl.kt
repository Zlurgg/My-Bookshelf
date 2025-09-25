package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.AllShelvesResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository

@OptIn(ExperimentalCoroutinesApi::class)
class GetAllShelvesUseCaseImpl(
    private val repository: BookcaseRepository
) : GetAllShelvesUseCase {

    override suspend fun execute(): Flow<AllShelvesResult> {
        return repository.getAllShelves()
            .flatMapLatest { shelves ->
                if (shelves.isEmpty()) {
                    // If no shelves, just emit empty result
                    flowOf(AllShelvesResult(shelves = emptyList(), bookCounts = emptyMap()))
                } else {
                    // Create individual flows for each shelf's book count
                    val countFlows = shelves.map { shelf ->
                        repository.getBookCountForShelf(shelf.id)
                            .map { count -> shelf.id to count }
                    }

                    // Combine all count flows together and return both shelves and counts
                    combine(countFlows) { countsArray ->
                        val countMap = countsArray.toMap()
                        AllShelvesResult(
                            shelves = shelves,
                            bookCounts = countMap
                        )
                    }
                }
            }
    }
}