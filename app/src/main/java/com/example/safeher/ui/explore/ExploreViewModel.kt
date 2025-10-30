package com.example.safeher.ui.explore

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.model.Alert
import com.example.safeher.data.repository.AuthRepository
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "ExploreViewModel"

    private val _alertMessage = MutableStateFlow<AlertMessage?>(null)
    val alertMessage: StateFlow<AlertMessage?> = _alertMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendInstantAlert() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserId = authRepository.getCurrentUserId()

                val currentUserProfile = userRepository.getUser(currentUserId)
                if (currentUserProfile == null) {
                    Log.e(TAG, "Could not send alert, sender's profile not found.")
                    _alertMessage.value = AlertMessage.Error("Unable to send alert. Please try again.")
                    _isLoading.value = false
                    return@launch
                }

                Log.d(TAG, "Starting instant alert for user: ${currentUserProfile.displayName}")

                val friendsList = friendRepository.getFriendsByUser(currentUserId).first().friends
                if (friendsList.isEmpty()) {
                    Log.d(TAG, "User has no friends to alert.")
                    _alertMessage.value = AlertMessage.Warning("You don't have any friends added yet. Add friends to use Instant Alert.")
                    _isLoading.value = false
                    return@launch
                }

                friendsList.forEach { friend ->
                    val alert = Alert(
                        senderId = currentUserProfile.id,
                        senderName = currentUserProfile.displayName
                    )

                    firestore.collection("alerts")
                        .document(friend.id)
                        .collection("pending_alerts")
                        .add(alert)
                        .await()

                    Log.d(TAG, "Successfully created alert for friend: ${friend.displayName} (${friend.id})")
                }

                Log.d(TAG, "SUCCESS: All alerts have been sent to Firestore.")
                _alertMessage.value = AlertMessage.Success("Emergency alert sent to ${friendsList.size} friend(s)!")

            } catch (e: IllegalStateException) {
                Log.e(TAG, "Cannot send alert, user is not logged in.", e)
                _alertMessage.value = AlertMessage.Error("Please log in to send alerts.")
            } catch (e: Exception) {
                Log.e(TAG, "An error occurred while sending the instant alert.", e)
                _alertMessage.value = AlertMessage.Error("Failed to send alert. Please check your connection and try again.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAlertMessage() {
        _alertMessage.value = null
    }
}

sealed class AlertMessage {
    data class Success(val message: String) : AlertMessage()
    data class Error(val message: String) : AlertMessage()
    data class Warning(val message: String) : AlertMessage()
}
