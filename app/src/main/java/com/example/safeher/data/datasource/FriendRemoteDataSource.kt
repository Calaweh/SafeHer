package com.example.safeher.data.datasource

import android.util.Log
import com.example.safeher.data.model.Friend
import com.example.safeher.data.model.Friends
import com.example.safeher.data.model.User
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FriendRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val FRIEND_COLLECTION = "friends"
        private const val USER_COLLECTION = "users"
        private const val REQUESTING_STATUS_FIELD_VALUE = "requesting"
        private const val ACCEPTED_STATUS_FIELD_VALUE = "accepted"
        private const val FRIEND_STATUS_FIELD = "status"
        private const val DELETED_AT_FIELD = "deletedAt"
    }

    suspend fun getFriendsByUser(userId: String): Flow<Friends> {
        Log.d("FriendRemoteDataSource", "Getting friends for user: $userId")
        return firestore.collection(USER_COLLECTION).document(userId)
            .collection(FRIEND_COLLECTION)
            .whereEqualTo(DELETED_AT_FIELD, null)
            .snapshots()
            .map { snapshot ->
                val allFriends = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Friend::class.java)?.copy(documentId = doc.id)
                }

                val friendsList = allFriends.filter { it.status == ACCEPTED_STATUS_FIELD_VALUE }
                val requestList = allFriends.filter { it.status == REQUESTING_STATUS_FIELD_VALUE }

                Friends(friends = friendsList, requestList = requestList)
            }
            .catch {
                emit(Friends(emptyList(), emptyList()))
            }
    }

    suspend fun createFriendRequest(userId: String, friend: User) : Result<Friend> {

        Log.d("FriendRemoteDataSource", "Creating friend request for user: $userId, friend: ${friend.id} + ${friend.displayName}")

        return try {
            val tmpFriend = Friend(id = friend.id, friend.imageUrl, friend.displayName, REQUESTING_STATUS_FIELD_VALUE)

            val documentReference = firestore.collection(USER_COLLECTION).document(userId)
                .collection(FRIEND_COLLECTION).add(tmpFriend).await()

            val createdFriendSnapshot = documentReference.get().await()

            val finalFriend = createdFriendSnapshot.toObject(Friend::class.java)?.copy(
                documentId = createdFriendSnapshot.id
            )

            if (finalFriend != null) {
                Result.success(finalFriend)
            } else {
                Result.failure(Exception("Failed to parse created friend."))
            }

        } catch (e: Exception) {
            Log.e("FriendRemoteDataSource", "Error creating friend request")
            Result.failure(e)
        }
    }

    suspend fun deleteFriend(friendId: String, userId: String) : Result<Unit> {
        return try {

            val deleteUpdate = mapOf(DELETED_AT_FIELD to FieldValue.serverTimestamp())
            firestore.collection(USER_COLLECTION).document(userId)
                .collection(FRIEND_COLLECTION).document(friendId)
                .set(deleteUpdate, SetOptions.merge()).await()

            Log.d("FriendRemoteDataSource", "Friend successfully marked as deleted: $friendId")
            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("FriendRemoteDataSource", "Error marking friend as deleted: $friendId", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(friendId: String, userId: String) : Result<Unit> {

        return try {

            val acceptUpdate = mapOf(FRIEND_STATUS_FIELD to ACCEPTED_STATUS_FIELD_VALUE)
            firestore.collection(USER_COLLECTION).document(userId)
                .collection(FRIEND_COLLECTION).document(friendId)
                .update(acceptUpdate).await()

            Log.d("FriendRemoteDataSource", "Friend successfully marked as accepted: $friendId")
            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("FriendRemoteDataSource", "Error marking friend as accepted: $friendId", e)
            Result.failure(e)
        }
    }

    suspend fun updateByUser(updatedUser: User): Result<Int> {
        return try {
            val friendQuery = firestore.collectionGroup(FRIEND_COLLECTION)
                .whereEqualTo("id", updatedUser.id)
                .whereEqualTo("deletedAt", null)
                .get()
                .await()

            val updateCount = friendQuery.documents.size

            if (updateCount > 0) {
                val batch = firestore.batch()
                friendQuery.documents.forEach { document ->
                    val updateData = mapOf(
                        "displayName" to updatedUser.displayName,
                        "imageUrl" to updatedUser.imageUrl
                    )
                    batch.update(document.reference, updateData)
                }
                batch.commit().await()
            }

            Log.d("FriendRemoteDataSource", "Updated $updateCount friend records for user: ${updatedUser.id}")
            Result.success(updateCount)

        } catch (e: Exception) {
            Log.e("FriendRemoteDataSource", "Error updating friend data for user: ${updatedUser.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteByUser(userIdToDelete: String): Result<Int> {
        return try {
            val friendQuery = firestore.collectionGroup(FRIEND_COLLECTION)
                .whereEqualTo("id", userIdToDelete)
                .whereEqualTo("deletedAt", null)
                .get()
                .await()

            val updateCount = friendQuery.documents.size

            if (updateCount > 0) {
                val batch = firestore.batch()
                friendQuery.documents.forEach { document ->
                    val updateData = mapOf(DELETED_AT_FIELD to FieldValue.serverTimestamp())
                    batch.update(document.reference, updateData)
                }
                batch.commit().await()
            }

            Log.d("FriendRemoteDataSource", "Delete $updateCount friend records for user: $userIdToDelete")
            Result.success(updateCount)

        } catch (e: Exception) {
            Log.e("FriendRemoteDataSource", "Error deleting friend data for user: $userIdToDelete", e)
            Result.failure(e)
        }
    }
}