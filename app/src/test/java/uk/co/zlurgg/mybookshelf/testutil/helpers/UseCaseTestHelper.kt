package uk.co.zlurgg.mybookshelf.testutil.helpers

import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.testutil.mocks.*

/**
 * Helper for creating configured UseCase collections for testing.
 * Provides common combinations and easy configuration.
 */
class UseCaseTestHelper {

    private val mockGetAllShelves = MockGetAllShelvesUseCase()
    private val mockCreateShelf = MockCreateShelfUseCase()
    private val mockDeleteShelf = MockDeleteShelfUseCase()
    private val mockReorderShelves = MockReorderShelvesUseCase()
    private val mockGetShelfById = MockGetShelfByIdUseCase()

    /**
     * Creates BookcaseUseCases with all mocks configured.
     */
    fun createBookcaseUseCases(): BookcaseUseCases {
        return BookcaseUseCases(
            getAllShelves = mockGetAllShelves,
            createShelf = mockCreateShelf,
            deleteShelf = mockDeleteShelf,
            reorderShelves = mockReorderShelves,
            getShelfById = mockGetShelfById
        )
    }

    /**
     * Access to individual mocks for configuration in tests.
     */
    fun getAllShelvesUseCase() = mockGetAllShelves
    fun createShelfUseCase() = mockCreateShelf
    fun deleteShelfUseCase() = mockDeleteShelf
    fun reorderShelvesUseCase() = mockReorderShelves
    fun getShelfByIdUseCase() = mockGetShelfById

    /**
     * Resets all mocks to their default state.
     */
    fun resetAll() {
        mockCreateShelf.reset()
        mockDeleteShelf.reset()
        mockReorderShelves.reset()
        mockGetShelfById.reset()
    }

    companion object {
        /**
         * Creates minimal UseCases for basic UI testing where business logic is not the focus.
         */
        fun createMinimalBookcaseUseCases(): BookcaseUseCases {
            return UseCaseTestHelper().createBookcaseUseCases()
        }
    }
}