package uk.co.zlurgg.mybookshelf.bookclub.data.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubComment
import java.util.Date

/**
 * Firestore DTO for book club comments.
 * Uses auto-generated document IDs (via .add()) to allow multiple comments per user.
 */
data class BookClubCommentDto(
    @DocumentId
    val id: String = "",

    @get:PropertyName("book_id") @set:PropertyName("book_id")
    var bookId: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    @get:PropertyName("display_name") @set:PropertyName("display_name")
    var displayName: String = "",

    val text: String = "",

    @ServerTimestamp @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: Date? = null,

    @ServerTimestamp @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    var updatedAt: Date? = null
) {
    fun toDomain(): BookClubComment = BookClubComment(
        id = id,
        bookId = bookId,
        userId = userId,
        displayName = displayName,
        text = text,
        createdAt = createdAt?.time ?: 0L,
        updatedAt = updatedAt?.time ?: 0L
    )

    companion object {
        fun fromDomain(comment: BookClubComment): BookClubCommentDto = BookClubCommentDto(
            id = comment.id,
            bookId = comment.bookId,
            userId = comment.userId,
            displayName = comment.displayName,
            text = comment.text,
            createdAt = if (comment.createdAt > 0) Date(comment.createdAt) else null,
            updatedAt = if (comment.updatedAt > 0) Date(comment.updatedAt) else null
        )

        /**
         * Creates a map for Firestore .add() operation (excludes id as it's auto-generated).
         */
        fun toFirestoreMap(comment: BookClubComment): Map<String, Any?> = mapOf(
            "book_id" to comment.bookId,
            "user_id" to comment.userId,
            "display_name" to comment.displayName,
            "text" to comment.text,
            "created_at" to Timestamp.now(),
            "updated_at" to Timestamp.now()
        )
    }
}
