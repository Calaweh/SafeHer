package com.example.safeher.ui.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.model.Alert
import com.example.safeher.data.model.AlertHistory
import com.example.safeher.data.model.AlertStatus
import com.example.safeher.data.model.AlertType
import com.example.safeher.data.repository.AlertHistoryRepository
import com.example.safeher.data.repository.AuthRepository
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val alertHistoryRepository: AlertHistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "ExploreViewModel"
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _alertMessage = MutableStateFlow<AlertMessage?>(null)
    val alertMessage: StateFlow<AlertMessage?> = _alertMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _countdownSeconds = MutableStateFlow<Int?>(null)
    val countdownSeconds: StateFlow<Int?> = _countdownSeconds.asStateFlow()

    private var countdownJob: Job? = null

    fun startAlertCountdown() {
        cancelCountdown()

        countdownJob = viewModelScope.launch {
            try {
                for (i in 5 downTo 1) {
                    _countdownSeconds.value = i
                    delay(1000)
                }

                _countdownSeconds.value = null
                sendInstantAlert()

            } catch (e: Exception) {
                Log.e(TAG, "Countdown error", e)
                _countdownSeconds.value = null
            }
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownSeconds.value = null
        _alertMessage.value = AlertMessage.Warning("Alert cancelled")
    }

    private fun sendInstantAlert() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (!hasLocationPermission()) {
                    _alertMessage.value = AlertMessage.Warning("Location permission is required to send your location with the alert.")
                    _isLoading.value = false
                    return@launch
                }

                val currentUserId = authRepository.getCurrentUserId()

                val currentUserProfile = userRepository.getUser(currentUserId)
                if (currentUserProfile == null) {
                    Log.e(TAG, "Could not send alert, sender's profile not found.")
                    _alertMessage.value = AlertMessage.Error("Unable to send alert. Please try again.")
                    _isLoading.value = false
                    return@launch
                }

                Log.d(TAG, "Starting instant alert for user: ${currentUserProfile.displayName}")

                val location = getCurrentLocation()
                if (location == null) {
                    _alertMessage.value = AlertMessage.Error("Unable to get your location. Please ensure location services are enabled.")
                    _isLoading.value = false
                    return@launch
                }

                val locationName = getAddressFromLocation(location.latitude, location.longitude)
                Log.d(TAG, "Location: ${location.latitude}, ${location.longitude} - $locationName")

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
                        senderName = currentUserProfile.displayName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        locationName = locationName
                    )

                    firestore.collection("alerts")
                        .document(friend.id)
                        .collection("pending_alerts")
                        .add(alert)
                        .await()

                    val alertHistory = AlertHistory(
                        senderId = currentUserProfile.id,
                        senderName = currentUserProfile.displayName,
                        receiverId = friend.id,
                        receiverName = friend.displayName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        locationName = locationName,
                        type = AlertType.SENT,
                        status = AlertStatus.DELIVERED
                    )

                    Log.d(TAG, "💾 SAVING SENT ALERT:")
                    Log.d(TAG, "  Sender: ${alertHistory.senderId} (${alertHistory.senderName})")
                    Log.d(TAG, "  Receiver: ${alertHistory.receiverId} (${alertHistory.receiverName})")
                    Log.d(TAG, "  Type: ${alertHistory.type}")

                    alertHistoryRepository.saveAlertHistory(alertHistory)
                    Log.d(TAG, "Successfully created alert with location for friend: ${friend.displayName}")
                }

                Log.d(TAG, "SUCCESS: All alerts with location have been sent.")
                _alertMessage.value = AlertMessage.Success("Emergency alert with your location sent to ${friendsList.size} friend(s)!")

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

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        Log.e(TAG, "Error getting location", exception)
                        continuation.resume(null)
                    }
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location", e)
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                // Build a readable address
                buildString {
                    address.thoroughfare?.let { append("$it, ") }
                    address.locality?.let { append("$it, ") }
                    address.adminArea?.let { append(it) }
                }
            } else {
                "Location: $latitude, $longitude"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address", e)
            "Location: $latitude, $longitude"
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