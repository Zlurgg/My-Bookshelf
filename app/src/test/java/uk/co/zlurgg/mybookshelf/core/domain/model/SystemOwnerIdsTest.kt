package uk.co.zlurgg.mybookshelf.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemOwnerIdsTest {

    @Test
    fun `TUTORIAL constant has expected value`() {
        assertEquals("__system_tutorial__", SystemOwnerIds.TUTORIAL)
    }

    @Test
    fun `TUTORIAL_SHELF_ID constant has expected value`() {
        assertEquals("shelf-tutorial", SystemOwnerIds.TUTORIAL_SHELF_ID)
    }

    @Test
    fun `isSystemOwner returns true for TUTORIAL owner`() {
        assertTrue(SystemOwnerIds.isSystemOwner(SystemOwnerIds.TUTORIAL))
    }

    @Test
    fun `isSystemOwner returns true for tutorial string literal`() {
        assertTrue(SystemOwnerIds.isSystemOwner("__system_tutorial__"))
    }

    @Test
    fun `isSystemOwner returns false for null`() {
        assertFalse(SystemOwnerIds.isSystemOwner(null))
    }

    @Test
    fun `isSystemOwner returns false for regular user ID`() {
        assertFalse(SystemOwnerIds.isSystemOwner("firebase-user-123"))
    }

    @Test
    fun `isSystemOwner returns false for empty string`() {
        assertFalse(SystemOwnerIds.isSystemOwner(""))
    }

    @Test
    fun `isSystemOwner returns false for similar but different string`() {
        assertFalse(SystemOwnerIds.isSystemOwner("__system_tutorial"))  // Missing trailing underscore
        assertFalse(SystemOwnerIds.isSystemOwner("system_tutorial"))    // Missing double underscores
        assertFalse(SystemOwnerIds.isSystemOwner("__SYSTEM_TUTORIAL__")) // Wrong case
    }
}
