package com.example.safeher.ui.checkintimer

import android.Manifest
import android.content.Context
import android.content.Intent
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
import com.example.safeher.data.repository.CheckInTimerStateManager
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.data.repository.UserRepository
import com.example.safeher.data.service.CheckInTimerService
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
class CheckInTimerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val alertHistoryRepository: AlertHistoryRepository,
    private val firestore: FirebaseFirestore,
    val timerStateManager: CheckInTimerStateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val TAG = "CheckInTimerViewModel"
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)

    private val prefs = context.getSharedPreferences("check_in_timer_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<CheckInTimerUiState>(CheckInTimerUiState.Idle)
    val uiState: StateFlow<CheckInTimerUiState> = _uiState.asStateFlow()

    private val _hasPinSet = MutableStateFlow(false)
    val hasPinSet: StateFlow<Boolean> = _hasPinSet.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0
    private var durationMillis: Long = 0

    init {
        checkPinExists()
    }

    private fun checkPinExists() {
        val pin = prefs.getString(PIN_KEY, null)
        _hasPinSet.value = !pin.isNullOrEmpty()
        Log.d(TAG, "PIN exists: ${_hasPinSet.value}")
    }

    fun savePin(pin: String) {
        prefs.edit().putString(PIN_KEY, pin).apply()
        _hasPinSet.value = true
        Log.d(TAG, "PIN saved successfully")
    }

    fun startTimer(minutes: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = CheckInTimerUiState.Loading

                val currentUserId = authRepository.getCurrentUserId()
                val friendsList = friendRepository.getFriendsByUser(currentUserId).first().friends

                if (friendsList.isEmpty()) {
                    _uiState.value = CheckInTimerUiState.Error(
                        "You need to add friends as emergency contacts first"
                    )
                    return@launch
                }

                if (!hasLocationPermission()) {
                    _uiState.value = CheckInTimerUiState.Error(
                        "Location permission required for emergency alerts"
                    )
                    return@launch
                }

                val durationMillis = minutes * 60 * 1000L
                val intent = Intent(context, CheckInTimerService::class.java).apply {
                    action = CheckInTimerService.ACTION_START_TIMER
                    putExtra(CheckInTimerService.EXTRA_DURATION, durationMillis)
                }
                context.startService(intent)

                timerStateManager.startTimer(minutes)

                _uiState.value = CheckInTimerUiState.Success("Check-in timer started for $minutes minutes")

                Log.d(TAG, "Check-in timer service started for $minutes minutes")

            } catch (e: Exception) {
                Log.e(TAG, "Error starting timer", e)
                _uiState.value = CheckInTimerUiState.Error(
                    "Failed to start timer: ${e.message}"
                )
            }
        }
    }

    fun validatePinAndStop(enteredPin: String): Boolean {
        val savedPin = prefs.getString(PIN_KEY, "")

        return if (enteredPin == savedPin) {
            val intent = Intent(context, CheckInTimerService::class.java).apply {
                action = CheckInTimerService.ACTION_STOP_TIMER
            }
            context.startService(intent)

            timerStateManager.stopTimer()

            Log.d(TAG, "Timer stopped - user checked in successfully")
            true
        } else {
            Log.w(TAG, "Incorrect PIN entered")
            false
        }
    }

    private fun startCountdown() {
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                val remaining = durationMillis - elapsed

                if (remaining <= 0) {
                    Log.w(TAG, "Check-in timer expired! Sending emergency alert...")
                    sendEmergencyAlert()
                    break
                }

                _uiState.value = CheckInTimerUiState.Active(
                    remainingMillis = remaining,
                    totalMillis = durationMillis
                )

                delay(1000)
            }
        }
    }

    fun stopTimer(enteredPin: String) {
        val savedPin = prefs.getString(PIN_KEY, "")
        
        if (enteredPin == savedPin) {
            timerJob?.cancel()
            _uiState.value = CheckInTimerUiState.Success("Check-in successful! You're marked as safe.")
            Log.d(TAG, "Timer stopped - user checked in successfully")
        } else {
            _uiState.value = (_uiState.value as? CheckInTimerUiState.Active)?.copy(
                incorrectPinAttempts = (_uiState.value as? CheckInTimerUiState.Active)?.incorrectPinAttempts?.plus(1) ?: 1
            ) ?: _uiState.value
            Log.w(TAG, "Incorrect PIN entered")
        }
    }

    fun stopTimerViaService(enteredPin: String) {
        val savedPin = prefs.getString(PIN_KEY, "")

        if (enteredPin == savedPin) {
            val intent = Intent(context, CheckInTimerService::class.java).apply {
                action = CheckInTimerService.ACTION_STOP_TIMER
            }
            context.startService(intent)

            _uiState.value = CheckInTimerUiState.Success("Check-in successful! You're marked as safe.")
            Log.d(TAG, "Timer stopped via service - user checked in successfully")
        } else {
            _uiState.value = CheckInTimerUiState.Error("Incorrect PIN")
            Log.w(TAG, "Incorrect PIN entered")
        }
    }

    private suspend fun sendEmergencyAlert() {
        try {
            val currentUserId = authRepository.getCurrentUserId()
            val currentUserProfile = userRepository.getUser(currentUserId)
            
            if (currentUserProfile == null) {
                Log.e(TAG, "Cannot send alert - user profile not found")
                _uiState.value = CheckInTimerUiState.Error("Failed to send alert")
                return
            }

            val location = getCurrentLocation()
            if (location == null) {
                Log.e(TAG, "Cannot send alert - location unavailable")
                _uiState.value = CheckInTimerUiState.Error("Failed to get location")
                return
            }

            val locationName = getAddressFromLocation(location.latitude, location.longitude)
            val friendsList = friendRepository.getFriendsByUser(currentUserId).first().friends

            Log.d(TAG, "Sending CHECK-IN TIMER EXPIRED alert to ${friendsList.size} friends")

            friendsList.forEach { friend ->
                val alert = Alert(
                    senderId = currentUserProfile.id,
                    senderName = "⏰ ${currentUserProfile.displayName} (Check-In Timer Expired)",
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
                    senderName = "⏰ ${currentUserProfile.displayName} (Check-In Timer)",
                    receiverId = friend.id,
                    receiverName = friend.displayName,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = locationName,
                    type = AlertType.SENT,
                    status = AlertStatus.DELIVERED
                )

                alertHistoryRepository.saveAlertHistory(alertHistory)
                Log.d(TAG, "Check-in timer alert sent to: ${friend.displayName}")
            }

            _uiState.value = CheckInTimerUiState.Expired(
                "Check-in timer expired. Emergency alert sent to ${friendsList.size} friend(s)"
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error sending emergency alert", e)
            _uiState.value = CheckInTimerUiState.Error("Failed to send emergency alert")
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

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val PIN_KEY = "check_in_timer_pin"
    }
}

sealed class CheckInTimerUiState {
    object Idle : CheckInTimerUiState()
    object Loading : CheckInTimerUiState()
    data class Active(
        val remainingMillis: Long,
        val totalMillis: Long,
        val incorrectPinAttempts: Int = 0
    ) : CheckInTimerUiState()
    data class Success(val message: String) : CheckInTimerUiState()
    data class Expired(val message: String) : CheckInTimerUiState()
    data class Error(val message: String) : CheckInTimerUiState()
}
