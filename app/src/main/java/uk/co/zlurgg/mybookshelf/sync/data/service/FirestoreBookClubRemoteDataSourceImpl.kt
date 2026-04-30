package uk.co.zlurgg.mybookshelf.sync.data.service

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubBookDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubCommentDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMemberDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubMetadataDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookClubReviewDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.BookClubRemoteDataSource
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.BOOK_CLUBS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.CLUB_BOOKS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.COMMENTS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.FIELD_CLUB_MEMBERSHIPS
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.MEMBERS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.PREFERENCES_DOCUMENT
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.REVIEWS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.SETTINGS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.USERS_COLLECTION

// Implements BookClubRemoteDataSource: CRUD, membership, books, reviews, comments, and account deletion
@Suppress("TooManyFunctions")
internal class FirestoreBookClubRemoteDataSourceImpl(
    private val firestore: FirebaseFirestore,
) : BookClubRemoteDataSource {

    private val helper = FirestoreOperationHelper("FirestoreBookClub")

    override suspend fun createBookClub(
        code: String,
        metadata: BookClubMetadataDto,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("createBookClub") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .set(metadata)
                .await()
        }
    }

    override suspend fun getBookClubMetadata(code: String): Result<BookClubMetadataDto?, DataError.Sync> {
        return helper.execute("getBookClubMetadata") {
            val snapshot = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.toObject(BookClubMetadataDto::class.java)
            } else {
                null
            }
        }
    }

    override suspend fun addBookClubMember(
        code: String,
        member: BookClubMemberDto,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("addBookClubMember") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .collection(MEMBERS_COLLECTION)
                .document(member.userId)
                .set(member)
                .await()
        }
    }

    override suspend fun removeBookClubMember(
        code: String,
        userId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("removeBookClubMember") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .collection(MEMBERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
        }
    }

    override suspend fun getBookClubMembers(code: String): Result<List<BookClubMemberDto>, DataError.Sync> {
        return helper.execute("getBookClubMembers") {
            val snapshot = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .collection(MEMBERS_COLLECTION)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(BookClubMemberDto::class.java)
            }
        }
    }

    override suspend fun isMember(code: String, userId: String): Result<Boolean, DataError.Sync> {
        return helper.execute("isMember") {
            val doc = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .collection(MEMBERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            doc.exists()
        }
    }

    override suspend fun addBookToClub(
        code: String,
        book: BookClubBookDto,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("addBookToClub") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(book.id)
                .set(book)
                .await()
        }
    }

    override suspend fun removeBookFromClub(
        code: String,
        bookId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("removeBookFromClub") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .delete()
                .await()
        }
    }

    override suspend fun getClubBooks(code: String): Result<List<BookClubBookDto>, DataError.Sync> {
        return helper.execute("getClubBooks") {
            val snapshot = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .collection(CLUB_BOOKS_COLLECTION)
                .get(Source.SERVER)
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(BookClubBookDto::class.java)
            }
        }
    }

    override suspend fun updateBookClubCounts(
        code: String,
        bookCount: Int,
        memberCount: Int,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("updateBookClubCounts") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .update(
                    mapOf(
                        "book_count" to bookCount,
                        "member_count" to memberCount
                    )
                )
                .await()
        }
    }

    override suspend fun updateBookClubName(
        code: String,
        name: String,
        lastModifiedAt: Long,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("updateBookClubName") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .update(
                    mapOf(
                        "name" to name,
                        "last_modified_at" to lastModifiedAt
                    )
                )
                .await()
        }
    }

    override suspend fun updateBookClubStyle(
        code: String,
        style: String,
        lastModifiedAt: Long,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("updateBookClubStyle") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(code)
                .update(
                    mapOf(
                        "shelf_style" to style,
                        "last_modified_at" to lastModifiedAt
                    )
                )
                .await()
        }
    }

    override suspend fun deleteBookClub(code: String): Result<Unit, DataError.Sync> {
        return helper.execute("deleteBookClub") {
            val clubRef = firestore.collection(BOOK_CLUBS_COLLECTION).document(code)

            val members = clubRef.collection(MEMBERS_COLLECTION).get().await()
            for (doc in members.documents) {
                doc.reference.delete().await()
            }

            val books = clubRef.collection(CLUB_BOOKS_COLLECTION).get().await()
            for (doc in books.documents) {
                doc.reference.delete().await()
            }

            clubRef.delete().await()
        }
    }

    override suspend fun addClubMembership(userId: String, clubCode: String): Result<Unit, DataError.Sync> {
        return helper.execute("addClubMembership") {
            val data = mapOf(FIELD_CLUB_MEMBERSHIPS to FieldValue.arrayUnion(clubCode))
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document(PREFERENCES_DOCUMENT)
                .set(data, SetOptions.merge())
                .await()
        }
    }

    override suspend fun removeClubMembership(userId: String, clubCode: String): Result<Unit, DataError.Sync> {
        return helper.execute("removeClubMembership") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document(PREFERENCES_DOCUMENT)
                .update(FIELD_CLUB_MEMBERSHIPS, FieldValue.arrayRemove(clubCode))
                .await()
        }
    }

    override suspend fun getBookReviews(
        clubCode: String,
        bookId: String,
    ): Result<List<BookClubReviewDto>, DataError.Sync> {
        return helper.execute("getBookReviews") {
            val snapshot = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .collection(REVIEWS_COLLECTION)
                .get(Source.SERVER)
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(BookClubReviewDto::class.java)
            }
        }
    }

    override suspend fun upsertBookReview(
        clubCode: String,
        bookId: String,
        review: BookClubReviewDto,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("upsertBookReview") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .collection(REVIEWS_COLLECTION)
                .document(review.userId)
                .set(review)
                .await()
        }
    }

    override suspend fun deleteBookReview(
        clubCode: String,
        bookId: String,
        userId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("deleteBookReview") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .collection(REVIEWS_COLLECTION)
                .document(userId)
                .delete()
                .await()
        }
    }

    override suspend fun getBookComments(
        clubCode: String,
        bookId: String,
    ): Result<List<BookClubCommentDto>, DataError.Sync> {
        return helper.execute("getBookComments") {
            val snapshot = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .collection(COMMENTS_COLLECTION)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get(Source.SERVER)
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(BookClubCommentDto::class.java)
            }
        }
    }

    override suspend fun addBookComment(
        clubCode: String,
        bookId: String,
        comment: BookClubCommentDto,
    ): Result<String, DataError.Sync> {
        return helper.execute("addBookComment") {
            val docRef = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .collection(COMMENTS_COLLECTION)
                .add(BookClubCommentDto.toFirestoreMap(comment.toDomain()))
                .await()

            docRef.id
        }
    }

    override suspend fun editBookComment(
        clubCode: String,
        bookId: String,
        commentId: String,
        newText: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("editBookComment") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .collection(COMMENTS_COLLECTION)
                .document(commentId)
                .update(
                    mapOf(
                        "text" to newText,
                        "updated_at" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
        }
    }

    override suspend fun deleteBookComment(
        clubCode: String,
        bookId: String,
        commentId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("deleteBookComment") {
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .document(bookId)
                .collection(COMMENTS_COLLECTION)
                .document(commentId)
                .delete()
                .await()
        }
    }

    override suspend fun getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync> {
        return helper.execute("getClubsCreatedByUser") {
            val snapshot = firestore.collection(BOOK_CLUBS_COLLECTION)
                .whereEqualTo("created_by", userId)
                .get()
                .await()

            snapshot.documents.map { it.id }
        }
    }

    override suspend fun getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync> {
        return helper.execute("getClubMembershipsForUser") {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document(PREFERENCES_DOCUMENT)
                .get()
                .await()

            if (snapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                val memberships = snapshot.get(FIELD_CLUB_MEMBERSHIPS) as? List<String>
                memberships.orEmpty()
            } else {
                emptyList()
            }
        }
    }

    override suspend fun removeUserFromClub(
        clubCode: String,
        userId: String,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("removeUserFromClub") {
            // Remove member doc
            firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(MEMBERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            // Remove user's reviews across all books in the club
            val books = firestore.collection(BOOK_CLUBS_COLLECTION)
                .document(clubCode)
                .collection(CLUB_BOOKS_COLLECTION)
                .get()
                .await()

            for (bookDoc in books.documents) {
                bookDoc.reference
                    .collection(REVIEWS_COLLECTION)
                    .document(userId)
                    .delete()
                    .await()
            }
            // Reviews are keyed by userId (direct delete). Comments use auto-generated IDs
            // and are intentionally kept — removing them creates gaps in discussions.
            // The user's attribution remains but the account is gone.

            // Remove club from user's membership list using merge for idempotency
            val data = mapOf(FIELD_CLUB_MEMBERSHIPS to FieldValue.arrayRemove(clubCode))
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document(PREFERENCES_DOCUMENT)
                .set(data, SetOptions.merge())
                .await()
        }
    }
}
