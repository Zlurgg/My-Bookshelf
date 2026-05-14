package uk.co.zlurgg.mybookshelf.bookdetail.presentation.util

import uk.co.zlurgg.mybookshelf.book.domain.model.BookComment
import uk.co.zlurgg.mybookshelf.book.domain.model.BookReview

val BookComment.isEdited: Boolean get() = updatedAt > createdAt

val BookReview.isEdited: Boolean get() = updatedAt > createdAt
