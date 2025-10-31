package com.example.safeher.data.repository

import android.util.Log
import com.example.safeher.data.datasource.FriendRemoteDataSource
import com.example.safeher.data.model.Friend
import com.example.safeher.data.model.Friends
import com.example.safeher.data.model.LiveLocation
import com.example.safeher.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FriendRepository @Inject constructor(
    private val friendDataSource: FriendRemoteDataSource
) {
    suspend fun getFriendsByUser(userId: String): Flow<Friends> {
        Log.d("FriendRepository", "getFriendsByUser")
        return friendDataSource.getFriendsByUser(userId)
    }

    suspend fun createFriendRequest(userId: String, friend: User) : Result<Friend> {
        return friendDataSource.createFriendRequest(userId, friend)
    }

    suspend fun deleteFriend(friendId: String, userId: String) : Result<Unit> {
        return friendDataSource.deleteFriend(friendId, userId)
    }

    suspend fun acceptFriendRequest(friendId: String, userId: String) : Result<Unit> {
        return friendDataSource.acceptFriendRequest(friendId, userId)
    }

    suspend fun updateByUser(updatedUser: User): Result<Int> {
        return friendDataSource.updateByUser(updatedUser)
    }

    suspend fun deleteByUser(userIdToDelete: String): Result<Int> {
        return friendDataSource.deleteByUser(userIdToDelete)
    }

}