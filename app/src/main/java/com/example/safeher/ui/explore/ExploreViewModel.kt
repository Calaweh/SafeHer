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

    fun sendInstantAlert() {
        viewModelScope.launch {
            try {
                val currentUserId = authRepository.getCurrentUserId()

                val currentUserProfile = userRepository.getUser(currentUserId)
                if (currentUserProfile == null) {
                    Log.e(TAG, "Could not send alert, sender's profile not found.")
                    return@launch
                }

                Log.d(TAG, "Starting instant alert for user: ${currentUserProfile.displayName}")

                val friendsList = friendRepository.getFriendsByUser(currentUserId).first().friends
                if (friendsList.isEmpty()) {
                    Log.d(TAG, "User has no friends to alert.")
                    // TODO: Show a user-friendly message
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
                // TODO: Show a user-friendly success message

            } catch (e: IllegalStateException) {
                Log.e(TAG, "Cannot send alert, user is not logged in.", e)
            } catch (e: Exception) {
                Log.e(TAG, "An error occurred while sending the instant alert.", e)
                // TODO: Show a user-friendly error message
            }
        }
    }
}