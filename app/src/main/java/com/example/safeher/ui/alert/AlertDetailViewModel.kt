package com.example.safeher.ui.alert

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.model.User
import com.example.safeher.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "AlertDetailViewModel"

    private val _senderUser = MutableStateFlow<User?>(null)
    val senderUser: StateFlow<User?> = _senderUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadSenderUser(senderId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Loading sender user: $senderId")
                val user = userRepository.getUser(senderId)
                _senderUser.value = user
                Log.d(TAG, "Sender loaded: ${user?.displayName}, Phone: ${user?.contactNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sender user", e)
                _senderUser.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}