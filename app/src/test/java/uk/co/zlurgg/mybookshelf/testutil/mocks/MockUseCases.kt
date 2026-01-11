package uk.co.zlurgg.mybookshelf.testutil.mocks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookcase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.*
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.testutil.builders.TestShelfBuilder

/**
 * Reusable mock implementations of UseCases for testing.
 * Provides configurable behavior and tracking for test scenarios.
 */

class MockGetAllShelvesUseCase(
    private var bookcaseToReturn: Bookcase = Bookcase(
        id = "test-bookcase",
        bookshelves = emptyList(),
        bookCounts = emptyMap()
    )
) : GetAllShelvesUseCase {

    override suspend operator fun invoke(): Flow<Bookcase> = flowOf(bookcaseToReturn)

    fun configureBookcase(bookcase: Bookcase) {
        bookcaseToReturn = bookcase
    }
}

class MockCreateShelfUseCase : CreateShelfUseCase {
    var executed = false
    var lastUsedName: String? = null
    var lastUsedStyle: ShelfStyle? = null
    var lastUsedExistingShelves: List<Bookshelf>? = null
    var shouldReturnError = false
    var errorToReturn: DataError.Local = DataError.Local.UNKNOWN

    override suspend operator fun invoke(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError.Local> {
        executed = true
        lastUsedName = name
        lastUsedStyle = style
        lastUsedExistingShelves = existingShelves

        return if (shouldReturnError) {
            Result.Error(errorToReturn)
        } else {
            Result.Success(
                TestShelfBuilder()
                    .withName(name)
                    .withStyle(style)
                    .build()
            )
        }
    }

    fun reset() {
        executed = false
        lastUsedName = null
        lastUsedStyle = null
        lastUsedExistingShelves = null
        shouldReturnError = false
        errorToReturn = DataError.Local.UNKNOWN
    }
}

class MockDeleteShelfUseCase : DeleteShelfUseCase {
    var executeCallCount = 0
    var restoreCallCount = 0
    var lastDeletedShelfId: String? = null
    var lastRestoredShelf: Bookshelf? = null
    var shouldReturnError = false

    override suspend operator fun invoke(shelfId: String): Result<Unit, DataError.Local> {
        executeCallCount++
        lastDeletedShelfId = shelfId
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(Unit)
        }
    }

    override suspend fun restore(shelf: Bookshelf): Result<Unit, DataError.Local> {
        restoreCallCount++
        lastRestoredShelf = shelf
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(Unit)
        }
    }

    fun reset() {
        executeCallCount = 0
        restoreCallCount = 0
        lastDeletedShelfId = null
        lastRestoredShelf = null
        shouldReturnError = false
    }
}

class MockReorderShelvesUseCase : ReorderShelvesUseCase {
    var callCount = 0
    var lastShelfToMove: Bookshelf? = null
    var lastNewPosition: Int? = null
    var lastCurrentShelves: List<Bookshelf>? = null
    var shouldReturnError = false
    var reorderedShelvesToReturn: List<Bookshelf> = emptyList()

    override suspend operator fun invoke(
        shelfToMove: Bookshelf,
        newPosition: Int,
        currentShelves: List<Bookshelf>
    ): Result<List<Bookshelf>, DataError.Local> {
        callCount++
        lastShelfToMove = shelfToMove
        lastNewPosition = newPosition
        lastCurrentShelves = currentShelves

        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(reorderedShelvesToReturn.ifEmpty { currentShelves })
        }
    }

    fun reset() {
        callCount = 0
        lastShelfToMove = null
        lastNewPosition = null
        lastCurrentShelves = null
        shouldReturnError = false
        reorderedShelvesToReturn = emptyList()
    }
}

class MockGetShelfByIdUseCase : GetShelfByIdUseCase {
    var callCount = 0
    var lastRequestedShelfId: String? = null
    var shelfToReturn: Bookshelf? = null
    var shouldReturnError = false

    override suspend operator fun invoke(shelfId: String): Result<Bookshelf?, DataError.Local> {
        callCount++
        lastRequestedShelfId = shelfId
        return if (shouldReturnError) {
            Result.Error(DataError.Local.UNKNOWN)
        } else {
            Result.Success(shelfToReturn)
        }
    }

    fun reset() {
        callCount = 0
        lastRequestedShelfId = null
        shelfToReturn = null
        shouldReturnError = false
    }
}

class MockRenameShelfUseCase : RenameShelfUseCase {
    var callCount = 0
    var lastShelfId: String? = null
    var lastNewName: String? = null
    var shouldReturnError = false
    var errorToReturn: DataError.Local = DataError.Local.UNKNOWN

    override suspend operator fun invoke(shelfId: String, newName: String): Result<Unit, DataError.Local> {
        callCount++
        lastShelfId = shelfId
        lastNewName = newName
        return if (shouldReturnError) {
            Result.Error(errorToReturn)
        } else {
            Result.Success(Unit)
        }
    }

    fun reset() {
        callCount = 0
        lastShelfId = null
        lastNewName = null
        shouldReturnError = false
        errorToReturn = DataError.Local.UNKNOWN
    }
}

class MockUpdateShelfStyleUseCase : UpdateShelfStyleUseCase {
    var callCount = 0
    var lastShelfId: String? = null
    var lastNewStyle: ShelfStyle? = null
    var shouldReturnError = false
    var errorToReturn: DataError.Local = DataError.Local.UNKNOWN

    override suspend operator fun invoke(shelfId: String, newStyle: ShelfStyle): Result<Unit, DataError.Local> {
        callCount++
        lastShelfId = shelfId
        lastNewStyle = newStyle
        return if (shouldReturnError) {
            Result.Error(errorToReturn)
        } else {
            Result.Success(Unit)
        }
    }

    fun reset() {
        callCount = 0
        lastShelfId = null
        lastNewStyle = null
        shouldReturnError = false
        errorToReturn = DataError.Local.UNKNOWN
    }
}

class MockDuplicateShelfUseCase : DuplicateShelfUseCase {
    var callCount = 0
    var lastShelfId: String? = null
    var shouldReturnError = false
    var errorToReturn: DataError.Local = DataError.Local.UNKNOWN
    var shelfToReturn: Bookshelf? = null

    override suspend operator fun invoke(shelfId: String): Result<Bookshelf, DataError.Local> {
        callCount++
        lastShelfId = shelfId
        return if (shouldReturnError) {
            Result.Error(errorToReturn)
        } else {
            Result.Success(
                shelfToReturn ?: TestShelfBuilder()
                    .withName("Copy of Test Shelf")
                    .build()
            )
        }
    }

    fun reset() {
        callCount = 0
        lastShelfId = null
        shouldReturnError = false
        errorToReturn = DataError.Local.UNKNOWN
        shelfToReturn = null
    }
}

class MockShareBookshelfUseCase : uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase {
    var callCount = 0
    var lastShelfId: String? = null
    var shouldReturnError = false
    var errorToReturn: DataError.Local = DataError.Local.UNKNOWN

    override suspend operator fun invoke(shelfId: String): Result<Unit, DataError.Local> {
        callCount++
        lastShelfId = shelfId
        return if (shouldReturnError) {
            Result.Error(errorToReturn)
        } else {
            Result.Success(Unit)
        }
    }

    fun reset() {
        callCount = 0
        lastShelfId = null
        shouldReturnError = false
        errorToReturn = DataError.Local.UNKNOWN
    }
}
