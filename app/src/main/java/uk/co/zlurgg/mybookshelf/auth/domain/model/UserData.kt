package uk.co.zlurgg.mybookshelf.auth.domain.model

data class UserData(
    val userId: String,
    val username: String?,
    val profilePictureUrl: String?,
)
