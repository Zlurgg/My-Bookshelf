package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface GetOrCreateTutorialShelfUseCase {
    suspend operator fun invoke(): Result<String, DataError.Local>
}
