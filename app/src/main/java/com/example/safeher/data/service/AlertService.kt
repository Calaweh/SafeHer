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

        startListeningForAlerts()

        return START_STICKY
    }

    private fun startListeningForAlerts() {
        alertListenerJob?.cancel()

        alertListenerJob = lifecycleScope.launch {
            authRepository.currentUserIdFlow.collectLatest { userId ->
                if (userId != null) {
                    Log.d(TAG, "Starting alert listener for user: $userId")

                    alertRepository.getNewAlerts(userId).collect { alerts ->
                        alerts.forEach { alert ->
                            Log.d(TAG, "New alert received from: ${alert.senderName} (ID: ${alert.id})")
                            showAlertNotification(alert)

                            alertRepository.deleteAlert(userId, alert.id)
                        }
                    }
                } else {
                    Log.d(TAG, "No user logged in, stopping listener and service.")
                    stopSelf()
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
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("🚨 EMERGENCY ALERT!")
            .setContentText("${alert.senderName} needs help immediately!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createForegroundNotification(): android.app.Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SafeHer Protection Active")
            .setContentText("Monitoring for emergency alerts")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Receive emergency alerts from your friends"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
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