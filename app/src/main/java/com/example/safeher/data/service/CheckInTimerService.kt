package com.example.safeher.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.safeher.MainActivity
import com.example.safeher.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import com.example.safeher.data.repository.AuthRepository
import com.example.safeher.data.repository.FriendRepository
import com.example.safeher.data.repository.UserRepository
import com.example.safeher.data.repository.AlertHistoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.safeher.data.model.Alert
import com.example.safeher.data.model.AlertHistory
import com.example.safeher.data.model.AlertType
import com.example.safeher.data.model.AlertStatus
import android.location.Geocoder
import android.location.Location
import com.example.safeher.data.repository.CheckInTimerStateManager
import com.huawei.hms.location.FusedLocationProviderClient
import com.huawei.hms.location.LocationServices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.coroutines.resume

@AndroidEntryPoint
class CheckInTimerService : Service() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var friendRepository: FriendRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var alertHistoryRepository: AlertHistoryRepository

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var timerStateManager: CheckInTimerStateManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    private val TAG = "CheckInTimerService"
    private val NOTIFICATION_CHANNEL_ID = "check_in_timer_channel"
    private val NOTIFICATION_ID = 222

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d(TAG, "CheckInTimerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION, 0L)
                startTimer(durationMillis)
            }
            ACTION_STOP_TIMER -> {
                stopTimer(success = true)
            }
            ACTION_CHECK_IN -> {
                val checkInIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("show_check_in_dialog", true)
                }
                startActivity(checkInIntent)
            }
        }
        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + durationMillis

        startForeground(NOTIFICATION_ID, createNotification(durationMillis))

        timerJob = serviceScope.launch {
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()

                if (remaining <= 0) {
                    sendEmergencyAlert()
                    stopTimer(success = false)
                    break
                }

                if (remaining % 60000 < 1000) {
                    updateNotification(remaining)
                }

                delay(1000)
            }
        }

        Log.d(TAG, "Timer started for ${durationMillis / 60000} minutes")
    }

    private fun stopTimer(success: Boolean) {
        timerJob?.cancel()
        timerJob = null

        if (success) {
            showSuccessNotification()
        }

        serviceScope.launch {
            delay(3000)
            stopSelf()
        }

        Log.d(TAG, "Timer stopped. Success: $success")
    }

    private suspend fun sendEmergencyAlert() {
        try {
            val currentUserId = authRepository.getCurrentUserId()
            val currentUserProfile = userRepository.getUser(currentUserId)

            if (currentUserProfile == null) {
                Log.e(TAG, "Cannot send alert - user profile not found")
                return
            }

            val location = getCurrentLocation()
            if (location == null) {
                Log.e(TAG, "Cannot send alert - location unavailable")
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
            }

            showAlertSentNotification()

        } catch (e: Exception) {
            Log.e(TAG, "Error sending emergency alert", e)
        }
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
            val geocoder = Geocoder(this, Locale.getDefault())
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

    private fun createNotification(remainingMillis: Long): android.app.Notification {
        createNotificationChannel()

        val minutes = (remainingMillis / 60000).toInt()

        val checkInIntent = Intent(this, CheckInTimerService::class.java).apply {
            action = ACTION_CHECK_IN
        }
        val checkInPendingIntent = PendingIntent.getService(
            this, 0, checkInIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("⏰ Check-In Timer Active")
            .setContentText("$minutes minutes remaining - Tap to check in")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(checkInPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Check In Now",
                checkInPendingIntent
            )
            .build()
    }

    private fun updateNotification(remainingMillis: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(remainingMillis))
    }

    private fun showSuccessNotification() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("✅ Check-In Successful")
            .setContentText("You've been marked as safe")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showAlertSentNotification() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🚨 Timer Expired - Alert Sent")
            .setContentText("Emergency alert sent to your contacts")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Check-In Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Check-In Timer status"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "CheckInTimerService destroyed")
    }

    companion object {
        const val ACTION_START_TIMER = "com.example.safeher.START_TIMER"
        const val ACTION_STOP_TIMER = "com.example.safeher.STOP_TIMER"
        const val ACTION_CHECK_IN = "com.example.safeher.CHECK_IN"
        const val EXTRA_DURATION = "duration"
    }
}