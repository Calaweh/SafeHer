package com.example.safeher.data.repository

import android.net.Uri
import android.util.Log
import com.example.safeher.data.datasource.FriendRemoteDataSource
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
    private val mediaRemoteDataSource: MediaRemoteDataSource,
    private val friendRemoteDataSource: FriendRemoteDataSource
) {

    private val usersCollection = firestore.collection("users")
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
        friendRemoteDataSource.updateByUser(user)

    }

    suspend fun delete(userId: String) {
        userDataSource.delete(userId)
        friendRemoteDataSource.deleteByUser(userId)
    }

    suspend fun updateProfile(
        userId: String,
        newName: String,
        newImageUrl: String?,
        newImageUri: Uri?
    ) {
        val user = getUser(userId) ?: throw IllegalStateException("User to update not found")

        val finalImageUrl = if (newImageUri != null) {
            try {
                val mediaId = mediaRemoteDataSource.uploadImageFromUri(newImageUri, userId)

                "${MediaRemoteDataSource.MEDIA_ID_PREFIX}$mediaId"
            } catch (e: Exception) {
                Log.e("UserRepository", "Failed to upload profile image via MediaRemoteDataSource", e)
                throw e
            }
        } else {
            newImageUrl?.takeIf { it.isNotBlank() } ?: user.imageUrl
        }

        val updates = mapOf(
            "displayName" to newName,
            "imageUrl" to finalImageUrl
        )

        usersCollection.document(userId).update(updates).await()

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
}