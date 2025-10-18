package com.example.safeher.ui.checkin

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.safeher.data.repository.LocationSharingRepository
import com.example.safeher.data.service.LocationSharingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val repository: LocationSharingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val sharingState = repository.sharingState

    fun startInstantShare(durationMinutes: Long) {
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_START_INSTANT
            putExtra(LocationSharingService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        context.startForegroundService(intent)
    }

    fun startDelayedShare(delayMinutes: Long) {
        val intent = Intent(context, LocationSharingService::class.java).apply {
            action = LocationSharingService.ACTION_START_DELAYED
            putExtra(LocationSharingService.EXTRA_DURATION_MINUTES, delayMinutes)
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