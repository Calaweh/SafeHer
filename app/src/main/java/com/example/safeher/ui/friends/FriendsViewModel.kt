package com.example.safeher.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.datasource.UserDataSource
import com.example.safeher.data.model.Friends
import com.example.safeher.data.model.User
import com.example.safeher.data.repository.FriendRepository
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

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _addFriendSuccess = MutableStateFlow(false)
    val addFriendSuccess: StateFlow<Boolean> = _addFriendSuccess.asStateFlow()

    private val currentUser: User?
        get() = userDataSource.userState.value

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
                            Log.e("FriendsViewModel", "Error loading friends", exception)
                        }
                        .collect { friends ->
                            _friendsState.value = friends
                        }
                } else {
                    Log.d("FriendsViewModel", "User is null. Clearing friends list.")
                    _friendsState.value = Friends(emptyList(), emptyList(), emptyList())
                }
            }
        }
    }

    fun addFriend(email: String) {
        viewModelScope.launch {
            _addFriendSuccess.value = false
            currentUser?.let { user ->
                if (email == user.email) {
                    _errorState.value = "You cannot send a friend request to yourself."
                    Log.d("FriendsViewModel", "Attempted to send friend request to self: $email")
                    return@launch
                }

                val friendUser: User? = userDataSource.getUserByEmail(email)
                if (friendUser != null) {
                    Log.d("FriendsViewModel", "Friend user: ${friendUser.displayName}")
                    Log.d("FriendsViewModel", "Heading to... friendRepository.createFriendRequest")
                    val result = friendRepository.createFriendRequest(user.id, friendUser)
                    if (result.isSuccess) {
                        _errorState.value = null
                        _addFriendSuccess.value = true
                    } else {
                        val errorMessage = when {
                            result.exceptionOrNull()?.message?.contains("PERMISSION_DENIED") == true ->
                                "Permission denied: Unable to send friend request."
                            else -> "Failed to send friend request: ${result.exceptionOrNull()?.message}"
                        }
                        _errorState.value = errorMessage
                        Log.e("FriendsViewModel", "Error sending friend request: $errorMessage")
                    }
                } else {
                    _errorState.value = "User with email $email not found."
                    Log.d("FriendsViewModel", "Friend user not found for email: $email")
                }
            } ?: run {
                _errorState.value = "Current user not found. Please sign in."
                Log.d("FriendsViewModel", "Current user is null")
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun acceptFriendRequest(friendId: String) {
        viewModelScope.launch {
            currentUser?.id?.let { userId ->
                friendRepository.acceptFriendRequest(friendId, userId)
            }
        }
    }

    fun rejectFriendRequest(friendId: String) {
        viewModelScope.launch {
            currentUser?.id?.let { userId ->
                friendRepository.deleteFriend(friendId, userId)
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            currentUser?.id?.let { userId ->
                friendRepository.deleteFriend(friendId, userId)
            }
        }
    }

    fun resetAddFriendSuccess() {
        _addFriendSuccess.value = false
    }
}