package uk.co.zlurgg.mybookshelf.auth.presentation.service

import android.app.Activity
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface CredentialFetcher {
    suspend fun fetch(activity: Activity): Result<String, DataError.Local>
}
