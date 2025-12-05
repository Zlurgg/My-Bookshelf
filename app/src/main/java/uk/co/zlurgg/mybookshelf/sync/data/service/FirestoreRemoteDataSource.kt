package uk.co.zlurgg.mybookshelf.sync.data.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto
import uk.co.zlurgg.mybookshelf.sync.data.mapper.toBookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.mapper.toBookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.mapper.toSharedShelfDto

/**
 * Firestore implementation of RemoteSyncDataSource.
 *
 * Works directly with DTOs (no domain model conversions needed).
 *
 * Document structure:
 * - /users/{userId}/books/{bookId}
 * - /users/{userId}/bookshelves/{shelfId}
 * - /sharedShelves/{shareCode}
 */
class FirestoreRemoteDataSource(
    private val firestore: FirebaseFirestore
) : RemoteSyncDataSource {

    // ==================== Books ====================

    override suspend fun uploadBook(
        userId: String,
        book: BookFirestoreDto
    ): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("uploadBook") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKS_COLLECTION)
                .document(book.id)
                .set(book)
                .await()
        }
    }

    override suspend fun downloadBook(
        userId: String,
        bookId: String
    ): Result<BookFirestoreDto?, DataError.Sync> {
        return executeFirestoreOperation("downloadBook") {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKS_COLLECTION)
                .document(bookId)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.data?.toBookFirestoreDto(snapshot.id)
            } else {
                null
            }
        }
    }

    override suspend fun downloadBooksSince(
        userId: String,
        sinceTimestamp: Long
    ): Result<List<BookFirestoreDto>, DataError.Sync> {
        return executeFirestoreOperation("downloadBooksSince") {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKS_COLLECTION)
                .whereGreaterThan(FIELD_LAST_MODIFIED, sinceTimestamp)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.toBookFirestoreDto(doc.id)
            }
        }
    }

    override suspend fun deleteBook(
        userId: String,
        bookId: String
    ): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("deleteBook") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKS_COLLECTION)
                .document(bookId)
                .delete()
                .await()
        }
    }

    // ==================== Bookshelves ====================

    override suspend fun uploadBookshelf(
        userId: String,
        shelf: BookshelfFirestoreDto
    ): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("uploadBookshelf") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKSHELVES_COLLECTION)
                .document(shelf.id)
                .set(shelf)
                .await()
        }
    }

    override suspend fun downloadBookshelf(
        userId: String,
        shelfId: String
    ): Result<BookshelfFirestoreDto?, DataError.Sync> {
        return executeFirestoreOperation("downloadBookshelf") {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKSHELVES_COLLECTION)
                .document(shelfId)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.data?.toBookshelfFirestoreDto(snapshot.id)
            } else {
                null
            }
        }
    }

    override suspend fun downloadBookshelvesSince(
        userId: String,
        sinceTimestamp: Long
    ): Result<List<BookshelfFirestoreDto>, DataError.Sync> {
        return executeFirestoreOperation("downloadBookshelvesSince") {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKSHELVES_COLLECTION)
                .whereGreaterThan(FIELD_LAST_MODIFIED, sinceTimestamp)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.toBookshelfFirestoreDto(doc.id)
            }
        }
    }

    override suspend fun deleteBookshelf(
        userId: String,
        shelfId: String
    ): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("deleteBookshelf") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKSHELVES_COLLECTION)
                .document(shelfId)
                .delete()
                .await()
        }
    }

    // ==================== Shared Shelves ====================

    override suspend fun shareShelf(sharedShelf: SharedShelfDto): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("shareShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(sharedShelf.shareCode)
                .set(sharedShelf)
                .await()
        }
    }

    override suspend fun unshareShelf(shareCode: String): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("unshareShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(shareCode)
                .delete()
                .await()
        }
    }

    override suspend fun getSharedShelf(shareCode: String): Result<SharedShelfDto?, DataError.Sync> {
        return executeFirestoreOperation("getSharedShelf") {
            val snapshot = firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(shareCode)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.data?.toSharedShelfDto(snapshot.id)
            } else {
                null
            }
        }
    }

    override suspend fun subscribeToShelf(
        shareCode: String,
        userId: String
    ): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("subscribeToShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(shareCode)
                .update(FIELD_SUBSCRIBER_IDS, FieldValue.arrayUnion(userId))
                .await()
        }
    }

    override suspend fun unsubscribeFromShelf(
        shareCode: String,
        userId: String
    ): Result<Unit, DataError.Sync> {
        return executeFirestoreOperation("unsubscribeFromShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(shareCode)
                .update(FIELD_SUBSCRIBER_IDS, FieldValue.arrayRemove(userId))
                .await()
        }
    }

    // ==================== Batch Operations ====================

    override suspend fun uploadBooks(
        userId: String,
        books: List<BookFirestoreDto>
    ): Result<Int, DataError.Sync> {
        if (books.isEmpty()) return Result.Success(0)

        return executeFirestoreOperation("uploadBooks") {
            val batch = firestore.batch()
            val booksCollection = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKS_COLLECTION)

            books.forEach { book ->
                val docRef = booksCollection.document(book.id)
                batch.set(docRef, book)
            }

            batch.commit().await()
            books.size
        }
    }

    override suspend fun uploadBookshelves(
        userId: String,
        shelves: List<BookshelfFirestoreDto>
    ): Result<Int, DataError.Sync> {
        if (shelves.isEmpty()) return Result.Success(0)

        return executeFirestoreOperation("uploadBookshelves") {
            val batch = firestore.batch()
            val shelvesCollection = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKSHELVES_COLLECTION)

            shelves.forEach { shelf ->
                val docRef = shelvesCollection.document(shelf.id)
                batch.set(docRef, shelf)
            }

            batch.commit().await()
            shelves.size
        }
    }

    // ==================== Helper Methods ====================

    private suspend fun <T> executeFirestoreOperation(
        operationName: String,
        operation: suspend () -> T
    ): Result<T, DataError.Sync> {
        return try {
            val result = operation()
            Timber.tag(TAG).d("%s: success", operationName)
            Result.Success(result)
        } catch (e: FirebaseFirestoreException) {
            val error = mapFirestoreException(e)
            Timber.tag(TAG).e(e, "%s failed: %s", operationName, error)
            Result.Error(error)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "%s failed with unexpected error", operationName)
            Result.Error(DataError.Sync.UNKNOWN)
        }
    }

    private fun mapFirestoreException(e: FirebaseFirestoreException): DataError.Sync {
        return when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.Sync.PERMISSION_DENIED
            FirebaseFirestoreException.Code.NOT_FOUND -> DataError.Sync.DOCUMENT_NOT_FOUND
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> DataError.Sync.QUOTA_EXCEEDED
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> DataError.Sync.NOT_SIGNED_IN
            FirebaseFirestoreException.Code.UNAVAILABLE -> DataError.Sync.NETWORK_ERROR
            FirebaseFirestoreException.Code.ABORTED -> DataError.Sync.CONFLICT_UNRESOLVED
            else -> DataError.Sync.UNKNOWN
        }
    }

    companion object {
        private const val TAG = "FirestoreSync"

        // Collection names
        private const val USERS_COLLECTION = "users"
        private const val BOOKS_COLLECTION = "books"
        private const val BOOKSHELVES_COLLECTION = "bookshelves"
        private const val SHARED_SHELVES_COLLECTION = "sharedShelves"

        // Field names
        private const val FIELD_LAST_MODIFIED = "last_modified_at"
        private const val FIELD_SUBSCRIBER_IDS = "subscriber_ids"
    }
}
