package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface GetOrCreateTutorialShelfUseCase {
    suspend fun execute(): Result<String, DataError.Local>
}