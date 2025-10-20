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
        private const val DIRECTION_FIELD = "direction"
        private const val SENT_DIRECTION = "sent"
        private const val RECEIVED_DIRECTION = "received"
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
                Log.d("FriendDataSource", "User $userId - allFriends before filter: $allFriends")

                val friendsList = allFriends.filter { it.status == ACCEPTED_STATUS_FIELD_VALUE }
                val requestList = allFriends.filter {
                    it.status == REQUESTING_STATUS_FIELD_VALUE && it.direction == RECEIVED_DIRECTION
                }

                val sentRequestList = allFriends.filter {
                    it.status == REQUESTING_STATUS_FIELD_VALUE && it.direction == SENT_DIRECTION
                }

                Friends(friends = friendsList, requestList = requestList, sentRequestList = sentRequestList)
            }
            .catch {
                emit(Friends(emptyList(), emptyList()))
            }
    }

    suspend fun createFriendRequest(userId: String, friend: User) : Result<Friend> {

        Log.d("FriendRemoteDataSource", "Creating friend request for user: $userId, friend: ${friend.id} + ${friend.displayName}")
        return try {
            val batch = firestore.batch()

            val senderUserSnapshot = firestore.collection(USER_COLLECTION).document(userId).get().await()
            val senderUser = senderUserSnapshot.toObject(User::class.java)
                ?: return Result.failure(Exception("Sender user data not found for ID: $userId"))

            Log.d("FriendRemoteDataSource", "Sender user data: $senderUser")

            val senderFriend = Friend(
                id = friend.id,  // Receiver's ID
                imageUrl = friend.imageUrl,
                displayName = friend.displayName,
                status = REQUESTING_STATUS_FIELD_VALUE,
                direction = SENT_DIRECTION,
                fromUserId = userId,
                toUserId = friend.id
            )
            val senderDocRef = firestore.collection(USER_COLLECTION).document(userId)
                .collection(FRIEND_COLLECTION).document(friend.id)
            Log.d("FriendRemoteDataSource", "Sender friend data: $senderFriend, docRef: ${senderDocRef.path}")
            batch.set(senderDocRef, senderFriend)

            val receiverFriend = Friend(
                id = userId,  // Sender's ID
                imageUrl = senderUser.imageUrl,
                displayName = senderUser.displayName,
                status = REQUESTING_STATUS_FIELD_VALUE,
                direction = RECEIVED_DIRECTION,
                fromUserId = userId,
                toUserId = friend.id
            )
            val receiverDocRef = firestore.collection(USER_COLLECTION).document(friend.id)
                .collection(FRIEND_COLLECTION).document(userId)
            Log.d("FriendRemoteDataSource", "Receiver friend data: $receiverFriend, docRef: ${receiverDocRef.path}")
            batch.set(receiverDocRef, receiverFriend)

            // Commit the batch
            try {
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("FriendRemoteDataSource", "Batch commit failed: ${e.message}", e)
                throw e
            }

            // Fetch the created sender document
            val createdFriendSnapshot = senderDocRef.get().await()
            val finalFriend = createdFriendSnapshot.toObject(Friend::class.java)?.copy(
                documentId = createdFriendSnapshot.id
            )

            if (finalFriend != null) {
                Log.d("FriendRemoteDataSource", "Friend request created successfully: $finalFriend")
                Result.success(finalFriend)
            } else {
                Log.e("FriendRemoteDataSource", "Failed to parse created friend for friend: ${friend.id}")
                Result.failure(Exception("Failed to parse created friend."))
            }

        } catch (e: Exception) {
            Log.e("FriendRemoteDataSource", "Error creating friend request for friend: ${friend.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteFriend(friendId: String, userId: String) : Result<Unit> {
        return try {

            val batch = firestore.batch()
            val senderUpdate = mapOf(DELETED_AT_FIELD to FieldValue.serverTimestamp())
            batch.set(
                firestore.collection(USER_COLLECTION).document(userId)
                    .collection(FRIEND_COLLECTION).document(friendId),
                senderUpdate,
                SetOptions.merge()
            )

            batch.set(
                firestore.collection(USER_COLLECTION).document(friendId)
                    .collection(FRIEND_COLLECTION).document(userId),
                senderUpdate,
                SetOptions.merge()
            )
            batch.commit().await()

            Log.d("FriendRemoteDataSource", "Friend successfully marked as deleted: $friendId")
            Result.success(Unit)

        } catch (e: Exception) {

            Log.e("FriendRemoteDataSource", "Error marking friend as deleted: $friendId", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(friendId: String, userId: String) : Result<Unit> {

        return try {
            val batch = firestore.batch()

            // Update receiver's document (direction = "received")
            val receiverUpdate = mapOf(FRIEND_STATUS_FIELD to ACCEPTED_STATUS_FIELD_VALUE)
            val receiverDocRef = firestore.collection(USER_COLLECTION).document(userId)
                .collection(FRIEND_COLLECTION).document(friendId)
            batch.update(receiverDocRef, receiverUpdate)

            // Update sender's document (direction = "sent")
            val senderUpdate = mapOf(FRIEND_STATUS_FIELD to ACCEPTED_STATUS_FIELD_VALUE)
            val senderDocRef = firestore.collection(USER_COLLECTION).document(friendId)
                .collection(FRIEND_COLLECTION).document(userId)
            batch.update(senderDocRef, senderUpdate)

            // Commit the batch
            batch.commit().await()

            Log.d("FriendRemoteDataSource", "Friend request successfully accepted for friend: $friendId")
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