package uk.co.zlurgg.mybookshelf.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookcase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCase

class FakeGetAllShelvesUseCase : GetAllShelvesUseCase {

    override suspend fun execute(): Flow<Bookcase> {
        return flowOf(
            Bookcase(
                id = "fake-bookcase-id",
                bookshelves = emptyList(),
                bookCounts = emptyMap()
            )
        )
    }
}