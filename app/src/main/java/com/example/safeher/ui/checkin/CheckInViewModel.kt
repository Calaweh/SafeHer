package com.example.safeher.ui.checkin

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.model.Friend
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.data.repository.LocationSharingRepository
import com.example.safeher.data.repository.AuthRepository
import com.example.safeher.data.service.LocationSharingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val repository: LocationSharingRepository,
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val sharingState = repository.sharingState

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends = _friends.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserIdFlow.first { it != null }

                if (userId != null)
                    friendRepository.getFriendsByUser(userId).collect { friendsList ->
                        _friends.value = friendsList.friends
                    }

            } catch (e: Exception) {
                Log.e("CheckInViewModel", "Error loading friends", e)
                _friends.value = emptyList()
            }
        }
    }

    fun startInstantShare(durationMinutes: Long, sharedWithIds: List<String>) {
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_START_INSTANT
            putExtra(LocationSharingService.EXTRA_DURATION_MINUTES, durationMinutes)
            putStringArrayListExtra(LocationSharingService.EXTRA_SHARED_WITH_IDS, ArrayList(sharedWithIds))
        }
        context.startForegroundService(intent)
    }

    fun startDelayedShare(delayMinutes: Long, sharedWithIds: List<String>) {
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_START_DELAYED
            putExtra(LocationSharingService.EXTRA_DURATION_MINUTES, delayMinutes)
            putStringArrayListExtra(LocationSharingService.EXTRA_SHARED_WITH_IDS, ArrayList(sharedWithIds))
        }
        context.startForegroundService(intent)
    }

    fun stopSharing() {
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_STOP
        }
        context.startForegroundService(intent)
    }
}