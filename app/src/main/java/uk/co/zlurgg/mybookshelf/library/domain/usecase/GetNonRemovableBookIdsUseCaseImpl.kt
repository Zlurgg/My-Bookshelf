package uk.co.zlurgg.mybookshelf.library.domain.usecase

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository

class GetNonRemovableBookIdsUseCaseImpl(
    private val bookRepository: BookRepository,
) : GetNonRemovableBookIdsUseCase {

    override operator fun invoke(): Flow<Set<String>> {
        return bookRepository.getNonRemovableBookIds()
    }
}
