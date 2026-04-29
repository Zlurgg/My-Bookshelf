package uk.co.zlurgg.mybookshelf.sync.data.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto
import uk.co.zlurgg.mybookshelf.sync.data.mappers.toBookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.mappers.toSharedShelfDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.ShelfSyncDataSource
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.BOOKSHELVES_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.FIELD_LAST_MODIFIED
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.FIELD_SUBSCRIBER_IDS
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.SHARED_SHELVES_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.USERS_COLLECTION

internal class FirestoreShelfSyncDataSourceImpl(
    private val firestore: FirebaseFirestore,
) : ShelfSyncDataSource {

    private val helper = FirestoreOperationHelper("FirestoreShelfSync")

    override suspend fun uploadBookshelf(
        userId: String,
        shelf: BookshelfFirestoreDto,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("uploadBookshelf") {
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
        shelfId: String,
    ): Result<BookshelfFirestoreDto?, DataError.Sync> {
        return helper.execute("downloadBookshelf") {
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
        sinceTimestamp: Long,
    ): Result<List<BookshelfFirestoreDto>, DataError.Sync> {
        return helper.execute("downloadBookshelvesSince") {
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
        shelfId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("deleteBookshelf") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKSHELVES_COLLECTION)
                .document(shelfId)
                .delete()
                .await()
        }
    }

    override suspend fun uploadBookshelves(
        userId: String,
        shelves: List<BookshelfFirestoreDto>,
    ): Result<Int, DataError.Sync> {
        if (shelves.isEmpty()) return Result.Success(0)

        return helper.execute("uploadBookshelves") {
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

    override suspend fun shareShelf(sharedShelf: SharedShelfDto): Result<Unit, DataError.Sync> {
        return helper.execute("shareShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(sharedShelf.shareCode)
                .set(sharedShelf)
                .await()
        }
    }

    override suspend fun unshareShelf(shareCode: String): Result<Unit, DataError.Sync> {
        return helper.execute("unshareShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(shareCode)
                .delete()
                .await()
        }
    }

    override suspend fun getSharedShelf(shareCode: String): Result<SharedShelfDto?, DataError.Sync> {
        return helper.execute("getSharedShelf") {
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
        userId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("subscribeToShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(shareCode)
                .update(FIELD_SUBSCRIBER_IDS, FieldValue.arrayUnion(userId))
                .await()
        }
    }

    override suspend fun unsubscribeFromShelf(
        shareCode: String,
        userId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("unsubscribeFromShelf") {
            firestore.collection(SHARED_SHELVES_COLLECTION)
                .document(shareCode)
                .update(FIELD_SUBSCRIBER_IDS, FieldValue.arrayRemove(userId))
                .await()
        }
    }

    override suspend fun deleteAllBookshelves(userId: String): Result<Unit, DataError.Sync> {
        return helper.execute("deleteAllBookshelves") {
            val shelvesCollection = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKSHELVES_COLLECTION)

            val docs = shelvesCollection.get().await().documents
            docs.chunked(BATCH_LIMIT).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
        }
    }

    companion object {
        private const val BATCH_LIMIT = 500
    }
}
