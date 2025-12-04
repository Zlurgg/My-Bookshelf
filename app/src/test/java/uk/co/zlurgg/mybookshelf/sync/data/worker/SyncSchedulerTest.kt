package uk.co.zlurgg.mybookshelf.sync.data.worker

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.sync.domain.service.SyncSchedulerService

/**
 * Unit tests for SyncScheduler interface compliance.
 *
 * Note: Full WorkManager integration tests are in androidTest/
 * since work-testing library requires Android runtime.
 */
class SyncSchedulerTest {

    @Test
    fun `SyncScheduler implements SyncSchedulerService interface`() {
        // Verifies that SyncScheduler implements the domain interface
        // This is a compile-time check, but we include a runtime verification
        val scheduler: Class<*> = SyncScheduler::class.java
        val interfaces = scheduler.interfaces

        assertTrue(
            "SyncScheduler should implement SyncSchedulerService",
            interfaces.any { it == SyncSchedulerService::class.java }
        )
    }

    @Test
    fun `SyncWorker WORK_NAME constants are defined`() {
        // Verify constants are defined and not null
        assertNotNull("WORK_NAME should be defined", SyncWorker.WORK_NAME)
        assertNotNull("WORK_NAME_PERIODIC should be defined", SyncWorker.WORK_NAME_PERIODIC)

        // Verify they are distinct
        assertTrue(
            "WORK_NAME and WORK_NAME_PERIODIC should be different",
            SyncWorker.WORK_NAME != SyncWorker.WORK_NAME_PERIODIC
        )
    }

    @Test
    fun `SyncSchedulerService interface has required methods`() {
        // Verify the interface has all required methods
        val interfaceMethods = SyncSchedulerService::class.java.methods.map { it.name }

        assertTrue(
            "schedulePeriodicSync should be in interface",
            interfaceMethods.contains("schedulePeriodicSync")
        )
        assertTrue(
            "triggerImmediateSync should be in interface",
            interfaceMethods.contains("triggerImmediateSync")
        )
        assertTrue(
            "cancelAllSync should be in interface",
            interfaceMethods.contains("cancelAllSync")
        )
    }
}
