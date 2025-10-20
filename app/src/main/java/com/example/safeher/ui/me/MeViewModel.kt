package com.example.safeher.ui.me

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.data.repository.AuthRepository
import com.example.safeher.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject
constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeUiState())
    val uiState = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteState = _deleteState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateState = _updateState.asStateFlow()

    init {
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            authRepository.currentUserIdFlow.collectLatest { userId ->
                if (userId == null) {
                    _uiState.value = MeUiState(isLoading = false, error = "User not found.")
                } else {
                    userRepository.getUser(userId)?.let { user ->
                        _uiState.value = MeUiState(user = user, isLoading = false)
                    }
                }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteState.value = DeleteAccountState.Loading
            val userId = authRepository.currentUser?.uid

            if (userId == null) {
                _deleteState.value = DeleteAccountState.Error("Could not find user to delete.")
                return@launch
            }

            try {
                // IMPORTANT: Delete user data from Firestore FIRST.
                userRepository.delete(userId)

                // THEN, delete the authentication account.
                authRepository.deleteAccount()

                _deleteState.value = DeleteAccountState.Success

            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthRecentLoginRequiredException ->
                        "This is a sensitive action. Please sign out and sign back in before deleting your account."
                    else -> e.localizedMessage ?: "An error occurred during deletion."
                }
                _deleteState.value = DeleteAccountState.Error(errorMessage)
            }
        }
    }

    fun updateProfile(newName: String, newImageUrl: String, newImageUri: Uri?) {
        viewModelScope.launch {
            val user = _uiState.value.user
            if (user == null) {
                _updateState.value = UpdateProfileState.Error("User not found.")
                return@launch
            }
            if (newName.isBlank()) {
                _updateState.value = UpdateProfileState.Error("Display name cannot be empty.")
                return@launch
            }

            _updateState.value = UpdateProfileState.Loading

            try {
                userRepository.updateProfile(user.id, newName, newImageUrl, newImageUri)

                val updatedUser = userRepository.getUser(user.id)
                _uiState.value = _uiState.value.copy(user = updatedUser)

                _updateState.value = UpdateProfileState.Success
            } catch (e: Exception) {
                _updateState.value = UpdateProfileState.Error(e.localizedMessage ?: "Failed to update profile.")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateProfileState.Idle
    }
}