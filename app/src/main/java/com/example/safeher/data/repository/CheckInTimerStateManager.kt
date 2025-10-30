package com.example.safeher.data.repository

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInTimerStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("check_in_timer_state", Context.MODE_PRIVATE)

    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    private val _endTimeMillis = MutableStateFlow(0L)
    val endTimeMillis: StateFlow<Long> = _endTimeMillis.asStateFlow()

    init {
        loadTimerState()
    }

    private fun loadTimerState() {
        val endTime = prefs.getLong(KEY_END_TIME, 0L)
        val currentTime = System.currentTimeMillis()

        if (endTime > currentTime) {
            _isTimerActive.value = true
            _endTimeMillis.value = endTime
            Log.d(TAG, "Timer loaded: ${(endTime - currentTime) / 1000}s remaining")
        } else {
            _isTimerActive.value = false
            _endTimeMillis.value = 0L

            if (endTime > 0) {
                Log.d(TAG, "Timer was expired, cleaning up")
                stopTimer()
            }
        }
    }

    fun startTimer(durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_END_TIME, endTime)
            .putBoolean(KEY_IS_ACTIVE, true)
            .apply()

        _isTimerActive.value = true
        _endTimeMillis.value = endTime

        Log.d(TAG, "Timer started: $durationMinutes minutes")
    }

    fun stopTimer() {
        prefs.edit()
            .remove(KEY_END_TIME)
            .putBoolean(KEY_IS_ACTIVE, false)
            .apply()

        _isTimerActive.value = false
        _endTimeMillis.value = 0L

        Log.d(TAG, "Timer stopped")
    }

    fun getRemainingMillis(): Long {
        if (!_isTimerActive.value) return 0L
        val remaining = _endTimeMillis.value - System.currentTimeMillis()

        if (remaining <= 0) {
            Log.d(TAG, "Timer expired in getRemainingMillis()")
            stopTimer()
            return 0L
        }

        return remaining
    }

    fun isExpired(): Boolean {
        return getRemainingMillis() <= 0
    }

    companion object {
        private const val TAG = "CheckInTimerStateManager"
        private const val KEY_END_TIME = "timer_end_time"
        private const val KEY_IS_ACTIVE = "timer_is_active"
    }
}