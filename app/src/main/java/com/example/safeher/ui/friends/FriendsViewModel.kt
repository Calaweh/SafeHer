package com.example.safeher.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.model.Friends
import com.example.safeher.data.model.User
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.data.datasource.UserDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userDataSource: UserDataSource
) : ViewModel() {

    private val _friendsState = MutableStateFlow(Friends(emptyList(), emptyList()))
    val friendsState: StateFlow<Friends> = _friendsState.asStateFlow()

    private val currentUserId: String?
        get() = userDataSource.userState.value?.id

    init {
        observeUserStateAndLoadFriends()
    }

    private fun observeUserStateAndLoadFriends() {
        viewModelScope.launch {
            userDataSource.userState.collect { user ->
                if (user != null) {
                    Log.d("FriendsViewModel", "User state received: ${user.id}. Fetching friends.")
                    friendRepository.getFriendsByUser(user.id)
                        .catch { exception ->
                            Log.e("FriendsViewModel", "Error loading friends", exception);
                        }
                        .collect { friends ->
                            _friendsState.value = friends
                        }
                } else {
                    Log.d("FriendsViewModel", "User is null. Clearing friends list.")
                    _friendsState.value = Friends(emptyList(), emptyList())
                }
            }
        }
    }

    fun addFriend(email: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                 val friendUser: User? = userDataSource.getUserByEmail(email)
                Log.d("FriendsViewModel", "Friend user: ${friendUser?.displayName}")
                 if (friendUser != null) {
                     Log.d("FriendsViewModel", "Heading to... friendRepository.createFriendRequest")
                     friendRepository.createFriendRequest(userId, friendUser)
                 }
            }
        }
    }

    fun acceptFriendRequest(friendId: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                friendRepository.acceptFriendRequest(friendId, userId)
            }
        }
    }

    fun rejectFriendRequest(friendId: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                friendRepository.deleteFriend(friendId, userId)
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                friendRepository.deleteFriend(friendId, userId)
            }
        }
    }
}