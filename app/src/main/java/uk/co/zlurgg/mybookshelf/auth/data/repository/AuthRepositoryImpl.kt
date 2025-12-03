package uk.co.zlurgg.mybookshelf.auth.data.repository

import android.content.Context
import uk.co.zlurgg.mybookshelf.auth.data.service.GoogleAuthUiClient
import uk.co.zlurgg.mybookshelf.auth.domain.model.SignInResult
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData
import uk.co.zlurgg.mybookshelf.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    context: Context
) : AuthRepository {

    private val googleAuthUiClient = GoogleAuthUiClient(context)

    override suspend fun signIn(context: Context): SignInResult {
        return googleAuthUiClient.signIn(context)
    }

    override suspend fun signOut() {
        googleAuthUiClient.signOut()
    }

    override fun getSignedInUser(): UserData? {
        return googleAuthUiClient.getSignedInUser()
    }
}
