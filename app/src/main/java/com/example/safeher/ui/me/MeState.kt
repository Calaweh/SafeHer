package com.example.safeher.ui.me

import com.example.safeher.data.model.User

data class MeUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class DeleteAccountState {
    object Idle : DeleteAccountState()
    object Loading : DeleteAccountState()
    object Success : DeleteAccountState()
    data class Error(val message: String) : DeleteAccountState()
}

sealed class UpdateProfileState {
    object Idle : UpdateProfileState()
    object Loading : UpdateProfileState()
    object Success : UpdateProfileState()
    data class Error(val message: String) : UpdateProfileState()
}