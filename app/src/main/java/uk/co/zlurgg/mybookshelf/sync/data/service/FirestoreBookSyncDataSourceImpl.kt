package uk.co.zlurgg.mybookshelf.sync.data.service

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.mappers.toBookFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.BookSyncDataSource
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.BOOKS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.FIELD_LAST_MODIFIED
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.USERS_COLLECTION

internal class FirestoreBookSyncDataSourceImpl(
    private val firestore: FirebaseFirestore,
) : BookSyncDataSource {

    private val helper = FirestoreOperationHelper("FirestoreBookSync")

    override suspend fun uploadBook(
        userId: String,
        book: BookFirestoreDto,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("uploadBook") {
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
        bookId: String,
    ): Result<BookFirestoreDto?, DataError.Sync> {
        return helper.execute("downloadBook") {
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
        sinceTimestamp: Long,
    ): Result<List<BookFirestoreDto>, DataError.Sync> {
        return helper.execute("downloadBooksSince") {
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
        bookId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("deleteBook") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKS_COLLECTION)
                .document(bookId)
                .delete()
                .await()
        }
    }

    override suspend fun uploadBooks(
        userId: String,
        books: List<BookFirestoreDto>,
    ): Result<Int, DataError.Sync> {
        if (books.isEmpty()) return Result.Success(0)

        return helper.execute("uploadBooks") {
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

    override suspend fun deleteAllBooks(userId: String): Result<Unit, DataError.Sync> {
        return helper.execute("deleteAllBooks") {
            val booksCollection = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(BOOKS_COLLECTION)

            val docs = booksCollection.get().await().documents
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
