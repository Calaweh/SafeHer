package com.example.safeher.ui.splash

import androidx.lifecycle.ViewModel
import com.example.safeher.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import com.example.safeher.data.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val userState: StateFlow<User?> = userRepository.userState
}