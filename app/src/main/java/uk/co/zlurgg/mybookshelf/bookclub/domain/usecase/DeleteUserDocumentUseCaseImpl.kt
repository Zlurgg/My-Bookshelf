package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.data.remote.BookClubRemoteDataSource
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class DeleteUserDocumentUseCaseImpl(
    private val remoteDataSource: BookClubRemoteDataSource
) : DeleteUserDocumentUseCase {
    override suspend fun invoke(userId: String): Result<Unit, DataError.Sync> {
        return remoteDataSource.deleteUserDocument(userId)
    }
}
