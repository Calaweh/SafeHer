package com.example.safeher.ui.navigation

import MainViewModel
import com.example.safeher.data.repository.AuthRepository
import com.example.safeher.data.service.ConfigurationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
//    private val configurationService: ConfigurationService,
    private val authRepository: AuthRepository
) : MainViewModel() {

    fun signOut() {
        authRepository.signOut()
    }
}