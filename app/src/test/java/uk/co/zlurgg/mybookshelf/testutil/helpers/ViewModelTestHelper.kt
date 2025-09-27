package uk.co.zlurgg.mybookshelf.testutil.helpers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/**
 * Helper utilities for ViewModel testing.
 * Provides common patterns for StateFlow testing and async operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTestHelper<T>(
    private val testScope: TestScope,
    private val stateFlow: StateFlow<T>
) {
    private var currentState: T? = null
    private var stateCollectorJob = testScope.launch {
        stateFlow.collect { currentState = it }
    }

    /**
     * Gets the current state value after ensuring all pending coroutines complete.
     */
    suspend fun getCurrentState(): T? {
        testScope.advanceUntilIdle()
        return currentState
    }

    /**
     * Executes an action and returns the resulting state.
     */
    suspend fun executeAndGetState(action: suspend () -> Unit): T? {
        action()
        return getCurrentState()
    }

    /**
     * Waits for the state to be collected and returns it.
     */
    suspend fun awaitState(): T? {
        testScope.advanceUntilIdle()
        return currentState
    }

    /**
     * Cleanup method to cancel the state collection job.
     * Call this at the end of each test.
     */
    fun cleanup() {
        stateCollectorJob.cancel()
    }
}

/**
 * Extension function to create a ViewModelTestHelper for any StateFlow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> StateFlow<T>.testHelper(testScope: TestScope): ViewModelTestHelper<T> {
    return ViewModelTestHelper(testScope, this)
}

/**
 * Extension function to execute an action and assert the resulting state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend inline fun <T> ViewModelTestHelper<T>.executeAndAssert(
    noinline action: suspend () -> Unit,
    assertion: (T?) -> Unit
) {
    val state = executeAndGetState(action)
    assertion(state)
}