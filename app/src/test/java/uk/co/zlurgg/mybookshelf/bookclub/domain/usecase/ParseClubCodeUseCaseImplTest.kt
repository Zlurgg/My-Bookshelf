package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ParseClubCodeUseCaseImplTest {

    private val useCase = ParseClubCodeUseCaseImpl()

    // ========== Valid Code Tests ==========

    @Test
    fun `returns success for valid 12-char code`() {
        val result = useCase("ABCD2345EFGH")

        assertTrue(result is Result.Success)
        assertEquals("ABCD2345EFGH", (result as Result.Success).data)
    }

    @Test
    fun `returns success for valid code with all allowed characters`() {
        val result = useCase("HJKMNPQR2345")

        assertTrue(result is Result.Success)
        assertEquals("HJKMNPQR2345", (result as Result.Success).data)
    }

    @Test
    fun `converts lowercase input to uppercase`() {
        val result = useCase("abcd2345efgh")

        assertTrue(result is Result.Success)
        assertEquals("ABCD2345EFGH", (result as Result.Success).data)
    }

    @Test
    fun `trims whitespace from input`() {
        val result = useCase("  ABCD2345EFGH  ")

        assertTrue(result is Result.Success)
        assertEquals("ABCD2345EFGH", (result as Result.Success).data)
    }

    // ========== Invalid Code Tests ==========

    @Test
    fun `returns error for empty input`() {
        val result = useCase("")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for blank input`() {
        val result = useCase("   ")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code with excluded character O`() {
        val result = useCase("ABCDO2345FGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code with excluded character I`() {
        val result = useCase("ABCDI2345FGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code with excluded character L`() {
        val result = useCase("ABCDL2345FGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code with excluded digit 0`() {
        val result = useCase("ABCD02345FGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code with excluded digit 1`() {
        val result = useCase("ABCD12345FGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code too short`() {
        val result = useCase("ABCD2345EFG")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code too long`() {
        val result = useCase("ABCD2345EFGHJ")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `returns error for code with special characters`() {
        val result = useCase("ABCD-2345FGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `rejects old 8-char codes`() {
        val result = useCase("ABCD2345")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `rejects URL input as invalid code`() {
        val result = useCase("https://zlurgg.github.io/My-Bookshelf/club/ABCD2345EFGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }

    @Test
    fun `rejects app deeplink as invalid code`() {
        val result = useCase("mybookshelf://club/ABCD2345EFGH")

        assertTrue(result is Result.Error)
        assertEquals(DataError.Validation.INVALID_CLUB_CODE, (result as Result.Error).error)
    }
}
