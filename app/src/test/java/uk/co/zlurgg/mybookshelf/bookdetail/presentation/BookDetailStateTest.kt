package uk.co.zlurgg.mybookshelf.bookdetail.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailStateTest {

    private fun stateWith(
        isBookClub: Boolean = false,
        currentUserId: String? = null,
        clubCreatorId: String? = null,
        addedByUserId: String? = null,
    ) = BookDetailState(
        isBookClub = isBookClub,
        currentUserId = currentUserId,
        clubCreatorId = clubCreatorId,
        addedByUserId = addedByUserId,
    )

    @Test
    fun `canRemoveFromShelf is true for personal shelf`() {
        val state = stateWith(isBookClub = false)
        assertTrue(state.canRemoveFromShelf)
    }

    @Test
    fun `canRemoveFromShelf is true for club owner viewing any book`() {
        val state = stateWith(
            isBookClub = true,
            currentUserId = "owner-1",
            clubCreatorId = "owner-1",
            addedByUserId = "other-member",
        )
        assertTrue(state.canRemoveFromShelf)
    }

    @Test
    fun `canRemoveFromShelf is true for member viewing own addition`() {
        val state = stateWith(
            isBookClub = true,
            currentUserId = "member-1",
            clubCreatorId = "owner-1",
            addedByUserId = "member-1",
        )
        assertTrue(state.canRemoveFromShelf)
    }

    @Test
    fun `canRemoveFromShelf is false for member viewing another members book`() {
        val state = stateWith(
            isBookClub = true,
            currentUserId = "member-1",
            clubCreatorId = "owner-1",
            addedByUserId = "member-2",
        )
        assertFalse(state.canRemoveFromShelf)
    }

    @Test
    fun `canRemoveFromShelf is false for guest on club shelf`() {
        val state = stateWith(
            isBookClub = true,
            currentUserId = null,
            clubCreatorId = "owner-1",
            addedByUserId = "member-1",
        )
        assertFalse(state.canRemoveFromShelf)
    }

    @Test
    fun `canRemoveFromShelf is false when addedByUserId is null on club shelf`() {
        val state = stateWith(
            isBookClub = true,
            currentUserId = "member-1",
            clubCreatorId = "owner-1",
            addedByUserId = null,
        )
        assertFalse(state.canRemoveFromShelf)
    }
}
