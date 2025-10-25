package com.example.safeher.data.datasource

import android.util.Log
import com.example.safeher.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    init {
        auth.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                firestore.collection("users").document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _userState.value = null
                            return@addSnapshotListener
                        }
                        _userState.value = snapshot?.toObject(User::class.java)
                    }
            } else {
                _userState.value = null
            }
        }
    }

    fun getUsers(userId: String): Flow<List<User>> {
        return firestore.collection(USER_COLLECTION)
            .whereEqualTo(USER_ID_FIELD, userId)
            .dataObjects()
    }

    suspend fun getUser(userId: String): User? {
        return firestore.collection(USER_COLLECTION).document(userId).get().await().toObject()
    }

    suspend fun getUserByEmail(email: String): User? {
        return try {
            Log.d("UserDataSource", "Fetching user by email: $email")
            val querySnapshot = firestore.collection(USER_COLLECTION)
                .whereEqualTo(EMAIL_FIELD, email)
                .limit(1)
                .get()
                .await()

            querySnapshot.toObjects<User>().firstOrNull()
        } catch (e: Exception) {
            Log.e("UserDataSource", "Error fetching user by email: $email", e)
            null
        }
    }

    suspend fun create(user: User): String {
        return firestore.collection(USER_COLLECTION).add(user).await().id
    }

    suspend fun update(user: User) {
        firestore.collection(USER_COLLECTION).document(user.id).set(user).await()
    }

    suspend fun delete(userId: String) {
        val deleteUpdate = mapOf("deletedAt" to FieldValue.serverTimestamp())
        firestore.collection(USER_COLLECTION).document(userId).update(deleteUpdate).await()
    }

    suspend fun updateProfile(userId: String, newName: String, newImageUrl: String) {
        firestore.collection(USER_COLLECTION).document(userId).update(
            mapOf(
                "displayName" to newName,
                "imageUrl" to newImageUrl
            )
        ).await()
    }

    companion object {
        private const val USER_ID_FIELD = "userId"
        private const val USER_COLLECTION = "users"
        private const val EMAIL_FIELD = "email"
    }
}