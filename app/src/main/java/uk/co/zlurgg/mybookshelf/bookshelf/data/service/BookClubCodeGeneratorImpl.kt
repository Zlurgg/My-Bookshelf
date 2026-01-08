package uk.co.zlurgg.mybookshelf.bookshelf.data.service

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookClubCodeGenerator
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.repository.RemoteSyncDataSource
import kotlin.random.Random

/**
 * Implementation of BookClubCodeGenerator that generates 8-character alphanumeric codes.
 *
 * Uses a character set that excludes confusing characters (0/O, 1/I/L) for better readability
 * when codes need to be manually entered.
 */
class BookClubCodeGeneratorImpl(
    private val remoteDataSource: RemoteSyncDataSource,
) : BookClubCodeGenerator {
    override suspend fun generateUniqueCode(): Result<String, DataError.Sync> {
        repeat(MAX_RETRIES) { attempt ->
            val code = generateCode()
            Timber.tag(TAG).d("Generated code attempt %d: %s", attempt + 1, code)

            val existsResult = remoteDataSource.getBookClubMetadata(code)

            when (existsResult) {
                is Result.Success -> {
                    if (existsResult.data == null) {
                        // Code doesn't exist, we can use it
                        Timber.tag(TAG).d("Code %s is unique, using it", code)
                        return Result.Success(code)
                    } else {
                        // Code exists, try again
                        Timber.tag(TAG).w("Code %s already exists, retrying...", code)
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
            repeat(CODE_LENGTH) {
                append(ALLOWED_CHARS[Random.nextInt(ALLOWED_CHARS.length)])
            }
        }
    }

    companion object {
        private const val TAG = "BookClubCode"
        private const val CODE_LENGTH = 8
        private const val MAX_RETRIES = 5

        // Excludes confusing characters: 0/O, 1/I/L
        private const val ALLOWED_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}
