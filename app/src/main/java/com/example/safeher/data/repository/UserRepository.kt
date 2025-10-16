package com.example.safeher.data.repository

import android.net.Uri
import android.util.Log
import com.example.safeher.data.datasource.MediaRemoteDataSource
import com.example.safeher.data.datasource.UserDataSource
import com.example.safeher.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDataSource: UserDataSource,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val mediaRemoteDataSource: MediaRemoteDataSource
) {

    private val usersCollection = firestore.collection("users") // new add
    private val _cancellationEvent = MutableSharedFlow<Unit>()
    val cancellationEvent = _cancellationEvent.asSharedFlow()

    val userState: StateFlow<User?> = userDataSource.userState

    fun getUsers(userId: String): Flow<List<User>> {
        return userDataSource.getUsers(userId)
    }

    suspend fun getUser(userId: String): User? {
        return userDataSource.getUser(userId)
    }

    suspend fun create(user: User): String {
        return userDataSource.create(user)
    }

    suspend fun update(user: User) {
        userDataSource.update(user)
    }

    suspend fun delete(userId: String) {
        userDataSource.delete(userId)
    }

    suspend fun updateProfile(
        userId: String,
        newName: String,
        newImageUrl: String?, // URL from text input
        newImageUri: Uri?     // URI from file picker
    ) {
        val user = getUser(userId) ?: throw IllegalStateException("User to update not found")

        // Determine the final image URL. Upload from URI has priority.
        val finalImageUrl = if (newImageUri != null) {
            try {
                // 1. Upload the image URI using the data source, which handles the
                //    Base64 conversion and chunking to Firestore.
                val mediaId = mediaRemoteDataSource.uploadImageFromUri(newImageUri, userId)

                // 2. Construct the special Firestore media URL with the required prefix.
                "${MediaRemoteDataSource.MEDIA_ID_PREFIX}$mediaId"
            } catch (e: Exception) {
                Log.e("UserRepository", "Failed to upload profile image via MediaRemoteDataSource", e)
                throw e // Propagate the error to be caught by the ViewModel
            }
        } else {
            newImageUrl?.takeIf { it.isNotBlank() } ?: user.imageUrl
        }

        val updates = mapOf(
            "displayName" to newName,
            "imageUrl" to finalImageUrl
        )

        usersCollection.document(userId).update(updates).await()

//        user.artistId?.let { artistId ->
//            artistRepository.updateArtistImage(artistId, finalImageUrl)
//        }
        ///////////////////////////////

    }

    suspend fun cancelCurrentUserSubscription() {
        val userId = authRepository.getCurrentUserId()
        val userDocRef = firestore.collection("users").document(userId)

        val updates = mapOf(
            "isPremium" to false,
            "cancellationDate" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        userDocRef.update(updates).await()
        _cancellationEvent.emit(Unit)
    }

    suspend fun updateCurrentUserToPremium() {
        val userId = authRepository.getCurrentUserId()

        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.update("isPremium", true).await()
    }

    suspend fun updateUserArtistId(userId: String, artistId: String) { // this fun new add
        try {
            usersCollection.document(userId).update("artistId", artistId).await()
        } catch (e: Exception) {
            // Handle error, e.g., log it
        }
    }

    suspend fun updateAutoDebitSettings(isEnabled: Boolean, paymentMethod: String?) {
        val userId = authRepository.getCurrentUserId()
        val userDocRef = firestore.collection("users").document(userId)

        val updates = mapOf(
            "autoDebitEnabled" to isEnabled,
            "savedPaymentMethod" to paymentMethod
        )
        userDocRef.update(updates).await()
    }

    suspend fun savePaymentDetails(details: Map<String, String>?) {
        val userId = authRepository.getCurrentUserId()
        val userDocRef = firestore.collection("users").document(userId)
        userDocRef.update("savedPaymentDetails", details).await()
    }
}