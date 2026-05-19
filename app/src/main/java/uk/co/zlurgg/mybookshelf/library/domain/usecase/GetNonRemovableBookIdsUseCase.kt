package uk.co.zlurgg.mybookshelf.library.domain.usecase

import kotlinx.coroutines.flow.Flow

interface GetNonRemovableBookIdsUseCase {
    operator fun invoke(): Flow<Set<String>>
}
