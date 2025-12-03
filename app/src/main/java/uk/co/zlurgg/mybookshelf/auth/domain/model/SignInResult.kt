package uk.co.zlurgg.mybookshelf.auth.domain.model

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)
