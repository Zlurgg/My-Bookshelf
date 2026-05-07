package uk.co.zlurgg.mybookshelf.bookclub.data.remote

import com.google.firebase.firestore.FirebaseFirestoreException
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

internal class FirestoreOperationHelper(private val tag: String) {

    @Suppress("TooGenericExceptionCaught")
    suspend fun <T> execute(
        operationName: String,
        operation: suspend () -> T,
    ): Result<T, DataError.Sync> {
        return try {
            val result = operation()
            Timber.tag(tag).d("%s: success", operationName)
            Result.Success(result)
        } catch (e: FirebaseFirestoreException) {
            val error = mapFirestoreException(e)
            Timber.tag(tag).e(e, "%s failed: %s", operationName, error)
            Result.Error(error)
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "%s failed with unexpected error: %s", operationName, e.message)
            Result.Error(DataError.Sync.UNKNOWN)
        }
    }

    private fun mapFirestoreException(e: FirebaseFirestoreException): DataError.Sync {
        Timber.tag(tag).e("Firestore error code: %s, message: %s", e.code, e.message)
        return when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.Sync.PERMISSION_DENIED
            FirebaseFirestoreException.Code.NOT_FOUND -> DataError.Sync.DOCUMENT_NOT_FOUND
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> DataError.Sync.QUOTA_EXCEEDED
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> DataError.Sync.NOT_SIGNED_IN
            FirebaseFirestoreException.Code.UNAVAILABLE -> DataError.Sync.NETWORK_ERROR
            FirebaseFirestoreException.Code.ABORTED -> DataError.Sync.CONFLICT_UNRESOLVED
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                Timber.tag(tag).e("FAILED_PRECONDITION - likely missing index. Check message above for creation URL")
                DataError.Sync.UNKNOWN
            }
            else -> DataError.Sync.UNKNOWN
        }
    }
}
