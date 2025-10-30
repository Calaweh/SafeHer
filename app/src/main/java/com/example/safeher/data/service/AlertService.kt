package com.example.safeher.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.safeher.MainActivity
import com.example.safeher.R
import com.example.safeher.data.model.Alert
import com.example.safeher.data.repository.AlertRepository
import com.example.safeher.data.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlertService : LifecycleService() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var alertRepository: AlertRepository

    private var alertListenerJob: Job? = null
    private val TAG = "AlertService"
    private val NOTIFICATION_CHANNEL_ID = "emergency_alert_channel"
    private val NOTIFICATION_ID = 111

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(NOTIFICATION_ID, createForegroundNotification())

        Log.d(TAG, "AlertService started.")

        if (alertListenerJob == null) {
            startListeningForAlerts()
        }

        return START_STICKY
    }

    private fun startListeningForAlerts() {
        alertListenerJob?.cancel()

        alertListenerJob = lifecycleScope.launch {
            authRepository.currentUserIdFlow.collectLatest { userId ->
                if (userId != null) {
                    alertRepository.getNewAlerts(userId).collect { alerts ->
                        alerts.forEach { alert ->
                            Log.d(TAG, "New alert received with ID: ${alert.id}")
                            showAlertNotification(alert)

                            alertRepository.deleteAlert(userId, alert.id)
                        }
                    }
                } else {
                    Log.d(TAG, "No user logged in, stopping listener.")
                }
            }
        }
    }

    private fun showAlertNotification(alert: Alert) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("EMERGENCY ALERT!")
            .setContentText("${alert.senderName} needs help. Check on them immediately!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createForegroundNotification(): android.app.Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SafeHer is Active")
            .setContentText("Listening for emergency alerts in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a proper icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for receiving emergency alerts from friends."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlertService destroyed.")
        alertListenerJob?.cancel()
    }
}