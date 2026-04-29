package uk.co.zlurgg.mybookshelf.auth.domain.usecase

import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData

interface GetSignedInUserUseCase {
    operator fun invoke(): UserData?
}
