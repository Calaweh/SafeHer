package com.example.safeher.ui.alert

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.model.AlertHistory
import com.example.safeher.data.repository.AlertHistoryRepository
import com.example.safeher.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertHistoryViewModel @Inject constructor(
    private val alertHistoryRepository: AlertHistoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val TAG = "AlertHistoryViewModel"

    private val _allAlerts = MutableStateFlow<List<AlertHistory>>(emptyList())
    val allAlerts: StateFlow<List<AlertHistory>> = _allAlerts.asStateFlow()

    private val _sentAlerts = MutableStateFlow<List<AlertHistory>>(emptyList())
    val sentAlerts: StateFlow<List<AlertHistory>> = _sentAlerts.asStateFlow()

    private val _receivedAlerts = MutableStateFlow<List<AlertHistory>>(emptyList())
    val receivedAlerts: StateFlow<List<AlertHistory>> = _receivedAlerts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFilter = MutableStateFlow(AlertFilter.ALL)
    val selectedFilter: StateFlow<AlertFilter> = _selectedFilter.asStateFlow()

    init {
        Log.d(TAG, "✅ AlertHistoryViewModel initialized")
        loadAlertHistory()
    }

    private fun loadAlertHistory() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                Log.d(TAG, "📋 Loading alert history for user: $userId")

                alertHistoryRepository.getAlertHistory(userId).collect { alerts ->
                    Log.d(TAG, "📥 Received ${alerts.size} alerts from repository")

                    alerts.forEachIndexed { index, alert ->
                        Log.d(TAG, "Alert $index:")
                        Log.d(TAG, "  Type: ${alert.type}")
                        Log.d(TAG, "  Sender: ${alert.senderId} (${alert.senderName})")
                        Log.d(TAG, "  Receiver: ${alert.receiverId} (${alert.receiverName})")
                        Log.d(TAG, "  Should show? ${alert.senderId == userId || alert.receiverId == userId}")
                    }

                    _allAlerts.value = alerts
                    _isLoading.value = false
                    Log.d(TAG, "✅ Loading complete. isLoading = false")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading alert history", e)
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: AlertFilter) {
        Log.d(TAG, "🔍 Filter changed to: $filter")
        _selectedFilter.value = filter
    }
}

enum class AlertFilter {
    ALL,
    SENT,
    RECEIVED
}