package uk.co.zlurgg.mybookshelf.bookclub.data.service

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubCode
import uk.co.zlurgg.mybookshelf.bookclub.domain.service.BookClubCodeGenerator
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.bookclub.data.remote.BookClubRemoteDataSource
import java.security.SecureRandom

/**
 * Implementation of BookClubCodeGenerator that generates 12-character alphanumeric codes.
 *
 * Uses a character set that excludes confusing characters (0/O, 1/I/L) for better readability
 * when codes need to be manually entered.
 */
class BookClubCodeGeneratorImpl(
    private val remoteDataSource: BookClubRemoteDataSource
) : BookClubCodeGenerator {

    override suspend fun generateUniqueCode(): Result<String, DataError.Sync> {
        repeat(MAX_RETRIES) { attempt ->
            val code = generateCode()
            Timber.tag(TAG).d("Generated code attempt %d", attempt + 1)

            val existsResult = remoteDataSource.getBookClubMetadata(code)

            when (existsResult) {
                is Result.Success -> {
                    if (existsResult.data == null) {
                        // Code doesn't exist, we can use it
                        Timber.tag(TAG).d("Code is unique, using it")
                        return Result.Success(code)
                    } else {
                        // Code exists, try again
                        Timber.tag(TAG).w("Code already exists, retrying...")
                    }
                }
                is Result.Error -> {
                    // Network/Firestore error - propagate it
                    Timber.tag(TAG).e("Failed to check code existence: %s", existsResult.error)
                    return Result.Error(existsResult.error)
                }
            }
        }

        Timber.tag(TAG).e("Failed to generate unique code after %d attempts", MAX_RETRIES)
        return Result.Error(DataError.Sync.GENERATION_FAILED)
    }

    private fun generateCode(): String {
        return buildString {
            repeat(BookClubCode.CODE_LENGTH) {
                append(BookClubCode.VALID_CHARS[random.nextInt(BookClubCode.VALID_CHARS.length)])
            }
        }
    }

    companion object {
        private const val TAG = "BookClubCode"
        private const val MAX_RETRIES = 5
        private val random = SecureRandom()
    }
}
