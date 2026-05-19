package uk.co.zlurgg.mybookshelf.library.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCase

data class LibraryUseCases(
    val getAllLibraryBooks: GetAllLibraryBooksUseCase,
    val searchBooks: SearchBooksUseCase,
    val upsertBook: UpsertBookUseCase,
    val deleteBooks: DeleteBooksFromLibraryUseCase,
    val getNonRemovableBookIds: GetNonRemovableBookIdsUseCase,
)
