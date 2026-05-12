package uk.co.zlurgg.mybookshelf.library.domain.usecase

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.Book

interface GetAllLibraryBooksUseCase {
    operator fun invoke(): Flow<List<Book>>
}
