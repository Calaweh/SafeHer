package com.example.safeher.data.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.safeher.MainActivity
import com.example.safeher.R
import com.example.safeher.data.datasource.LocationRemoteDataSource
import com.example.safeher.data.datasource.UserDataSource
import com.example.safeher.data.model.LocationSharingState
import com.example.safeher.data.model.SharingMode
import com.example.safeher.data.repository.LocationSharingRepository
import com.example.safeher.utils.ILocationProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class LocationSharingService : LifecycleService() {

    @Inject lateinit var locationProvider: ILocationProvider
    @Inject lateinit var locationDataSource: LocationRemoteDataSource
    @Inject lateinit var userDataSource: UserDataSource
    @Inject lateinit var repository: LocationSharingRepository

    private var locationJob: Job? = null
    private var timer: CountDownTimer? = null
    private var notificationManager: NotificationManager? = null

    companion object {
        const val ACTION_START_INSTANT = "ACTION_START_INSTANT"
        const val ACTION_START_DELAYED = "ACTION_START_DELAYED"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_DURATION_MINUTES = "EXTRA_DURATION_MINUTES"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "location_sharing_channel"
        const val EXTRA_SHARED_WITH_IDS = "EXTRA_SHARED_WITH_IDS"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val sharedWithIds = intent?.getStringArrayListExtra(EXTRA_SHARED_WITH_IDS) ?: emptyList()

        when (intent?.action) {
            ACTION_START_INSTANT -> {
                val duration = intent.getLongExtra(EXTRA_DURATION_MINUTES, 0L)
                startInstantSharing(duration, sharedWithIds)
            }
            ACTION_START_DELAYED -> {
                val delay = intent.getLongExtra(EXTRA_DURATION_MINUTES, 0L)
                startDelayedSharing(delay, sharedWithIds)
            }
            ACTION_STOP -> {
                stopServiceAndSharing()
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun startInstantSharing(durationMinutes: Long, sharedWithIds: List<String>) {

        Log.d("LocationSharingService","start instant location sharing")

        stopCurrentTask()
        val durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes)

        startForeground(NOTIFICATION_ID, createNotification())

        if (durationMillis > 0) {
            timer = object : CountDownTimer(durationMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    repository.updateState(
                        LocationSharingState(SharingMode.SHARING, millisUntilFinished, durationMillis)
                    )
                    updateNotification()
                }
                override fun onFinish() {
                    stopServiceAndSharing()
                }
            }.start()
        } else {
            // Indefinite: Start updates without timer, but keep notification
            repository.updateState(LocationSharingState(SharingMode.SHARING, 0, 0))
        }

        startLocationUpdates(sharedWithIds)
    }

    private fun startDelayedSharing(delayMinutes: Long, sharedWithIds: List<String>) {

        Log.d("LocationSharingService","start delayed location sharing")

        stopCurrentTask()
        val delayMillis = TimeUnit.MINUTES.toMillis(delayMinutes)

        startForeground(NOTIFICATION_ID, createNotification())

        timer = object : CountDownTimer(delayMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                repository.updateState(
                    LocationSharingState(SharingMode.COUNTDOWN, millisUntilFinished, delayMillis)
                )
                updateNotification()
            }
            override fun onFinish() {
                repository.updateState(LocationSharingState(SharingMode.SHARING, 0, 0))
                updateNotification()
                startLocationUpdates(sharedWithIds)
            }
        }.start()
    }

    private fun startLocationUpdates(sharedWithIds: List<String>) {

        Log.d("LocationSharingService", "startLocationUpdates called")

        if (locationJob?.isActive == true) return

        if (!hasLocationPermissions()) {
            Log.e("LocationSharingService", "Missing location permissions")
            stopServiceAndSharing()
            return
        }

        locationJob = lifecycleScope.launch {
            val user = withTimeoutOrNull(5000) { userDataSource.userState.first { it != null } }
            if (user == null) {
                Log.e("LocationSharingService", "User not available")
                stopServiceAndSharing()
                return@launch
            }

            Log.d("LocationSharingService", "User available: ${user.id}. Starting updates.")

            try {
                locationProvider.getLocationUpdates().collect { location ->
                    Log.d("LocationSharingService", "Updating location")
                    locationDataSource.updateUserLocation(
                        userId = user.id,
                        displayName = user.displayName,
                        imageUrl = user.imageUrl,
                        lat = location.latitude,
                        lon = location.longitude,
                        sharedWithFriendIds = sharedWithIds
                    )
                }
            } catch (e: Exception) {
                Log.e("LocationSharingService", "Location updates failed: ${e.message}")
                stopServiceAndSharing()
            }
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateNotification() {
        notificationManager?.notify(NOTIFICATION_ID, createNotification())
    }

    private fun stopServiceAndSharing() {
        stopCurrentTask()
        lifecycleScope.launch {
            userDataSource.userState.value?.id?.let {
                locationDataSource.stopSharingLocation(it)
            }
        }
        stopForeground(true)
        stopSelf()
    }

    private fun stopCurrentTask() {
        timer?.cancel()
        locationJob?.cancel()
        repository.updateState(LocationSharingState(mode = SharingMode.IDLE))
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        val state = repository.sharingState.value

        val title: String
        val text: String
        when (state.mode) {
            SharingMode.SHARING -> {
                title = "Sharing Your Location"
                text = if (state.totalDurationInMillis > 0) {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(state.timeLeftInMillis)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(state.timeLeftInMillis) % 60
                    "Time left: ${String.format("%02d:%02d", minutes, seconds)}"
                } else {
                    "Sharing active indefinitely"
                }
            }
            SharingMode.COUNTDOWN -> {
                title = "Location Sharing Countdown"
                val minutes = TimeUnit.MILLISECONDS.toMinutes(state.timeLeftInMillis)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(state.timeLeftInMillis) % 60
                text = "Sharing will start in ${String.format("%02d:%02d", minutes, seconds)}"
            }
            else -> { // IDLE
                title = "SafeHer"
                text = "Location sharing is idle."
            }
        }

        val stopIntent = Intent(this, LocationSharingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(this, 1, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your own icon
            .setContentIntent(openAppPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Cancel & Stop", stopPendingIntent) // Replace with icon
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Location Sharing",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null // Not a bound service
    }

    override fun onDestroy() {
        stopCurrentTask()
        super.onDestroy()
    }
}