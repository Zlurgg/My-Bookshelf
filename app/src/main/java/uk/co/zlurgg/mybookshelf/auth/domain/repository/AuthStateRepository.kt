package uk.co.zlurgg.mybookshelf.auth.domain.repository

interface AuthStateRepository {
    suspend fun isSignedIn(): Boolean
    suspend fun setSignedInState(isSignedIn: Boolean)
}
