package uk.co.zlurgg.mybookshelf.sync.data.service

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.UserPreferencesFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.repository.UserPreferencesDataSource
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.PREFERENCES_DOCUMENT
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.SETTINGS_COLLECTION
import uk.co.zlurgg.mybookshelf.sync.data.service.FirestoreCollections.USERS_COLLECTION

internal class FirestoreUserPreferencesDataSourceImpl(
    private val firestore: FirebaseFirestore,
) : UserPreferencesDataSource {

    private val helper = FirestoreOperationHelper("FirestoreUserPrefs")

    override suspend fun getUserPreferences(
        userId: String,
    ): Result<UserPreferencesFirestoreDto?, DataError.Sync> {
        return helper.execute("getUserPreferences") {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document(PREFERENCES_DOCUMENT)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.toObject(UserPreferencesFirestoreDto::class.java)
            } else {
                null
            }
        }
    }

    override suspend fun setUserPreferences(
        userId: String,
        preferences: UserPreferencesFirestoreDto,
    ): Result<Unit, DataError.Sync> {
        return helper.execute("setUserPreferences") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document(PREFERENCES_DOCUMENT)
                .set(preferences)
                .await()
        }
    }

    override suspend fun deleteUserPreferences(userId: String): Result<Unit, DataError.Sync> {
        return helper.execute("deleteUserPreferences") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SETTINGS_COLLECTION)
                .document(PREFERENCES_DOCUMENT)
                .delete()
                .await()
        }
    }

    override suspend fun deleteUserDocument(userId: String): Result<Unit, DataError.Sync> {
        return helper.execute("deleteUserDocument") {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
        }
    }
}
