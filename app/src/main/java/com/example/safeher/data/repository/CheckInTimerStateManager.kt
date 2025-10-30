package com.example.safeher.data.repository

import android.content.Context
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
        } else {
            _isTimerActive.value = false
            _endTimeMillis.value = 0L
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
    }

    fun stopTimer() {
        prefs.edit()
            .remove(KEY_END_TIME)
            .putBoolean(KEY_IS_ACTIVE, false)
            .apply()

        _isTimerActive.value = false
        _endTimeMillis.value = 0L
    }

    fun getRemainingMillis(): Long {
        if (!_isTimerActive.value) return 0L
        val remaining = _endTimeMillis.value - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    companion object {
        private const val KEY_END_TIME = "timer_end_time"
        private const val KEY_IS_ACTIVE = "timer_is_active"
    }
}