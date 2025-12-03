package uk.co.zlurgg.mybookshelf.auth.domain.repository

import android.content.Context
import uk.co.zlurgg.mybookshelf.auth.domain.model.SignInResult
import uk.co.zlurgg.mybookshelf.auth.domain.model.UserData

interface AuthRepository {
    suspend fun signIn(context: Context): SignInResult
    suspend fun signOut()
    fun getSignedInUser(): UserData?
}
